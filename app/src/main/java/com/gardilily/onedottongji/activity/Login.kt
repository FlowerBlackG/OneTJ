// SPDX-License-Identifier: MulanPSL-2.0
package com.gardilily.onedottongji.activity

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.gardilily.onedottongji.R
import com.gardilily.onedottongji.tools.GarCloudApi
import com.gardilily.onedottongji.tools.MacroDefines
import com.gardilily.onedottongji.tools.tongjiapi.TongjiApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/** 首页欢迎页面。 */
class Login : Activity() {

    private lateinit var backgroundImageView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val tryLoginSuccess = AtomicBoolean(false)

        thread {
            TongjiApi.instance.refreshAccessToken()
            if (!tryLoginSuccess.get()) {
                runOnUiThread { tryAutoLogin() }
            }
        }

        tryLoginSuccess.set(tryAutoLogin())
        if (tryLoginSuccess.get()) {
            return
        }

        setContentView(R.layout.activity_login)

        backgroundImageView = findViewById(R.id.login_backgroundImg)
        loadBackgroundImage()
        showVersionInfo()

        initJoinQQGroupButton(findViewById(R.id.login_button_joinQQGroup))

        findViewById<Button>(R.id.login_button_toUniLogin).setOnClickListener {
            startActivityForResult(
                Intent(this@Login, TongjiOAuth::class.java),
                MacroDefines.UNILOGIN_WEBVIEW_FOR_1SESSIONID
            )

        }

        GarCloudApi.checkUpdate(this, false)
    }

    companion object {
        const val SP_KEY_BACKGROUND_BASE64 = "login.bg-base64"
    }


    private fun initJoinQQGroupButton(btn: Button) {
        btn.setOnClickListener {

            val imgView = ImageView(this)
            imgView.setImageResource(R.drawable.qq_group_qrcode)

            AlertDialog.Builder(this)
                .setTitle("加入QQ群")
                .setMessage("群号：322324184")
                .setPositiveButton("好") { _, _ ->

                }
                .setNeutralButton("链接加群") { _, _ ->
                    val groupUrl = "http://qm.qq.com/cgi-bin/qm/qr?_wv=1027&k=YIF3M8HCGgW4_q6J4XjOrres3aaLhPsm&authKey=L%2F29m%2Bc8HmnYWupK%2F7dzAlptgdDc3DoBhKZ7p3BJw4NOufa1dAo4QsgCUzBKdJ8C&noverify=0&group_code=322324184"
                    val uri = Uri.parse(groupUrl)
                    this@Login.startActivity(Intent(Intent.ACTION_VIEW, uri))
                }
                .setView(imgView)
                .setCancelable(true)
                .show()
        }
    }

    private fun tryAutoLogin(): Boolean {
        if (TongjiApi.instance.tokenAvailable()) {
            startActivity(Intent(this, Home::class.java))
            finish()
            return true
        }

        return false
    }
    private fun setUISpinning(loading: Boolean) {

        val pb = findViewById<ProgressBar>(R.id.login_loading_progressBar)
        val btn = findViewById<Button>(R.id.login_button_toUniLogin)

        if (loading) {
            pb.visibility = View.VISIBLE
            btn.visibility = View.GONE
        } else {
            pb.visibility = View.GONE
            btn.visibility = View.VISIBLE
        }
    }

    private var backgroundImageBitmap: Bitmap? = null


    private val switchBackgroundLock = Semaphore(1, 0)
    private val spBackgroundLock = Semaphore(1, 0)

    private fun stageBackground(bitmap: Bitmap?) {

        if (bitmap == null) {
            // 无法呈现图片。
            return
        }

        runBlocking {
            switchBackgroundLock.acquire()
        }

        runOnUiThread {
            val fadeInAnim = AlphaAnimation(0f, 1f)
            fadeInAnim.interpolator = DecelerateInterpolator()
            fadeInAnim.duration = 670
            backgroundImageView.startAnimation(fadeInAnim)
            backgroundImageView.setImageBitmap(bitmap)

            switchBackgroundLock.release()
        }
    }

    private fun fetchNewBackground() {
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

            val bytes = resBody.bytes()
            backgroundImageBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            val base64 = Base64.encode(bytes, Base64.DEFAULT)
            val base64Str = String(base64)

            runBlocking {
                spBackgroundLock.acquire()
            }
            getSharedPreferences(MacroDefines.SHARED_PREFERENCES_STORE_NAME, MODE_PRIVATE)
                .edit()
                .putString(SP_KEY_BACKGROUND_BASE64, base64Str)
                .apply()
            spBackgroundLock.release()

            stageBackground(backgroundImageBitmap)
        }
    }

    private fun loadOldBackground() {
        thread {
            val sp = getSharedPreferences(MacroDefines.SHARED_PREFERENCES_STORE_NAME, MODE_PRIVATE)

            runBlocking {
                spBackgroundLock.acquire()
            }

            val base64 = sp.getString(SP_KEY_BACKGROUND_BASE64, null)
            spBackgroundLock.release()

            if (base64 == null) {
                return@thread
            }

            val decoded = Base64.decode(base64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.size)

            stageBackground(bitmap)

        }
    }

    /**
     * 设置必应每日壁纸。
     */
    private fun loadBackgroundImage() {
        loadOldBackground()
        fetchNewBackground()
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

                data ?: return

                val error = data.getStringExtra(TongjiOAuth.RESULT_ERROR)

                if (error == null) {
                    startActivity(Intent(this@Login, Home::class.java))
                    finish()
                }

            } // MacroDefines.UNILOGIN_WEBVIEW_FOR_1SESSIONID ->
        } // when (requestCode)
    } // override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)

    override fun onDestroy() {
        backgroundImageBitmap?.recycle()
        super.onDestroy()
    }

    /**
     * 显示软件版本信息。
     */
    private fun showVersionInfo() {
        val version = "${resources.getString(R.string.version)}: ${packageManager.getPackageInfo(packageName, 0).versionName}" +
                " (${packageManager.getPackageInfo(packageName, 0).longVersionCode})"
        findViewById<TextView>(R.id.login_appVersion).text = version
    }
}
