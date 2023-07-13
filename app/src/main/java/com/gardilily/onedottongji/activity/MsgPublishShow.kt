// SPDX-License-Identifier: MulanPSL-2.0
package com.gardilily.onedottongji.activity

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.text.Html
import android.util.Base64
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.text.HtmlCompat
import com.gardilily.onedottongji.R
import com.gardilily.onedottongji.service.BackgroundDownload
import com.gardilily.onedottongji.tools.MacroDefines
import com.gardilily.onedottongji.tools.Utils
import com.gardilily.onedottongji.tools.Utils.Companion.isReqSessionAvailable
import com.gardilily.onedottongji.tools.tongjiapi.TongjiApi
import com.gardilily.onedottongji.view.MsgPublishShowAttachCard
import com.google.android.material.button.MaterialButtonToggleGroup
import okhttp3.*
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder
import kotlin.concurrent.thread

class MsgPublishShow : OneTJActivityBase(
    backOnTitleBar = true,
    hasTitleBar = true,
    withSpinning = true
) {
    private lateinit var basicDataObj: JSONObject
    private lateinit var contentDataJson: JSONObject

    private lateinit var contentWebView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_msg_publish_show)

        prepareContentWebView()

        stageSpinningProgressBar(findViewById(R.id.msgPublishShow_rootContainer))

        basicDataObj = JSONObject(intent.getStringExtra("basicDataObj")!!)


        findViewById<TextView>(R.id.msgPublishShow_createUser).text =
                basicDataObj.getString("createUser")

        findViewById<TextView>(R.id.msgPublishShow_publishTime).text =
                basicDataObj.getString("publishTime")

        fetchAndShowContentAndAttach()

        title = basicDataObj.getString("title")

        val renderBackendSelect = findViewById<MaterialButtonToggleGroup>(R.id.msgPublishShow_renderBackendSelect)
        renderBackendSelect.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (checkedId == R.id.msgPublishShow_renderBackendSelect_textView && isChecked) {
                switchRenderBackend(useTextView = true)
            } else if (checkedId == R.id.msgPublishShow_renderBackendSelect_webView && isChecked) {
                switchRenderBackend(useWebView = true)
            }
        }
    }

    private fun prepareContentWebView() {
        contentWebView = findViewById(R.id.msgPublishShow_contentWebView)
        val settings = contentWebView.settings
        settings.setSupportZoom(false)
        settings.builtInZoomControls = false
        settings.displayZoomControls = false
    }

    private fun String.toMobileFriendlyHtml(): String {
        val head = "<head>" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, user-scalable=no\"> " +
                "<style>img{max-width: 100%; width:100%; height:auto;}*{margin:0px;}</style>" +
                "</head>";
        return "<html>$head<body>$this</body></html>";
    }

    private fun switchRenderBackend(useWebView: Boolean = false, useTextView: Boolean = false) {
        if (useWebView) {
            findViewById<WebView>(R.id.msgPublishShow_contentWebView).visibility = View.VISIBLE
            findViewById<TextView>(R.id.msgPublishShow_content).visibility = View.GONE
        } else {
            findViewById<WebView>(R.id.msgPublishShow_contentWebView).visibility = View.GONE
            findViewById<TextView>(R.id.msgPublishShow_content).visibility = View.VISIBLE
        }
    }


    private fun fetchAndShowContentAndAttach() {
        setSpinning(true)
        thread {

            contentDataJson = TongjiApi.instance.getOneTongjiMessageDetail(
                this@MsgPublishShow,
                basicDataObj.getInt("id")
            ) ?: return@thread

            runOnUiThread {
                setSpinning(false)

                val web = contentWebView

                web.loadDataWithBaseURL(
                    null,
                    contentDataJson.getString("content").toMobileFriendlyHtml(),
                    "text/html",
                    "utf-8",
                    null
                )

                val tv = findViewById<TextView>(R.id.msgPublishShow_content)
                tv.text = HtmlCompat.fromHtml(
                    contentDataJson.getString("content"),
                    HtmlCompat.FROM_HTML_OPTION_USE_CSS_COLORS or HtmlCompat.FROM_HTML_MODE_COMPACT,
                    { source ->
                        val base64str = source.substring(
                            source.indexOf(',') + 1,
                            source.length - (if (source.contains('\n')) 1 else 0)
                        )

                        val data = Base64.decode(base64str, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
                        val drawable = BitmapDrawable(resources, bitmap)
                        drawable.setBounds(0, 0, tv.measuredWidth, drawable.intrinsicHeight)

                        drawable
                    },
                    null
                )
            }

            val attachmentList = contentDataJson.getJSONArray("commonAttachmentList")
            val attachListLen = attachmentList.length()
            for (i in 0 until attachListLen) {
                runOnUiThread {
                    val card = MsgPublishShowAttachCard(this, attachmentList.getJSONObject(i)) {

                        var loc = it.getString("fileLacation")
                        if (loc.isEmpty()) {
                            loc = it.getString("fileLocation")
                        }

                        val filename = "(${basicDataObj.getInt("id")}) ${it.getString("fileName")}"
                        if (!attemptOpenAttachment(filename)) {
                            downloadAttachment(loc, filename)
                        }
                    }

                    findViewById<LinearLayout>(R.id.msgPublishShow_mainLinearLayout).addView(card)
                }
            }
        }
    }

    private fun attemptOpenAttachment(fileName: String): Boolean {
        val file = File(filesDir.absolutePath
                + MacroDefines.FILEPATH_DOWNLOAD_MSG_ATTACHMENT, fileName)
        if (file.exists()) {
            startActivity(Utils.generateOpenFileIntent(this, file))
            return true
        }
        return false
    }

    private fun downloadAttachment(fileLocation: String, fileName: String) {

        Toast.makeText(this, "暂不支持，等待信息办提供相应能力...", Toast.LENGTH_SHORT).show()
        return

        val tarUrl = "https://1.tongji.edu.cn/api/commonservice/obsfile/downloadfile?" +
                "objectkey=$fileLocation" +
                "&realName=${URLEncoder.encode(fileName, "UTF-8")}"

        val sessionid = intent.getStringExtra("sessionid")!!
        val cookie = "sessionid=$sessionid"

        val intent = Intent(this, BackgroundDownload::class.java)
        intent.putExtra(BackgroundDownload.INTENT_KEY_FILEURL, tarUrl)
                .putExtra(BackgroundDownload.INTENT_KEY_FILENAME, fileName)
                .putExtra(BackgroundDownload.INTENT_KEY_COOKIE, cookie)
                .putExtra(BackgroundDownload.INTENT_KEY_FOLDERPATH,
                    MacroDefines.FILEPATH_DOWNLOAD_MSG_ATTACHMENT)
                .putExtra(BackgroundDownload.INTENT_KEY_SHOULD_AUTOLAUNCH, true)

        startService(intent)
    }
}
