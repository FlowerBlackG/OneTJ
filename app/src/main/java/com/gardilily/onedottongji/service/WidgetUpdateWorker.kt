package com.gardilily.onedottongji.service

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.RemoteViews
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gardilily.onedottongji.R
import com.gardilily.onedottongji.appwidget.SingleDayCurriculumAppWidgetProvider
import com.gardilily.onedottongji.service.SingleDayCurriculumAppWidgetGridContainerService.CourseInfo
import com.gardilily.onedottongji.tools.tongjiapi.TongjiApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.text.ifEmpty
import androidx.core.net.toUri

class WidgetUpdateWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
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

    override suspend fun doWork(): Result{
        return try {
            Log.d("WidgetUpdateWorker", "Worker开始工作 id:${id}")
            coroutineScope {
                val calendarDeferred = async(Dispatchers.IO) {
                    return@async try {
                        TongjiApi.instance.getOneTongjiSchoolCalendar()
                    } catch (e: Exception) {
                        Log.e("WidgetUpdateWorker", "校历请求失败：${e.message}")
                        null
                    }
                }

                val timetableDeferred = async(Dispatchers.IO) {
                    return@async try {
                        TongjiApi.instance.getOneTongjiStudentTimetable()
                    } catch (e: Exception) {
                        Log.e("WidgetUpdateWorker", "课程表请求失败：${e.message}")
                        null
                    }
                }

                Log.d("WidgetUpdateWorker", "等待请求中 id:${id}")
                val calendarData = calendarDeferred.await()
                val timetableData = timetableDeferred.await()

                Log.d("WidgetUpdateWorker", "请求成功 id:${id}")

                val todayWeek = calendarData?.schoolWeek?.toInt()
                val todayDayOfWeek = (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)+6)%7

                val infoList = getTodayCourseInfo(todayWeek ?: 1, todayDayOfWeek, timetableData)

                /* 数据写入服务块 */
                SingleDayCurriculumAppWidgetGridContainerService.infoList = infoList
                SingleDayCurriculumAppWidgetGridContainerService.isDataLoaded = true
//
                val currentTimeMillis = System.currentTimeMillis()
                val formatTime = SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss",
                    Locale.getDefault()
                ).format(Date(currentTimeMillis))

                val context = applicationContext
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(
                    ComponentName(
                        context,
                        SingleDayCurriculumAppWidgetProvider::class.java
                    )
                )

                appWidgetIds.forEach { appWidgetId ->
                    val views = RemoteViews(context.packageName, R.layout.appwidget_single_day_curriculum)

                    views.setTextViewText(R.id.appwidget_single_day_curriculum_last_refresh_time, "更新时间：${formatTime}")

                    val intent = Intent(context, SingleDayCurriculumAppWidgetGridContainerService::class.java)
                    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)

                    intent.setData(intent.toUri(Intent.URI_INTENT_SCHEME).toUri())

                    views.setRemoteAdapter(R.id.appwidget_single_day_curriculum_card_container, intent)

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
                Log.d("WidgetUpdateWorker", "更新成功 id:${id}")
                Result.success()
            }
        }catch (e : Exception){
            Log.e("WidgetUpdateWorker", e.toString())
            Result.retry()
        }
    }

}