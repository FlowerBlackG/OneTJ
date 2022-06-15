package com.gardilily.onedottongji.activity.func

import android.app.Activity
import android.os.Bundle
import android.widget.LinearLayout
import com.gardilily.onedottongji.R
import com.gardilily.onedottongji.tools.MacroDefines
import com.gardilily.onedottongji.view.LocalAttachmentsFileCard
import java.io.File
import kotlin.concurrent.thread

class LocalAttachments: Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_func_localattachments)

        initCards()
    }

    private fun initCards() {
        val layout = findViewById<LinearLayout>(R.id.func_localAttachments_linearLayout)

        thread {
            val f = File(filesDir.absolutePath + MacroDefines.FILEPATH_DOWNLOAD_ROOT)

            fun listFiles(root: File) {
                if (!root.exists()) {
                    return
                }
                if (root.isFile) {
                    runOnUiThread {

                        layout.addView(object : LocalAttachmentsFileCard(this, root) {
                            override fun delete(file: File, card: LocalAttachmentsFileCard) {
                                file.delete()
                                layout.removeView(card)
                            }

                        })
                    }
                } else {
                    // root is directory
                    val files = root.listFiles()!!
                    files.forEach {
                        Thread.sleep(28)
                        listFiles(it)
                    }
                }
            }

            listFiles(f)

        }
    }
}
