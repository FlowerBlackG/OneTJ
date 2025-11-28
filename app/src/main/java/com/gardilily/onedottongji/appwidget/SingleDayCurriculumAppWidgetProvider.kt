// SPDX-License-Identifier: MulanPSL-2.0

package com.gardilily.onedottongji.appwidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.WorkManager
import com.gardilily.onedottongji.tools.WidgetUpdateUtils.isLastUpdateDateExpired
import com.gardilily.onedottongji.tools.WidgetUpdateUtils.widgetImmediatelyUpdate
import com.gardilily.onedottongji.tools.WidgetUpdateUtils.widgetPeriodUpdate

class SingleDayCurriculumAppWidgetProvider : AppWidgetProvider() {

    override fun onEnabled(context: Context?) {
        super.onEnabled(context)
        context ?: return

        widgetPeriodUpdate(context)

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

        if (isLastUpdateDateExpired(context)) {
            widgetImmediatelyUpdate(context)

        }

    }

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d("Provider onReceive", "onReceive tick, action: ${intent?.action}")

        super.onReceive(context, intent)

    }


}
