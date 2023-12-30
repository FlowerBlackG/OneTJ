// SPDX-License-Identifier: MulanPSL-2.0
package com.gardilily.onedottongji.activity.func

import android.app.Activity
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import com.gardilily.common.view.card.InfoCard
import com.gardilily.onedottongji.R
import com.gardilily.onedottongji.activity.OneTJActivityBase
import com.gardilily.onedottongji.tools.tongjiapi.TongjiApi
import org.json.JSONArray
import org.json.JSONObject
import kotlin.concurrent.thread

class StuExamEnquiries : OneTJActivityBase(
	hasTitleBar = true,
	backOnTitleBar = true,
	withSpinning = true
) {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_func_studenttimetable_termcomplete)

		stageSpinningProgressBar(findViewById(R.id.func_studentTimeTable_termComplete_rootContainer))
		setSpinning(true)

		title = "我的考试"

		loadData()
	}

	private lateinit var examData: JSONObject

	private fun loadData() {
		thread {

			// fetch data

			examData = TongjiApi.instance.getOneTongjiStudentExams(this@StuExamEnquiries) ?: return@thread

			// show data

			runOnUiThread {
				setSpinning(false)

				val dataList = examData.getJSONArray("list")

				findViewById<TextView>(R.id.func_studentTimeTable_termComplete_termName)
					.text = dataList.getJSONObject(0).getString("calendar")

				val len = dataList.length()

				for (i in 0 until len) {
					val it = dataList.getJSONObject(i)

					val card = InfoCard.Builder(this)
						.setHasIcon(true)
						.setHasEndMark(false)
						.setTitle(it.getString("courseName"))
						.setSpMultiply(resources.displayMetrics.scaledDensity)
						.setInnerMarginBetweenSp(12f)
						.addInfo(InfoCard.Info("课号", it.getString("courseCode")))

					val examSituation: Int? = try {
						it.getInt("examSituation")
					} catch (_: Exception) {
						null
					}

					val roomName = try {
						val value = it.getString("roomName")

						if (value == "null") {
							null
						} else {
							value
						}

					} catch (_: Exception) {
						null
					}

					if (examSituation == 1 || roomName != null) {
						card.addInfo(InfoCard.Info("地点", it.getString("roomName")))
							.addInfo(InfoCard.Info("时间", it.getString("examTime")))
							.addInfo(InfoCard.Info("备注", it.getString("remark")))
							.setIcon("fluentemoji/black_nib_color.svg")
					} else {
						card.addInfo(InfoCard.Info("备注", it.getString("remark")))
							.setIcon("fluentemoji/desktop_computer_color.svg")
					}

					findViewById<LinearLayout>(R.id.func_studentTimeTable_termComplete_linearLayout)
						.addView(card.build())
				}

			}
		}
	}
}
