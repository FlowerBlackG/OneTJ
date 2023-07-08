// SPDX-License-Identifier: MulanPSL-2.0
package com.gardilily.onedottongji.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.webkit.*
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.view.setMargins
import com.gardilily.onedottongji.R
import com.gardilily.onedottongji.tools.MacroDefines

/** 同济大学统一身份认证登录浏览器。 */
class WebViewUniLogin : Activity() {

    private lateinit var webView : WebView
    private lateinit var cookieManager: CookieManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("WebViewUniLogin", "LifeCycle:onCreate")

        setContentView(R.layout.activity_webview_unilogin)

        cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.removeAllCookies(null)
        cookieManager.flush()

        webView = findViewById(R.id.webViewUniLogin_webView)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.cacheMode = WebSettings.LOAD_NO_CACHE

        webView.settings.useWideViewPort = true
        webView.settings.setSupportZoom(true)
        webView.setInitialScale(100)

        // 使用电脑页面登录，防止缩放问题。
        webView.settings.userAgentString =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.114 Safari/537.36"

        webView.webViewClient = object : WebViewClient() {
            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                view!!.loadUrl("about:blank")
                showNetworkErrorView()
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                var cookieStr = CookieManager.getInstance().getCookie("https://1.tongji.edu.cn")
                Log.d("WebViewUniLogin:ProgressChange:CookieStr", "res=|$cookieStr|")

                if (cookieStr != null && cookieStr.isNotEmpty()) {
                    cookieStr = cookieStr.replace(" ", "")
                        .replace("\n", "")
                        .replace("\r", "")

                    Log.d("WebViewUniLogin:ProgressChange:CookieStr:phase2",
                        "res=|$cookieStr|"
                    )

                    val cookies = cookieStr.split(';')
                    cookies.forEach {
                        val (cName, cValue) = it.split('=').map { element -> element }
                        if (cName == "sessionid") {
                            setResult(
                                MacroDefines.ACTIVITY_RESULT_SUCCESS,
                                Intent().putExtra("sessionid", cValue)
                            )
                            finish()
                        }
                    }
                }
            }
        }

        webView.loadUrl("https://1.tongji.edu.cn")

    }

    /**
     * 登录页面加载失败时，展现这个页面。
     */
    private fun showNetworkErrorView() {
        val mainLayout = RelativeLayout(this)
        val mainLayoutParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.MATCH_PARENT
        )
        mainLayout.layoutParams = mainLayoutParams
        mainLayout.background = getDrawable(R.drawable.shape_page_background_gradient)

        val msgText = TextView(this)
        msgText.text = "登录页加载失败 _(:τ」∠)_" +
                "\n\n可能的原因：" +
                "\n1. 你的设备没有连接到网络" +
                "\n2. 设备未连接到同济校园网" +
                "\n3. 同济一块钱系统出现异常" +
                "\n4. 同济统一身份认证站异常"
        val msgTxtParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        )
        msgTxtParams.addRule(RelativeLayout.CENTER_IN_PARENT)
        msgText.layoutParams = msgTxtParams
        msgText.textSize = 24f
        mainLayout.addView(msgText)

        val exitButton = Button(this)
        exitButton.background = getDrawable(R.drawable.shape_login_page_box)
        exitButton.text = "返回"
        exitButton.textSize = 18f
        val exitButtonParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        )
        exitButtonParams.setMargins((22f * resources.displayMetrics.scaledDensity).toInt())
        exitButtonParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
        exitButton.layoutParams = exitButtonParams
        exitButton.setOnClickListener {
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
        mainLayout.addView(exitButton)

        this.setContentView(mainLayout)
    }
}
