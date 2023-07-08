// SPDX-License-Identifier: MulanPSL-2.0
package com.gardilily.onedottongji.tools

/** 项目使用到的一些宏定义。 */
class MacroDefines {
    companion object {

        /**
         * Name of the Shared Preferences of the App.
         */
        const val SHARED_PREFERENCES_STORE_NAME = "OneDotTongjiRefresh"
        const val SP_KEY_USER_ID = "login.uid"
        const val SP_KEY_USER_PW = "login.upw"
        const val SP_KEY_SESSIONID = "sessionid"

        /**
         * 页面跳转请求代码。
         */
        const val UNILOGIN_WEBVIEW_FOR_1SESSIONID = 1

        const val ACTIVITY_RESULT_SUCCESS = 1

        const val HOME_FUNC_UNDEFINED = -1
        const val HOME_FUNC_ABOUT_APP = 1
        const val HOME_FUNC_MY_GRADES = 2

        const val HOME_FUNC_GET_SESSIONID = 10

        /**
         * 学期内完整课表
         */
        const val HOME_FUNC_GRADUATE_STUDENT_TIME_TABLE_TERM_COMPLETE = 3

        /**
         * 单日课表
         */
        const val HOME_FUNC_GRADUATE_STUDENT_TIME_TABLE_SINGLE_DAY = 4

        /**
         * 本地文件
         */
        const val HOME_FUNC_LOCAL_ATTACHMENTS = 5

        /**
         * 登出
         */
        const val HOME_FUNC_LOGOUT = 6

        /**
         * 自动抢课
         */
        const val HOME_FUNC_AUTO_COURSE_ELECT = 7

        /**
         * 我的考试
         */
        const val HOME_FUNC_STU_EXAM_ENQUIRIES = 8

        /**
         * 个人选课
         */
        const val HOME_FUNC_STUDENT_ELECT = 9

        /**
         * 本地文件管理
         */
        const val FILEPATH_DOWNLOAD_ROOT = "/download"
        const val FILEPATH_DOWNLOAD_MSG_ATTACHMENT = "/download/attachment"
        const val FILEPATH_DOWNLOAD_APP_UPDATE_APK = "/download/appUpdate"

    }
}