package com.gardilily.onedottongji.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.gardilily.onedottongji.R
import com.gardilily.onedottongji.tools.Utils
import okhttp3.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class BackgroundDownload : Service() {

    /**
     * 对外开放的一些宏定义。
     */
    companion object {
        const val INTENT_KEY_FILEURL = "__0"
        const val INTENT_KEY_FILENAME = "__1"
        const val INTENT_KEY_COOKIE = "__2"
        const val INTENT_KEY_FOLDERPATH = "__3"

        const val INTENT_KEY_SHOULD_AUTOLAUNCH = "__4"
    }

    /**
     * Service 内部需要使用的一些宏定义。
     */
    private class Defines {
        companion object {
            /**
             * 关于"后台下载"通知相关的信息
             */
            const val NOTI_CHANNEL_BACK_DOWNLOAD_ID = "background download"
            const val NOTI_CHANNEL_BACK_DOWNLOAD_NAME = "后台下载"

            /**
             * 关于"下载通知"通知相关的信息
             */
            const val NOTI_CHANNEL_DOWNLOAD_INS_MSG_ID = "download ins msg"
            const val NOTI_CHANNEL_DOWNLOAD_INS_MSG_NAME = "下载通知"
        }
    }

    private lateinit var client: OkHttpClient
    private lateinit var notiManager: NotificationManager
    private lateinit var backDownloadNotiChannel: NotificationChannel
    private lateinit var insMsgNotiChannel: NotificationChannel

    private class KeyGenerator(keyMax: Int) {
        private val KEY_MAX = keyMax
        private val keyAvailable = BooleanArray(KEY_MAX) { true }

        fun generate(): Int {
            for (i in 0..KEY_MAX) {
                if (keyAvailable[i]) {
                    keyAvailable[i] = false
                    return i
                }
            }
            return -1
        }

        fun recycle(key: Int) {
            if (key < KEY_MAX) {
                keyAvailable[key] = true
            }
        }

        fun clear() {
            for (i in 0..KEY_MAX) {
                keyAvailable[i] = true
            }
        }
    }

    private val keyGen = KeyGenerator(16384)

    /**
     * onCreate 生命周期。
     */
    override fun onCreate() {
        super.onCreate()

        Log.d("BackgroundDownload", "Life Cycle: On Create")

        client = OkHttpClient()

        initNotificationService()
    }

    /**
     * 初始化通知服务。
     */
    private fun initNotificationService() {
        notiManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        backDownloadNotiChannel = NotificationChannel(Defines.NOTI_CHANNEL_BACK_DOWNLOAD_ID,
                Defines.NOTI_CHANNEL_BACK_DOWNLOAD_NAME,
                NotificationManager.IMPORTANCE_DEFAULT)

        notiManager.createNotificationChannel(backDownloadNotiChannel)

        insMsgNotiChannel = NotificationChannel(Defines.NOTI_CHANNEL_DOWNLOAD_INS_MSG_ID,
                Defines.NOTI_CHANNEL_DOWNLOAD_INS_MSG_NAME,
                NotificationManager.IMPORTANCE_HIGH)
        insMsgNotiChannel.enableLights(true)

        notiManager.createNotificationChannel(insMsgNotiChannel)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("BackgroundDownload", "Life Cycle: On Start Command")

        fun safeIntentGetStringExtra(key: String): String {
            val solidIntent = intent!!
            val res = solidIntent.getStringExtra(key)
            return res ?: ""
        }

        downloadFile(safeIntentGetStringExtra(INTENT_KEY_FILEURL),
                safeIntentGetStringExtra(INTENT_KEY_FILENAME),
                safeIntentGetStringExtra(INTENT_KEY_FOLDERPATH),
                safeIntentGetStringExtra(INTENT_KEY_COOKIE),
                intent!!.getBooleanExtra(INTENT_KEY_SHOULD_AUTOLAUNCH, false))

        return START_STICKY
        //return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    /**
     * 后台下载文件。
     * 下载时显示下载进度通知（不可删除）。
     * 下载结束后，通知变为可删除，且点击可打开文件。
     *
     * @param fileUrl 要下载的文件的路径
     * @param fileName 文件命名
     * @param cookie 传递的 cookie 信息
     */
    private fun downloadFile(
        fileUrl: String,
        fileName: String,
        fileFolderPath: String,
        cookie: String = "",
        shouldAutoLaunch: Boolean = false) {

        val noti = Notification.Builder(this, Defines.NOTI_CHANNEL_BACK_DOWNLOAD_ID)
                .setContentTitle("后台下载")
                .setContentText("文件：$fileName")
                .setSmallIcon(R.drawable.logo)

        val notiKey = keyGen.generate()
        startForeground(notiKey, noti.build())

        val request = Request.Builder()
                .url(fileUrl)
                .addHeader("Cookie", cookie)
                .build()

        client.newCall(request).enqueue(object : Callback {
            /**
             * Called when the request could not be executed due to cancellation, a connectivity problem or
             * timeout. Because networks can fail during an exchange, it is possible that the remote server
             * accepted the request before the failure.
             */
            override fun onFailure(call: Call, e: IOException) {
                stopForeground(notiKey)
                notiManager.cancel(notiKey)

                val noti = Notification.Builder(this@BackgroundDownload,
                        Defines.NOTI_CHANNEL_DOWNLOAD_INS_MSG_ID)
                        .setContentTitle("下载失败")
                        .setContentText("文件 $fileName 下载失败...")
                        .setSmallIcon(R.drawable.logo)
                        .setAutoCancel(true)
                        .build()
                notiManager.notify(notiKey, noti)
            }

            /**
             * Called when the HTTP response was successfully returned by the remote server. The callback may
             * proceed to read the response body with [Response.body]. The response is still live until its
             * response body is [closed][ResponseBody]. The recipient of the callback may consume the response
             * body on another thread.
             *
             * Note that transport-layer success (receiving a HTTP response code, headers and body) does not
             * necessarily indicate application-layer success: `response` may still indicate an unhappy HTTP
             * response code like 404 or 500.
             */
            override fun onResponse(call: Call, response: Response) {
                val resCode = response.code

                if (resCode == 200) {

                    val targetFilePath = filesDir.absolutePath + fileFolderPath

                    val filePath = File(targetFilePath)
                    if (!filePath.exists()) {
                        filePath.mkdirs()
                    }

                    val inputStream = response.body!!.byteStream()
                    val file = File(targetFilePath, fileName)
                    val fileOutputStream = FileOutputStream(file)

                    var len: Int
                    val buf = ByteArray(2048)

                    while (true) {
                        len = inputStream.read(buf)
                        if (len == -1) {
                            break
                        }
                        fileOutputStream.write(buf, 0, len)
                    }

                    fileOutputStream.flush()
                    fileOutputStream.close()
                    inputStream.close()

                    stopForeground(notiKey)
                    notiManager.cancel(notiKey)

                    val intent = Utils.generateOpenFileIntent(this@BackgroundDownload, file)

                    val pendingIntent = PendingIntent.getActivity(this@BackgroundDownload,
                            0,
                            intent,
                            0)

                    val noti = Notification.Builder(this@BackgroundDownload,
                            Defines.NOTI_CHANNEL_DOWNLOAD_INS_MSG_ID)
                            .setContentTitle("下载完毕")
                            .setContentText("点击以打开文件：$fileName")
                            .setContentIntent(pendingIntent)
                            .setSmallIcon(R.drawable.logo)
                            .setAutoCancel(true)
                            .build()
                    notiManager.notify(notiKey, noti)

                    if (shouldAutoLaunch) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                    }

                } else {
                    stopForeground(notiKey)
                    notiManager.cancel(notiKey)

                    val noti = Notification.Builder(this@BackgroundDownload,
                            Defines.NOTI_CHANNEL_DOWNLOAD_INS_MSG_ID)
                            .setContentTitle("下载失败（状态码：${resCode}）")
                            .setContentText("文件 $fileName 下载失败...")
                            .setSmallIcon(R.drawable.logo)
                            .setAutoCancel(true)
                            .build()
                    notiManager.notify(notiKey, noti)
                }
            }
        })
    }
}
