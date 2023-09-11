// SPDX-License-Identifier: MulanPSL-2.0
package com.gardilily.onedottongji.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Base64
import android.util.Log
import com.gardilily.onedottongji.R
import com.gardilily.onedottongji.tools.GarCloudApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.concurrent.thread

/**
 *
 * 每个任务进程由课程 teachClassId (Long) 判断
 */
class BackgroundAutoCourseElect : Service() {

	companion object {
		const val INTENT_PARAM_ACTION = "__1"
		const val ACTION_NULL = -1
		const val ACTION_START_TASK = 1
		const val ACTION_STOP_TASK = 2
		const val ACTION_STOP_ALL = 3

		/*****************
		 * info json 说明
		 * 嵌套：无
		 * 内容：
		 *
		 * roundId: String
		 * courseName: String
		 * courseCode: String
		 * teachClassId: Long
		 * teachClassCode: String
		 * sessionid: String
		 * studentId: String 学号
		 * calendarId: Int
		 ****************/
		const val INTENT_PARAM_COURSE_INFO_JSON = "__3"
	}

	/** 选课目标列表。 */
	private val taskList = ArrayList<Long>()

	/** 截止列表。 */
	private val toBeClosedList = ArrayList<Long>()

	/** 课号信息映射。 */
	private val courseMap = HashMap<Long, JSONObject>()

	/** 课号信息映射锁。 */
	private val courseMapLock = Mutex()

	/** 选课轮次。 */
	private var roundId: String = "0"

	private var sessionid: String = ""

	private var studentId: String = ""

	private var calendarId: Int = 0

	private class Defines {
		companion object {
			/**
			 * 关于"后台自动选课工作进程"通知相关的信息
			 */
			const val NOTI_CHANNEL_BACK_AUTOELECT_ID = "background course elect"
			const val NOTI_CHANNEL_BACK_AUTOELECT_NAME = "自动选课后台进程"
			const val NOTI_ID_BACK_AUTOELECT_SERVICE = 1

			/**
			 * 关于"后台自动选课结果"通知相关的信息
			 */
			const val NOTI_CHANNEL_AUTOELECT_RES_MSG_ID = "course elect res"
			const val NOTI_CHANNEL_AUTOELECT_RES_MSG_NAME = "自动选课结果"
		}
	}

	/**
	 * [keyMin, keyMax)
	 */
	private class KeyGenerator(keyMin: Int, keyMax: Int) {
		private val KEY_MAX = keyMax
		private val KEY_MIN = keyMin
		private val keyAvailable = BooleanArray(KEY_MAX - KEY_MIN) { true }

		fun generate(): Int {
			for (i in 0..(KEY_MAX - KEY_MIN)) {
				if (keyAvailable[i]) {
					keyAvailable[i] = false
					return i + KEY_MIN
				}
			}
			return -1
		}

		fun recycle(key: Int) {
			if (key - KEY_MIN < KEY_MAX && key >= KEY_MIN) {
				keyAvailable[key] = true
			}
		}

		fun clear() {
			for (i in 0..(KEY_MAX - KEY_MIN)) {
				keyAvailable[i] = true
			}
		}
	}

	private val keyGen = KeyGenerator(2048, 16384)

	//private lateinit var client: OkHttpClient
	private lateinit var notiManager: NotificationManager
	private lateinit var workerNotiChannel: NotificationChannel
	private lateinit var resMsgNotiChannel: NotificationChannel

	private lateinit var electThreadNotiBuilder: Notification.Builder

	override fun onCreate() {
		super.onCreate()

		initNotificationService()

		electThreadNotiBuilder = Notification
			.Builder(this, Defines.NOTI_CHANNEL_BACK_AUTOELECT_ID)
			.setSmallIcon(R.drawable.logo)
			.setContentTitle("自动抢课后台进程")
			.setContentText("就绪")
			.setOnlyAlertOnce(true)

		startForeground(Defines.NOTI_ID_BACK_AUTOELECT_SERVICE, electThreadNotiBuilder.build())
	}

