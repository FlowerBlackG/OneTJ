package com.gardilily.onedottongji.activity.func.studenttimetable

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.gardilily.onedottongji.R
import com.gardilily.onedottongji.view.funcstudenttimetable.SingleDayCourseInfoCard
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.thread

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

        val arrLen = fullClassDataObj.length()

        for (i in 0 until arrLen) {
            val timeTableList = fullClassDataObj.getJSONObject(i).getJSONArray("timeTableList")
            val len = timeTableList.length()
            for (j in 0 until len) {
                courseArray.add(Course(timeTableList.getJSONObject(j)))
            }
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
            if (pageDayOfWeek == 0 && pageWeek == 19) {
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
        findViewById<TextView>(R.id.func_studentTimeTable_singleDay_dayOfWeek).text = "星期${dayOfWeekCh[pageDayOfWeek]}"

        thread {
            courseArray.forEach {
                if (it.courseWeek.contains(pageWeek) && (it.courseDayOfWeek == pageDayOfWeek)) {
                    Thread.sleep(8)
                    runOnUiThread {
                        val card = SingleDayCourseInfoCard(this)
                        card.setInfo(it.dataObj)
                        cardContainer.addView(card)
                    }
                }
            }
        }
    }
}
