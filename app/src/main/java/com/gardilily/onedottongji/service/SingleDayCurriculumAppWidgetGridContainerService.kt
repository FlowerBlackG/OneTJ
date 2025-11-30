// SPDX-License-Identifier: MulanPSL-2.0

package com.gardilily.onedottongji.service

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.gardilily.onedottongji.R

class SingleDayCurriculumAppWidgetGridContainerService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent?): RemoteViewsFactory {
        return SingleDayCurriculumAppWidgetGridContainerRemoteViewsFactory(this, intent!!)
    }
    companion object{
        @Volatile
        var infoList: List<CourseInfo> = emptyList()
        @Volatile
        var isDataLoaded: Boolean = false
    }

    data class CourseInfo(
        var name: String = "",
        var timeStart: Int = 1,
        var timeEnd: Int = 1,
        var courseNumber: String = "",
        var teacher: String = "",
        var room: String = ""
    )

    protected class SingleDayCurriculumAppWidgetGridContainerRemoteViewsFactory(
        val context: Context,
        val intent: Intent
    ) : RemoteViewsFactory {
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)


        override fun onCreate() {

        }

        override fun onDataSetChanged() {

        }

        override fun onDestroy() {

        }

        override fun getCount(): Int {
            return infoList.size
        }

        override fun getViewAt(position: Int): RemoteViews {
            val remoteView = RemoteViews(context.packageName, R.layout.appwidget_singledaycurriculum_item)

            val course = infoList[position]
            remoteView.setTextViewText(R.id.widget_course_name, course.name)
            remoteView.setTextViewText(R.id.widget_course_room, course.room)
            remoteView.setTextViewText(R.id.widget_course_teacher, course.teacher)
            remoteView.setTextViewText(R.id.widget_course_time, "${course.timeStart}-${course.timeEnd}")

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