	/**
	 * 初始化通知服务。
	 */
	private fun initNotificationService() {
		notiManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

		workerNotiChannel = NotificationChannel(
			Defines.NOTI_CHANNEL_BACK_AUTOELECT_ID,
			Defines.NOTI_CHANNEL_BACK_AUTOELECT_NAME,
			NotificationManager.IMPORTANCE_DEFAULT)

		notiManager.createNotificationChannel(workerNotiChannel)

		resMsgNotiChannel = NotificationChannel(
			Defines.NOTI_CHANNEL_AUTOELECT_RES_MSG_ID,
			Defines.NOTI_CHANNEL_AUTOELECT_RES_MSG_NAME,
			NotificationManager.IMPORTANCE_HIGH)
		resMsgNotiChannel.enableLights(true)

		notiManager.createNotificationChannel(resMsgNotiChannel)
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = runBlocking {

		if (intent == null) {
			Log.e("[auto course elect (background)]", "onStartCommand received null intent.")
			return@runBlocking START_STICKY
		}

		fun safeIntentGetStringExtra(key: String): String {
			val solidIntent = intent!!
			val res = solidIntent.getStringExtra(key)
			return res ?: ""
		}

		val action = intent!!.getIntExtra(INTENT_PARAM_ACTION, ACTION_NULL)

		val infoJson =
			if (action == ACTION_NULL || action == ACTION_STOP_ALL)
				JSONObject()
			else
				JSONObject(safeIntentGetStringExtra(INTENT_PARAM_COURSE_INFO_JSON))

		when (action) {
			ACTION_START_TASK -> {

				// 设置选课轮次
				roundId = infoJson.getString("roundId")

				sessionid = infoJson.getString("sessionid")

				studentId = infoJson.getString("studentId")

				calendarId = infoJson.getInt("calendarId")

				// 将选课目标加入映射表。
				courseMapLock.withLock {
					courseMap[infoJson.getLong("teachClassId")] = infoJson
				}

				fireMsgNotiWithAutoKey("${infoJson.getString("courseName")}", "已加入队列")

				electLoopEntry()
			}

			ACTION_STOP_TASK -> {
				if (taskList.contains(infoJson.getLong("teachClassId"))) {

					toBeClosedList.add(infoJson.getLong("teachClassId"))

					taskList.remove(infoJson.getLong("teachClassId"))
				}
			}

			ACTION_STOP_ALL -> {
				courseMapLock.withLock {
					courseMap.clear()
				}
				electLoopRunning = false
			}

			else -> {}
		}

		return@runBlocking START_STICKY
	}

	private var electLoopRunning = false


	private fun JSONObject.toCourseElectEncrypted(secret: GarCloudApi.Companion.CourseElectSecret): JSONObject {
		val res = JSONObject()

		val firstCourse = this.getJSONArray("elecClassList").getJSONObject(0)

		val elements = arrayOf(
			studentId, // a 学号
			firstCourse.getString("courseCode"), // o 课号
			firstCourse.getString("teachClassId"), // n 第一门的 teach class id
			calendarId.toString(), // c 学期 id
			System.currentTimeMillis().toString(), // u 时间戳
			this.toString(), // s "{"roundId": xxx, elecClassList: [], withdraw...}"
		)

		/**
		 * 连接
		 * 例如，将 ["a", "b", "c"] 连接成 "a+b+c"
		 */
		fun Array<String>.toConnected(connector: String): String {
			val builder = StringBuilder()
			this.forEachIndexed { index, s ->
				if (index > 0) {
					builder.append(connector)
				}

				builder.append(s)
			}

			return builder.toString()
		}

		val plusConnectedStr = elements.toConnected("+")
		val andConnectedStr = elements.toConnected("&")

		val checkCodeBytes = MessageDigest.getInstance("MD5").digest(plusConnectedStr.toByteArray())
		val checkCodeBuilder = StringBuilder()

		checkCodeBytes.forEach { byte: Byte ->

			val iByte = (byte.toUInt() % 0xFFu).toInt()
			if (iByte < 0x10) {
				checkCodeBuilder.append('0')
			}

			checkCodeBuilder.append(Integer.toHexString(iByte))
		}

		val checkCode = checkCodeBuilder.toString()

		val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
		val secretKeySpec = SecretKeySpec(secret.key.toByteArray(), "AES")
		val ivSpec = IvParameterSpec(secret.iv.toByteArray())
		cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivSpec)
		val cipherBytes = cipher.doFinal(URLEncoder.encode(andConnectedStr, "UTF-8").toByteArray())

		val cipherB64 = Base64.encodeToString(cipherBytes, Base64.NO_WRAP)
		val cipherUrlEncoded = URLEncoder.encode(cipherB64, "UTF-8")

		res.put("checkCode", checkCode)
		res.put("ciphertext", cipherUrlEncoded)
		return res
	}

