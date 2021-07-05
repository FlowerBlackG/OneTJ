package com.gardilily.onedottongji.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.gardilily.onedottongji.R
import com.gardilily.onedottongji.activity.Login
import com.gardilily.onedottongji.activity.func.autocourseelect.AutoCourseElect
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
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
		 *
		 ****************/
		const val INTENT_PARAM_COURSE_INFO_JSON = "__3"
	}

	private val taskList = ArrayList<Long>()
	private val toBeClosedList = ArrayList<Long>()

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

		//client = OkHttpClient()
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

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		fun safeIntentGetStringExtra(key: String): String {
			val solidIntent = intent!!
			val res = solidIntent.getStringExtra(key)
			return res ?: ""
		}

		val action = intent!!.getIntExtra(INTENT_PARAM_ACTION, ACTION_NULL)

		val infoJson =
			if (action == ACTION_NULL)
				JSONObject()
			else
				JSONObject(safeIntentGetStringExtra(INTENT_PARAM_COURSE_INFO_JSON))

		when (action) {
			ACTION_START_TASK -> {
				if (taskList.isNotEmpty()) {
					fireMsgNotiWithAutoKey("已在抢课", "暂时只能单线选课..._(:τ」∠)_")

				} else {
					taskList.add(infoJson.getLong("teachClassId"))
					electLoop(infoJson)
				}
				/*
				if (!taskList.contains(infoJson.getLong("teachClassId"))) {


				} else {
					fireMsgNotiWithAutoKey("已在抢课", "努力抢课ing...")
				}*/
			}
			ACTION_STOP_TASK -> {
				if (taskList.contains(infoJson.getLong("teachClassId"))) {

					toBeClosedList.add(infoJson.getLong("teachClassId"))

					taskList.remove(infoJson.getLong("teachClassId"))
				}
			}
		}

		return START_STICKY
	}

	private fun electLoop(infoJson: JSONObject) {
		val noti = electThreadNotiBuilder
			.setContentTitle(infoJson.getString("courseName") + " 点击以停止")
			.setContentText("就绪")
			//.setContentIntent(pendingIntent)
			.build()

		notiManager.notify(Defines.NOTI_ID_BACK_AUTOELECT_SERVICE, noti)

		thread {
			/*
			val intent = Intent(this, AutoCourseElect::class.java)
			intent.putExtra(AutoCourseElect.INTENT_PARAM_SERVICE_CONTROL_ACTION,
					AutoCourseElect.SERVICE_ACTION_STOP_TASK
				)
				.putExtra(AutoCourseElect.INTENT_PARAM_SERVICE_CONTROL_TASK_JSON,
					infoJson.toString()
				)
				.putExtra("sessionid", infoJson.getString("sessionid"))
				.putExtra("studentId", infoJson.getString("studentId"))

			val pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)
*/

			var count = 0
			val electUrl = "https://1.tongji.edu.cn/api/electionservice/student/elect"
			val electResUrl =
				"https://1.tongji.edu.cn/api/electionservice/student/" +
						infoJson.getString("roundId") +
						"/electRes"
			val client = OkHttpClient()
			while (!toBeClosedList.contains(infoJson.getLong("teachClassId"))) {
				count++
				val elecClassJsonObj = JSONObject()
				elecClassJsonObj.put("teachClassId", infoJson.getLong("teachClassId"))
					.put("teachClassCode", infoJson.getString("teachClassCode"))
					.put("courseCode", infoJson.getString("courseCode"))

				val mediaTypeJSON = "application/json; charset=utf-8".toMediaType()
				val reqJson = JSONObject()
				reqJson.put("elecClassList", JSONArray().put(elecClassJsonObj))
					.put("roundId", infoJson.getString("roundId").toInt())
					.put("withdrawClassList", JSONArray())
				val reqBody = reqJson.toString().toRequestBody(mediaTypeJSON)

				val request = Request.Builder()
					.url(electUrl)
					.addHeader("Cookie", "sessionid=${infoJson.getString("sessionid")}")
					.post(reqBody)
					.build()

				val resRequest = Request.Builder()
					.url(electResUrl)
					.addHeader("Cookie", "sessionid=${infoJson.getString("sessionid")}")
					.post(FormBody.Builder().build())
					.build()

				client.newCall(request).execute()
				Thread.sleep(1000)
				val resJson = JSONObject(client.newCall(resRequest).execute().body!!.string())

				Log.d("Func.AutoCourseElectCard.resJson", resJson.toString())

				val textTryCount = "第${count}次尝试"
				var outputStr = ""
				val data = resJson.getJSONObject("data")
				if (data.getJSONArray("successCourses").isNull(0)) {
					outputStr += "失败：" + data.getJSONObject("failedReasons").toString()
				} else {
					outputStr += "成功。"
					toBeClosedList.add(infoJson.getLong("teachClassId"))
					taskList.remove(infoJson.getLong("teachClassId"))
				}

				val noti = electThreadNotiBuilder
					.setContentTitle(infoJson.getString("courseName"))
					.setContentText(textTryCount)
					//.setContentIntent(pendingIntent)
					.setStyle(Notification.BigTextStyle()
						.bigText(textTryCount + '\n' + outputStr))
					.build()

				notiManager.notify(Defines.NOTI_ID_BACK_AUTOELECT_SERVICE, noti)
				Thread.sleep(1000)
			}

			stopForeground(STOP_FOREGROUND_REMOVE)
		}
	}

	override fun onBind(intent: Intent?): IBinder? {
		return null
	}

	override fun onDestroy() {
		super.onDestroy()
		taskList.forEach {
			toBeClosedList.add(it)
		}
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
