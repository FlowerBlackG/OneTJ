package com.gardilily.onedottongji.view

import android.content.Context
import android.view.LayoutInflater
import android.widget.RelativeLayout
import android.widget.TextView
import com.gardilily.onedottongji.R
import org.json.JSONObject

class MsgPublishShowAttachCard(context: Context,
                         dataObj: JSONObject,
                         clickProcessor: (dataObj: JSONObject) -> Unit) : RelativeLayout(context) {

    init {
        LayoutInflater
                .from(context)
                .inflate(R.layout.card_msg_publish_show_attach, this, true)

        findViewById<RelativeLayout>(R.id.card_msgPublishShow_attachCard_layout)
                .setOnClickListener { clickProcessor(dataObj) }

        findViewById<TextView>(R.id.card_msgPublishShow_attachCard_filename).text =
                dataObj.getString("fileName")
    }
}
