// SPDX-License-Identifier: MulanPSL-2.0
package com.gardilily.onedottongji.activity.func

import android.content.res.Configuration
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import com.gardilily.common.view.card.CardShelf
import com.gardilily.common.view.card.InfoCard
import com.gardilily.onedottongji.R
import com.gardilily.onedottongji.activity.OneTJActivityBase
import com.gardilily.onedottongji.tools.tongjiapi.TongjiApi
import org.json.JSONArray
import org.json.JSONObject
import kotlin.concurrent.thread
import kotlin.math.abs

class MyGrades : OneTJActivityBase(
    hasTitleBar = true, backOnTitleBar = true, withSpinning = true
) {

    private lateinit var dataObj: JSONObject
    //private lateinit var layout: LinearLayout

    private lateinit var generalInfoContainer: LinearLayout
    private lateinit var gradeInfoContainer: LinearLayout

    private var spMultiply: Float = 2f
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_func_my_grades)

        spMultiply = resources.displayMetrics.scaledDensity

        title = getString(R.string.my_grades)

        stageSpinningProgressBar(findViewById(R.id.func_myGrades_rootContainer))
        setSpinning(true)

        loadData()

    }

    private fun loadData() {
        thread {

            dataObj = TongjiApi.instance.getOneTongjiUndergraduateScore(this@MyGrades) ?: return@thread

            runOnUiThread {

                setSpinning(false)

                generalInfoContainer = findViewById(R.id.func_myGrades_generalInfoContainer)
                gradeInfoContainer = findViewById(R.id.func_myGrades_gradeInfoContainer)

                showBasicGradeInfo()
                showAllTermGradeInfo(dataObj.getJSONArray("term"))
            }
        }
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
                params.topMargin = (18f * spMultiply).toInt()
            }

            params.bottomMargin = (12f * spMultiply).toInt()
            mLayout.layoutParams = params
            return mLayout
        }

        var cardCount = 0

        fun createAndShowInfoCard(title: String, content: String, tarRow: LinearLayout) {
            val layout = RelativeLayout(this)

            val params = LinearLayout.LayoutParams(0, (90f * spMultiply).toInt())

            params.weight = 1f

            if (cardCount % 2 == 0) {
                params.marginEnd = (6f * spMultiply).toInt()
            } else {
                params.marginStart = (6f * spMultiply).toInt()
            }

            layout.layoutParams = params


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

        generalInfoContainer.addView(row1)
        generalInfoContainer.addView(row2)

        createAndShowInfoCard("总均绩点",
            stringFloat2doublePrecStringFloat(dataObj.getString("totalGradePoint")), row1)
        createAndShowInfoCard("总修学分",
            stringFloat2doublePrecStringFloat(dataObj.getString("actualCredit")), row1)

        createAndShowInfoCard("失利学分",
            stringFloat2doublePrecStringFloat(dataObj.getString("failingCredits")), row2)
        createAndShowInfoCard("失利门数", dataObj.getString("failingCourseCount"), row2)
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
                return "fluentemoji/strawberry_color.svg" // 🍓
            }
            if (point == 4) {
                return "fluentemoji/cherries_color.svg" // 🍒
            }
            if (point == 3) {
                return "fluentemoji/tangerine_color.svg" // 🍊
            }
            if (point == 2) {
                return "fluentemoji/lemon_color.svg" // 🍋
            }
            return "fluentemoji/grapes_color.svg" // 🍇
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

                if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT)
                    gradeInfoContainer.addView(card.build())
                else {

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
