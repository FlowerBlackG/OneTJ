package com.gardilily.onedottongji.activity.func.autocourseelect

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.*
import com.gardilily.common.view.card.InfoCard
import com.gardilily.onedottongji.R
import com.gardilily.onedottongji.service.BackgroundAutoCourseElect
import com.gardilily.onedottongji.tools.Utils
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import kotlin.concurrent.thread

/**
 *
 * intent 传入数据：
 * + sessionid
 * + studentId 学号
 */
class AutoCourseElect : Activity() {

	companion object {
		const val INTENT_PARAM_SERVICE_CONTROL_ACTION = "__1"
		const val SERVICE_ACTION_NULL = -1
		const val SERVICE_ACTION_STOP_TASK = 1
		const val INTENT_PARAM_SERVICE_CONTROL_TASK_JSON = "__2"
	}

	private lateinit var linearLayout: LinearLayout

	private var roundId = ""
	private var calendarId = 0

	private lateinit var uniHttpClient: OkHttpClient

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_autocourseelect)
		linearLayout = findViewById(R.id.func_autoCourseElect_linearLayout)

		uniHttpClient = OkHttpClient()

		initRoundId()
		initSearchButton()

		/**
		 * 初始化后台进程。
		 */
		val backgroundInitIntent = Intent(this, BackgroundAutoCourseElect::class.java)
		backgroundInitIntent.putExtra(
			BackgroundAutoCourseElect.INTENT_PARAM_ACTION,
			BackgroundAutoCourseElect.ACTION_NULL
		)
		startService(backgroundInitIntent)
	}

	private fun initRoundId() {
		thread {
			val getRoundIdUrl = "https://1.tongji.edu.cn/api/electionservice/student/getRounds?projectId=1"
			val getRoundIdRequestFormBody = FormBody.Builder()
				.add("projectId", "1")
				.build()
			val request = Request.Builder()
				.url(getRoundIdUrl)
				.post(getRoundIdRequestFormBody)
				.addHeader("Cookie",
					"sessionid=${intent.getStringExtra("sessionid")}")
				.build()
			val response = Utils.safeNetworkRequest(request, uniHttpClient)
			if (response == null) {
				runOnUiThread {
					Toast.makeText(this, "网络异常", Toast.LENGTH_SHORT).show()
				}
				return@thread
			}

			val resStr = response.body!!.string()

			if (!Utils.isReqSessionAvailable(this, JSONObject(resStr)) { finish() }) {
				return@thread
			}

			val rounds = JSONObject(resStr).getJSONArray("data")
			if (rounds.length() > 1) {
				val items = Array(rounds.length()) { idx -> rounds.getJSONObject(idx).getString("name") }
				runOnUiThread {
					val alertBuilder = AlertDialog.Builder(this)
						.setTitle("选择轮次")
						.setItems(items) { dialog, idx ->
							roundId = rounds.getJSONObject(idx).getString("id")
							calendarId = rounds.getJSONObject(idx).getInt("calendarId")
							dialog.dismiss()
						}
						.setCancelable(false)
						.create()
						.show()
				}

			} else if (rounds.length() <= 0) {
				runOnUiThread {
					Toast.makeText(this, "当前没有选课轮次...", Toast.LENGTH_SHORT).show()
					finish()
				}
			} else {
				roundId = rounds.getJSONObject(0).getString("id")
				calendarId = rounds.getJSONObject(0).getInt("calendarId")
			}
		}
	}

	private fun initSearchButton() {
		findViewById<TextView>(R.id.func_autoCourseElect_courseCodeField_search).setOnClickListener {
			val courseCode =
				findViewById<EditText>(R.id.func_autoCourseElect_courseCodeField_input)
					.text
					.toString()

			if (courseCode.length != 6) {
				Toast.makeText(this, "课号长度应为6", Toast.LENGTH_SHORT).show()
				return@setOnClickListener
			}

			try {
				courseCode.toInt()
			} catch (e: Exception) {
				Toast.makeText(this, "课号应为6位整数", Toast.LENGTH_SHORT).show()
				return@setOnClickListener
			}

			linearLayout.removeAllViews()

			val getCourseInfoUrl =
				"https://1.tongji.edu.cn/api/electionservice/student/getTeachClass4Limit" +
						"?roundId=$roundId&courseCode=$courseCode" +
						"studentId=${intent.getStringExtra("studentId")!!}&calendarId=$calendarId"

			val getCourseInfoRequestFormBody = FormBody.Builder()
				.add("roundId", roundId)
				.add("courseCode", courseCode)
				.add("studentId", intent.getStringExtra("studentId")!!)
				.build()

			val courseInfoReq = Request.Builder()
				.url(getCourseInfoUrl)
				.post(getCourseInfoRequestFormBody)
				.addHeader("Cookie",
					"sessionid=${intent.getStringExtra("sessionid")}")
				.build()

			thread {

				val courseInfoResponse = Utils.safeNetworkRequest(courseInfoReq, uniHttpClient)
				if (courseInfoResponse == null) {
					runOnUiThread {
						Toast.makeText(this, "网络异常", Toast.LENGTH_SHORT).show()
					}
					return@thread
				}

				val resStr = courseInfoResponse.body!!.string()
				val resObj = JSONObject(resStr)
				if (!Utils.isReqSessionAvailable(this, resObj) { finish() }) {
					return@thread
				}

				val spMultiply = linearLayout.resources.displayMetrics.scaledDensity

				val data = resObj.getJSONArray("data")

				Log.d("Func.AutoCourseElect.List", data.toString())

				val len = data.length()

				fun buildDetailTextView(text: String, textSize: Float, lineIdx: Int): TextView {
					val tv = TextView(this)

					tv.setTextColor(Color.WHITE)
					tv.text = text
					tv.textSize = textSize

					val params = RelativeLayout.LayoutParams(
						RelativeLayout.LayoutParams.WRAP_CONTENT,
						RelativeLayout.LayoutParams.WRAP_CONTENT
					)
					params.marginStart = (8 * spMultiply).toInt()
					params.topMargin = ((8 + (lineIdx - 1) * 22) * spMultiply).toInt()

					tv.layoutParams = params

					return tv
				}

				fun getTimeTableListAndRoom(arr: JSONArray): String {
					var res = ""
					val len = arr.length()
					for (i in 0 until len) {
						if (res.isNotEmpty()) {
							res += '\n'
						}
						res += arr.getJSONObject(i).getString("timeAndRoom") + " " +
								arr.getJSONObject(i).getString("roomIdI18n")
					}
					return res
				}

				for (i in 0 until len) {
					val singleObj = data.getJSONObject(i)
/*
					val card = RelativeLayout(this)
					val cardLayoutParams = LinearLayout.LayoutParams(
						LinearLayout.LayoutParams.MATCH_PARENT,
						LinearLayout.LayoutParams.WRAP_CONTENT//(120 * spMultiply).toInt()
					)

					cardLayoutParams.bottomMargin = (12 * spMultiply).toInt()

					card.layoutParams = cardLayoutParams
					card.isClickable = true
					card.background = getDrawable(R.drawable.shape_login_page_box)

					card.addView(
						buildDetailTextView(singleObj.getString("courseName"), 16f, 1),
					)
					card.addView(
						buildDetailTextView(singleObj.getString("teacherName"), 16f, 2),
					)
					card.addView(
						buildDetailTextView(singleObj.getString("roomLable"), 16f, 3),
					)
					card.addView(
						buildDetailTextView(
							getTimeTableListAndRoom(singleObj.getJSONArray("timeTableList")),
							16f, 4),
					)*/

					val card = InfoCard.Builder(this)
						.setTitle(singleObj.getString("courseName"))
						.addInfo(InfoCard.Info("教师", singleObj.getString("teacherName")))
						.addInfo(InfoCard.Info("时间", getTimeTableListAndRoom(singleObj.getJSONArray("timeTableList"))))
						.setCardBackground(getDrawable(R.drawable.shape_login_page_box))
						.setIcon(listOf("🍓", "🍊", "🫐", "🍏", "🍍", "🥥", "🥝", "🍋", "🍒", "🍈", "🍎", "🍑", "🍉").random())
						.setOuterMarginBottomSp(18f)
						.build()

					card.setOnClickListener {
						/*****************
						 * info json 说明
						 * 嵌套：无
						 * 内容：
						 * roundId: String
						 * courseName: String
						 * courseCode: String
						 * teachClassId: Long
						 * teachClassCode: String
						 * sessionid: String
						 * studentId: String 学号
						 ****************/
						val infoJson = JSONObject()
						infoJson.put("roundId", roundId)
						infoJson.put("courseName", singleObj.getString("courseName"))
						infoJson.put("courseCode", singleObj.getString("courseCode"))
						infoJson.put("teachClassId", singleObj.getLong("teachClassId"))
						infoJson.put("teachClassCode", singleObj.getString("teachClassCode"))
						infoJson.put("studentId", intent.getStringExtra("studentId"))
						infoJson.put("sessionid", intent.getStringExtra("sessionid"))

						val intent = Intent(this, BackgroundAutoCourseElect::class.java)
						intent.putExtra(BackgroundAutoCourseElect.INTENT_PARAM_COURSE_INFO_JSON, infoJson.toString())
							.putExtra(BackgroundAutoCourseElect.INTENT_PARAM_ACTION, BackgroundAutoCourseElect.ACTION_START_TASK)

						startService(intent)
					}

					runOnUiThread {
						linearLayout.addView(card)
					}
				}
			}
		}

		// 全部停止
		findViewById<TextView>(R.id.func_autoCourseElect_funcBtnRow_stopAll).setOnClickListener {
			val intent = Intent(this, BackgroundAutoCourseElect::class.java)
			intent.putExtra(BackgroundAutoCourseElect.INTENT_PARAM_ACTION, BackgroundAutoCourseElect.ACTION_STOP_ALL)
			startService(intent)

			Toast.makeText(this, "正在停止选课...", Toast.LENGTH_SHORT).show()
		}
	}

}
