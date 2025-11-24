package com.gardilily.onedottongji.tools

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.Window

/**
 * 空Activity代理。用于应对需要Activity才能调用的函数，但操作方并不关心对Activity的反馈。
 *
 * @author: oierxjn
 */
class ProxyActivity : Activity(){

    // 对各个实现进行屏蔽

    @SuppressLint("MissingSuperCall")
    override fun onCreate(savedInstanceState: Bundle?) {

    }

    @SuppressLint("MissingSuperCall")
    override fun onStart() {

    }

    @SuppressLint("MissingSuperCall")
    override fun onPause() {

    }

    @SuppressLint("MissingSuperCall")
    override fun onDestroy() {

    }

    // 屏蔽WindowManager
    override fun getSystemService(name: String): Any? {
        if(WINDOW_SERVICE == name){
            return null
        }
        return super.getSystemService(name)
    }

    // 保持Activity有效
    override fun isDestroyed(): Boolean = false
    override fun isFinishing(): Boolean = false

    // 无窗口无布局处理
    override fun getWindow() = null
    override fun setContentView(layoutResID: Int) {}
    override fun <T : View?> findViewById(id: Int) = null

    // 创建代理实例
    companion object{
        fun create(context: Context): ProxyActivity{
            return ProxyActivity().apply {
                attachBaseContext(context.applicationContext)
            }
        }
    }
}