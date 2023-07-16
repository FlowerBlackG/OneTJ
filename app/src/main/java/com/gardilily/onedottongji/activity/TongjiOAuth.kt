// SPDX-License-Identifier: MulanPSL-2.0
package com.gardilily.onedottongji.activity

import android.content.Intent
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.gardilily.onedottongji.R
import com.gardilily.onedottongji.tools.tongjiapi.TongjiApi
import com.google.android.material.elevation.SurfaceColors
import kotlin.concurrent.thread

/**
 *
 * Intent 传入：
 *   scope: String 需要授权的项目。之间用空格分开。
 *
 * activity 信息返回格式：
 *   oauthResult: Array<String?>(error, code)
 *     err 非空，表示登录失败。
 *     code 非空，可用 code 换结果。
 */
class TongjiOAuth : OneTJActivityBase(
    hasTitleBar = false,
    withSpinning = true
) {

    companion object {
        /**
         * 需要授权的选项。
         * String
         */
        const val INTENT_PARAM_SCOPE = "scope"

        /**
         * 授权错误信息。
         * String
         */
        const val RESULT_ERROR = "oauthError"

        /**
         * 授权得到的 code。
         * String
         */
        const val RESULT_AUTH_CODE = "oauthCode"

    }

    private lateinit var webView : WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tongji_oauth)

        stageSpinningProgressBar(findViewById(R.id.tongjiOAuth_rootContainer))

        val scope = intent.getStringExtra(INTENT_PARAM_SCOPE)

        initWebView()
        loadOAuthPage(scope)

    }

    private fun initWebView() {

        webView = findViewById(R.id.tongjiOAuth_webView)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true

        webView.settings.cacheMode = WebSettings.LOAD_DEFAULT

        val cookieMgr = CookieManager.getInstance()
        cookieMgr.setAcceptCookie(true)
        if (TongjiApi.instance.switchAccountRequired) {
            TongjiApi.instance.switchAccountRequired = false
            cookieMgr.removeAllCookies(null)
            cookieMgr.flush()
        }

        webView.settings.setSupportZoom(true)

        webView.webViewClient = object : WebViewClient() {

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {

                request ?: return false

                if (request.url.toString().startsWith(TongjiApi.OAUTH_REDIRECT_URL)) {

                    val uri = request.url
                    val code = uri.getQueryParameter("code")
                    val error = uri.getQueryParameter("error")

                    val resIntent = Intent()
                    resIntent.putExtra(RESULT_AUTH_CODE, code)
                    resIntent.putExtra(RESULT_ERROR, error)

                    code?.let {

                        thread {
                            val res = TongjiApi.instance.code2token(it, this@TongjiOAuth)


                            runOnUiThread {
                                if (!res) {

                                    Toast.makeText(
                                        this@TongjiOAuth,
                                        "授权失败（code2token）",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    resIntent.putExtra(RESULT_ERROR, "code2token returns false")
                                }

                                setResult(0, resIntent)
                                finish()

                            } // runOnUiThread {
                        } // thread {
                    } // code?.let {

                    return true // 防止白屏。
                }

                return super.shouldOverrideUrlLoading(view, request)
            }
        }

        webView.webChromeClient = object : WebChromeClient() {}
    }

    private fun loadOAuthPage(scope: String?) {
        // https://api.tongji.edu.cn/docs/#/architecture/authentication
        val urlBuilder = StringBuilder("${TongjiApi.BASE_URL}/keycloak/realms/OpenPlatform/protocol/openid-connect/auth")

        urlBuilder.append("?redirect_uri=${TongjiApi.OAUTH_REDIRECT_URL}")
        urlBuilder.append("&client_id=${TongjiApi.CLIENT_ID}")
        urlBuilder.append("&response_type=code")

        val scopeBuilder = StringBuilder()
        if (scope == null) {
            TongjiApi.SCOPE_LIST.forEach {
                if (it != TongjiApi.SCOPE_LIST.first()) {
                    scopeBuilder.append(' ')
                }

                scopeBuilder.append(it)
            }
        }

        urlBuilder.append("&scope=${scope ?: scopeBuilder}")

        webView.loadUrl(urlBuilder.toString())

    }




}