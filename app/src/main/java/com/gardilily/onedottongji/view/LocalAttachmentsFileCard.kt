package com.gardilily.onedottongji.view

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.widget.RelativeLayout
import android.widget.TextView
import com.gardilily.onedottongji.R
import com.gardilily.onedottongji.tools.Utils
import java.io.File

/**
 * 显示单个文件卡片。
 *
 * @param context Context
 * @param file 文件
 */
abstract class LocalAttachmentsFileCard(context: Context, file: File) : RelativeLayout(context) {

    /**
     * 显示文件名。
     */
    var filename = ""
        set(value) {
            field = value
            findViewById<TextView>(R.id.card_func_localAttachments_fileCard_filename).text = value
        }

    init {
        LayoutInflater.from(context).inflate(R.layout.card_func_localattachments_file, this, true)
        filename = file.name
        findViewById<TextView>(R.id.card_func_localAttachments_fileCard_delete).setOnClickListener {
            delete(file, this)
        }

        findViewById<RelativeLayout>(R.id.card_func_localAttachments_fileCard_layout).setOnClickListener {
            context.startActivity(Utils.generateOpenFileIntent(context, file))
        }
    }

    /**
     * 删除一个文件。同时将本视图从容器中移除。
     *
     * @param file 本视图代表的文件。
     * @param card 本视图本体。
     */
    abstract fun delete(file: File, card: LocalAttachmentsFileCard)
}
