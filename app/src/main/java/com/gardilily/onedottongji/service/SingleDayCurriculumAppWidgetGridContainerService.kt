// SPDX-License-Identifier: MulanPSL-2.0

package com.gardilily.onedottongji.service

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.gardilily.onedottongji.R

class SingleDayCurriculumAppWidgetGridContainerService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent?): RemoteViewsFactory {
        return SingleDayCurriculumAppWidgetGridContainerRemoteViewsFactory(this, intent!!)
    }

    data class CourseInfo(
        var name: String = "",
        var timeStart: Int = 1,
        var timeEnd: Int = 1,
        var courseNumber: String = "",
        var teacher: String = "",
    )

    protected class SingleDayCurriculumAppWidgetGridContainerRemoteViewsFactory(
        val context: Context,
        val intent: Intent
    ) : RemoteViewsService.RemoteViewsFactory {
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)

        val infoList = ArrayList<CourseInfo>()

        override fun onCreate() {

        }

        override fun onDataSetChanged() {

        }

        override fun onDestroy() {

        }

        override fun getCount(): Int {
            return infoList.size + 10
        }

        override fun getViewAt(position: Int): RemoteViews {
            val remoteView = RemoteViews(context.packageName, R.layout.appwidget_singledaycurriculum_item)
            remoteView.setTextViewText(R.id.appwidget_singledaycurriculum_grid_item, "${System.currentTimeMillis()}") // todo

            return remoteView
        }

        override fun getLoadingView(): RemoteViews? {
            return null
        }

        override fun getViewTypeCount(): Int {
            return 1
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun hasStableIds(): Boolean {
            return true
        }


    }



    override fun onDestroy() {


        super.onDestroy()
    }
}
