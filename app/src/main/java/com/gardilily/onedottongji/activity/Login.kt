package com.gardilily.onedottongji.activity

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.util.Base64
import android.util.Log
import android.widget.*
import com.gardilily.onedottongji.R
import com.gardilily.onedottongji.tools.MacroDefines
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.thread


class Login : Activity() {
    private class Cookie(var name: String, var value: String)
    private val cookieList = ArrayList<Cookie>()

    private fun storeCookie(name: String, value: String) {
        cookieList.forEach {
            Log.d("Login.CookieStore", "Comparing ${it.name} with $name")
            if (it.name == name) {
                it.value = value
                return
            }
        }

        cookieList.add(Cookie(name, value))
    }

    private fun getCookieStr(): String {
        var ret = ""
        for (i in 0 until cookieList.size) {
            if (i > 0) {
                ret += ';'
            }
            ret += "${cookieList[i].name}=${cookieList[i].value}"
        }
        return ret
    }

    private fun getSingleCookie(tar: String): String {
        for (i in 0 until cookieList.size) {
            if (cookieList[i].name == tar) {
                return cookieList[i].value
            }
        }
        return ""
    }

    private fun storeCookies(str: List<String>?) {
        if (str == null) {
            Log.d("Login.storeCookies", "str == null")
            return
        }
        str.forEach {
            val sub = it.split('=')
            val name = sub[0]
            val value = sub[1].substring(0,
                    if (sub[1].contains(';')) sub[1].indexOf(';') else sub[1].length)
            storeCookie(name, value)
        }
    }

    private lateinit var uniHttpClient: OkHttpClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        uniHttpClient = OkHttpClient().newBuilder()
                .followSslRedirects(false)
                .followRedirects(false)
                .build()

        //checkUpdate()

        showVersionInfo()

        autoLoginByLastSessionId()

        findViewById<Button>(R.id.login_button_confirm).setOnClickListener {
            login()
        }

        val sp = getSharedPreferences(MacroDefines.SHARED_PREFERENCES_STORE_NAME, MODE_PRIVATE)

        findViewById<EditText>(R.id.login_input_uid).text =
                SpannableStringBuilder(sp.getString(MacroDefines.SP_KEY_USER_ID, ""))

