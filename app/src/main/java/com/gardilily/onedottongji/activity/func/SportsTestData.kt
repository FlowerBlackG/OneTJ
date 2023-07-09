package com.gardilily.onedottongji.activity.func

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.view.GravityCompat
import androidx.core.view.marginBottom
import com.gardilily.onedottongji.R
import com.gardilily.onedottongji.activity.OneTJActivityBase
import com.gardilily.onedottongji.tools.tongjiapi.TongjiApi
import com.google.android.material.card.MaterialCardView
import com.google.android.material.elevation.SurfaceColors
import kotlinx.coroutines.sync.Semaphore
import org.json.JSONObject
import kotlin.concurrent.thread

class SportsTestData : OneTJActivityBase(
    hasTitleBar = true,
    backOnTitleBar = true,
    withSpinning = true
) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sports_test_data)

        title = "体锻体测"
        stageSpinningProgressBar(findViewById(R.id.func_sportsTestData_rootContainer))
        setSpinning(true)

        loadData()

    }

    private val dataLoadedSemaphore = Semaphore(2, 2)

    /**
     *
     * 需要在 UI 线程里调用。
     */
    private fun releaseSemaphoreAndTryHideSpinning() {
        dataLoadedSemaphore.release()
        if (dataLoadedSemaphore.availablePermits == 2) {
            setSpinning(false)
        }
    }

    private fun loadSportsData() {
        thread {
            val sportsData = TongjiApi.instance.getOneTongjiSportsTestData(this@SportsTestData)
            sportsData ?: return@thread

            val runCount = sportsData.getString("stRun")
            val gymCount = sportsData.getString("stSport")
            runOnUiThread {
                releaseSemaphoreAndTryHideSpinning()
                findViewById<TextView>(R.id.func_sportsTestData_runCount).text = runCount
                findViewById<TextView>(R.id.func_sportsTestData_gymCount).text = gymCount
                val updateTimeTxt = "更新时间：${sportsData.getString("updateTime")}"
                findViewById<TextView>(R.id.func_sportsTestData_sportsDataUpdateTime).text = updateTimeTxt
            }

        }
    }

    private fun processGeneralHealthData(infoObj: JSONObject) {
        val container = findViewById<LinearLayout>(R.id.func_sportsTestData_termTestScoreContainer)

        fun makeAndAddCard(title: String, data: String, focused: Boolean = false, isFinalOne: Boolean = false) {
            val view = MaterialCardView(this)
            val viewParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT)
            viewParams.weight = 1f
            if (!isFinalOne) {
                viewParams.marginEnd = 24
            }
            view.layoutParams = viewParams
            container.addView(view)

            val innerLinearLayout = LinearLayout(this)
            val innerLinearParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT)
            innerLinearLayout.layoutParams = innerLinearParams
            innerLinearLayout.orientation = LinearLayout.VERTICAL
            view.addView(innerLinearLayout)

            val titleView = TextView(this)
            titleView.text = title
            val titleParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0)
            titleParams.weight = 40f
            titleParams.topMargin = 40
            titleView.layoutParams = titleParams
            titleView.gravity = Gravity.CENTER
            titleView.textSize = 18f

            val dataView = TextView(this)
            dataView.text = data
            val dataParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0)
            dataParams.weight = 60f
            dataParams.bottomMargin = 40
            dataView.gravity = Gravity.CENTER
            dataView.layoutParams = dataParams
            dataView.textSize = 26f

            innerLinearLayout.addView(titleView)
            innerLinearLayout.addView(dataView)

            view.isClickable = true

            findViewById<TextView>(R.id.func_sportsTestData_avgScore).text = infoObj.getString("avgScore")

        }

        makeAndAddCard("大一", infoObj.getString("firstScore"))
        makeAndAddCard("大二", infoObj.getString("secondScore"))
        makeAndAddCard("大三", infoObj.getString("thirdScore"))
        makeAndAddCard("大四", infoObj.getString("fourthScore"), isFinalOne = true)

    }

    private fun processThisTermHealthData(infoObj: JSONObject) {
        val container = findViewById<LinearLayout>(R.id.func_sportsTestData_thisTermDetailLinearContainer)

        fun addData(title: String, data: String, isFinalOne: Boolean = false) {

            val view = RelativeLayout(this)
            val viewParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, 128)
            if (!isFinalOne) {
                viewParams.topMargin = 4
            }
            view.layoutParams = viewParams
            container.addView(view)

            val titleView = TextView(this)
            val titleParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.MATCH_PARENT)
            titleView.layoutParams = titleParams
            titleView.text = title
            titleView.textSize = 18f
            titleView.gravity = Gravity.CENTER

            view.addView(titleView)


            val dataView = TextView(this)
            val dataParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.MATCH_PARENT)
            dataParams.addRule(RelativeLayout.ALIGN_PARENT_END)
            dataView.layoutParams = dataParams
            dataView.text = data
            dataView.textSize = 24f
            dataView.gravity = Gravity.CENTER

            view.addView(dataView)

        }

        addData("身高", "${infoObj.getString("height")} 米")
        addData("体重", "${infoObj.getString("weight")} 千克")
        val enduranceRunData = infoObj.getString("enduranceRunning").split(".")
        addData("长跑", "${enduranceRunData[0]} 分 ${enduranceRunData[1]} 秒")
        addData("50米", "${infoObj.getString("fiftyMeters")} 秒")

        if (infoObj.getString("sexName") == "女") {
            addData("体前屈", "${infoObj.getString("sitUp")} 个")
        } else {
            addData("引体", "${infoObj.getString("pullUp")} 个")
        }

        addData("肺活量", "${infoObj.getString("vitalCapacity")} 毫升", true)

    }

    private fun loadSportsHealthData() {
        thread {
            val healthData = TongjiApi.instance.getOneTongjiSportsTestHealthData(this@SportsTestData)
            healthData ?: return@thread

            runOnUiThread {
                releaseSemaphoreAndTryHideSpinning()
                processGeneralHealthData(healthData)
                findViewById<TextView>(R.id.func_sportsTestData_remarks).text = healthData.getString("remarks")
                val stuNameTxt = "${healthData.getString("userId")} ${healthData.getString("name")} (${healthData.getString("sexName")})"
                findViewById<TextView>(R.id.func_sportsTestData_studentName).text = stuNameTxt
                processThisTermHealthData(healthData)
            }
        }
    }

    fun loadData() {
        loadSportsData()
        loadSportsHealthData()
    }
}
