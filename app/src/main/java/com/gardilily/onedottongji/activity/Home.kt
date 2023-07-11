// SPDX-License-Identifier: MulanPSL-2.0
package com.gardilily.onedottongji.activity

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.XmlResourceParser
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.graphics.drawable.VectorDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.animation.AlphaAnimation
import android.view.animation.DecelerateInterpolator
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.caverock.androidsvg.SVG
import com.caverock.androidsvg.SVGImageView
import com.gardilily.onedottongji.R
import com.gardilily.onedottongji.activity.func.CetScore
import com.gardilily.onedottongji.activity.func.LocalAttachments
import com.gardilily.onedottongji.activity.func.MyGrades
import com.gardilily.onedottongji.activity.func.SportsTestData
import com.gardilily.onedottongji.activity.func.StuExamEnquiries
import com.gardilily.onedottongji.activity.func.TermArrangement
import com.gardilily.onedottongji.activity.func.autocourseelect.AutoCourseElect
import com.gardilily.onedottongji.activity.func.studenttimetable.SingleDay
import com.gardilily.onedottongji.activity.func.studenttimetable.TermComplete
import com.gardilily.onedottongji.tools.GarCloudApi
import com.gardilily.onedottongji.tools.MacroDefines
import com.gardilily.onedottongji.tools.Utils
import com.gardilily.onedottongji.tools.Utils.Companion.REQ_RES_CHECK_NOTI_LEVEL_ALERTDIALOG
import com.gardilily.onedottongji.tools.Utils.Companion.REQ_RES_CHECK_NOTI_LEVEL_TOAST
import com.gardilily.onedottongji.tools.Utils.Companion.isNotReqResCorrect
import com.gardilily.onedottongji.tools.Utils.Companion.isReqSessionAvailable
import com.gardilily.onedottongji.tools.tongjiapi.TongjiApi
import com.gardilily.onedottongji.view.FuncCardShelf
import com.gardilily.onedottongji.view.HomeMsgPublishCard
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.net.URLDecoder
import java.net.URLEncoder
import kotlin.concurrent.thread

class Home : OneTJActivityBase(hasTitleBar = false) {

    private lateinit var uniHttpClient: OkHttpClient

    private var sessionid = ""
    private var uid = ""
    private var username = ""
    private var termId = 111

    private var termName = ""
    private var termWeek = 0

    private var userInfoReported = false

    private var studentInfo: TongjiApi.StudentInfo? = null
    private var schoolCalendar: TongjiApi.SchoolCalendar? = null


    private var studentInfoLoadedSemaphore = Semaphore(1, 1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)


        uniHttpClient = OkHttpClient().newBuilder()
                .followRedirects(false)
                .followSslRedirects(false)
                .build()

        fetchUserBasicInfo()
        fetchTermBasicInfo()

        initFuncButtons()
        initCommonMsgPublish()

        GarCloudApi.checkUpdate(this, false)

        loadWeather()

