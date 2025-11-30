package com.gardilily.onedottongji.service

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gardilily.onedottongji.R
import com.gardilily.onedottongji.appwidget.SingleDayCurriculumAppWidgetProvider
import com.gardilily.onedottongji.tools.tongjiapi.TongjiApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.core.net.toUri
import com.gardilily.onedottongji.tools.WidgetUpdateUtils.PERIODIC_WORKER_TAG
import com.gardilily.onedottongji.tools.WidgetUpdateUtils.getTodayCourseInfo
import com.gardilily.onedottongji.tools.WidgetUpdateUtils.saveLastUpdateDate
import com.gardilily.onedottongji.tools.WidgetUpdateUtils.widgetPeriodicUpdateDeviationCheck
import com.gardilily.onedottongji.tools.WidgetUpdateUtils.widgetPeriodUpdateExistenceCheck

class WidgetUpdateWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {


    override suspend fun doWork(): Result{
        return try {
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

                val calendarData = calendarDeferred.await()
                val timetableData = timetableDeferred.await()

                val todayWeek = calendarData?.schoolWeek?.toInt()
                val todayDayOfWeek = (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)+6)%7

                val infoList = getTodayCourseInfo(todayWeek ?: 1, todayDayOfWeek, timetableData)

                /* 数据写入服务块 */
                SingleDayCurriculumAppWidgetGridContainerService.infoList = infoList
                SingleDayCurriculumAppWidgetGridContainerService.isDataLoaded = true

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
                    if (infoList.isEmpty()) {
                        Log.d("TEST", "课程表为空")
                        views.setViewVisibility(R.id.appwidget_single_day_curriculum_tips_text, View.VISIBLE)
                        views.setTextViewText(R.id.appwidget_single_day_curriculum_tips_text, "今天没有课哦~")
                    }else{
                        views.setViewVisibility(R.id.appwidget_single_day_curriculum_tips_text, View.GONE)
                    }
                    val intent = Intent(context, SingleDayCurriculumAppWidgetGridContainerService::class.java)
                    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)

                    intent.setData(intent.toUri(Intent.URI_INTENT_SCHEME).toUri())

                    views.setRemoteAdapter(R.id.appwidget_single_day_curriculum_card_container, intent)
                    appWidgetManager.updateAppWidget(appWidgetId, views)

                }
            }

            val isPeriodicTask = tags.contains(PERIODIC_WORKER_TAG)

            if (isPeriodicTask) {
                widgetPeriodicUpdateDeviationCheck(applicationContext)
            }else{
                widgetPeriodUpdateExistenceCheck(applicationContext)
            }
            saveLastUpdateDate(applicationContext)

            Result.success()
        }catch (e : Exception){
            Log.e("WidgetUpdateWorker", e.toString())
            Result.retry()
        }
    }

}