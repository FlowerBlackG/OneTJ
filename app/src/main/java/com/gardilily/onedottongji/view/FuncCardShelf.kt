// SPDX-License-Identifier: MulanPSL-2.0
package com.gardilily.onedottongji.view

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.caverock.androidsvg.SVGImageView
import com.gardilily.onedottongji.R
import com.gardilily.onedottongji.activity.Home

/** 功能卡书架。 */
class FuncCardShelf(context: Context) : LinearLayout(context) {
    private val c = context
    private val layout: LinearLayout
    private var cardCount = 0
    private var rowLayout: LinearLayout? = null

    private val spMultiply = resources.displayMetrics.scaledDensity
    var targetCardWidthPx = 360

    var CARD_PER_ROW = 4

    init {
        LayoutInflater.from(context).inflate(R.layout.card_function_shelf, this, true)
        layout = findViewById(R.id.cardshelf_layout)
    }

    private fun addCard(v: View) {
        if (cardCount % CARD_PER_ROW == 0) {
            rowLayout = createRowLayout()
            layout.addView(rowLayout)
        }
        rowLayout!!.addView(v)
        cardCount++
    }

    private fun createRowLayout(): LinearLayout {
        val mLayout = LinearLayout(c)
        val params = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        params.bottomMargin = (12f * spMultiply).toInt()
        mLayout.layoutParams = params
        return mLayout
    }


    fun addFuncCard(
        iconPath: String,
        text: String,
        func: Home.HomeFunc,
        isVisible: Boolean = true,
        action: (func: Home.HomeFunc)->Unit
    ) {
        val layout = LinearLayout(c)
        layout.orientation = VERTICAL

        val params = LayoutParams(0, (112 * spMultiply).toInt())
        params.weight = 1f

        if (cardCount % CARD_PER_ROW != 0) {
            params.marginStart = (12f * spMultiply).toInt()
        }

        layout.layoutParams = params


        layout.isClickable = true
        layout.gravity = Gravity.CENTER

        if (!isVisible) {
            layout.visibility = INVISIBLE
        }

        val iconView = SVGImageView(c)
        iconView.setImageAsset(iconPath)
        val iconParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        val iconSize = 128
        iconParams.width = iconSize
        iconParams.height = iconSize
        iconView.layoutParams = iconParams
        layout.addView(iconView)

        val textView = TextView(c)
        textView.text = text
        textView.textSize = 18f

        val textParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        textParams.topMargin = (8f * spMultiply).toInt()
        textView.layoutParams = textParams
        layout.addView(textView)

        layout.setOnClickListener { action(func) }

        addCard(layout)
    }

    fun addEmptyTransparentCard() = addFuncCard("", "", Home.HomeFunc.NONE, false) {}

    fun fillBlank() {
        while (cardCount % CARD_PER_ROW != 0) {
            addEmptyTransparentCard()
        }
    }
}