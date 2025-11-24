// SPDX-License-Identifier: MulanPSL-2.0

package com.gardilily.onedottongji.activity

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.elevation.SurfaceColors

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

        window.statusBarColor = color
        window.navigationBarColor = color

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

}