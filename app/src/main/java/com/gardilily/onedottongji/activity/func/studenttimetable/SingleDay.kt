package com.gardilily.onedottongji.activity.func.studenttimetable

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.gardilily.common.view.card.InfoCard
import com.gardilily.onedottongji.R
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class SingleDay : Activity() {

    private lateinit var cardContainer: LinearLayout

    private var termWeek = 1
    private var termDayOfWeek = 1

    private var pageWeek = 1
    private var pageDayOfWeek = 1

    private val dayOfWeekCh = arrayOf("日", "一", "二", "三", "四", "五", "六")

    private val courseArray = ArrayList<Course>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_func_studenttimetable_singleday)

        initCourseArray()
        initWeekInfo()

        cardContainer = findViewById(R.id.func_studentTimeTable_singleDay_linearLayout)

        initDirButtons()

        refreshPage()
    }

    private fun initCourseArray() {
        val fullClassDataObj = JSONArray(intent.getStringExtra("JsonDataObj")!!)

        Log.d("OneFun:SingleDay:fullClassData", fullClassDataObj.toString())

        val arrLen = fullClassDataObj.length()

        for (i in 0 until arrLen) {

            Log.d("OneFun:SingleDay:Init:CP1",
                fullClassDataObj.getJSONObject(i).toString())

            try {
                val timeTableList = fullClassDataObj.getJSONObject(i).getJSONArray("timeTableList")
                val len = timeTableList.length()
                for (j in 0 until len) {
                    courseArray.add(Course(timeTableList.getJSONObject(j)))
                }
            } catch (e: Exception) { }
        }

        courseArray.sortBy { it.timeBeg }
    }

    private class Course(courseObj: JSONObject) {
        val dataObj: JSONObject = courseObj

        var timeBeg = courseObj.getInt("timeStart")
        var courseDayOfWeek = courseObj.getInt("dayOfWeek") % 7
        var courseWeek = ArrayList<Int>()

        init {
            val weekList = courseObj.getJSONArray("weeks")
            val len = weekList.length()
            for (i in 0 until len) {
                courseWeek.add(weekList.getInt(i))
            }
        }
    }

    private fun initDirButtons() {
        findViewById<TextView>(R.id.func_studentTimeTable_singleDay_dir_prevDay).setOnClickListener {
            if (pageDayOfWeek == 1 && pageWeek == 1) {
                Toast.makeText(this, "不能再往前啦", Toast.LENGTH_SHORT).show()
            } else {
                if (pageDayOfWeek == 1) {
                    pageWeek--
                }
                pageDayOfWeek += 6
                pageDayOfWeek %= 7
                refreshPage()
            }
        }

        findViewById<TextView>(R.id.func_studentTimeTable_singleDay_dir_today).setOnClickListener {
            pageWeek = termWeek
            pageDayOfWeek = termDayOfWeek
            refreshPage()
        }

        findViewById<TextView>(R.id.func_studentTimeTable_singleDay_dir_nextDay).setOnClickListener {
            if (pageDayOfWeek == 0 && pageWeek == 21) {
                Toast.makeText(this, "不能再往后啦", Toast.LENGTH_SHORT).show()
            } else {
                if (pageDayOfWeek == 0) {
                    pageWeek++
                }
                pageDayOfWeek++
                pageDayOfWeek %= 7
                refreshPage()
            }
        }
    }

    private fun zellerDayOfTheWeek(Y: Int, M: Int, D: Int): Int{
        var y = Y
        var m = M
        var d = D

        if (m == 1){
            m = 13
            y--
        }
        else if (m == 2){
            m = 14
            y--
        }

        val c = y / 100
        y %= 100

        var dayOfWk = y + y / 4 + c / 4 - 2 * c + (26 * (m + 1)) / 10 + d - 1

        dayOfWk %= 7
        dayOfWk += 7
        dayOfWk %= 7

        return dayOfWk
    }

    private fun initWeekInfo() {
        termWeek = intent.getIntExtra("TermWeek", 1)
        pageWeek = termWeek

        val format_year = SimpleDateFormat("yyyy")
        val format_month = SimpleDateFormat("MM")
        val format_day = SimpleDateFormat("dd")
        val date = Date(System.currentTimeMillis())

        val y = format_year.format(date).toInt()
        val m = format_month.format(date).toInt()
        val d = format_day.format(date).toInt()

        termDayOfWeek = zellerDayOfTheWeek(y, m, d)
        pageDayOfWeek = termDayOfWeek
    }

    private fun refreshPage() {
        cardContainer.removeAllViews()

        findViewById<TextView>(R.id.func_studentTimeTable_singleDay_weekNum).text = "第${pageWeek}周"
        findViewById<TextView>(R.id.func_studentTimeTable_singleDay_dayOfWeek).text =
            "星期${dayOfWeekCh[pageDayOfWeek]}"

        courseArray.forEach {
            val data = it.dataObj
            if (it.courseWeek.contains(pageWeek) && (it.courseDayOfWeek == pageDayOfWeek)) {
                val card = InfoCard.Builder(this)
                    .setSpMultiply(resources.displayMetrics.scaledDensity)
                    .setCardBackground(this.getDrawable(R.drawable.shape_login_page_box))
                    .setHasEndMark(true)
                    .setHasIcon(true)
                    .setIcon(getCourseIcon(data.getInt("timeStart")))
                    .setEndMark(
                        "${data.getString("timeStart")}-${data.getString("timeEnd")}"
                    )
                    .setIconTextSizeSp(48f)
                    .setEndMarkTextSizeSp(42f)
                    .setTitle(data.getString("courseName"))
                    .setTitleTextSizeSp(24f)
                    .setInfoTextSizeSp(16f)
                    .setLayoutHeightSp(132f)
                    .addInfo(InfoCard.Info(
                        "课号", data.getString("classCode")
                    ))
                    .addInfo(InfoCard.Info(
                        "教师", data.getString("teacherName")
                    ))
                    .addInfo(InfoCard.Info(
                        "地点", fetchCourseRoom(data)
                    ))

                cardContainer.addView(card.build())
                //cardContainer.addView(cardL)
            }
        }
    }

    private fun getCourseIcon(timeBegin: Int): String {
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

    private fun fetchCourseRoom(courseData: JSONObject): String {
        var res = courseData.getString("roomIdI18n")
        if (res.isEmpty()) {
            res = courseData.getString("roomLable")
        }
        return res
    }
}