        findViewById<SVGImageView>(R.id.home_userinfobox_avatar).setImageAsset("fluentemoji/strawberry_color.svg")

    }

    private fun fetchUserBasicInfo() {
        thread {

            studentInfo = TongjiApi.instance.getStudentInfo(this) ?: return@thread

            runOnUiThread {
                findViewById<TextView>(R.id.home_userinfobox_username).text = studentInfo!!.name
                findViewById<TextView>(R.id.home_userinfobox_uid).text = studentInfo!!.userId
                findViewById<TextView>(R.id.home_userinfobox_facultyName).text = studentInfo!!.deptName
                findViewById<TextView>(R.id.home_userinfobox_grade).text = "${studentInfo!!.currentGrade}级"

                if (studentInfo!!.gender == TongjiApi.StudentInfo.Gender.MALE) {
                    findViewById<SVGImageView>(R.id.home_userinfobox_avatar).setImageAsset("fluentemoji/sleeping_face_color.svg")
                } else {
                    findViewById<SVGImageView>(R.id.home_userinfobox_avatar).setImageAsset("fluentemoji/smiling_face_with_hearts_color.svg")
                }

                studentInfoLoadedSemaphore.release()
            }

            if (!userInfoReported) {
                GarCloudApi.uploadUserInfo(this, studentInfo!!.userId!!, studentInfo!!.name!!)
                userInfoReported = true
            }

        }
    }

    private fun fetchTermBasicInfo() {
        thread {

            schoolCalendar = TongjiApi.instance.getOneTongjiSchoolCalendar(this) ?: return@thread

            runOnUiThread {
                findViewById<TextView>(R.id.home_terminfobox_terminfo).text = schoolCalendar!!.simpleName
                findViewById<TextView>(R.id.home_terminfobox_weeknumber).text = "第${schoolCalendar!!.schoolWeek}周"
            }

        }
    }

    private lateinit var shelf: FuncCardShelf

    enum class HomeFunc {
        NONE,
        GRADUATE_STUDENT_TIME_TABLE_SINGLE_DAY,
        GRADUATE_STUDENT_TIME_TABLE_TERM_COMPLETE,
        MY_GRADES,
        STU_EXAM_ENQUIRIES,
        CET_SCORE,
        AUTO_COURSE_ELECT,
        LOGOUT,
        SHARE_APP,
        ABOUT_APP,
        SPORTS_TEST_DATA,
        TERM_ARRANGEMENT,
    }

    /**
     * 初始化主页功能按钮。
     */
    private fun initFuncButtons() {
        val spMultiply = resources.displayMetrics.scaledDensity
        val screenWidthPx = windowManager.defaultDisplay.width
        val targetCardWidthPx = ((screenWidthPx - (2f * 18f + 2f * 12f) * spMultiply) / 3f).toInt()

        shelf = FuncCardShelf(this)
        shelf.targetCardWidthPx = targetCardWidthPx
        findViewById<LinearLayout>(R.id.home_funcBtnLinearLayout).addView(shelf)

        shelf.addFuncCard("fluentemoji/alarm_clock_color.svg", "今日课表", HomeFunc.GRADUATE_STUDENT_TIME_TABLE_SINGLE_DAY, true) { funcButtonClick(it) }
        shelf.addFuncCard("fluentemoji/notebook_color.svg", "学期课表", HomeFunc.GRADUATE_STUDENT_TIME_TABLE_TERM_COMPLETE, true) { funcButtonClick(it) }
        shelf.addFuncCard("fluentemoji/anguished_face_color.svg", "我的成绩", HomeFunc.MY_GRADES, true) { funcButtonClick(it) }

        shelf.addFuncCard("fluentemoji/memo_color.svg", "我的考试", HomeFunc.STU_EXAM_ENQUIRIES, true) { funcButtonClick(it) }
        shelf.addFuncCard("fluentemoji/money_with_wings_color.svg", "四六级", HomeFunc.CET_SCORE, true) { funcButtonClick(it) }
        shelf.addFuncCard("fluentemoji/badminton_color.svg", "体测体锻", HomeFunc.SPORTS_TEST_DATA, true) { funcButtonClick(it) }
        shelf.addFuncCard("fluentemoji/speedboat_color.svg", "全校课表", HomeFunc.TERM_ARRANGEMENT, true) { funcButtonClick(it) }

        shelf.addFuncCard("fluentemoji/shushing_face_color.svg", "抢课", HomeFunc.AUTO_COURSE_ELECT, true) { funcButtonClick(it) }

        //shelf.addFuncCard("fluentemoji/alarm_clock_color.svg", "本地文件", HomeFunc.LOCAL_ATTACHMENTS, true) { funcButtonClick(it) }



        shelf.addFuncCard("fluentemoji/wilted_flower_color.svg", "退出登录", HomeFunc.LOGOUT, true) { funcButtonClick(it) }
        shelf.addFuncCard("fluentemoji/hatching_chick_color.svg", "分享App", HomeFunc.SHARE_APP, true) { funcButtonClick(it) }
        shelf.addFuncCard("fluentemoji/teddy_bear_color.svg", "关于App", HomeFunc.ABOUT_APP, true) { funcButtonClick(it) }

        // shelf.addFuncCard("🔧", "提取SessionId", MacroDefines.HOME_FUNC_GET_SESSIONID, true) { funcButtonClick(it) }

        shelf.fillBlank()
    }

    /**
     * 初始化通知列表。
     */
    private fun initCommonMsgPublish() {

        return // todo

        val container = findViewById<LinearLayout>(R.id.home_commonMsgPublishContainer)

        thread {
            val requestFormBody = FormBody.Builder()
                .add("pageNum_", "1")
                .add("pageSize_", "9999")
                .add("total", "0")
                .build()

            val client = OkHttpClient()

            val req = Request.Builder()
                    .url("https://1.tongji.edu.cn/api/commonservice/commonMsgPublish/findMyCommonMsgPublish")
                    .post(requestFormBody)
                    .addHeader("Cookie", "sessionid=$sessionid")
                    .build()

            val response = Utils.safeNetworkRequest(req, client)

            if (response == null) {
                runOnUiThread {
                    Toast.makeText(this, "网络异常", Toast.LENGTH_SHORT).show()
                }
                return@thread
            }

            val resObj = JSONObject(response.body?.string())

            if (!isReqSessionAvailable(this, resObj) { funcLogout() }) {
                return@thread
            }

            if (isNotReqResCorrect(this, resObj, "获取消息列表", REQ_RES_CHECK_NOTI_LEVEL_TOAST)) {
                return@thread
            }

            val resDataObj = resObj.getJSONObject("data")
            val dataArr = resDataObj.getJSONArray("list")
            val len = dataArr.length()

            for (i in 0 until len) {
                Thread.sleep(56)
                runOnUiThread {
                    try {
                        val card = HomeMsgPublishCard(this, dataArr.getJSONObject(i)) { dataObj: JSONObject ->
                            runOnUiThread {
                                val intent = Intent(this@Home, MsgPublishShow::class.java)
                                intent.putExtra("basicDataObj", dataObj.toString())
                                intent.putExtra("sessionid", sessionid)
                                startActivity(intent)
                                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                            }
                        }

                        container.addView(card)

                    } catch (e: Exception) {
                        return@runOnUiThread
                    }
                }
            }
        }
    }

    private fun defUrlEnc(str: String?): String {
        return URLEncoder.encode(str, "UTF-8")
    }

    private fun defUrlDec(str: String?): String {
        return URLDecoder.decode(str, "UTF-8")
    }

    private fun jumpToAutoCourseElectActivity() {
        // 先校验身份，再进行跳转。

        // 采用跑吗的验证接口进行身份校验。

        thread {
            val fakerunMockClientVersion = 9
            val authApiUrl = "https://www.gardilily.com/fakeRun/api/auth.php" +
                    "?ac_key=" + "B9D934C1D10F29B1C5201C84291133F4" +
                    "&version=$fakerunMockClientVersion" +
                    "&keycode=" + username +
                    "&device=${defUrlEnc(Build.BRAND + Build.MODEL)}"
            val request = Request.Builder()
                .url(authApiUrl)
                .build()
            var response: Response? = null
            try {
                response = uniHttpClient.newCall(request).execute()
            } catch (e: Exception) { }
            if (response?.code == 200) {
                val result = defUrlDec(response.body?.string())
                val resInt = result.toInt()

                if (resInt < 0) {
                    runOnUiThread {
                        Toast.makeText(this, "拒绝使用。请联系负责人员", Toast.LENGTH_SHORT).show()
                    }
                } else if (resInt > 0) {
                    runOnUiThread {
                        val intent = Intent(this, AutoCourseElect::class.java)
                        intent.putExtra("studentId", studentInfo?.userId)
                        startActivity(intent)
                    }
                }
            } else {
                runOnUiThread {
                    Toast.makeText(this, "网络异常", Toast.LENGTH_SHORT).show()
                }
            }
        }


    }

    private var weatherIconBitmap: Bitmap? = null

    /**
     * 加载天气数据。
     */
    private fun loadWeather() {
        thread {
            val request = Request.Builder()
                .url("https://www.gardilily.com/oneDotTongji/shWeather.php")
                .get()
                .build()

            val response = try {
                uniHttpClient.newCall(request).execute()
            } catch (_: Exception) {
                return@thread
            }

            response.body ?: return@thread

            try {
                val data = JSONObject(response.body!!.string()).getJSONArray("results").getJSONObject(0)
                val now = data.getJSONObject("now")
                val dataText = now.getString("text")
                val dataCode = now.getString("code")
                val temperature = now.getString("temperature")

                // 获取天气图片素材。
                val request = Request.Builder()
                    .url("https://www.gardilily.com/oneDotTongji/weatherIcons/$dataCode@2x.png")
                    .get()
                    .build()
                val response = uniHttpClient.newCall(request).execute()
                response.body ?: return@thread

                val istream = response.body!!.byteStream()
                weatherIconBitmap?.recycle()
                weatherIconBitmap = BitmapFactory.decodeStream(istream)
                istream.close()

                // 准备展示天气。
                runOnUiThread {
                    val fadeInAnim = AlphaAnimation(0f, 1f)
                    fadeInAnim.interpolator = DecelerateInterpolator()
                    fadeInAnim.duration = 670

                    findViewById<LinearLayout>(R.id.home_userinfobox_weatherContainer)?.startAnimation(fadeInAnim)

                    findViewById<TextView>(R.id.home_userinfobox_weatherText)?.text = "上海$temperature°C"
                    val imgView = findViewById<ImageView>(R.id.home_userinfobox_weatherImgView)
                    imgView?.setImageBitmap(weatherIconBitmap)

                    val temperatureFloat = temperature.toFloat()
                    if (temperatureFloat.toFloat() >= 27.9 || temperatureFloat.toFloat() <= 2.1) {
                        thread {
                            runBlocking {
                                studentInfoLoadedSemaphore.acquire()
                            }

                            runOnUiThread {
                                if (temperatureFloat > 27) {
                                    findViewById<SVGImageView>(R.id.home_userinfobox_avatar).setImageAsset(
                                        "fluentemoji/melting_face_color.svg"
                                    )
                                } else {
                                    findViewById<SVGImageView>(R.id.home_userinfobox_avatar).setImageAsset(
                                        "fluentemoji/cold_face_color.svg"
                                    )
                                }
                                studentInfoLoadedSemaphore.release()
                            }
                        }
                    }
                }

            } catch (e: Exception) {
                // nothing to do.
            }
        }
    }

    private fun funcButtonClick(action: HomeFunc) {
        when (action) {
            HomeFunc.LOGOUT -> funcLogout()
            HomeFunc.MY_GRADES -> startActivity(Intent(this, MyGrades::class.java))
            HomeFunc.GRADUATE_STUDENT_TIME_TABLE_TERM_COMPLETE -> funcShowStudentTimetable(FUNC_TIMETABLE_TERM_COMPLETE)
            HomeFunc.GRADUATE_STUDENT_TIME_TABLE_SINGLE_DAY -> funcShowStudentTimetable(FUNC_TIMETABLE_SINGLE_DAY)
            /*HomeFunc.LOCAL_ATTACHMENTS -> {
                startActivity(Intent(this, LocalAttachments::class.java))
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }*/
            HomeFunc.ABOUT_APP -> startActivity(Intent(this, About::class.java))
            HomeFunc.AUTO_COURSE_ELECT -> {
                val warningText = "本功能仅可用于选课机制研究与开发测试等必要情景，请勿将其用于任何违规违法活动。\n" +
                        "违反此忠告者，产生的一切后果自负。本程序设计者及相关研发人员拒绝承担任何责任。"
                AlertDialog.Builder(this)
                    .setTitle("免责声明")
                    .setMessage(warningText)
                    .setPositiveButton("好") { _, _ ->
                        jumpToAutoCourseElectActivity()
                    }
                    .setNegativeButton("不要", null)
                    .create()
                    .show()
            }

            HomeFunc.STU_EXAM_ENQUIRIES -> startActivity(Intent(this, StuExamEnquiries::class.java))
            HomeFunc.CET_SCORE -> startActivity(Intent(this, CetScore::class.java))
            HomeFunc.SPORTS_TEST_DATA -> startActivity(Intent(this, SportsTestData::class.java))
            HomeFunc.SHARE_APP -> shareApp()
            HomeFunc.TERM_ARRANGEMENT -> {
                val intent = Intent(this, TermArrangement::class.java)
                intent.putExtra("calendarId", schoolCalendar?.calendarId)
                startActivity(intent)
            }
            else -> {}
        }
    }

    private fun shareApp() {
        val imgView = ImageView(this)
        imgView.setImageResource(R.drawable.r44download)
        AlertDialog.Builder(this)
            .setTitle("分享App")
            .setPositiveButton("好") { _, _ -> }
            .setMessage("扫描二维码，使用浏览器打开 🥳\n适用安卓（含鸿蒙、WSA）设备。")
            .setView(imgView)
            .show()
    }

    private fun funcLogout() {

        AlertDialog.Builder(this)
            .setTitle("退出登录")
            .setMessage("真的要退出登录吗？🥀")
            .setPositiveButton("退出") { _, _ ->
                TongjiApi.instance.clearCache()
                TongjiApi.instance.switchAccountRequired = true
                getSharedPreferences(MacroDefines.SHARED_PREFERENCES_STORE_NAME, MODE_PRIVATE)
                    .edit().putString(MacroDefines.SP_KEY_SESSIONID, "").apply()
                startActivity(Intent(this, Login::class.java))
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                finish()
            }
            .setNegativeButton("算了") { _, _ -> }
            .show()

    }


    private val FUNC_TIMETABLE_TERM_COMPLETE = 1
    private val FUNC_TIMETABLE_SINGLE_DAY = 2
    private fun funcShowStudentTimetable(type: Int) {

        if (type == FUNC_TIMETABLE_SINGLE_DAY) {
            startActivity(Intent(this, SingleDay::class.java))
        } else {
            val intent = Intent(this, TermComplete::class.java)
            intent.putExtra("TermName", schoolCalendar?.simpleName)
            startActivity(intent)
        }

    }

    override fun onDestroy() {
        weatherIconBitmap?.recycle()
        super.onDestroy()
    }
}
