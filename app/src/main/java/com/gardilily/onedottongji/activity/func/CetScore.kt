// SPDX-License-Identifier: MulanPSL-2.0

package com.gardilily.onedottongji.activity.func

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.ScrollView
import com.gardilily.common.view.card.InfoCard
import com.gardilily.onedottongji.R
import com.gardilily.onedottongji.activity.OneTJActivityBase
import com.gardilily.onedottongji.tools.tongjiapi.TongjiApi
import kotlin.concurrent.thread

class CetScore : OneTJActivityBase(
    backOnTitleBar = true,
    hasTitleBar = true,
    withSpinning = true
) {

    private lateinit var rootLayout: RelativeLayout
    private lateinit var linearContainer: LinearLayout



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prepareRootLayout()
        setContentView(rootLayout)

        prepareLinearContainer()

        stageSpinningProgressBar(rootLayout)
        setSpinning(true)

        title = getString(R.string.cet)

        loadData()
    }

    private fun prepareRootLayout() {
        rootLayout = RelativeLayout(this)
        val params = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        rootLayout.layoutParams = params
    }

    private fun prepareLinearContainer() {
        val scrollView = ScrollView(this)
        val scrollParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT)
        scrollView.layoutParams = scrollParams
        rootLayout.addView(scrollView)

        linearContainer = LinearLayout(this)
        val linearParam = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.MATCH_PARENT)
        linearContainer.orientation = LinearLayout.VERTICAL

        linearContainer.layoutParams = linearParam
        scrollView.addView(linearContainer)
    }

    private fun loadData() {
        thread {

            val data = TongjiApi.instance.getOneTongjiCetScore(this@CetScore) ?: return@thread

            runOnUiThread {
                setSpinning(false)

                for (idx in 0 until data.length()) {
                    val it = data.getJSONObject(idx)
                    val isCet4 = it.getString("cetType") == "1"
                    val termName = it.getString("calendarYearTermCn")
                    val score = it.getString("score")
                    val cardNo = it.getString("cardNo") // 准考证号
                    val writtenSubjectName = it.getString("writtenSubjectName")
                    val oralScore = if (it.getString("oralScore") == "null") "无" else it.getString("oralScore")

                    val card = InfoCard.Builder(this)
                        .setHasIcon(true)
                        .setHasEndMark(true)
                        .setEndMark(score.split(".")[0])
                        .setTitle(termName)
                        .setSpMultiply(resources.displayMetrics.scaledDensity)
                        .setOuterMarginTopSp(16f)
                        .setOuterMarginBottomSp(0f)
                        .setOuterMarginStartSp(16f)
                        .setOuterMarginEndSp(16f)
                        .addInfo(InfoCard.Info("准考证", cardNo))
                        .addInfo(InfoCard.Info("学生", "${it.getString("studentId")} ${it.getString("studentName")}"))
                        .addInfo(InfoCard.Info("科目", writtenSubjectName))
                        .addInfo(InfoCard.Info("口试", oralScore))
                        .setIcon(
                            if (isCet4) "fluentemoji/thinking_face_color.svg"
                            else "fluentemoji/exploding_head_color.svg"
                        )
                        .setIconSize(96)
                        .setEndMarkTextSizeSp(36f)

                    linearContainer.addView(card.build())
                }

            }
        }
    }

}