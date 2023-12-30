// SPDX-License-Identifier: MulanPSL-2.0

package com.gardilily.onedottongji.appwidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.RemoteViews
import com.gardilily.onedottongji.R
import com.gardilily.onedottongji.service.SingleDayCurriculumAppWidgetGridContainerService

class SingleDayCurriculumAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context?,
        appWidgetManager: AppWidgetManager?,
        appWidgetIds: IntArray?
    ) {
        context ?: return
        appWidgetManager ?: return
        appWidgetIds ?: return

        Log.d("single day curriculum app widget", "onUpdate tick")

        appWidgetIds.forEach { appWidgetId ->
            val views = RemoteViews(context.packageName, R.layout.appwidget_single_day_curriculum)

            val intent = Intent(context, SingleDayCurriculumAppWidgetGridContainerService::class.java)
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)

            intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)))

            views.setRemoteAdapter(R.id.appwidget_single_day_curriculum_card_container, intent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

    }

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)

        Log.d("single day curri widget provider", "onReceive tick")
    }

}