        findViewById<EditText>(R.id.login_input_upw).text =
                SpannableStringBuilder(sp.getString(MacroDefines.SP_KEY_USER_PW, ""))
    }

    override fun onResume() {
        super.onResume()
        initLoginPage()
    }


    private fun initLoginPage() {
        thread {
            try {
                val request = Request.Builder()
                        .url("https://ids.tongji.edu.cn:8443/nidp/app/login?" +
                                "id=180&sid=0&option=credential&sid=0")
                        .get()
                        .build()
                val call = uniHttpClient.newCall(request)
                val initStep1Response = call.execute()

                val initStep1CookieStr = initStep1Response.headers.toMultimap()["Set-Cookie"]

                Log.d("Login.initLoginPage.step1", initStep1CookieStr.toString())
                storeCookies(initStep1CookieStr!!)

                val codeUrlRequest = Request.Builder()
                        .url("https://ids.tongji.edu.cn:8443/nidp/app/login?" +
                                "sid=0&sid=0&flag=true")
                        .get()
                        .addHeader("Cookie", getCookieStr())
                        .build()

                val codeUrlResponse = uniHttpClient.newCall(codeUrlRequest).execute()

                val base64ori = codeUrlResponse.body!!.string()
                val initStep2CookieStr = codeUrlResponse.headers.toMultimap()["Set-Cookie"]

                storeCookies(initStep2CookieStr)

                Log.d("Login.initLoginPage", base64ori)

                val base64str = base64ori.substring(base64ori.indexOf(',') + 1,
                        base64ori.length -
                                if (base64ori.contains('\n'))
                                    1
                                else
                                    0)

                Log.d("Login.initLoginPage.base64str", base64str)
                val decodedString = Base64.decode(base64str, Base64.DEFAULT)
                val decodedByte = BitmapFactory.decodeByteArray(decodedString,
                        0,
                        decodedString.size)
                runOnUiThread {
                    findViewById<ImageView>(R.id.login_image_captcha).setImageBitmap(decodedByte)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "状态异常。请检查网络并重启 App...",
                            Toast.LENGTH_SHORT)
                            .show()
                }
            }
        }
    }

    private fun login() {
        val uid = findViewById<EditText>(R.id.login_input_uid).text.toString()
        val upw = findViewById<EditText>(R.id.login_input_upw).text.toString()
        val captcha = findViewById<EditText>(R.id.login_input_captcha).text.toString()

        thread {
            try {
                val loginUrl = "https://ids.tongji.edu.cn:8443/nidp/app/login?sid=0&sid=0"

                val loginRequestFormBody = FormBody.Builder()
                        .add("option", "credential")
                        .add("Ecom_User_ID", uid)
                        .add("Ecom_Password", upw)
                        .add("Ecom_code", captcha)
                        .build()

                val loginRequest = Request.Builder()
                        .url(loginUrl)
                        .post(loginRequestFormBody)
                        .addHeader("Cookie", getCookieStr())
                        .build()

                val loginReqResponse = uniHttpClient.newCall(loginRequest).execute()

                val loginResHeadersCookies = loginReqResponse.headers.toMultimap()["Set-Cookie"]

                storeCookies(loginResHeadersCookies)

                Log.d("Login.login.step1.loginResHeaders", loginResHeadersCookies.toString())

                val authzUrl = "https://ids.tongji.edu.cn:8443/nidp/oauth/nam/authz?" +
                        "scope=profile" +
                        "&response_type=code" +
                        "&redirect_uri=" +
                        URLEncoder.encode("https://1.tongji.edu.cn/api/ssoservice/system/loginIn",
                                "UTF-8") +
                        "&client_id=5fcfb123-b94d-4f76-89b8-475f33efa194"

                val authzRequest = Request.Builder()
                        .url(authzUrl)
                        .addHeader("Cookie", getCookieStr())
                        .addHeader("Referer", loginUrl)
                        //.addHeader("Accept",
                        //        "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9")
                        .addHeader("Connection", "keep-alive")
                        //.addHeader("Accept-Encoding", "gzip, deflate, br")
                        .addHeader("Host", "ids.tongji.edu.cn:8443")
                        //.addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 11_2_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.82 Safari/537.36")
                        .build()

                val authzResponse = uniHttpClient.newCall(authzRequest).execute()

                val authzRes = authzResponse.headers.toString()
                Log.d("Login.login.step2.authzResHeaders", authzRes)

                val authzCookies = authzResponse.headers.toMultimap()["Set-Cookie"]
                storeCookies(authzCookies)
                val authzRedirectUrl = authzResponse.headers.toMultimap()["Location"]!![0]
                Log.d("Login.login.authzRedirect", authzRedirectUrl)

                val firstRedirectRequest = Request.Builder()
                        .url(authzRedirectUrl)
                        .addHeader("Cookie", getCookieStr())
                        .get()
                        .build()
                val firstRedirectResponse = uniHttpClient.newCall(firstRedirectRequest).execute()
                val firstRedirectCookies = firstRedirectResponse.headers.toMultimap()["Set-Cookie"]
                storeCookies(firstRedirectCookies)

                val second302url = firstRedirectResponse.headers.toMultimap()["Location"]!![0]
                val token = second302url.substring(second302url.indexOf('=') + 1,
                        second302url.indexOf('&'))

                val ts = second302url.substring(second302url.indexOf("ts") + 3)
                val second302form = FormBody.Builder()
                        .add("token", token)
                        .add("ts", ts)
                        .add("uid", uid)
                        .build()

                val second302tarUrl = "https://1.tongji.edu.cn/api/sessionservice/session/login"
                val second302request = Request.Builder()
                        .url(second302tarUrl)
                        .post(second302form)
                        .addHeader("Cookie", getCookieStr())
                        .build()
                val second302response = uniHttpClient.newCall(second302request).execute()
                storeCookies(second302response.headers.toMultimap()["Set-Cookie"]!!)

                val sessionid = getSingleCookie("sessionid")
                Log.d("Login.login.final.sessionid", sessionid)

                val intent = Intent(this@Login, Home::class.java)
                intent.putExtra("uid", uid)
                        .putExtra("sessionid", sessionid)

                val sp = getSharedPreferences(MacroDefines.SHARED_PREFERENCES_STORE_NAME, MODE_PRIVATE)

                sp.edit().putString(MacroDefines.SP_KEY_USER_ID, uid)
                        .putString(MacroDefines.SP_KEY_USER_PW, upw)
                        .putString(MacroDefines.SP_KEY_SESSIONID, sessionid)
                        .apply()

                runOnUiThread {
                    startActivity(intent)
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    finish()
                }

            } catch (e: Exception) {
                initLoginPage()
                runOnUiThread {
                    Toast.makeText(this, "登录失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
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

    private fun showVersionInfo() {
        var version = "版本：${packageManager.getPackageInfo(packageName, 0).versionName}" +
                " (${packageManager.getPackageInfo(packageName, 0).longVersionCode})"
        findViewById<TextView>(R.id.login_appVersion).text = version
    }
}
