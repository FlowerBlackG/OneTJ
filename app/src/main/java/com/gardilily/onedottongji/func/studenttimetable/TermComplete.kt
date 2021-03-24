package com.gardilily.onedottongji.activity.func.studenttimetable

import android.app.Activity
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import com.gardilily.onedottongji.R
import com.gardilily.onedottongji.view.funcstudenttimetable.TermCompleteCourseInfoCard
import org.json.JSONArray
import org.json.JSONObject
import kotlin.concurrent.thread

class TermComplete : Activity() {

    private lateinit var layout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_func_studenttimetable_termcomplete)

        findViewById<TextView>(R.id.func_studentTimeTable_termComplete_termName).text =
                intent.getStringExtra("TermName")

        layout = findViewById(R.id.func_studentTimeTable_termComplete_linearLayout)

        val dataObj = JSONArray(intent.getStringExtra("JsonDataObj")!!)

        val courseCount = dataObj.length()

        thread {
            for (i in 0 until courseCount) {
                Thread.sleep(56)
                runOnUiThread {
                    val card = TermCompleteCourseInfoCard(this)
                    card.setInfo(dataObj.getJSONObject(i))
                    layout.addView(card)
                }
            }
        }


    }
}