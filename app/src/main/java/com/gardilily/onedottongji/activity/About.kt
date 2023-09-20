// SPDX-License-Identifier: MulanPSL-2.0
package com.gardilily.onedottongji.activity

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService
import androidx.core.graphics.drawable.RoundedBitmapDrawable
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import com.gardilily.onedottongji.R
import com.gardilily.onedottongji.tools.GarCloudApi
import com.gardilily.onedottongji.tools.tongjiapi.TongjiApi
import com.google.android.material.elevation.SurfaceColors

/** "关于"页面。 */
class About : AppCompatActivity() {

    private lateinit var logoDrawableRound: RoundedBitmapDrawable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val color = SurfaceColors.SURFACE_2.getColor(this)
        window.navigationBarColor = color
        window.statusBarColor = color

        title = resources.getString(R.string.about)

        val versionInfo = makeVersionInfo()

        findViewById<TextView>(R.id.about_versionInfo).text = versionInfo

        logoDrawableRound = RoundedBitmapDrawableFactory.create(resources, BitmapFactory.decodeResource(resources, R.drawable.logo))
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

        prepareBottomTextClickListener()
    }

    private fun makeVersionInfo(): String {
        return packageManager.getPackageInfo(packageName, 0).versionName +
                " (" + packageManager.getPackageInfo(packageName, 0).longVersionCode + ")" +
                "\n" + resources.getString(R.string.app_buildTime)
    }


    private var bottomTextClickCount = 0
    private var bottomTextLastClickTimeMillis = 0L
    /** 两次点击的时间间隔。 */
    private val bottomTextClickIntervalThresholdMillis = 800
    private val bottomTextDebugInfoTriggerThreshold = 5
    private fun prepareBottomTextClickListener() {
        val textView = findViewById<TextView>(R.id.about_bottomText)
        textView.setOnClickListener {
            val currTime = System.currentTimeMillis()
            if (currTime - bottomTextLastClickTimeMillis < bottomTextClickIntervalThresholdMillis) {
                bottomTextClickCount ++
            } else {
                bottomTextClickCount = 1
            }

            bottomTextLastClickTimeMillis = currTime

            if (bottomTextClickCount >= bottomTextDebugInfoTriggerThreshold) {
                bottomTextClickCount = 0
                showDebugInfo()
            }
        }
    }


    private fun showDebugInfo() {
        val tongjiApiToken = TongjiApi.instance.getTokenData()
        val debugMsgBuilder = StringBuilder()

        debugMsgBuilder.append("以下信息仅用于调试使用。请勿将ta们交给任何不可信的人！\n\n")
            .append("tjapi token:\n").append(tongjiApiToken.token).append("\n\n")
            .append("tjapi token expire:\n").append(tongjiApiToken.expireTimeSec).append("\n\n")
            .append("tjapi ref token:\n").append(tongjiApiToken.refreshToken).append("\n\n")
            .append("tjapi ref token exp:\n").append(tongjiApiToken.refreshTokenExpireSec).append("\n\n")
            .append("version info:\n").append(makeVersionInfo().replace("\n", "[\\n]")).append("\n\n")

        AlertDialog.Builder(this)
            .setTitle("调试信息")
            .setMessage(debugMsgBuilder)
            .setPositiveButton("关闭") { v, _ ->
                v.dismiss()
            }
            .setNeutralButton("复制") { _, _ ->
                val clipboard = getSystemService<ClipboardManager>()

                if (clipboard == null) {
                    Toast.makeText(this@About, "复制失败。", Toast.LENGTH_SHORT).show()
                    return@setNeutralButton
                }

                val clip = ClipData.newPlainText("OneTJ Debug", debugMsgBuilder)
                clipboard.setPrimaryClip(clip)

                Toast.makeText(this@About, "已复制。", Toast.LENGTH_SHORT).show()
            }
            .show()

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        finish()
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        logoDrawableRound.bitmap?.recycle()
        super.onDestroy()
    }
}
