// SPDX-License-Identifier: MulanPSL-2.0
package com.gardilily.onedottongji.view.coursetable

import android.app.Activity
import android.content.Context
import android.widget.RelativeLayout
import com.gardilily.onedottongji.R

class CourseTableContainer(context: Context) {
    val layout: RelativeLayout = RelativeLayout(context)

    private val activity = context as Activity
    private val spMultiply = layout.resources.displayMetrics.scaledDensity

    init {
        layout.background = context.getDrawable(R.drawable.shape_login_page_box)
        val params = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            (100 * spMultiply).toInt()
        )
        layout.layoutParams = params

        layout.isClickable = true
    }
}
