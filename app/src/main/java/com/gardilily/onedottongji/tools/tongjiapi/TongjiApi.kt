// SPDX-License-Identifier: MulanPSL-2.0

package com.gardilily.onedottongji.tools.tongjiapi

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.gardilily.onedottongji.activity.Login
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.IOException
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

/**
 * 同济数据开放平台封装。
 * 以单例模式运行。
 *
 * 使用本工具前，应先初始化。一次初始化后，应用生命期内持续有效。
 * 初始化见 [TongjiApi.init]
 *
 * 使用时，请通过内部对象访问：
 *
 * ```kotlin
 * TongjiApi.instance.someMethod()
 * ```
 */
class TongjiApi {

    companion object {

        const val CLIENT_ID = "authorization-xxb-onedottongji-yuchen"
        const val BASE_URL = "https://api.tongji.edu.cn"

        const val OAUTH_REDIRECT_URL = "onetj://fakeredir.gardilily.com"

        const val CODE2TOKEN_URL = "$BASE_URL/v1/token"

        val SCOPE_LIST = listOf(
            "dc_user_student_info",
            "rt_onetongji_cet_score",
            "rt_onetongji_school_calendar_current_term_calendar",
            "rt_onetongji_undergraduate_score",
            "rt_teaching_info_undergraduate_summarized_grades", // 暂未使用
            "rt_onetongji_student_timetable",
            "rt_onetongji_student_exams",
            "rt_teaching_info_sports_test_data",
            "rt_teaching_info_sports_test_health",
            "rt_onetongji_manual_arrange",
            "rt_onetongji_school_calendar_all_term_calendar",
            "rt_onetongji_msg_list",
            "rt_onetongji_msg_detail",
        )

        private var _instance: TongjiApi? = null

        val instance: TongjiApi
            get() {
                if (_instance == null) {
                    _instance = TongjiApi()
                }

                return _instance!!
            }



        const val SHARED_PREFERENCES_NAME = "onetj.tjapi"
        const val SP_KEY_TOKEN_DATA = "tkdata"
        const val SP_KEY_SWITCH_ACCOUNT_REQUIRED = "swacc"

    }

    private val client = OkHttpClient()
    private lateinit var sp : SharedPreferences

