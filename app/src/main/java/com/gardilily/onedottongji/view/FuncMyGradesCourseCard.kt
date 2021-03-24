package com.gardilily.onedottongji.view

import android.content.Context
import android.view.LayoutInflater
import android.widget.RelativeLayout
import android.widget.TextView
import com.gardilily.onedottongji.R
import org.json.JSONObject

class FuncMyGradesCourseCard(context: Context) : RelativeLayout(context) {
    private val c = context
    init {
        LayoutInflater.from(context).inflate(R.layout.card_func_my_grades_course_card, this, true)
    }

    fun setInfo(creditObj: JSONObject) {
        fun gradePoint2gradeEngCh(point: Int): String {
            if (point == 5) {
                return "A"
            }
            if (point == 4) {
                return "B"
            }
            if (point == 3) {
                return "C"
            }
            if (point == 2) {
                return "D"
            }
            return "E"
        }
        fun gradePoint2gradeIcon(point: Int): String {
            if (point == 5) {
                return "🍓"
            }
            if (point == 4) {
                return "🍒"
            }
            if (point == 3) {
                return "🍊"
            }
            if (point == 2) {
                return "🍋"
            }
            return "🍇"
        }

        findViewById<TextView>(R.id.card_func_myGrades_courseName).text = creditObj.getString("courseName")
        findViewById<TextView>(R.id.card_func_myGrades_courseCode).text = "课号：" + creditObj.getString("courseCode")
        findViewById<TextView>(R.id.card_func_myGrades_courseCredits).text = "学分：" + "${creditObj.getInt("credit")}"
        findViewById<TextView>(R.id.card_func_myGrades_courseGrade).text = gradePoint2gradeEngCh(creditObj.getInt("gradePoint"))
        findViewById<TextView>(R.id.card_func_myGrades_gradeUpdateDate).text = "更新：" + creditObj.getString("updateTime").split(' ')[0]
        findViewById<TextView>(R.id.card_func_myGrades_icon).text = gradePoint2gradeIcon(creditObj.getInt("gradePoint"))
    }
}
