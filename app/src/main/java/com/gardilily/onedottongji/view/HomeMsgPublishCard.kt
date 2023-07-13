// SPDX-License-Identifier: MulanPSL-2.0
package com.gardilily.onedottongji.view

import android.content.Context
import android.view.LayoutInflater
import android.widget.RelativeLayout
import android.widget.TextView
import com.gardilily.onedottongji.R
import com.google.android.material.card.MaterialCardView
import org.json.JSONObject

class HomeMsgPublishCard(
    context: Context,
    dataObj: JSONObject,
    clickProcessor: (dataObj: JSONObject) -> Unit
) : MaterialCardView(context) {

    init {
        LayoutInflater
                .from(context)
                .inflate(R.layout.card_home_msg_publish, this, true)

        findViewById<RelativeLayout>(R.id.card_home_commonMsgPublish_layout)
                .setOnClickListener { clickProcessor(dataObj) }

        findViewById<TextView>(R.id.card_home_commonMsgPublish_title).text =
                dataObj.getString("title")

        findViewById<TextView>(R.id.card_home_commonMsgPublish_date).text =
                dataObj.getString("publishTime").split(' ')[0]
    }
}