    fun init(context: Context) {
        sp = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    data class TokenData(
        var token: String,
        var expireTimeSec: Long,
        var refreshToken: String,
        var refreshTokenExpireSec: Long,
    ) {

        constructor() : this(
            token = "",
            expireTimeSec = 0,
            refreshToken = "",
            refreshTokenExpireSec = 0
        )

        override fun toString(): String {
            val json = JSONObject()

            val ref = this::class.java

            this::class.java.declaredFields.filter { it.name != "Companion" }.forEach {
                json.put(it.name, it.get(this))
            }

            return json.toString()
        }

        companion object {

            fun from(jsonStr: String): TokenData? {
                return from(JSONObject(jsonStr))
            }

            fun from(jsonObject: JSONObject): TokenData? {
                return try {
                    val data = TokenData()

                    data::class.java.declaredFields.filter { it.name != "Companion" }.forEach {
                        val prevAcc = it.isAccessible
                        it.isAccessible = true
                        it.set(data, jsonObject.get(it.name))
                        it.isAccessible = prevAcc
                    }

                    data
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
        }

    }

    /**
     * accessToken 是否过期。
     */
    fun tokenAvailable(): Boolean {
        val tokenData = getTokenData()
        return tokenData.expireTimeSec > System.currentTimeMillis() / 1000 + 10
    }

    /**
     * 删除本地缓存的 access token。
     */
    fun clearCache() {
        sp.edit().remove(SP_KEY_TOKEN_DATA).apply()
    }

    /**
     * 是否请求切换账号。
     * 如果请求切换账号，统一认证登录页面的浏览器将不加载 cookie。
     */
    var switchAccountRequired: Boolean
        get() {
            return sp.getBoolean(SP_KEY_SWITCH_ACCOUNT_REQUIRED, false)
        }
        set(value) {
            sp.edit().putBoolean(SP_KEY_SWITCH_ACCOUNT_REQUIRED, value).apply()
        }

    private fun storeTokenData(data: TokenData) {
        sp.edit().putString(SP_KEY_TOKEN_DATA, data.toString()).apply()
    }

    private fun SharedPreferences.getStringOrNull(key: String): String? {
        return if (this.contains(key))
            this.getString(key, null)
        else null
    }

    /**
     * 读取本地存储的 token 信息。
     * 如果本地没有存，将会返回一个已经过期的假 token 信息。
     *
     * @return TokenData
     */
    private fun getTokenData(): TokenData {
        return try {
            sp.getStringOrNull(SP_KEY_TOKEN_DATA)?.let {
                TokenData.from(it)
            } ?: TokenData()
        } catch (_: Exception) {
            TokenData()
        }
    }

    /**
     * 向网络请求加入鉴权信息。
     * 将在 header 里面加入 authorization 信息，携带 access token。
     */
    fun Request.Builder.addAuthorization(): Request.Builder {
        val token = getTokenData().token
        return this.addHeader("Authorization", "Bearer ${getTokenData().token}")
    }

    private fun processGetTokenResponse(json: JSONObject) {
        val accessToken = json.getString("access_token")
        // 有效期。单位为秒（应该是吧）
        val expiresIn = json.getLong("expires_in")
        val currentTime = System.currentTimeMillis() / 1000
        val expireTime = currentTime + expiresIn - 10

        val refreshToken = json.getString("refresh_token")
        val refreshTokenExpiresIn = json.getLong("refresh_expires_in")
        val refreshTokenExpireTime = currentTime + refreshTokenExpiresIn - 10

        storeTokenData(TokenData(
            token = accessToken,
            expireTimeSec = expireTime,
            refreshToken = refreshToken,
            refreshTokenExpireSec = refreshTokenExpireTime
        ))
    }

    fun code2token(code: String, activity: Activity): Boolean {

        val body = FormBody.Builder()
            .add("grant_type", "authorization_code")
            .add("client_id", CLIENT_ID)
            .add("code", code)
            .add("redirect_uri", OAUTH_REDIRECT_URL)
            .build()

        val request = Request.Builder()
            .url(CODE2TOKEN_URL)
            .post(body)
            .build()

        val response = try {
            client.newCall(request).execute()
        } catch (_: Exception) {
            activity.runOnUiThread {
                Toast.makeText(activity, "网络连接失败。", Toast.LENGTH_SHORT).show()
            }
            null
        } ?: return false

        val bodyString = response.body?.string()
        val responseJson = bodyString?.let { JSONObject(it) } ?: return false

        processGetTokenResponse(responseJson)

        return true
    }

    /**
     *
     * 参考济星云后端项目。
     */
    fun refreshAccessToken(): Boolean {
        val tokenData = getTokenData()
        val currTime = System.currentTimeMillis() / 1000
        if (tokenData.refreshTokenExpireSec < currTime) {
            return false // 无法刷新。
        }

        val body = FormBody.Builder()
            .add("refresh_token", tokenData.refreshToken)
            .add("grant_type", "refresh_token")
            .add("client_id", CLIENT_ID)
            .build()

        val request = Request.Builder()
            .url(CODE2TOKEN_URL)
            .post(body)
            .build()

        val response = try {
            client.newCall(request).execute()
        } catch (_: Exception) {
            null
        } ?: return false

        val responseBody = response.body?.string() ?: return false

        val resJson = try {
            JSONObject(responseBody)
        } catch (_: Exception) {
            null
        } ?: return false

        processGetTokenResponse(resJson)

        return false
    }

    data class StudentInfo(
        var userId: String? = null,
        var name: String? = null,
        var gender: Gender? = null,
        var deptName: String? = null,
        var secondDeptName: String? = null,
        var schoolName: String? = null,
        var currentGrade: String? = null
    ) {
        enum class Gender(val code: Int) {
            UNKNOWN(0),
            MALE(1),
            FEMALE(2),
            UNTOLD(9)

            ;

            companion object {
                fun make(code: Int): Gender {
                    return when (code) {
                        UNKNOWN.code -> UNKNOWN
                        MALE.code -> MALE
                        FEMALE.code -> FEMALE
                        UNTOLD.code -> UNTOLD
                        else -> UNKNOWN
                    }
                }
            }

        }
    }

    private fun basicRequestBuilder(url: String): Request.Builder {
        return Request.Builder().addAuthorization().url(url)
    }

    private val netErrorDialogOnSemaphore = Semaphore(1, 0)

    private fun<T> Request.Builder.execute(
        activity: Activity,
        apiFailureCriticalLevel: ApiFailureCriticalLevel = ApiFailureCriticalLevel.CRITICAL
    ): T? {
        return this.build().execute<T>(activity, apiFailureCriticalLevel)
    }

    private fun<T> Request.execute(
        activity: Activity,
        apiFailureCriticalLevel: ApiFailureCriticalLevel = ApiFailureCriticalLevel.CRITICAL
    ): T? {
        val response = try {
            client.newCall(this).execute()
        } catch (e: IOException) {

            val semaphoreAcquired = runBlocking {
                netErrorDialogOnSemaphore.tryAcquire()
            }

            if (semaphoreAcquired) {
                val msg = "请检查网络连接，然后重新打开此页面。\n\n" +
                        "详细信息：\n" + e.message

                activity.runOnUiThread {
                    AlertDialog.Builder(activity)
                        .setTitle("网络错误")
                        .setMessage(msg)
                        .setPositiveButton("好") { _, _ -> }
                        .setOnDismissListener {
                            netErrorDialogOnSemaphore.release()
                        }
                        .show()
                }

            }

            return null

        }

        return response.checkErrorAndGetData<T>(activity, apiFailureCriticalLevel)
    }

    enum class ApiFailureCriticalLevel {
        CRITICAL,
        WARNING
    }

    /**
     *
     * @return JSONObject or JSONArray
     */
    private fun<T> Response?.checkErrorAndGetData(
        activity: Activity,
        criticalLevel: ApiFailureCriticalLevel = ApiFailureCriticalLevel.CRITICAL
    ): T? {

        val requestDetailMessageBuilder = StringBuilder()
        requestDetailMessageBuilder.append("地址\n${this?.request?.url}\n")
            .append("状态\n${this?.code}\n")
            .append("信息\n${this?.message}\n")

        fun solveError(msg: String = "无") {

            when (criticalLevel) {
                ApiFailureCriticalLevel.CRITICAL -> {
                    clearCache()

                    val msgBuilder = StringBuilder()
                    msgBuilder.append("请重新登录。\n\n")
                        .append("错误信息: \n")
                        .append(msg)
                        .append("\n\n详细信息：\n")
                        .append(requestDetailMessageBuilder)

                    activity.runOnUiThread {
                        AlertDialog.Builder(activity)
                            .setTitle("登录状态异常")
                            .setMessage(msgBuilder)
                            .setPositiveButton("OK") { _, _ ->
                                activity.startActivity(Intent(activity, Login::class.java))
                                activity.finish()
                            }
                            .setCancelable(false)
                            .show()
                    }
                }

                ApiFailureCriticalLevel.WARNING -> {
                    val msgBuilder = StringBuilder()
                    msgBuilder.append("数据获取失败。\n\n")
                        .append("错误信息: \n")
                        .append(msg)
                        .append("\n\n详细信息：\n")
                        .append(requestDetailMessageBuilder)

                    activity.runOnUiThread {
                        AlertDialog.Builder(activity)
                            .setTitle("数据获取失败")
                            .setMessage(msgBuilder)
                            .setPositiveButton("OK") { _, _ ->

                            }
                            .setCancelable(true)
                            .show()
                    }
                }
            } // when (criticalLevel)


        }


        if (this?.body == null) {
            solveError()
            return null
        }

        val json = try {
            val bodyString = this.body!!.string()
            JSONObject(bodyString)
        } catch (_: Exception) {
            solveError("json 解析错误。${this.code}")
            return null
        }

        if (json.has("error_error")) {
            solveError(json.getJSONObject("error_error").toString(2))
            return null
        }

        if (json.getString("code") != "A00000") {
            solveError()
            return null
        }

        return json.get("data") as T

    }

    fun getStudentInfo(activity: Activity): StudentInfo? {
        val url = "$BASE_URL/v1/dc/user/student_info"

        val request = basicRequestBuilder(url)
            .get()
            .build()

        val data = request.execute<JSONArray>(activity)?.getJSONObject(0) ?: return null

        return StudentInfo(
            userId = data.getString("userId"),
            name = data.getString("name"),
            deptName = data.getString("deptName"),
            secondDeptName = data.getString("secondDeptName"),
            currentGrade = data.getString("currentGrade"),
            gender = StudentInfo.Gender.make(data.getString("sexCode").toInt())
        )
    }

    data class SchoolCalendar(
        var calendarId: String? = null,
        var year: String? = null,
        var term: String? = null,
        var schoolWeek: String? = null,
        var simpleName: String? = null
    )

    fun getOneTongjiSchoolCalendar(activity: Activity): SchoolCalendar? {
        val url = "$BASE_URL/v1/rt/onetongji/school_calendar_current_term_calendar"
        val request = basicRequestBuilder(url)
            .get()
            .build()

        val data = request.execute<JSONObject>(activity) ?: return null

        val schoolCalendar = data.getJSONObject("schoolCalendar")

        return SchoolCalendar(
            calendarId = schoolCalendar.getString("id"),
            simpleName = data.getString("simpleName"),
            schoolWeek = data.getString("week"),
        )
    }



    fun getOneTongjiUndergraduateScore(activity: Activity): JSONObject? {
        val url = "$BASE_URL/v1/rt/onetongji/undergraduate_score"

        return basicRequestBuilder("$url?calendarId=-1")
            .get()
            .build()
            .execute<JSONObject>(activity)

    }

    fun getOneTongjiStudentTimetable(activity: Activity): JSONArray? {
        val url = "$BASE_URL/v1/rt/onetongji/student_timetable"
        return basicRequestBuilder(url)
            .get()
            .execute<JSONArray>(activity)
    }


    fun getOneTongjiStudentExams(activity: Activity): JSONObject? {
        val url = "$BASE_URL/v1/rt/onetongji/student_exams"
        return basicRequestBuilder(url)
            .get()
            .execute(activity)
    }

    /**
     *
     * @return data.list: JSONArray
     */
    fun getOneTongjiCetScore(activity: Activity): JSONArray? {
        val url = "$BASE_URL/v1/rt/onetongji/cet_score"
        val data = basicRequestBuilder(url)
            .get()
            .execute<JSONObject>(activity) ?: return null
        return data.getJSONArray("list")
    }


    fun getOneTongjiSportsTestHealthData(activity: Activity): JSONObject? {
        val url = "$BASE_URL/v1/rt/teaching_info/sports_test_health"
        return try {
            basicRequestBuilder(url)
                .get()
                .execute<JSONObject>(activity)
                ?.getJSONArray("userInfos")
                ?.getJSONObject(0)
        } catch (_: Exception) {
            activity.runOnUiThread {
                AlertDialog.Builder(activity)
                    .setTitle("没有体锻数据")
                    .setMessage("暂无数据")
                    .setPositiveButton("好") { _, _ -> }
                    .show()
            }

            null
        }


    }

    fun getOneTongjiSportsTestData(activity: Activity): JSONObject? {
        val url = "$BASE_URL/v1/rt/teaching_info/sports_test_data"
        return try {
            basicRequestBuilder(url)
                .get()
                .execute<JSONObject>(activity)
                ?.getJSONArray("userInfos")
                ?.getJSONObject(0)
        } catch (_: Exception) {
            activity.runOnUiThread {
                AlertDialog.Builder(activity)
                    .setTitle("没有体锻数据")
                    .setMessage("暂无数据")
                    .setPositiveButton("好") { _, _ -> }
                    .show()
            }

            null
        }


    }


    data class CourseArrangement(

        /** 课号。 */
        var code: String? = null,

        var courseLabelName: String? = null,
        var courseLabelId: Int? = null,
        var assessmentMode: AssessmentMode? = null,
        var assessmentModeI18n: String? = null,

        /** 学分。 */
        var credits: String? = null,

        /** 额定人数。 */
        var number: String? = null,

        /** 选课人数。 */
        var elcNumber: String? = null,

        /** 开课学院。 */
        var facultyI18n: String? = null,

        var campusI18n: String? = null,
        var campus: String? = null,

        var courseName: String? = null,
        var courseCode: String? = null,
        var arrangeInfo: String? = null,
    ) {

        enum class AssessmentMode(val value: Int) {
            EXAM(1),
            PAPER(2),
            UNKNOWN(-1)

            ;

            companion object {
                fun make(value: Int): AssessmentMode {
                    return when (value) {
                        EXAM.value -> EXAM
                        PAPER.value -> PAPER
                        else -> UNKNOWN
                    }
                }
            }
        }
    }

    data class GetOneTongjiTermArrangementApiControl(
        var stop: AtomicBoolean = AtomicBoolean(false)
    )

    /**
     * 全校课表。
     * 会阻塞很久。
     */
    fun getOneTongjiTermArrangement(
        calendarId: String,
        activity: Activity,
        apiControl: GetOneTongjiTermArrangementApiControl = GetOneTongjiTermArrangementApiControl(),
        onProgressUpdate: ((progress: Int) -> Unit)? = null
    ): List<CourseArrangement>? {

        val result = ArrayList<CourseArrangement>()
        val resultMutex = Mutex(false)

        fun processApiData(jsonObj: JSONObject) {

            if (apiControl.stop.get()) {
                return
            }

            val jsonArr = jsonObj.getJSONArray("list")

            runBlocking {
                resultMutex.lock()
            }

            for (idx in 0 until jsonArr.length()) {
                val courseObj = jsonArr.getJSONObject(idx)
                result.add(CourseArrangement(
                    code = courseObj.getString("code"),
                    courseName = courseObj.getString("courseName"),
                    facultyI18n = courseObj.getString("facultyI18n"),
                    number = courseObj.getString("number"),
                    elcNumber = courseObj.getString("elcNumber"),
                    credits = courseObj.getString("credits"),
                    courseLabelName = courseObj.getString("courseLabelName"),
                    courseLabelId = courseObj.getInt("courseLabelId"),
                    courseCode = courseObj.getString("courseCode"),
                    arrangeInfo = courseObj.getString("arrangeInfo"),
                    assessmentModeI18n = courseObj.getString("assessmentModeI18n"),
                    assessmentMode = CourseArrangement.AssessmentMode.make(courseObj.getString("assessmentMode").toInt()),
                    campusI18n = courseObj.getString("campusI18n"),
                    campus = courseObj.getString("campus")
                ))
            }

            resultMutex.unlock()

        }

        val url = "$BASE_URL/v1/rt/onetongji/manual_arrange"

        val PAGE_SIZE = 50
        val CONCURRENT_SIZE = 9

        val concurrentPermits = Semaphore(CONCURRENT_SIZE, 0)

        fun fetchData(pageNo: Int): JSONObject? {

            val responseJson = basicRequestBuilder("$url?pageNum=$pageNo&pageSize=$PAGE_SIZE&calendarId=$calendarId")
                .get()
                .build()
                .execute<JSONObject>(activity) ?: return null

            return responseJson
        }

        val firstTry = fetchData(1) ?: return null
        val total = firstTry.getInt("total_")

        val pages = (total + PAGE_SIZE - 1) / PAGE_SIZE // 可用页号：[1, pages]

        val progress = AtomicInteger(0)

        processApiData(firstTry)

        val errorCount = AtomicInteger(0)

        val threadsListMutex = Mutex(false)
        val threads = ArrayList<Thread>()

        val unfiredThreads = AtomicInteger(pages)
        unfiredThreads.decrementAndGet() // 最开始那个试探性的页面。
        val allThreadsFiredSemaphore = Semaphore(1, 1)

        if (unfiredThreads.get() == 0) {
            allThreadsFiredSemaphore.release()
        }

        for (page in 2 .. pages step CONCURRENT_SIZE) {

            if (apiControl.stop.get()) {
                return null
            }

            thread {
                val currPage = page
                for (subPage in 0 until CONCURRENT_SIZE) {

                    if (apiControl.stop.get()) {
                        if (unfiredThreads.decrementAndGet() == 0) {
                            allThreadsFiredSemaphore.release()
                        }
                        continue
                    }

                    val thisPage = currPage + subPage

                    if (thisPage > pages) {
                        return@thread
                    }

                    runBlocking {
                        concurrentPermits.acquire()
                    }

                    val t = thread {
                        val res = fetchData(thisPage)
                        concurrentPermits.release()
                        if (res == null) {
                            errorCount.incrementAndGet()
                            return@thread
                        }

                        val currProgress = thisPage * 100 / pages
                        if (currProgress > progress.get()) {
                            progress.set(currProgress)
                            onProgressUpdate?.let { it(currProgress) }
                        }

                        processApiData(res)
                    }

                    runBlocking {
                        threadsListMutex.withLock {
                            Log.e("Tongji API get arrangement", "thread fired. ${thisPage - 2}")
                            threads.add(t)
                        }
                    }

                    if (unfiredThreads.decrementAndGet() == 0) {
                        allThreadsFiredSemaphore.release()
                    }
                }
            }

            Thread.sleep(1000)
        }

        runBlocking {
            allThreadsFiredSemaphore.acquire()
        }

        if (apiControl.stop.get()) {
            Log.i("Tongji API OneTongji Term Arrangement", "stopped by control message. return null.")
            return null
        }

        threads.forEachIndexed { idx, it ->
            Log.i("Tongji API OneTongji Term Arrangement", "joining thread: $idx")
            it.join()
        }

        Log.i("Tongji API OneTongji Term Arrangement", "all threads joined.")

        if (errorCount.get() > 0) {
            activity.runOnUiThread {
                Toast.makeText(activity, "错误计数：${errorCount.get()}", Toast.LENGTH_SHORT).show()
            }
        }

        return result
    }


    fun getOneTongjiSchoolCalendarAllTermCalendar(activity: Activity): JSONArray? {
        val url = "$BASE_URL/v1/rt/onetongji/school_calendar_all_term_calendar"
        return basicRequestBuilder(url)
            .get()
            .execute(activity)
    }


    fun getOneTongjiMessageList(activity: Activity): JSONArray? {
        val url = "$BASE_URL/v1/rt/onetongji/msg_list?pageNum_=1&pageSize_=9999&total=0"
        val res = basicRequestBuilder(url)
            .get()
            .execute<JSONObject>(activity, ApiFailureCriticalLevel.WARNING) ?: return null

        return res.getJSONArray("list")
    }

    fun getOneTongjiMessageDetail(activity: Activity, id: Int): JSONObject? {
        val url = "$BASE_URL/v1/rt/onetongji/msg_detail?id=$id"
        return basicRequestBuilder(url)
            .get()
            .execute(activity, ApiFailureCriticalLevel.WARNING)
    }

}

