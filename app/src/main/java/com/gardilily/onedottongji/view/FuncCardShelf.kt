package com.gardilily.onedottongji.view

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.gardilily.onedottongji.R

class FuncCardShelf(context: Context) : LinearLayout(context) {
    private val c = context
    private val layout: LinearLayout
    private var cardCount = 0
    private var rowLayout: LinearLayout? = null

    private val spMultiply = resources.displayMetrics.scaledDensity
    var targetCardWidthPx = 480

    init {
        LayoutInflater.from(context).inflate(R.layout.card_function_shelf, this, true)
        layout = findViewById(R.id.cardshelf_layout)
    }

    private fun addCard(v: View) {
        if (cardCount % 3 == 0) {
            rowLayout = createRowLayout()
            layout.addView(rowLayout)
        }
        rowLayout!!.addView(v)
        cardCount++
    }
/*
    private fun legacy_createRowLayout(): RelativeLayout {
        val mLayout = RelativeLayout(c)
        val params = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        )
        params.bottomMargin = (12f * spMultiply).toInt()
        mLayout.layoutParams = params
        return mLayout
    }
*/
    private fun createRowLayout(): LinearLayout {
        val mLayout = LinearLayout(c)
        val params = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        params.bottomMargin = (12f * spMultiply).toInt()
        mLayout.layoutParams = params
        return mLayout
    }

    fun addFuncCard(icon: String, text: String, func: Int, action: (func: Int)->Unit) {
        val layout = LinearLayout(c)
        layout.orientation = VERTICAL
        val params = LayoutParams(targetCardWidthPx, (112 * spMultiply).toInt())

        /*
        if (cardCount % 2 == 0) {
            params.addRule(RelativeLayout.ALIGN_PARENT_START)
        } else {
            params.addRule(RelativeLayout.ALIGN_PARENT_END)
        }
         */

        if (cardCount % 3 != 0) {
            params.marginStart = (12f * spMultiply).toInt()
        }

        layout.layoutParams = params

        layout.background = c.getDrawable(R.drawable.shape_login_page_box)
        layout.isClickable = true
        layout.gravity = Gravity.CENTER

        val iconView = TextView(c)
        iconView.text = icon
        iconView.textSize = 36f
        iconView.setTextColor(Color.parseColor("#000000"))
        val iconParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        //iconAndTextCommonParams.leftMargin = (12f * spMultiply).toInt()
        iconView.layoutParams = iconParams
        layout.addView(iconView)

        val textView = TextView(c)
        textView.text = text
        textView.textSize = 18f
        textView.setTextColor(Color.parseColor("#000000"))
        val textParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        textParams.topMargin = (8f * spMultiply).toInt()
        textView.layoutParams = textParams
        layout.addView(textView)

        layout.setOnClickListener { action(func) }

        addCard(layout)
    }

}