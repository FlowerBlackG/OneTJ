package com.gardilily.onedottongji.activity.func

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.view.marginBottom
import com.gardilily.onedottongji.R
import com.gardilily.onedottongji.view.FuncMyGradesCourseCard
import org.json.JSONArray
import org.json.JSONObject
import kotlin.concurrent.thread

class MyGrades : Activity() {
    private lateinit var dataObj: JSONObject
    private lateinit var layout: LinearLayout
    private var spMultiply: Float = 2f
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_func_my_grades)

        spMultiply = resources.displayMetrics.scaledDensity

        dataObj = JSONObject(intent.getStringExtra("JsonObj")!!).getJSONObject("data")
        Log.d("func.MyGrades.jsonObjReceived", dataObj.toString())

        layout = findViewById(R.id.func_myGrades_linearLayout)

        showBasicGradeInfo()
        showAllTermGradeInfo(dataObj.getJSONArray("term"))
    }

    private fun showBasicGradeInfo() {
        val screenWidthPx = windowManager.defaultDisplay.width
        val targetCardWidthPx = ((screenWidthPx - (2f * 18f + 12f) * spMultiply) / 2f).toInt()

        fun createRowLayout(isFirstLine: Boolean = false): RelativeLayout {
            val mLayout = RelativeLayout(this)
            val params = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT)

            if (isFirstLine) {
                params.topMargin = (38f * spMultiply).toInt()
            }

            params.bottomMargin = (12f * spMultiply).toInt()
            mLayout.layoutParams = params
            return mLayout
        }

        var card_count = 0

        fun createAndShowInfoCard(title: String, content: String, tarRow: RelativeLayout) {
            val layout = RelativeLayout(this)
            val params = RelativeLayout.LayoutParams(targetCardWidthPx, (80f * spMultiply).toInt())

            if (card_count % 2 == 0) {
                params.addRule(RelativeLayout.ALIGN_PARENT_START)
            } else {
                params.addRule(RelativeLayout.ALIGN_PARENT_END)
            }

            layout.layoutParams = params

            layout.background = getDrawable(R.drawable.shape_login_page_box)
            layout.isClickable = true
            layout.gravity = Gravity.CENTER_VERTICAL

            val titleView = TextView(this)
            titleView.text = title
            titleView.textSize = 24f
         //   titleView.setTextColor(Color.parseColor("#000000"))
            val titleParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
            titleParams.leftMargin = (12f * spMultiply).toInt()
            titleView.layoutParams = titleParams
            layout.addView(titleView)

            val contentView = TextView(this)
            contentView.text = content
            contentView.textSize = 24f
          //  contentView.setTextColor(Color.parseColor("#000000"))
            val contentParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
            contentParams.rightMargin = (12f * spMultiply).toInt()
            contentParams.addRule(RelativeLayout.ALIGN_PARENT_END)
            contentView.layoutParams = contentParams
            layout.addView(contentView)

            tarRow.addView(layout)

            card_count++
        }

        val row1 = createRowLayout(true)
        val row2 = createRowLayout()
        layout.addView(row1)
        layout.addView(row2)

        createAndShowInfoCard("总均绩点", dataObj.getString("totalGradePoint"), row1)
        createAndShowInfoCard("总修学分", dataObj.getString("actualCredit"), row1)

        createAndShowInfoCard("挂科学分", dataObj.getString("failingCredits"), row2)
        createAndShowInfoCard("挂科门数", dataObj.getString("failingCourseCount"), row2)
    }

    private fun showAllTermGradeInfo(jsonArr: JSONArray) {
        val len = jsonArr.length()
        thread {
            for (i in (len - 1) downTo 0) {
                Thread.sleep(56)
                runOnUiThread {
                    val termName = TextView(this)
                    termName.text = jsonArr.getJSONObject(i).getString("termName")
                    termName.gravity = Gravity.CENTER
                    termName.background = getDrawable(R.drawable.shape_login_page_box)
                    termName.textSize = 24f
                    termName.isClickable = true
                    val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (80f * spMultiply).toInt())
                    params.bottomMargin = (12f * spMultiply).toInt()
                    termName.layoutParams = params
                    layout.addView(termName)

                    val termGrade = TextView(this)
                    termGrade.text = "平均绩点：${jsonArr.getJSONObject(i).getString("averagePoint")}"
                    termGrade.gravity = Gravity.CENTER
                    termGrade.background = getDrawable(R.drawable.shape_login_page_box)
                    termGrade.textSize = 24f
                    termGrade.isClickable = true
                    //val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (80f * spMultiply).toInt())
                    //params.bottomMargin = (12f * spMultiply).toInt()
                    termGrade.layoutParams = params
                    layout.addView(termGrade)
                    showSingleTermGradeInfo(jsonArr.getJSONObject(i).getJSONArray("creditInfo"))
                }
            }
        }
    }

    private fun showSingleTermGradeInfo(jsonArr: JSONArray) {
        val len = jsonArr.length()
        thread {
            for (i in 0 until len) {
                Thread.sleep(56)
                runOnUiThread {
                    val card = FuncMyGradesCourseCard(this)
                    card.setInfo(jsonArr.getJSONObject(i))
                    layout.addView(card)
                }
            }
        }
    }
}
