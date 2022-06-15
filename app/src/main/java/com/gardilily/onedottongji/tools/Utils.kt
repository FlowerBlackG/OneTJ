package com.gardilily.onedottongji.tools

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.gardilily.onedottongji.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.File
import java.util.*

/** 项目需要使用到的一些通用小工具。 */
class Utils {
    companion object {

        /**
         * 发起一个安全的网络请求。防止因为 Exception 而程序崩溃。
         *
         * @return 请求成功时，返回请求结果。请求失败时，返回 null
         */
        fun safeNetworkRequest(req: Request, client: OkHttpClient): Response? {
            return try {
                val res = client.newCall(req).execute()
                res
            } catch (e: Exception) {
                null
            }
        }

        /**
         * 创建一个打开文件的 Intent。
         *
         * @param context 发起创建请求者。
         * @param file 要打开的文件。
         */
        fun generateOpenFileIntent(context: Context, file: File): Intent {
            val intent = Intent(Intent.ACTION_VIEW)
            val uri = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.fileprovider", file)
            intent.setDataAndType(uri, getMIMEType(file))
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            return intent
        }

        /**
         * 在遇到错误时，使用 Alert Dialog 发出提醒。
         */
        const val REQ_RES_CHECK_NOTI_LEVEL_ALERTDIALOG = 1

        /**
         * 在遇到错误时，使用 Toast 发出提醒。
         */
        const val REQ_RES_CHECK_NOTI_LEVEL_TOAST = 2

        /**
         * 检查请求是否拿到了正确的返回内容。
         *
         * @param activity 发起检查请求的 activity.
         * @param obj 一块钱系统返回的 json 数据。
         * @param requestPurpose 为什么要发起请求。内容会被展示在错误提示中。
         * @param notiLevel 当发现返回内容不正确时，使用何种等级的方式提醒用户。
         *
         * @return 是否正确。
         */
        fun isReqResCorrect(activity: Activity, obj: JSONObject,
                            requestPurpose: String, notiLevel: Int): Boolean {

            val resCode = obj.getInt("code")
            if (resCode == 200) {
                return true
            }

            activity.runOnUiThread {
                when (notiLevel) {
                    REQ_RES_CHECK_NOTI_LEVEL_ALERTDIALOG -> {
                        val displayMsg = "状态码：$resCode" +
                                "\n信息：${obj.getString("msg")}"

                        AlertDialog.Builder(activity)
                                .setTitle("${requestPurpose}失败")
                                .setMessage(displayMsg)
                                .setPositiveButton("好的", null)
                                .show()
                    }
                    REQ_RES_CHECK_NOTI_LEVEL_TOAST -> {
                        Toast.makeText(activity,
                                "${requestPurpose}失败。状态码：$resCode",
                                Toast.LENGTH_SHORT).show()
                    }
                }
            }

            return false
        }

        /**
         * 检查请求是否拿到了不正确的返回内容。
         *
         * @param activity 发起检查请求的 activity.
         * @param obj 一块钱系统返回的 json 数据。
         * @param requestPurpose 为什么要发起请求。内容会被展示在错误提示中。
         * @param notiLevel 当发现返回内容不正确时，使用何种等级的方式提醒用户。
         *
         * @return 是否不正确。
         */
        fun isNotReqResCorrect(activity: Activity, obj: JSONObject,
                               requestPurpose: String, notiLevel: Int): Boolean {

            return !isReqResCorrect(activity, obj, requestPurpose, notiLevel)
        }

        /**
         * 通过检查 rawData JSON 中的 code，判断与一块钱系统的连接会话是否过期。
         *
         * @param activity 发起查询请求的 activity.
         * @param rawData 一块钱系统返回的 json 数据。
         * @param processor 在发现会话过期时执行的任务。
         *
         * @return 是否没有过期。
         */
        fun isReqSessionAvailable(activity: Activity, rawData: JSONObject, processor: ()->Unit): Boolean {
            Log.d("Utils.checkSessionAvailability", rawData.toString() + " ${rawData.has("code")}")

            if (!rawData.has("code")) {
                activity.runOnUiThread {
                    AlertDialog.Builder(activity)
                            .setTitle("会话过期")
                            .setMessage("请重新登录...")
                            .setPositiveButton("好的") { _, _ -> processor() }
                            .setNegativeButton("嗯嗯") { _, _ -> processor() }
                            .setNeutralButton("可以") { _, _ -> processor() }
                            .show()
                }

                return false
            }
            return true
        }

        /**
         * 根据文件后缀名获得对应的MIME类型。
         *
         * @param file 文件。
         */
        private fun getMIMEType(file: File): String {
            var type = "*/*"
            val fName = file.name //获取后缀名前的分隔符"."在fName中的位置。
            val dotIndex = fName.lastIndexOf(".")
            if (dotIndex < 0) {
                return type
            }
            /* 获取文件的后缀名*/
            val end = fName.substring(dotIndex, fName.length).lowercase(Locale.getDefault())
            if (end === "") return type
            //在MIME和文件类型的匹配表中找到对应的MIME类型。
            for (i in MIME_MapTable.indices) {
                //MIME_MapTable??在这里你一定有疑问，这个MIME_MapTable是什么？
                if (end == MIME_MapTable[i][0]) type = MIME_MapTable[i][1]
            }
            return type
        }

        private val MIME_MapTable = arrayOf(arrayOf(".3gp", "video/3gpp"),
                arrayOf(".apk", "application/vnd.android.package-archive"),
                arrayOf(".asf", "video/x-ms-asf"), arrayOf(".avi", "video/x-msvideo"),
                arrayOf(".bin", "application/octet-stream"), arrayOf(".bmp", "image/bmp"),
                arrayOf(".c", "text/plain"), arrayOf(".class", "application/octet-stream"),
                arrayOf(".conf", "text/plain"), arrayOf(".cpp", "text/plain"),
                arrayOf(".doc", "application/msword"),
                arrayOf(".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
                arrayOf(".xls", "application/vnd.ms-excel"),
                arrayOf(".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
                arrayOf(".exe", "application/octet-stream"), arrayOf(".gif", "image/gif"),
                arrayOf(".gtar", "application/x-gtar"), arrayOf(".gz", "application/x-gzip"),
                arrayOf(".h", "text/plain"), arrayOf(".htm", "text/html"), arrayOf(".html", "text/html"),
                arrayOf(".jar", "application/java-archive"), arrayOf(".java", "text/plain"),
                arrayOf(".jpeg", "image/jpeg"), arrayOf(".jpg", "image/jpeg"),
                arrayOf(".js", "application/x-JavaScript"), arrayOf(".log", "text/plain"),
                arrayOf(".m3u", "audio/x-mpegurl"), arrayOf(".m4a", "audio/mp4a-latm"),
                arrayOf(".m4b", "audio/mp4a-latm"), arrayOf(".m4p", "audio/mp4a-latm"),
                arrayOf(".m4u", "video/vnd.mpegurl"), arrayOf(".m4v", "video/x-m4v"),
                arrayOf(".mov", "video/quicktime"), arrayOf(".mp2", "audio/x-mpeg"),
                arrayOf(".mp3", "audio/x-mpeg"), arrayOf(".mp4", "video/mp4"),
                arrayOf(".mpc", "application/vnd.mpohun.certificate"), arrayOf(".mpe", "video/mpeg"),
                arrayOf(".mpeg", "video/mpeg"), arrayOf(".mpg", "video/mpeg"), arrayOf(".mpg4", "video/mp4"),
                arrayOf(".mpga", "audio/mpeg"), arrayOf(".msg", "application/vnd.ms-outlook"),
                arrayOf(".ogg", "audio/ogg"), arrayOf(".pdf", "application/pdf"),
                arrayOf(".png", "image/png"), arrayOf(".pps", "application/vnd.ms-powerpoint"),
                arrayOf(".ppt", "application/vnd.ms-powerpoint"),
                arrayOf(".pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
                arrayOf(".prop", "text/plain"), arrayOf(".rc", "text/plain"),
                arrayOf(".rmvb", "audio/x-pn-realaudio"), arrayOf(".rtf", "application/rtf"),
                arrayOf(".sh", "text/plain"), arrayOf(".tar", "application/x-tar"),
                arrayOf(".tgz", "application/x-compressed"), arrayOf(".txt", "text/plain"),
                arrayOf(".wav", "audio/x-wav"), arrayOf(".wma", "audio/x-ms-wma"), arrayOf(".wmv", "audio/x-ms-wmv"),
                arrayOf(".wps", "application/vnd.ms-works"), arrayOf(".xml", "text/plain"), arrayOf(".z", "application/x-compress"),
                arrayOf(".zip", "application/x-zip-compressed"), arrayOf("", "*/*"))
    }
}