package com.gardilily.onedottongji.activity.func

import android.app.Activity
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import com.gardilily.common.view.card.CardShelf
import com.gardilily.common.view.card.InfoCard
import com.gardilily.onedottongji.R
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs

class MyGrades : Activity() {
    private lateinit var dataObj: JSONObject
    //private lateinit var layout: LinearLayout

    private lateinit var generalInfoContainer: LinearLayout
    private lateinit var gradeInfoContainer: LinearLayout

    private var spMultiply: Float = 2f
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_func_my_grades)

        spMultiply = resources.displayMetrics.scaledDensity

        dataObj = JSONObject(intent.getStringExtra("JsonObj")!!).getJSONObject("data")
        Log.d("func.MyGrades.jsonObjReceived", dataObj.toString())

        //layout = findViewById(R.id.func_myGrades_linearLayout)
        generalInfoContainer = findViewById(R.id.func_myGrades_generalInfoContainer)
        gradeInfoContainer = findViewById(R.id.func_myGrades_gradeInfoContainer)

        showBasicGradeInfo()
        showAllTermGradeInfo(dataObj.getJSONArray("term"))
    }

    private fun showBasicGradeInfo() {
        val screenWidthPx = windowManager.defaultDisplay.width
        val targetCardWidthPx = ((screenWidthPx - (2f * 18f + 12f) * spMultiply) / 2f).toInt()

        fun createRowLayout(isFirstLine: Boolean = false): LinearLayout {
            val mLayout = LinearLayout(this)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            if (isFirstLine) {
                params.topMargin = (38f * spMultiply).toInt()
            }

            params.bottomMargin = (12f * spMultiply).toInt()
            mLayout.layoutParams = params
            return mLayout
        }

        var cardCount = 0

 /*       fun createAndShowInfoCard(title: String, content: String, tarRow: RelativeLayout) {
            val layout = RelativeLayout(this)
            val params = RelativeLayout.LayoutParams(targetCardWidthPx, (80f * spMultiply).toInt())

            if (cardCount % 2 == 0) {
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

            cardCount++
        }
*/

        fun createAndShowInfoCard(title: String, content: String, tarRow: LinearLayout) {
            val layout = RelativeLayout(this)
            //val params = LinearLayout.LayoutParams(0, (80f * spMultiply).toInt())
            val params = LinearLayout.LayoutParams(0, (90f * spMultiply).toInt())

            params.weight = 1f

            if (cardCount % 2 == 0) {
                params.marginEnd = (6f * spMultiply).toInt()
            } else {
                params.marginStart = (6f * spMultiply).toInt()
            }

            layout.layoutParams = params

            layout.background = getDrawable(R.drawable.shape_login_page_box)
            layout.isClickable = true
            //layout.gravity = Gravity.CENTER

            val titleView = TextView(this)
            titleView.text = title
            titleView.textSize = 24f
            //   titleView.setTextColor(Color.parseColor("#000000"))
            val titleParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT
            )
            titleParams.bottomMargin = (12f * spMultiply).toInt()
            titleParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
            titleParams.addRule(RelativeLayout.CENTER_HORIZONTAL)

            titleView.layoutParams = titleParams
            layout.addView(titleView)

            val contentView = TextView(this)
            contentView.text = content
            contentView.textSize = 24f
            //  contentView.setTextColor(Color.parseColor("#000000"))
            val contentParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT
            )
            contentParams.topMargin = (12f * spMultiply).toInt()
            //contentParams.addRule(RelativeLayout.ALIGN_PARENT_END)
            contentParams.addRule(RelativeLayout.ALIGN_PARENT_TOP)
            contentParams.addRule(RelativeLayout.CENTER_HORIZONTAL)

            contentView.layoutParams = contentParams
            layout.addView(contentView)

            tarRow.addView(layout)

            cardCount++
        }

        fun stringFloat2doublePrecStringFloat(str: String): String {
            val f = str.toFloat()
            return String.format("%.2f", f)
        }

        val row1 = createRowLayout(true)
        val row2 = createRowLayout()
        //layout.addView(row1)
        //layout.addView(row2)
        generalInfoContainer.addView(row1)
        generalInfoContainer.addView(row2)

        createAndShowInfoCard("总均绩点",
            stringFloat2doublePrecStringFloat(dataObj.getString("totalGradePoint")), row1)
        createAndShowInfoCard("总修学分",
            stringFloat2doublePrecStringFloat(dataObj.getString("actualCredit")), row1)

        createAndShowInfoCard("挂科学分",
            stringFloat2doublePrecStringFloat(dataObj.getString("failingCredits")), row2)
        createAndShowInfoCard("挂科门数", dataObj.getString("failingCourseCount"), row2)
    }

    private fun showAllTermGradeInfo(jsonArr: JSONArray) {
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

        fun showSingleTermGradeInfo(jsonArr: JSONArray) {

            var shelf: CardShelf? = null
            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                shelf = CardShelf.Builder(this)
                    .setSpMultiply(resources.displayMetrics.scaledDensity)
                    .setCardPerRow(2)
                    .setInnerMarginBetweenSp(12f)
                    .setOuterMarginBottomSp(0f)
                    .setOuterMarginTopSp(0f)
                    .setOuterMarginStartSp(0f)
                    .setOuterMarginEndSp(0f)
                    .setInnerPaddingEndSp(0f)
                    .setInnerPaddingStartSp(0f)
                    .setInnerPaddingTopSp(0f)
                    .build()
            }

            val len = jsonArr.length()
            for (i in 0 until len) {
                val data = jsonArr.getJSONObject(i)
                val card = InfoCard.Builder(this)
                    .setSpMultiply(resources.displayMetrics.scaledDensity)
                    .setCardBackground(getDrawable(R.drawable.shape_login_page_box))
                    .setLayoutHeightSp(
                        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT)
                            128f
                        else
                            74f
                    )
                    .setIconTextSizeSp(
                        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT)
                            48f
                        else
                            28f
                    )
                    .setEndMarkTextSizeSp(
                        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT)
                            52f
                        else
                            28f
                    )
                    .setTitleTextSizeSp(
                        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT)
                            24f
                        else
                            18f
                    )
                    .setInfoTextSizeSp(14f)
                    .setInnerMarginBetweenSp(12f)
                    .setHasIcon(true)
                    .setHasEndMark(true)
                    .setIcon(gradePoint2gradeIcon(data.getInt("gradePoint")))
                    .setEndMark(gradePoint2gradeEngCh(data.getInt("gradePoint")))
                    .setTitle(data.getString("courseName"))

                if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                    card.addInfo(
                            InfoCard.Info(
                                "课号", data.getString("courseCode")
                            )
                        )
                        .addInfo(InfoCard.Info(
                            "学分", run {
                                val courseCredits = data.getDouble("credit")
                                var courseCreditText = ""
                                if (abs(courseCredits - (courseCredits.toInt())) < 1e-5) {
                                    courseCreditText += courseCredits.toInt()
                                } else {
                                    courseCreditText += courseCredits
                                }
                                courseCreditText
                            }
                        ))
                        .addInfo(
                            InfoCard.Info(
                                "更新", data.getString("updateTime").split(' ')[0]
                            )
                        )
                }

                /*
                val cardCustomizedParams: LinearLayout.LayoutParams? =
                    if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        val params = LinearLayout.LayoutParams(
                            0,
                            card.layoutHeight
                        )
                        params.marginEnd = (6 * spMultiply).toInt()
                        params.marginStart = (6 * spMultiply).toInt()
                        params.topMargin = (6 * spMultiply).toInt()
                        params.bottomMargin = (6 * spMultiply).toInt()
                        params.weight = 1f

                        params
                    } else {
                        null
                    }*/


                //layout.addView(card.build())
                if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT)
                    gradeInfoContainer.addView(card.build())
                else {
                    //val cardBuilt = card.build()
                    //cardBuilt.layoutParams = cardCustomizedParams
                    shelf!!.addCard(card.build())
                }
            }

            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                shelf!!.fillLastRow()
                gradeInfoContainer.addView(shelf)
            }
        }

        val len = jsonArr.length()
        for (i in 0 until len) {
            val singleTermInfoLayout = LinearLayout(this)
            val singleTermInfoLayoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            singleTermInfoLayout.layoutParams = singleTermInfoLayoutParams
            singleTermInfoLayout.orientation =
                if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT)
                    LinearLayout.VERTICAL
                else
                    LinearLayout.HORIZONTAL

            val termName = TextView(this)
            termName.text = jsonArr.getJSONObject(i).getString("termName")
            termName.gravity = Gravity.CENTER
            termName.background = getDrawable(R.drawable.shape_login_page_box)
            termName.textSize =
                if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT)
                    24f
                else
                    20f
            termName.isClickable = true
            val nameParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (80f * spMultiply).toInt()
            )
            nameParams.bottomMargin = (12f * spMultiply).toInt()
            nameParams.weight = 1f
            nameParams.marginEnd = (6f * spMultiply).toInt()
            termName.layoutParams = nameParams
            //layout.addView(termName)
            singleTermInfoLayout.addView(termName)

            val termGrade = TextView(this)
            termGrade.text = "平均绩点：${jsonArr.getJSONObject(i).getString("averagePoint")}"
            termGrade.gravity = Gravity.CENTER
            termGrade.background = getDrawable(R.drawable.shape_login_page_box)
            termGrade.textSize =
                if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT)
                    24f
                else
                    20f
            termGrade.isClickable = true
            //val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (80f * spMultiply).toInt())
            //params.bottomMargin = (12f * spMultiply).toInt()
            val gradeParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (80f * spMultiply).toInt()
            )
            gradeParams.bottomMargin = (12f * spMultiply).toInt()
            gradeParams.weight = 1f
            gradeParams.marginStart = (6f * spMultiply).toInt()
            termGrade.layoutParams = gradeParams
            //layout.addView(termGrade)
            singleTermInfoLayout.addView(termGrade)
            gradeInfoContainer.addView(singleTermInfoLayout)
            showSingleTermGradeInfo(jsonArr.getJSONObject(i).getJSONArray("creditInfo"))
        }
    }
}
