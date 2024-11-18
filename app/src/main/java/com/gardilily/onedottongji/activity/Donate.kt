package com.gardilily.onedottongji.activity

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AlertDialog
import com.gardilily.onedottongji.R

class Donate : OneTJActivityBase(
    backOnTitleBar = true,
    hasTitleBar = true,
) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_donate)

        title = "捐助App"

        findViewById<Button>(R.id.donate_qrcode_wxpay).setOnClickListener {
            showBigQrCode(R.drawable.wxpay)
        }
        findViewById<Button>(R.id.donate_qrcode_alipay).setOnClickListener {
            showBigQrCode(R.drawable.alipay)
        }
        findViewById<Button>(R.id.donate_qrcode_wxreward).setOnClickListener {
            showBigQrCode(R.drawable.wxreward)
        }
    }


    fun showBigQrCode(@DrawableRes id: Int) {
        val imgView = ImageView(this)

        val drawable = resources.getDrawable(id)
        imgView.setImageDrawable(drawable)

        AlertDialog.Builder(this)
            .setView(imgView)
            .setPositiveButton("关闭") { dialog, which -> dialog.dismiss() }
            .show()
    }

}

