package com.gardilily.onedottongji.view

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import com.gardilily.onedottongji.R

class FuncCardShelf(context: Context) : LinearLayout(context) {
    private val c = context
    private val layout: LinearLayout
    private var card_count = 0
    private var row_layout: RelativeLayout? = null

    private val spMultiply = resources.displayMetrics.scaledDensity
    var targetCardWidthPx = 720

    init {
        LayoutInflater.from(context).inflate(R.layout.card_function_shelf, this, true)
        layout = findViewById(R.id.cardshelf_layout)
    }

    fun addCard(v: View) {
        if (card_count % 2 == 0) {
            row_layout = createRowLayout()
            layout.addView(row_layout)
        }
        row_layout!!.addView(v)
        card_count++
    }

    /*
    fun addFuncCard(icon: String, text: String, func: Int, action: (func: Int)->Unit) {
        val mButtonCard = FuncButtonCard(c)
        val params = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)//mButtonCard.layoutParams as RelativeLayout.LayoutParams
        if (card_count % 2 == 0) {
            params.addRule(RelativeLayout.ALIGN_PARENT_START)
        } else {
            params.addRule(RelativeLayout.ALIGN_PARENT_END)
        }
        mButtonCard.layoutParams = params
        mButtonCard.setInfo(icon, text, func)
        mButtonCard.setClickAction(action)
        addCard(mButtonCard)
    }
     */

    private fun createRowLayout(): RelativeLayout {
        val mLayout = RelativeLayout(c)
        val params = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
        params.bottomMargin = (12f * spMultiply).toInt()
        mLayout.layoutParams = params
        return mLayout
    }

    fun addFuncCard(icon: String, text: String, func: Int, action: (func: Int)->Unit) {
        val layout = LinearLayout(c)
        layout.orientation = LinearLayout.HORIZONTAL
        val params = RelativeLayout.LayoutParams(targetCardWidthPx, (80f * spMultiply).toInt())

        if (card_count % 2 == 0) {
            params.addRule(RelativeLayout.ALIGN_PARENT_START)
        } else {
            params.addRule(RelativeLayout.ALIGN_PARENT_END)
        }

        layout.layoutParams = params

        layout.background = c.getDrawable(R.drawable.shape_login_page_box)
        layout.isClickable = true
        layout.gravity = Gravity.CENTER_VERTICAL

        val iconView = TextView(c)
        iconView.text = icon
        iconView.textSize = 36f
        iconView.setTextColor(Color.parseColor("#000000"))
        val iconAndTextCommonParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
        iconAndTextCommonParams.leftMargin = (12f * spMultiply).toInt()
        iconView.layoutParams = iconAndTextCommonParams
        layout.addView(iconView)

        val textView = TextView(c)
        textView.text = text
        textView.textSize = 20f
        textView.setTextColor(Color.parseColor("#000000"))
        textView.layoutParams = iconAndTextCommonParams
        layout.addView(textView)

        layout.setOnClickListener { action(func) }

        addCard(layout)
    }

}
