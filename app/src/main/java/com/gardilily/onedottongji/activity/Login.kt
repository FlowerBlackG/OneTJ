package com.gardilily.onedottongji.activity

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.animation.AlphaAnimation
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.gardilily.onedottongji.R
import com.gardilily.onedottongji.tools.GarCloudApi
import com.gardilily.onedottongji.tools.MacroDefines
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.ByteArrayInputStream
import kotlin.concurrent.thread

/** 首页欢迎页面。 */
class Login : Activity() {

    private lateinit var backgroundImageView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        backgroundImageView = findViewById(R.id.login_backgroundImg)
        loadBackgroundImage()

        showVersionInfo()
        autoLoginByLastSessionId()

        findViewById<Button>(R.id.login_button_toUniLogin).setOnClickListener {
            startActivityForResult(
                Intent(this@Login, WebViewUniLogin::class.java),
                MacroDefines.UNILOGIN_WEBVIEW_FOR_1SESSIONID
            )
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        GarCloudApi.checkUpdate(this, false)
    }

    private var backgroundImageBitmap: Bitmap? = null

    /**
     * 设置必应每日壁纸。
     */
    private fun loadBackgroundImage() {
        thread {
            val url = "https://bing.icodeq.com/"
            val client = OkHttpClient()
            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            val resBody = try {
                client.newCall(request).execute().body ?: return@thread
            } catch (_: Exception) {
                return@thread
            }

            val istream = resBody.byteStream()
            backgroundImageBitmap = BitmapFactory.decodeStream(istream)
            istream.close()

            runOnUiThread {
                val fadeInAnim = AlphaAnimation(0f, 1f)
                fadeInAnim.interpolator = DecelerateInterpolator()
                fadeInAnim.duration = 670
                backgroundImageView.startAnimation(fadeInAnim)
                backgroundImageView.setImageBitmap(backgroundImageBitmap)
            }
        }
    }

    /**
     * 尝试使用上次留下的 sessionid 恢复登录。
     */
    private fun autoLoginByLastSessionId() {
        thread {
            val sp = getSharedPreferences(MacroDefines.SHARED_PREFERENCES_STORE_NAME, MODE_PRIVATE)
            val sessionid = sp.getString(MacroDefines.SP_KEY_SESSIONID, "")!!
            if (sessionid.isNotEmpty()) {
                val client = OkHttpClient()
                val req = Request.Builder()
                        .url("https://1.tongji.edu.cn/api/sessionservice/session/getSessionUser")
                        .addHeader("Cookie", "sessionid=$sessionid")
                        .build()

                try {
                    val res = client.newCall(req).execute()
                    if (JSONObject(res.body!!.string()).getInt("code") == 200) {
                        // 登录成功，跳转到首页。
                        val intent = Intent(this@Login, Home::class.java)
                        intent.putExtra("uid", sp.getString(MacroDefines.SP_KEY_USER_ID, "0"))
                                .putExtra("sessionid", sessionid)

                        runOnUiThread {
                            startActivity(intent)
                            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                            finish()
                        }
                    }
                } catch (e: Exception) {
                    // 登录失败。不做任何特殊处理。

                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            MacroDefines.UNILOGIN_WEBVIEW_FOR_1SESSIONID -> {
                Log.d("activityRes=", "$resultCode")

                if (resultCode == MacroDefines.ACTIVITY_RESULT_SUCCESS) {
                    val intent = Intent(this@Login, Home::class.java)
                    intent.putExtra("sessionid", data!!.getStringExtra("sessionid"))

                    val sp = getSharedPreferences(MacroDefines.SHARED_PREFERENCES_STORE_NAME, MODE_PRIVATE)

                    sp.edit()
                        .putString(
                            MacroDefines.SP_KEY_SESSIONID,
                            data.getStringExtra("sessionid")
                        )
                        .apply()

                    runOnUiThread {
                        startActivity(intent)
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                        finish()
                    }
                }

            }
        }
    }

    override fun onDestroy() {
        backgroundImageBitmap?.recycle()
        super.onDestroy()
    }

    /**
     * 显示软件版本信息。
     */
    private fun showVersionInfo() {
        val version = "版本：${packageManager.getPackageInfo(packageName, 0).versionName}" +
                " (${packageManager.getPackageInfo(packageName, 0).longVersionCode})"
        findViewById<TextView>(R.id.login_appVersion).text = version
    }
}
