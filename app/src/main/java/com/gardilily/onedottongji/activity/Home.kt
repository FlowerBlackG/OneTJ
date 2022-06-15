package com.gardilily.onedottongji.activity

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.gardilily.onedottongji.R
import com.gardilily.onedottongji.activity.func.LocalAttachments
import com.gardilily.onedottongji.activity.func.MyGrades
import com.gardilily.onedottongji.activity.func.StuExamEnquiries
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
import com.gardilily.onedottongji.view.FuncCardShelf
import com.gardilily.onedottongji.view.HomeMsgPublishCard
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

class Home : Activity() {
    private var sessionid = ""
    private lateinit var uniHttpClient: OkHttpClient

    private var uid = ""
    private var username = ""
    private var termId = 111

    private var termName = ""
    private var termWeek = 0

    private var userInfoReported = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        sessionid = intent.getStringExtra("sessionid")!!
        Log.d("Home.onCreate.sessionid", sessionid)

        uniHttpClient = OkHttpClient().newBuilder()
                .followRedirects(false)
                .followSslRedirects(false)
                .build()

        fetchUserBasicInfo()
        fetchTermBasicInfo()

        initFuncButtons()
        initCommonMsgPublish()

        GarCloudApi.checkUpdate(this, false)
    }

    private fun fetchUserBasicInfo() {
        thread {
            val request = Request.Builder()
                    .url("https://1.tongji.edu.cn/api/sessionservice/session/getSessionUser")
                    .addHeader("Cookie", "sessionid=$sessionid")
                    .get()
                    .build()

            //val response = uniHttpClient.newCall(request).execute()

            val response = Utils.safeNetworkRequest(request, uniHttpClient)

            if (response == null) {
                runOnUiThread {
                    Toast.makeText(this, "网络异常", Toast.LENGTH_SHORT).show()
                }
                return@thread
            }

            val resStr = response.body?.string()
            Log.d("Home.fetchUserBasicInfo", resStr!!)
            val jsonObj = JSONObject(resStr)

            if (!isReqSessionAvailable(this, jsonObj) { funcLogout() }) {
                return@thread
            }

            if (isNotReqResCorrect(this, jsonObj, "获取个人信息", REQ_RES_CHECK_NOTI_LEVEL_TOAST)) {
                return@thread
            }

            val userDataObj = jsonObj.getJSONObject("data").getJSONObject("user")
            username = userDataObj.getString("name")
            uid = userDataObj.getString("uid")

            if (!userInfoReported) {
                GarCloudApi.uploadUserInfo(this, uid, username)
                userInfoReported = true
            }

            runOnUiThread {
                findViewById<TextView>(R.id.home_userinfobox_username).text = username
                findViewById<TextView>(R.id.home_userinfobox_uid).text = uid
                findViewById<TextView>(R.id.home_userinfobox_facultyName).text = userDataObj.getString("facultyName")
                val USER_DATA_SEX_MALE = 1
                if (userDataObj.getInt("sex") == USER_DATA_SEX_MALE) {
                    findViewById<TextView>(R.id.home_userinfobox_avatar).text = "👨‍🎓"
                } else {
                    findViewById<TextView>(R.id.home_userinfobox_avatar).text = "👩‍🎓"
                }
                findViewById<TextView>(R.id.home_userinfobox_grade).text = "${userDataObj.getString("grade")}级"
            }
        }
    }

    private fun fetchTermBasicInfo() {
        thread {
            val req = Request.Builder()
                    .url("https://1.tongji.edu.cn/api/baseresservice/schoolCalendar/currentTermCalendar")
                    .addHeader("Cookie", "sessionid=$sessionid")
                    .get()
                    .build()

            val res = Utils.safeNetworkRequest(req, uniHttpClient)

            if (res == null) {
                runOnUiThread {
                    Toast.makeText(this, "网络异常", Toast.LENGTH_SHORT).show()
                }
                return@thread
            }

            val rawObj = JSONObject(res.body?.string())

            if (!isReqSessionAvailable(this, rawObj) { funcLogout() }) {
                return@thread
            }

            if (isNotReqResCorrect(this, rawObj, "获取学期信息", REQ_RES_CHECK_NOTI_LEVEL_TOAST)) {
                return@thread
            }

            val dataObj = rawObj.getJSONObject("data")
            termId = dataObj.getJSONObject("schoolCalendar").getInt("id")
            termName = dataObj.getString("simpleName")
            termWeek = dataObj.getInt("week")
            runOnUiThread {
                findViewById<TextView>(R.id.home_terminfobox_terminfo).text = termName
                findViewById<TextView>(R.id.home_terminfobox_weeknumber).text = "第${termWeek}周"
            }
        }
    }

    private lateinit var shelf: FuncCardShelf

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

        shelf.addFuncCard("🍕", "今日课表", MacroDefines.HOME_FUNC_GRADUATE_STUDENT_TIME_TABLE_SINGLE_DAY, true) { funcButtonClick(it) }
        shelf.addFuncCard("🍔", "学期课表", MacroDefines.HOME_FUNC_GRADUATE_STUDENT_TIME_TABLE_TERM_COMPLETE, true) { funcButtonClick(it) }
        shelf.addFuncCard("💧", "我的成绩", MacroDefines.HOME_FUNC_MY_GRADES, true) { funcButtonClick(it) }

        shelf.addFuncCard("🤯", "我的考试", MacroDefines.HOME_FUNC_STU_EXAM_ENQUIRIES, true) { funcButtonClick(it) }

        shelf.addFuncCard("👩‍🦼", "抢课", MacroDefines.HOME_FUNC_AUTO_COURSE_ELECT, true) { funcButtonClick(it) }

        shelf.addFuncCard("🥪", "本地文件", MacroDefines.HOME_FUNC_LOCAL_ATTACHMENTS, true) { funcButtonClick(it) }



        shelf.addFuncCard("🚗", "退出登录", MacroDefines.HOME_FUNC_LOGOUT, true) { funcButtonClick(it) }
        shelf.addFuncCard("🤔", "关于App", MacroDefines.HOME_FUNC_ABOUT_APP, true) { funcButtonClick(it) }

        // shelf.addFuncCard("🔧", "提取SessionId", MacroDefines.HOME_FUNC_GET_SESSIONID, true) { funcButtonClick(it) }

        shelf.fillBlank()
    }

    /**
     * 初始化通知列表。
     */
    private fun initCommonMsgPublish() {
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
                        intent.putExtra("sessionid", sessionid)
                        intent.putExtra("studentId", uid)
                        startActivity(intent)
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    }
                }
            } else {
                runOnUiThread {
                    Toast.makeText(this, "网络异常", Toast.LENGTH_SHORT).show()
                }
            }
        }


    }

    private fun funcButtonClick(action: Int) {
        when (action) {
            MacroDefines.HOME_FUNC_LOGOUT -> funcLogout()
            MacroDefines.HOME_FUNC_MY_GRADES -> funcShowMyGrades()
            MacroDefines.HOME_FUNC_GRADUATE_STUDENT_TIME_TABLE_TERM_COMPLETE -> funcShowStudentTimetable(FUNC_TIMETABLE_TERM_COMPLETE)
            MacroDefines.HOME_FUNC_GRADUATE_STUDENT_TIME_TABLE_SINGLE_DAY -> funcShowStudentTimetable(FUNC_TIMETABLE_SINGLE_DAY)
            MacroDefines.HOME_FUNC_LOCAL_ATTACHMENTS -> {
                startActivity(Intent(this, LocalAttachments::class.java))
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }
            MacroDefines.HOME_FUNC_ABOUT_APP -> {
                startActivity(Intent(this, About::class.java))
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }
            MacroDefines.HOME_FUNC_AUTO_COURSE_ELECT -> {
                val warningText = "本功能仅可用于选课机制研究与开发测试等必要情景，请勿将其用于任何违规违法活动。\n" +
                        "违反此忠告着，产生的一切后果自负。本程序设计者及相关研发人员拒绝承担任何责任。"
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
            MacroDefines.HOME_FUNC_STUDENT_ELECT -> {
                Toast.makeText(this, "暂缓开通", Toast.LENGTH_SHORT).show()
            }
            MacroDefines.HOME_FUNC_STU_EXAM_ENQUIRIES -> {
                class CalendarIdAndName(var id: Int, var name: String)

                fun getExamCalendarIdAndNameSync(): CalendarIdAndName? {

                    val mediaTypeJSON = "application/json; charset=utf-8".toMediaType()
                    val reqBody = "1".toRequestBody(mediaTypeJSON)
                    val req = Request.Builder()
                        .url("https://1.tongji.edu.cn/api/electionservice/underGraduateExamSwitch/getExamCalendar?examType=1&switchType=null")
                        .addHeader("Cookie", "sessionid=$sessionid")
                        .post(reqBody)
                        .build()

                    val response = Utils.safeNetworkRequest(req, uniHttpClient)

                    if (response == null) {
                        runOnUiThread {
                            Toast.makeText(this, "网络异常", Toast.LENGTH_SHORT).show()
                        }
                        return null
                    }

                    val resObj = JSONObject(response.body?.string())

                    if (!isReqSessionAvailable(this, resObj) { funcLogout() }) {
                        return null
                    }

                    if (isNotReqResCorrect(this, resObj,
                            "查询学期信息", REQ_RES_CHECK_NOTI_LEVEL_ALERTDIALOG)
                    )
                    {
                        return null
                    }

                    val resDataObj = resObj.getJSONObject("data")

                    return CalendarIdAndName(
                        resDataObj.getInt("calendarId"),
                        resDataObj.getString("calendarIdI18n")
                    )
                }
                thread {
                    val mediaTypeJSON = "application/json; charset=utf-8".toMediaType()
                    val calendarIdAndName = getExamCalendarIdAndNameSync()
                    if (calendarIdAndName == null) {
                        runOnUiThread {
                            Toast
                                .makeText(this, "无法获取学期信息", Toast.LENGTH_SHORT)
                                .show()
                        }
                        return@thread
                    }
                    val reqJsonCondition = JSONObject()
                    reqJsonCondition.put("calendarId", calendarIdAndName.id)
                        .put("examSituation", "")
                        .put("examType", 1)
                    val reqJson = JSONObject()
                    reqJson.put("pageSize_", 1000)
                        .put("pageNum_", 1)
                        .put("condition", reqJsonCondition)
                    val reqBody = reqJson.toString().toRequestBody(mediaTypeJSON)
                    val req = Request.Builder()
                        .url("https://1.tongji.edu.cn/api/electionservice/undergraduateExamQuery/getStudentListPage")
                        .addHeader("Cookie", "sessionid=$sessionid")
                        .post(reqBody)
                        .build()

                    val response = Utils.safeNetworkRequest(req, uniHttpClient)

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

                    if (isNotReqResCorrect(this, resObj,
                            "查询考试信息", REQ_RES_CHECK_NOTI_LEVEL_ALERTDIALOG)
                    )
                    {
                        return@thread
                    }

                    val resDataList = resObj
                        .getJSONObject("data")
                        .getJSONObject("data")
                        .getJSONArray("list")
                    val intent = Intent(this, StuExamEnquiries::class.java)
                    intent.putExtra("dataList", resDataList.toString())
                        .putExtra("calendarIdI18n", calendarIdAndName.name)
                    runOnUiThread {
                        startActivity(intent)
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    }
                }
            }
            MacroDefines.HOME_FUNC_GET_SESSIONID -> {
                val clipBoardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clipData = ClipData.newPlainText("OneDotTongji SessionId", sessionid)
                clipBoardManager.setPrimaryClip(clipData)

                Toast.makeText(this, "Session Id 已复制到剪切板。打开小程序粘贴使用。", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun funcLogout() {
        getSharedPreferences(MacroDefines.SHARED_PREFERENCES_STORE_NAME, MODE_PRIVATE)
                .edit().putString(MacroDefines.SP_KEY_SESSIONID, "").apply()
        startActivity(Intent(this, Login::class.java))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    private fun funcShowMyGrades() {
        thread {
            val req = Request.Builder()
                    .url("https://1.tongji.edu.cn/api/scoremanagementservice/scoreGrades/getMyGrades")
                    .addHeader("Cookie", "sessionid=$sessionid")
                    .get()
                    .build()

            val response = Utils.safeNetworkRequest(req, uniHttpClient)

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

            if (isNotReqResCorrect(this, resObj, "查询成绩", REQ_RES_CHECK_NOTI_LEVEL_ALERTDIALOG)) {
                return@thread
            }

            val resDataObj = resObj.getJSONObject("data")
            Log.d("Home.funcShowMyGrades", resDataObj.toString())
            runOnUiThread {
                val intent = Intent(this, MyGrades::class.java)
                intent.putExtra("JsonObj", resObj.toString())
                startActivity(intent)
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }
        }
    }

    private val FUNC_TIMETABLE_TERM_COMPLETE = 1
    private val FUNC_TIMETABLE_SINGLE_DAY = 2
    private fun funcShowStudentTimetable(type: Int) {
        thread {
            val req = Request.Builder()
                    .url("https://1.tongji.edu.cn/api/electionservice/reportManagement/findStudentTimetab?calendarId=$termId")
                    .addHeader("Cookie", "sessionid=$sessionid")
                    .get()
                    .build()
            //val response = uniHttpClient.newCall(req).execute()

            val response = Utils.safeNetworkRequest(req, uniHttpClient)

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

            if (isNotReqResCorrect(this, resObj, "查询课表", REQ_RES_CHECK_NOTI_LEVEL_ALERTDIALOG)) {
                return@thread
            }

            val resDataObj = resObj.getJSONArray("data")
            Log.d("Home.funcShowStudentTimetable", resDataObj.toString())

            runOnUiThread {
                val intent = Intent()
                when (type) {
                    FUNC_TIMETABLE_TERM_COMPLETE -> intent.setClass(this, TermComplete::class.java)
                    FUNC_TIMETABLE_SINGLE_DAY -> intent.setClass(this, SingleDay::class.java)
                }
                intent.putExtra("JsonDataObj", resDataObj.toString())
                intent.putExtra("TermName", termName)
                intent.putExtra("TermWeek", termWeek)
                startActivity(intent)
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                return@runOnUiThread
            }
        }
    }
}
