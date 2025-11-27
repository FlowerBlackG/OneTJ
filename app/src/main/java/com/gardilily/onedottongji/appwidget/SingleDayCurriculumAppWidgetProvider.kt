// SPDX-License-Identifier: MulanPSL-2.0

package com.gardilily.onedottongji.appwidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.RemoteViews
import com.gardilily.onedottongji.R
import com.gardilily.onedottongji.service.SingleDayCurriculumAppWidgetGridContainerService
import com.gardilily.onedottongji.service.SingleDayCurriculumAppWidgetGridContainerService.CourseInfo
import com.gardilily.onedottongji.tools.tongjiapi.TongjiApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.text.ifEmpty

class SingleDayCurriculumAppWidgetProvider : AppWidgetProvider() {

    /**
     * 通过输入的参数返回当天的课程
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


    override fun onUpdate(
        context: Context?,
        appWidgetManager: AppWidgetManager?,
        appWidgetIds: IntArray?
    ) {
        context ?: return
        appWidgetManager ?: return
        appWidgetIds ?: return

        Log.d("single day curriculum app widget", "onUpdate tick")

        CoroutineScope(Dispatchers.IO).launch {
            val calendarDeferred = async(Dispatchers.IO) {
                return@async try {
                    TongjiApi.instance.getOneTongjiSchoolCalendar()
                } catch (e: Exception) {
                    Log.e("RemoteViewsFactory", "校历请求失败：${e.message}")
                    null
                }
            }

            val timetableDeferred = async(Dispatchers.IO) {
                return@async try {
                    TongjiApi.instance.getOneTongjiStudentTimetable()
                } catch (e: Exception) {
                    Log.e("RemoteViewsFactory", "课程表请求失败：${e.message}")
                    null
                }
            }

            val calendarData = calendarDeferred.await()
            val timetableData = timetableDeferred.await()

            val todayWeek = calendarData?.schoolWeek?.toInt()
            val todayDayOfWeek = (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)+6)%7

//            Log.d("RemoteViewsFactory", "请求完成。今天是第${todayWeek}周，星期${todayDayOfWeek}")

            val infoList = getTodayCourseInfo(todayWeek ?: 1, todayDayOfWeek, timetableData)

            /* 数据写入服务块 */
            SingleDayCurriculumAppWidgetGridContainerService.infoList = infoList
            SingleDayCurriculumAppWidgetGridContainerService.isDataLoaded = true

            val currentTimeMillis = System.currentTimeMillis()
            val formatTime = SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                Locale.getDefault()
            ).format(Date(currentTimeMillis))

            withContext(Dispatchers.Main) {
                appWidgetIds.forEach { appWidgetId ->
                    val views = RemoteViews(context.packageName, R.layout.appwidget_single_day_curriculum)

                    views.setTextViewText(R.id.appwidget_single_day_curriculum_last_refresh_time, "更新时间：${formatTime}")

                    val intent = Intent(context, SingleDayCurriculumAppWidgetGridContainerService::class.java)
                    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)

                    intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)))

                    views.setRemoteAdapter(R.id.appwidget_single_day_curriculum_card_container, intent)

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        }

    }

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)

        Log.d("single day curri widget provider", "onReceive tick, action: ${intent?.action}")

        val context = context ?: return
        val action = intent?.action ?: return

        val targetActions = setOf(
            AppWidgetManager.ACTION_APPWIDGET_UPDATE,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_TICK
        )

        if(targetActions.contains(action)) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, this::class.java)
            )
            onUpdate(context, appWidgetManager, appWidgetIds)
        }
    }



}
