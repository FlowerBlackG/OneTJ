package com.gardilily.onedottongji.tools

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.Button
import com.gardilily.onedottongji.R

class QQGroupDialog(context: Context) : Dialog(context) {
    init {
        setContentView(R.layout.dialog_qq_group)

        window?.setBackgroundDrawableResource(android.R.color.transparent)
        setCancelable(true)
        setCanceledOnTouchOutside(true)

        initButtons()
    }

    private fun initButtons() {
        val btnCancel = findViewById<Button>(R.id.btn_cancel)
        btnCancel.setOnClickListener {
            dismiss()
        }
        val btnJoinGroup = findViewById<Button>(R.id.btn_join_group)
        btnJoinGroup.setOnClickListener {
            val groupUrl = MacroDefines.QQgruopUrl
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(groupUrl)))
            dismiss()  // 跳转后关闭对话框
        }
    }

}