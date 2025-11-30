package com.gardilily.onedottongji.tools

import android.content.Context
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.content.edit
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.gardilily.onedottongji.service.SingleDayCurriculumAppWidgetGridContainerService.CourseInfo
import com.gardilily.onedottongji.service.WidgetUpdateWorker
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.guava.asDeferred
import org.json.JSONArray
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.text.ifEmpty

/**
 * 工具类，用于处理Widget更新相关操作
 */
object WidgetUpdateUtils {
    private const val SP_KEY_LAST_UPDATE_DATE = "last_update_date"
    private const val SP_NAME = "WidgetUpdateSP"

    const val PERIODIC_WORKER_NAME = "WidgetPeriodicCurriculumUpdate"
    const val ONE_TIME_WORKER_NAME = "WidgetOneTimeCurriculumUpdate"
    const val PERIODIC_WORKER_TAG = "Widget_Daily_Update_Tag"

    // 存储上次更新成功的日期
    fun saveLastUpdateDate(context: Context) {
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
            .edit {
                putString(SP_KEY_LAST_UPDATE_DATE, currentDate)
            }
        Log.d("UpdateDateUtils", "已存储上次更新日期：$currentDate")
    }

    /**
     * 检查已更新日期是否比当前日期旧，若返回true，则建议更新
     *
     */
    fun isLastUpdateDateExpired(context: Context): Boolean {
        val sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
        val lastUpdateDate = sp.getString(SP_KEY_LAST_UPDATE_DATE, null) ?: return true

        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        return try {
            val lastDate = dateFormat.parse(lastUpdateDate)!!
            val currDate = dateFormat.parse(currentDate)!!
            lastDate.before(currDate)
        } catch (e: Exception) {
            Log.e("UpdateDateUtils", "日期对比失败", e)
            true
        }
    }

    // 计算当前时间与0点的偏差
    fun calculateDeviationFromMidnight(): Int {
        val now = Calendar.getInstance()
        val currentHour = now.get(Calendar.HOUR_OF_DAY)
        Log.d("UpdateDateUtils", "当前时间：${currentHour}:00，与0点偏差：$currentHour 小时")
        return currentHour
    }

    fun calculateInitialDelayToMidnight(): Long {
        val now = Calendar.getInstance()
        val midnight = Calendar.getInstance().apply {
            // 明天0点30，防止零点调度繁忙
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // 计算时间差（毫秒）
        var delay = midnight.timeInMillis - now.timeInMillis

        // 已过0点30分，设置为明天的0点30分
        if (delay < 0) {
            delay += 24 * 60 * 60 * 1000 // +24小时
        }

        return delay
    }

    /**
     * 异步查询每日任务状态
     *
     * 调试方法，或者在未来作为日志输出的部分
     */
    suspend fun queryDailyWorkState(context: Context) {
        val workManager = WorkManager.getInstance(context)
        val workFuture: ListenableFuture<List<WorkInfo>> = workManager
            .getWorkInfosForUniqueWork(PERIODIC_WORKER_NAME)

        try {
            val workInfos = workFuture.asDeferred().await()
            if (workInfos.isEmpty()) {
                Log.d("DailyUpdateDebug", "每日更新任务：未提交")
                return
            }
            val workInfo = workInfos[0]
            val nextTime = workInfo.nextScheduleTimeMillis.let {
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(it))
            } ?: "未知"
            Log.d("DailyUpdateDebug", "每日更新任务状态：${workInfo.state.name}")
            Log.d("DailyUpdateDebug", "下次执行时间：$nextTime")
        } catch (e: Exception) {
            Log.e("DailyUpdateDebug", "查询每日任务失败", e)
        }
    }

    fun widgetImmediatelyUpdate(
        context: Context
    ) {
        val updateRequest = OneTimeWorkRequestBuilder<WidgetUpdateWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .build()

        Log.d("appWidgetProvider", "Worker加入队列提交")
        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                ONE_TIME_WORKER_NAME,
                ExistingWorkPolicy.REPLACE,
                updateRequest
            )
    }

    fun widgetPeriodUpdate(context: Context) {
        val initialDelay = calculateInitialDelayToMidnight()
        val dailyRequest = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(
            repeatInterval = 24, repeatIntervalTimeUnit = TimeUnit.HOURS,
            flexTimeInterval = 20, flexTimeIntervalUnit = TimeUnit.MINUTES
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresBatteryNotLow(false)
                    .setRequiresCharging(false)
                    .build()
            )
            .addTag(PERIODIC_WORKER_TAG)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                PERIODIC_WORKER_NAME,
                ExistingPeriodicWorkPolicy.REPLACE,
                dailyRequest
            )

    }

    fun widgetPeriodicUpdateDeviationCheck(context: Context){
        // 因为各种原因被延后的小时数（相较于0点）
        val deviationHour = calculateDeviationFromMidnight()
        if (deviationHour > 1){
            Log.d("WidgetUpdateWorker", "延后的小时数：${deviationHour}，重新调度")
            WorkManager.getInstance(context)
                .cancelUniqueWork(PERIODIC_WORKER_NAME)

            widgetPeriodUpdate(context)
        }
    }

    suspend fun widgetPeriodUpdateExistenceCheck(context: Context){
        val workManager = WorkManager.getInstance(context)
        val workFuture: ListenableFuture<List<WorkInfo>> = workManager
            .getWorkInfosForUniqueWork(PERIODIC_WORKER_NAME)

        val isExist = try {
                val workInfos = workFuture.asDeferred().await()
                workInfos.isNotEmpty() && workInfos.any { workInfo ->
                    workInfo.state !in listOf(WorkInfo.State.SUCCEEDED, WorkInfo.State.FAILED)
                }
            }catch (e: Exception){
                Log.e("WidgetUpdateUtils", "检查失败：${e.toString()}")
                false
            }

        if(!isExist){
            widgetPeriodUpdate(context)
        }

    }

    /**
     * 给定Timetable JSON，获取当天课程信息
     */
    fun getTodayCourseInfo(week: Int, dayOfWeek: Int, json: JSONArray?): List<CourseInfo> {
        if (json == null) return emptyList()
        val courses = mutableListOf<CourseInfo>()
        for (i in 0 until json.length()) {
            try {
                /**
                 * timeTableList包含对应课程的所有节次
                 */
                val timeTableList = json.getJSONObject(i).getJSONArray("timeTableList")
                for (j in 0 until timeTableList.length()) {
                    /**
                     * 一周固定的某节课
                     */
                    val courseObj = timeTableList.getJSONObject(j)
                    val weeks = courseObj.getJSONArray("weeks")
                    if (courseObj.getInt("dayOfWeek") != dayOfWeek) { continue }

                    for (k in 0 until weeks.length()) {
                        val weekObj = weeks.getInt(k)
                        if (weekObj == week) {
                            courses.add(
                                CourseInfo(
                                    timeStart = courseObj.getInt("timeStart"),
                                    timeEnd = courseObj.getInt("timeEnd"),
                                    name = courseObj.getString("courseName"),
                                    teacher = courseObj.getString("teacherName"),
                                    room = courseObj.getString("roomIdI18n").ifEmpty { courseObj.getString("roomLable") }
                                )
                            )
                        }
                    }

                }
            } catch (_: Exception) { /* Ignore parsing errors */ }
        }
        return courses.sortedBy { it.timeStart }
    }
}