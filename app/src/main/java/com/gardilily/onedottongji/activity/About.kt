package com.gardilily.onedottongji.activity

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import com.gardilily.onedottongji.R
import com.gardilily.onedottongji.tools.GarCloudApi

class About : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        val versionInfo = "版本：" + packageManager.getPackageInfo(packageName, 0).versionName +
                "\n编译：" + packageManager.getPackageInfo(packageName, 0).longVersionCode +
                "\n时间：" + resources.getString(R.string.app_buildTime)

        findViewById<TextView>(R.id.about_versionInfo).text = versionInfo

        val logoDrawableRound = RoundedBitmapDrawableFactory.create(resources, BitmapFactory.decodeResource(resources, R.drawable.logo))
        logoDrawableRound.cornerRadius = 36f
        findViewById<RelativeLayout>(R.id.about_appLogo).background = logoDrawableRound

        findViewById<Button>(R.id.about_button_checkUpdate).setOnClickListener {
            GarCloudApi.checkUpdate(this@About, true)
            return@setOnClickListener
        }

        findViewById<Button>(R.id.about_button_viewSource).setOnClickListener {
            val uri = Uri.parse("https://github.com/FlowerBlackG/One-Dot-Tongji")
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
    }
}
