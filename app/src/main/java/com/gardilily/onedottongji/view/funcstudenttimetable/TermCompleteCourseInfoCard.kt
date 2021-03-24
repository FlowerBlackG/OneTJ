package com.gardilily.onedottongji.view.funcstudenttimetable

import android.content.Context
import android.view.LayoutInflater
import android.widget.RelativeLayout
import android.widget.TextView
import com.gardilily.onedottongji.R
import org.json.JSONObject

class TermCompleteCourseInfoCard(context: Context) : RelativeLayout(context)  {

    private val c = context
    init {
        LayoutInflater.from(context).inflate(R.layout.card_func_studenttimetable_termcomplete_courseinfo, this, true)
    }

    fun setInfo(courseObj: JSONObject) {
        fun getCourseIcon(): String {
            val arr = arrayOf("🍏", "🍎", "🍐", "🍊", "🍋", "🍉", "🍇", "🍓", "🫐", "🍈", "🍒", "🍑",
                    "🥭", "🍍", "🥝", "🍅", "🥑", "🥥", "🍌", "🍆", "🥕", "🌽", "🫑", "🥒", "🫒", "🧅")
            return arr[(arr.indices).random()]
        }

        findViewById<TextView>(R.id.card_func_studentTimeTable_termComplete_card_courseName).text = courseObj.getString("courseName")
        findViewById<TextView>(R.id.card_func_studentTimeTable_termComplete_card_courseCode).text = courseObj.getString("classCode")
        findViewById<TextView>(R.id.card_func_studentTimeTable_termComplete_card_courseCredits).text = "${courseObj.getInt("credits")}"
        findViewById<TextView>(R.id.card_func_studentTimeTable_termComplete_card_courseTeacher).text = courseObj.getString("teacherName")
        findViewById<TextView>(R.id.card_func_studentTimeTable_termComplete_card_courseClassTime).text = courseObj.getString("classTime")
        findViewById<TextView>(R.id.card_func_studentTimeTable_termComplete_card_courseClassRoom).text = fetchCourseRoom(courseObj)
        findViewById<TextView>(R.id.card_func_studentTimeTable_termComplete_card_icon).text = getCourseIcon()
    }

    private fun fetchCourseRoom(courseData: JSONObject): String {
        var res = courseData.getString("classRoomI18n")
        if (res.isEmpty()) {
            res = courseData.getString("roomLable")
        }
        return res
    }
}
