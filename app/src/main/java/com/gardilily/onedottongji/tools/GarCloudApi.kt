// SPDX-License-Identifier: MulanPSL-2.0
package com.gardilily.onedottongji.tools

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread

/** 鲤溪云功能接口。与后方服务器通讯，完成检查更新、基础用户信息采集功能。 */
class GarCloudApi {
    companion object {

        /**
         * 检查更新。
         *
         * @param showDialogIfIsUpToDate - 如果已经是最新版本，是否需要展示提示框。
         */
        fun checkUpdate(activity: Activity, showDialogIfIsUpToDate: Boolean) {
            thread {
                val packageManager = activity.packageManager
                val packageName = activity.packageName

                val tarUrl = "https://www.gardilily.com/oneDotTongji/checkUpdate.php?" +
                        "version=" + packageManager.getPackageInfo(packageName, 0).longVersionCode

                val client = OkHttpClient()
                val request = Request.Builder()
                    .url(tarUrl)
                    .build()

                val response = Utils.safeNetworkRequest(request, client)

                if (response == null) {
                    activity.runOnUiThread {
                        Toast.makeText(activity, "网络异常", Toast.LENGTH_SHORT).show()
                    }
                    return@thread
                }

                val jsonObj = JSONObject(response.body?.string())
                Log.d("Login.checkUpdate.jsonObj", jsonObj.toString())
                if (!jsonObj.getBoolean("isLatest")) {
                    // 有更新可用
                    val updateMsg = "版本：" + jsonObj.getString("newVersionName") +
                            "\n时间：" + jsonObj.getString("newVersionBuildTime") +
                            "\n说明：\n\n" + jsonObj.getString("updateLog") + "\n"
                    Log.d("Login.checkUpdate.updateMsg", updateMsg)
                    activity.runOnUiThread {
                        AlertDialog.Builder(activity)
                            .setTitle("有更新可用")
                            .setMessage(updateMsg)
                            .setPositiveButton("更新") { _, _ ->

                                val uri = Uri.parse(jsonObj.getString("updateUrl"))
                                activity.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            }
                            .setNegativeButton("取消", null)
                            .show()
                    }
                } else {
                    // 已是最新版
                    if (showDialogIfIsUpToDate) {
                        activity.runOnUiThread {
                            AlertDialog.Builder(activity)
                                .setTitle("当前版本已是最新")
                                .setMessage("当前版本：${packageManager.getPackageInfo(packageName, 0).versionName} (${packageManager.getPackageInfo(packageName, 0).longVersionCode})")
                                .setPositiveButton("好耶", null)
                                .show()
                        }
                    }
                }
            }
        }

        /**
         * 上报用户信息。包含以下内容：
         * · 登录时间
         * · 用户姓名
         * · 用户学号
         * · 用户手机品牌
         * · 用户手机型号
         * · 用户一块钱移动客户端版本
         */
        fun uploadUserInfo(activity: Activity, userID: String, userName: String) {
            thread {
                val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd_HH:mm:ss")
                val date = Date(System.currentTimeMillis())
                val url = "https://www.gardilily.com/oneDotTongji/userLoginInfoUpload.php?" +
                        "date=" + simpleDateFormat.format(date) +
                        "&userid=" + userID +
                        "&username=" + userName +
                        "&client_version=" +
                        activity.packageManager
                            .getPackageInfo(activity.packageName, 0).longVersionCode +
                        "&device_brand=" + Build.BRAND +
                        "&device_model=" + Build.MODEL
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url(url)
                    .build()

                Utils.safeNetworkRequest(request, client)
            }
        }

    }
}
