// SPDX-License-Identifier: MulanPSL-2.0
package com.gardilily.onedottongji.activity

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.text.Html
import android.util.Base64
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.gardilily.onedottongji.R
import com.gardilily.onedottongji.service.BackgroundDownload
import com.gardilily.onedottongji.tools.MacroDefines
import com.gardilily.onedottongji.tools.Utils
import com.gardilily.onedottongji.tools.Utils.Companion.isReqSessionAvailable
import com.gardilily.onedottongji.view.MsgPublishShowAttachCard
import okhttp3.*
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder
import kotlin.concurrent.thread

class MsgPublishShow : Activity() {
    private lateinit var basicDataObj: JSONObject
    private lateinit var contentDataJson: JSONObject
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_msg_publish_show)

        basicDataObj = JSONObject(intent.getStringExtra("basicDataObj")!!)

        findViewById<TextView>(R.id.msgPublishShow_title).text =
                basicDataObj.getString("title")

        findViewById<TextView>(R.id.msgPublishShow_createUser).text =
                basicDataObj.getString("createUser")

        findViewById<TextView>(R.id.msgPublishShow_publishTime).text =
                basicDataObj.getString("publishTime")

        fetchAndShowContentAndAttach()
    }

    private fun fetchAndShowContentAndAttach() {
        thread {
            val client = OkHttpClient()
            val req = Request.Builder()
                    .get()
                    .addHeader("Cookie", "sessionid=${intent.getStringExtra("sessionid")!!}")
                    .url("https://1.tongji.edu.cn/api/commonservice/commonMsgPublish/findCommonMsgPublishById?id=${basicDataObj.getInt("id")}")
                    .build()

            val response = Utils.safeNetworkRequest(req, client)

            if (response == null) {
                runOnUiThread {
                    Toast.makeText(this, "网络异常", Toast.LENGTH_SHORT).show()
                }
                return@thread
            }

            val resObj = JSONObject(response.body?.string())

            if (!isReqSessionAvailable(this, resObj) { finish() }) {
                return@thread
            }

            contentDataJson = resObj.getJSONObject("data")

            runOnUiThread {
                val tv = findViewById<TextView>(R.id.msgPublishShow_content)
                tv.text =
                        Html.fromHtml(contentDataJson.getString("content"), { source ->
                            val base64str = source.substring(source.indexOf(',') + 1,
                                    source.length -
                                            if (source.contains('\n'))
                                                1
                                            else
                                                0)

                            val data = Base64.decode(base64str, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
                            val drawable = BitmapDrawable(resources, bitmap)
                            drawable.setBounds(0, 0, tv.measuredWidth, drawable.intrinsicHeight)

                            drawable
                        }, null)
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
