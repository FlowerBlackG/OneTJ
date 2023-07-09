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
import androidx.appcompat.app.AppCompatActivity
import com.gardilily.onedottongji.R
import com.gardilily.onedottongji.tools.tongjiapi.TongjiApi
import com.google.android.material.elevation.SurfaceColors

/**
 *
 *
 * activity 信息返回格式：
 *   oauthResult: Array<String?>(error, code)
 *     err 非空，表示登录失败。
 *     code 非空，可用 code 换结果。
 */
class TongjiOAuth : AppCompatActivity() {

    companion object {
        const val RESULT_KEY = "oauthResult"
    }

    private lateinit var webView : WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_tongji_oauth)

        val color = SurfaceColors.SURFACE_2.getColor(this)
        window.navigationBarColor = color
        window.statusBarColor = color
        title = "同济大学开放平台 - 身份认证"

        initWebView()
        loadOAuthPage()

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

                    setResult(0, Intent().putExtra(RESULT_KEY, arrayOf(error, code)))
                    finish()

                    return true // 防止白屏。
                }

                return super.shouldOverrideUrlLoading(view, request)
            }
        }

        webView.webChromeClient = object : WebChromeClient() {}
    }

    private fun loadOAuthPage() {
        // https://api.tongji.edu.cn/docs/#/architecture/authentication
        val urlBuilder = StringBuilder("${TongjiApi.BASE_URL}/keycloak/realms/OpenPlatform/protocol/openid-connect/auth")

        urlBuilder.append("?redirect_uri=${TongjiApi.OAUTH_REDIRECT_URL}")
        urlBuilder.append("&client_id=${TongjiApi.CLIENT_ID}")
        urlBuilder.append("&response_type=code")

        val scopeBuilder = StringBuilder()
        TongjiApi.SCOPE_LIST.forEach {
            if (it != TongjiApi.SCOPE_LIST.first()) {
                scopeBuilder.append(' ')
            }

            scopeBuilder.append(it)
        }

        urlBuilder.append("&scope=$scopeBuilder")

        webView.loadUrl(urlBuilder.toString())

    }




}