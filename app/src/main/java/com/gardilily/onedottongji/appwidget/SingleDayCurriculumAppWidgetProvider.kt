// SPDX-License-Identifier: MulanPSL-2.0

package com.gardilily.onedottongji.appwidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.util.Log
import android.widget.RemoteViews
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.gardilily.onedottongji.R
import com.gardilily.onedottongji.service.SingleDayCurriculumAppWidgetGridContainerService
import com.gardilily.onedottongji.service.SingleDayCurriculumAppWidgetGridContainerService.CourseInfo
import com.gardilily.onedottongji.service.WidgetUpdateWorker
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
import java.util.concurrent.TimeUnit
import kotlin.text.ifEmpty

class SingleDayCurriculumAppWidgetProvider : AppWidgetProvider() {

    override fun onEnabled(context: Context?) {
        super.onEnabled(context)
        context ?: return

        val periodicRequest = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(
            repeatInterval = 30, repeatIntervalTimeUnit = TimeUnit.MINUTES,
            flexTimeInterval = 5, flexTimeIntervalUnit = TimeUnit.MINUTES
        ).setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        ).build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                "WidgetPeriodicCurriculumUpdate",
                ExistingPeriodicWorkPolicy.REPLACE,
                periodicRequest
            )

        Log.d("OnEnabled", "周期性任务创建")
    }

    override fun onDisabled(context: Context?) {
        super.onDisabled(context)
        context ?: return

        WorkManager.getInstance(context)
            .cancelUniqueWork("WidgetPeriodicCurriculumUpdate")
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
                "WidgetCurriculumUpdate",
                ExistingWorkPolicy.REPLACE,
                updateRequest
            )

    }

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d("Provider onReceive", "onReceive tick, action: ${intent?.action}")
        Log.d("Provider onReceive", "onReceive tick, packagename: ${intent?.component?.packageName}")

        super.onReceive(context, intent)

    }



}
