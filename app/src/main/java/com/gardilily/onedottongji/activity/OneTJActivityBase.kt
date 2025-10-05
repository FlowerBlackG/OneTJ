// SPDX-License-Identifier: MulanPSL-2.0

package com.gardilily.onedottongji.activity

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.elevation.SurfaceColors

/**
 * OneTJ基类
 *
 * @param hasTitleBar 是否有标题栏。默认为`ture`
 * @param activityTitle 标题。默认为`null`
 * @param backOnTitleBar 是否启用返回键。默认为`false`
 * @param withSpinning 是否准备加载动画。默认为`false`
 */
open class OneTJActivityBase(
    protected val hasTitleBar: Boolean = true,
    protected val activityTitle: String? = null,
    protected val backOnTitleBar: Boolean = false,
    protected val withSpinning: Boolean = false
) : AppCompatActivity() {

    private var spinningProgressBar: ProgressBar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val color = if (hasTitleBar) {
            SurfaceColors.SURFACE_2.getColor(this)
        } else {
            SurfaceColors.SURFACE_0.getColor(this)
        }

        setSystemBarsColor(window, color, color.isLightColor() )

        if (activityTitle != null) {
            title = activityTitle
        }

        if (backOnTitleBar) {
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }

        if (withSpinning) {
            val pg = ProgressBar(this)
            val attr = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            attr.addRule(RelativeLayout.CENTER_IN_PARENT)
            pg.layoutParams = attr
            pg.visibility = View.GONE
            spinningProgressBar = pg
        }


    }

    fun stageSpinningProgressBar(view: ViewGroup) {
        view.addView(spinningProgressBar)
    }

    fun setSpinning(spinning: Boolean) {
        spinningProgressBar?.visibility = if (spinning) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        finish()
        return super.onOptionsItemSelected(item)
    }
    fun setSystemBarsColor(window: android.view.Window, color: Int, isLight: Boolean) {
        window.statusBarColor = color
        window.navigationBarColor = color

        val controller = WindowInsetsControllerCompat(window, window.decorView)

        controller.isAppearanceLightStatusBars = isLight
        controller.isAppearanceLightNavigationBars = isLight
    }

    fun Int.isLightColor(): Boolean {
        val r = this shr 16 and 0xFF
        val g = this shr 8 and 0xFF
        val b = this and 0xFF
        val luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255
        return luminance > 0.5
    }

}