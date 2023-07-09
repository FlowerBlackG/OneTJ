// SPDX-License-Identifier: MulanPSL-2.0
package com.gardilily.onedottongji.activity

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import com.gardilily.onedottongji.R
import com.gardilily.onedottongji.tools.GarCloudApi
import com.google.android.material.elevation.SurfaceColors

/** "关于"页面。 */
class About : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val color = SurfaceColors.SURFACE_2.getColor(this)
        window.navigationBarColor = color
        window.statusBarColor = color

        title = resources.getString(R.string.about)

        val versionInfo = packageManager.getPackageInfo(packageName, 0).versionName +
                " (" + packageManager.getPackageInfo(packageName, 0).longVersionCode + ")" +
                "\n" + resources.getString(R.string.app_buildTime)

        findViewById<TextView>(R.id.about_versionInfo).text = versionInfo

        val logoDrawableRound = RoundedBitmapDrawableFactory.create(resources, BitmapFactory.decodeResource(resources, R.drawable.logo))
        logoDrawableRound.cornerRadius = 36f
        findViewById<RelativeLayout>(R.id.about_appLogo).background = logoDrawableRound

        findViewById<Button>(R.id.about_button_checkUpdate).setOnClickListener {
            GarCloudApi.checkUpdate(this@About, true)
            return@setOnClickListener
        }

        findViewById<Button>(R.id.about_button_viewSource).setOnClickListener {
            val uri = Uri.parse("https://github.com/FlowerBlackG/OneDotTongji")
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        finish()
        return super.onOptionsItemSelected(item)
    }
}
