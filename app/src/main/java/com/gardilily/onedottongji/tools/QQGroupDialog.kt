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
            val groupUrl = "http://qm.qq.com/cgi-bin/qm/qr?_wv=1027&k=YIF3M8HCGgW4_q6J4XjOrres3aaLhPsm&authKey=L%2F29m%2Bc8HmnYWupK%2F7dzAlptgdDc3DoBhKZ7p3BJw4NOufa1dAo4QsgCUzBKdJ8C&noverify=0&group_code=322324184"
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(groupUrl)))
            dismiss()  // 跳转后关闭对话框
        }
    }

}