	/**
	 * 选课循环。需要在外部异步调用。内部阻塞式执行。
	 */
	private fun electLoop() = runBlocking {

		// fetch secret
		val secret = GarCloudApi.getCourseElectSecret()

		if (secret == null) {
			val notiBuilder = electThreadNotiBuilder
				.setContentTitle("失败")
				.setContentText("无法获取选课密钥")

			notiManager.notify(Defines.NOTI_ID_BACK_AUTOELECT_SERVICE, notiBuilder.build())

			return@runBlocking
		}

		var count = 0
		val electUrl = "https://1.tongji.edu.cn/api/electionservice/student/elect"
		val electResUrl = "https://1.tongji.edu.cn/api/electionservice/student/$roundId/electRes"
		val client = OkHttpClient()

		while (electLoopRunning) {

			// 登记所有要尝试的选课
			val electCourseArray = JSONArray()
			val electCourseKotlinArray = ArrayList<JSONObject>()
			var electCourseArrayString = ""
			courseMapLock.withLock {
				courseMap.forEach { (courseId, courseInfo) ->
					val electCourseObj = JSONObject()
					electCourseObj.put("teachClassId", courseId)
						.put("teachClassCode", courseInfo.getString("teachClassCode"))
						.put("courseCode", courseInfo.getString("courseCode"))
						.put("courseName", courseInfo.getString("courseName"))
						.put("teacherName", courseInfo.getString("teacherName"))
					electCourseKotlinArray.add(electCourseObj)
					electCourseArrayString += courseInfo.getString("courseName") + " "
				}

				electCourseKotlinArray.sortByDescending { it.getLong("teachClassId") }

				electCourseKotlinArray.forEach { electCourseArray.put(it) }
			}

			if (electCourseArray.length() == 0) {
				break
			}

			count++ // 计数

			val mediaTypeJSON = "application/json; charset=utf-8".toMediaType()


			val rawReqJson = JSONObject()
			rawReqJson.put("roundId", roundId.toInt())
				.put("elecClassList", electCourseArray)
				.put("withdrawClassList", JSONArray())

			val reqJson = rawReqJson.toCourseElectEncrypted(secret)
			val reqBody = reqJson.toString().toRequestBody(mediaTypeJSON)

			val request = Request.Builder()
				.url(electUrl)
				.addHeader("Cookie", "sessionid=$sessionid")
				.post(reqBody)
				.build()

			val resRequest = Request.Builder()
				.url(electResUrl)
				.addHeader("Cookie", "sessionid=$sessionid")
				.post(FormBody.Builder().build())
				.build()

			Log.d("抢课请求数据", rawReqJson.toString())
			Log.d("抢课请求数据（加密）", reqJson.toString())

			val resJson = try {
				client.newCall(request).execute()
				Thread.sleep(1000)
				JSONObject(client.newCall(resRequest).execute().body!!.string())
			} catch (_: Exception) {
				// 解析异常
				continue
			}

			Log.d("Func.AutoCourseElectCard.resJson", resJson.toString())

			val textTryCount = "第${count}次尝试"
			var outputStr = ""
			val data = resJson.getJSONObject("data")
			val dataJsonArray = data.getJSONArray("successCourses")
			if (dataJsonArray.isNull(0)) {
				outputStr += "失败：" + data.getJSONObject("failedReasons").toString()
			} else {
				outputStr += "成功。"

				for (i in 0 until dataJsonArray.length()) {
					val teachClassId = dataJsonArray.getLong(i)

					try { // 加上 try catch，防止与其他抢课软件冲突。
						outputStr += courseMap[teachClassId]!!.getString("courseName") + " "
					} catch (_: Exception) {
						continue
					}

					fireMsgNotiWithAutoKey("成功", courseMap[teachClassId]!!.getString("courseName"))

					courseMapLock.withLock {
						courseMap.remove(teachClassId)
					}
				}
			}

			val notiBuilder = electThreadNotiBuilder
				.setContentTitle("进行中...")
				.setContentText(textTryCount)
				//.setContentIntent(pendingIntent)
				.setStyle(
					Notification.BigTextStyle()
						.bigText("$textTryCount\n$outputStr\n尝试列表：$electCourseArrayString")
				)

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
				notiBuilder.setFlag(Notification.FLAG_ONGOING_EVENT, true)
			}

			notiManager.notify(Defines.NOTI_ID_BACK_AUTOELECT_SERVICE, notiBuilder.build())
			Thread.sleep(1000)
		}

		stopForeground(STOP_FOREGROUND_REMOVE)
		electLoopRunning = false

	}

	private fun electLoopEntry() {
		if (electLoopRunning) {
			return
		} else {
			electLoopRunning = true
		}

		val notiBuilder = electThreadNotiBuilder
			.setContentTitle("自动抢课")
			.setContentText("就绪")

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			notiBuilder.setFlag(Notification.FLAG_ONGOING_EVENT, true)
		}


		notiManager.notify(Defines.NOTI_ID_BACK_AUTOELECT_SERVICE, notiBuilder.build())

		thread {
			electLoop()
		}
	}

	override fun onBind(intent: Intent?): IBinder? {
		return null
	}

	override fun onDestroy() = runBlocking {
		super.onDestroy()
		courseMapLock.withLock {
			courseMap.clear()
		}
		electLoopRunning = false
	}

	private fun fireMsgNotiWithAutoKey(title: String, msg: String) {
		val noti = Notification.Builder(this@BackgroundAutoCourseElect,
			Defines.NOTI_CHANNEL_AUTOELECT_RES_MSG_ID)
			.setContentTitle(title)
			.setContentText(msg)
			.setTicker(msg)
			.setSmallIcon(R.drawable.logo)
			.setAutoCancel(true)
			.build()
		notiManager.notify(keyGen.generate(), noti)
	}
}
