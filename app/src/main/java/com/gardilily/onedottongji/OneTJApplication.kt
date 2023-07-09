// SPDX-License-Identifier: MulanPSL-2.0

package com.gardilily.onedottongji

import android.app.Application
import com.gardilily.onedottongji.tools.tongjiapi.TongjiApi
import com.google.android.material.color.DynamicColors

class OneTJApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Material You 调色板
        DynamicColors.applyToActivitiesIfAvailable(this)
        TongjiApi.instance.init(this)
    }

}