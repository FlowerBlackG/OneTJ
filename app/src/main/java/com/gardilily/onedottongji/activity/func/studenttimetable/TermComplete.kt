// SPDX-License-Identifier: MulanPSL-2.0
package com.gardilily.onedottongji.activity.func.studenttimetable

import android.app.Activity
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.gardilily.common.view.card.InfoCard
import com.gardilily.onedottongji.R
import com.gardilily.onedottongji.activity.OneTJActivityBase
import com.gardilily.onedottongji.tools.tongjiapi.TongjiApi
import org.json.JSONArray
import org.json.JSONObject
import kotlin.concurrent.thread
import kotlin.math.abs

class TermComplete : OneTJActivityBase(
    backOnTitleBar = true,
    hasTitleBar = true,
    withSpinning = true
) {

    private lateinit var layout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_func_studenttimetable_termcomplete)

        stageSpinningProgressBar(findViewById(R.id.func_studentTimeTable_termComplete_rootContainer))
        setSpinning(true)

        loadData()

        title = getString(R.string.term_curriculums)

        return


    }

    private lateinit var timetableJson: JSONArray

    private fun loadData() {
        thread {

            timetableJson = TongjiApi.instance.getOneTongjiStudentTimetable(this@TermComplete) ?: return@thread


            runOnUiThread {
                setSpinning(false)
                findViewById<TextView>(R.id.func_studentTimeTable_termComplete_termName).text =
                    intent.getStringExtra("TermName")

                layout = findViewById(R.id.func_studentTimeTable_termComplete_linearLayout)

                val dataObj = timetableJson

                showCourseCards(dataObj, layout)
            }
        }
    }

    private fun showCourseCards(dataArr: JSONArray, layout: LinearLayout) {
        fun getCourseIcon(): String {
            val arr = arrayOf(
                "green_apple",
                "red_apple",
                "pear",
                "tangerine",
                "lemon",
                "watermelon",
                "grapes",
                "strawberry",
                "blueberries",
                "melon",
                "cherries",
                "peach",
                "pineapple",
                "kiwi_fruit",
                "avocado",
                "coconut",
                "banana",
                "eggplant",
                "carrot",
                "bell_pepper",
                "olive",
                "onion"
            )
            return "fluentemoji/${arr[(arr.indices).random()]}_color.svg"
        }

        fun fetchCourseRoom(courseData: JSONObject): String {
            var res = courseData.getString("classRoomI18n")
            if (res.isEmpty()) {
                res = courseData.getString("roomLable")
            }
            return res
        }

        thread {
            val len = dataArr.length()
            for (i in 0 until len) {
                val it = dataArr.getJSONObject(i)
                Thread.sleep(56)
                runOnUiThread {
                    val card = InfoCard.Builder(this)
                        .setHasIcon(true)
                        .setIcon(getCourseIcon())
                        .setHasEndMark(false)
                        .setTitle(it.getString("courseName"))
                        .setSpMultiply(resources.displayMetrics.scaledDensity)
                        .setInnerMarginBetweenSp(12f)

                    val courseCredits = it.getDouble("credits")
                    var courseCreditText = ""
                    if (abs(courseCredits - (courseCredits.toInt())) < 1e-5) {
                        courseCreditText += courseCredits.toInt()
                    } else {
                        courseCreditText += courseCredits
                    }

                    card.addInfo(InfoCard.Info("课号", it.getString("classCode")))
                        .addInfo(InfoCard.Info("教师", it.getString("teacherName")))
                        .addInfo(InfoCard.Info("学分", courseCreditText))
                        .addInfo(InfoCard.Info("地点", fetchCourseRoom(it)))
                        .addInfo(InfoCard.Info("时间", it.getString("classTime")))

                    layout.addView(card.build())
                }
            }
        }
    }
}
