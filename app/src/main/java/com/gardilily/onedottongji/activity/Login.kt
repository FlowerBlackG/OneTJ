package com.gardilily.onedottongji.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import com.gardilily.onedottongji.R
import com.gardilily.onedottongji.tools.GarCloudApi
import com.gardilily.onedottongji.tools.MacroDefines
import okhttp3.*
import org.json.JSONObject
import java.util.*
import kotlin.concurrent.thread

class Login : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

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

    private fun showVersionInfo() {
        val version = "版本：${packageManager.getPackageInfo(packageName, 0).versionName}" +
                " (${packageManager.getPackageInfo(packageName, 0).longVersionCode})"
        findViewById<TextView>(R.id.login_appVersion).text = version
    }
}
