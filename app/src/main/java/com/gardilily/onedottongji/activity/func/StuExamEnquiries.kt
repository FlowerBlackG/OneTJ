// SPDX-License-Identifier: MulanPSL-2.0
package com.gardilily.onedottongji.activity.func

import android.app.Activity
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import com.gardilily.common.view.card.InfoCard
import com.gardilily.onedottongji.R
import org.json.JSONArray

class StuExamEnquiries : Activity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_func_studenttimetable_termcomplete)

		findViewById<TextView>(R.id.func_studentTimeTable_termComplete_termName)
			.text = intent.getStringExtra("calendarIdI18n")

		val dataList = JSONArray(intent.getStringExtra("dataList"))
		val len = dataList.length()

		for (i in 0 until len) {
			val it = dataList.getJSONObject(i)

			val card = InfoCard.Builder(this)
				.setHasIcon(true)
				.setHasEndMark(false)
				.setTitle(it.getString("courseName"))
				.setCardBackground(
					getDrawable(R.drawable.shape_login_page_box)
				)
				.setSpMultiply(resources.displayMetrics.scaledDensity)
				.setInnerMarginBetweenSp(12f)
				.addInfo(InfoCard.Info("课号", it.getString("teachingClassCode")))

			var examSituation: Int? = null
			try {
				examSituation = it.getInt("examSituation")
			} catch (e: Exception) {}

			if (examSituation == 1) {
				card.addInfo(InfoCard.Info("地点", it.getString("examSite")))
					.addInfo(InfoCard.Info("时间", it.getString("examTime")))
					.setIcon("🪑")
			} else {
				card.addInfo(InfoCard.Info("备注", it.getString("remark")))
					.setIcon("📎")
			}

			findViewById<LinearLayout>(R.id.func_studentTimeTable_termComplete_linearLayout)
				.addView(card.build())
		}
	}
}
