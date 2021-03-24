package com.gardilily.onedottongji.view.funcstudenttimetable

import android.content.Context
import android.view.LayoutInflater
import android.widget.RelativeLayout
import android.widget.TextView
import com.gardilily.onedottongji.R
import org.json.JSONObject

class SingleDayCourseInfoCard(context: Context) : RelativeLayout(context)  {

    private val c = context
    init {
        LayoutInflater.from(context).inflate(R.layout.card_func_studenttimetable_singleday_courseinfo, this, true)
    }

    fun setInfo(timeTableObj: JSONObject) {
        fun getCourseIcon(timeBegin: Int): String {
            return if (timeBegin <= 2) {
                "🍞"
            } else if (timeBegin <= 4) {
                "🍛"
            } else if (timeBegin <= 6) {
                "🍹"
            } else if (timeBegin <= 9) {
                "🍔"
            } else {
                "🥮"
            }
        }

        findViewById<TextView>(R.id.card_func_studentTimeTable_singleDay_card_courseName).text = timeTableObj.getString("courseName")
        findViewById<TextView>(R.id.card_func_studentTimeTable_singleDay_card_courseCode).text = timeTableObj.getString("classCode")
        findViewById<TextView>(R.id.card_func_studentTimeTable_singleDay_card_courseTeacher).text = timeTableObj.getString("teacherName")
        findViewById<TextView>(R.id.card_func_studentTimeTable_singleDay_card_scheduleTime).text =
                "${timeTableObj.getString("timeStart")}-${timeTableObj.getString("timeEnd")}"
        findViewById<TextView>(R.id.card_func_studentTimeTable_singleDay_card_courseClassRoom).text = fetchCourseRoom(timeTableObj)
        findViewById<TextView>(R.id.card_func_studentTimeTable_singleDay_card_icon).text = getCourseIcon(timeTableObj.getInt("timeStart"))
    }

    private fun fetchCourseRoom(courseData: JSONObject): String {
        var res = courseData.getString("roomIdI18n")
        if (res.isEmpty()) {
            res = courseData.getString("roomLable")
        }
        return res
    }
}
