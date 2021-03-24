package com.gardilily.onedottongji.tools

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

class Utils {
    companion object {
        fun safeNetworkRequest(req: Request, client: OkHttpClient): Response? {
            return try {
                val res = client.newCall(req).execute()
                res
            } catch (e: Exception) {
                null
            }
        }
    }
}
