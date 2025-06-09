/*
 * Info Card
 * A convenient info card as part of the Gardilily Android Development Tools.
 *
 * Author : Flower Black
 * Version: 2021.07.05-13:06
 */

package com.gardilily.common.view.card

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.text.InputType
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import com.caverock.androidsvg.SVGImageView
import com.gardilily.common.view.card.InfoCard.Builder
import com.google.android.material.card.MaterialCardView

/**
 * 用于显示基本信息的卡片。继承自 RelativeLayout
 *
 * 卡片分为左、右、顶、中四个部分，分别为：
 *
 * · 标题（顶部靠左
 * · 图标（左侧方块，若为RTL布局则在右侧）
 * · 小标签（右侧方块，若为RTL布局则在左侧）
 * · 信息列表（中部）
 *
 * 其中，信息列表为多行布局。每行有小标题和内容，之间加入分隔符（默认为中文冒号"："）。
 *
 * 卡片整体样式如下：
 *
 *     张三
 * 🍓  学校：同济大学
 *     住址：上海市杨浦区
 *     收货：上海市杨浦区四平路
 *          1239号同济大学      A（标签）
 *     编号：001
 *
 * 使用 [Builder.build()][Builder.build] 构造对象。
 */



/**
 * 用于显示基本信息的卡片。继承自 RelativeLayout
 *
 * (Version modified to be mutable, allowing properties to be updated after creation.)
 *
 * 使用 [Builder.build()][Builder.build] 构造对象, and can be updated via public setter methods.
 */
open class InfoCard private constructor(
    builder: Builder
) : MaterialCardView(builder.c) {

    // Keep context and spMultiply as they are fundamental
    private val c = builder.c
    private var spMultiply = builder.spMultiply

    // --- Views promoted to member properties for setter access ---
    private val innerRelativeLayout: RelativeLayout
    private var iconView: SVGImageView? = null
    private val endMarkView: TextView
    private val titleTV: TextView
    private val infoLinearLayout: LinearLayout

    // --- Properties made mutable (`var`) to allow updates ---
    private var hasIcon = builder.hasIcon
    private var hasEndMark = builder.hasEndMark
    private var infoList = builder.infoList.toMutableList() // Use a mutable list

    // --- Layout properties stored for recalculation on update ---
    private val innerMarginStartSp = builder.innerMarginStartSp
    private val innerMarginEndSp = builder.innerMarginEndSp
    private val innerMarginBetweenSp = builder.innerMarginBetweenSp
    private val endMarkMarginEndSp = builder.endMarkMarginEndSp
    private val textLineSpaceSp = builder.textLineSpaceSp
    // ✅ **FIX**: Store infoTextSizeSp as a member variable
    private var infoTextSizeSp = builder.infoTextSizeSp


    init {
        val innerLayoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        innerRelativeLayout = RelativeLayout(builder.c).apply {
            layoutParams = innerLayoutParams
        }
        this.addView(innerRelativeLayout)

        val params = RelativeLayout.LayoutParams(builder.layoutWidth, builder.layoutHeight).apply {
            marginStart = floatSp2intPx(builder.outerMarginStartSp)
            marginEnd = floatSp2intPx(builder.outerMarginEndSp)
            topMargin = floatSp2intPx(builder.outerMarginTopSp)
            bottomMargin = floatSp2intPx(builder.outerMarginBottomSp)
        }
        this.layoutParams = params

        builder.cardBackground?.let { this.background = it }
        builder.strokeColor?.let { this.strokeColor = it }
        this.isClickable = true

        // --- Icon Setup ---
        if (hasIcon) {
            iconView = SVGImageView(c).apply {
                setImageAsset(builder.iconPath)
                visibility = View.VISIBLE
                val iconViewSize = builder.iconSize
                layoutParams = RelativeLayout.LayoutParams(iconViewSize, iconViewSize).apply {
                    addRule(RelativeLayout.CENTER_VERTICAL)
                    marginStart = floatSp2intPx(innerMarginStartSp)
                }
            }
            innerRelativeLayout.addView(iconView)
        }

        // --- End Mark Setup ---
        endMarkView = TextView(c).apply {
            text = builder.endMark
            textSize = builder.endMarkTextSizeSp
            visibility = if (hasEndMark) View.VISIBLE else View.GONE
            layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            )
        }
        val endMarkContainer = LinearLayout(c).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = floatSp2intPx(endMarkMarginEndSp)
                addRule(RelativeLayout.ALIGN_PARENT_END)
                addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
            }
            addView(endMarkView)
            val endMarkMarginBottomView = View(c).apply {
                layoutParams = LinearLayout.LayoutParams(0, floatSp2intPx(builder.endMarkMarginBottomSp))
            }
            addView(endMarkMarginBottomView)
        }
        innerRelativeLayout.addView(endMarkContainer)


        // --- Main Info Layout (Title + Info List) ---
        infoLinearLayout = LinearLayout(c).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
        }
        // Title View
        titleTV = TextView(c).apply {
            text = builder.title
            textSize = builder.titleTextSizeSp
            maxEms = builder.titleMaxEms
            maxLines = builder.titleMaxLines
            ellipsize = builder.titleEllipsize
        }
        infoLinearLayout.addView(titleTV)
        rebuildInfoList() // Initial build of the info list

        val infoLinearLayoutParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            marginStart = floatSp2intPx(
                innerMarginStartSp + if (hasIcon) innerMarginBetweenSp else 0f
            ) + (iconView?.layoutParams?.width ?: 0)
            marginEnd = floatSp2intPx(innerMarginEndSp)
            topMargin = floatSp2intPx(builder.innerMarginTopSp)
            bottomMargin = floatSp2intPx(builder.innerMarginBottomSp)
        }
        infoLinearLayout.layoutParams = infoLinearLayoutParams
        innerRelativeLayout.addView(infoLinearLayout)
    }

    // ========== PUBLIC SETTER METHODS ==========

    fun setSpMultiply(spMultiply: Float) {
        this.spMultiply = spMultiply
        // Note: Re-applying all dimensions would be complex. This is best set at creation.
    }

    fun setCardBackground(drawable: Drawable?) {
        this.background = drawable
    }

    fun setStrokeColor(color: Int?) {
        this.strokeColor = color ?: 0 // Or some default color
    }

    fun setIcon(iconPath: String) {
        if (hasIcon) {
            iconView?.setImageAsset(iconPath)
        }
    }

    fun setIconSize(size: Int) {
        iconView?.let {
            val params = it.layoutParams
            params.width = size
            params.height = size
            it.layoutParams = params
        }
    }

    fun setEndMark(mark: String) {
        if (hasEndMark) {
            endMarkView.text = mark
        }
    }

    fun setEndMarkTextSize(sizeSp: Float) {
        endMarkView.textSize = sizeSp
    }

    fun setTitle(title: String) {
        titleTV.text = title
    }

    fun setTitleTextSize(sizeSp: Float) {
        titleTV.textSize = sizeSp
    }

    fun setInfoTextSize(sizeSp: Float) {
        this.infoTextSizeSp = sizeSp
        rebuildInfoList() // Rebuild list with new text size
    }


    /**
     * Clears the current list of info items. Call this before adding new ones
     * if you are refreshing the card's data.
     */
    fun clearInfo() {
        infoList.clear()
        rebuildInfoList()
    }

    /**
     * Adds a single info item to the list and refreshes the view.
     */
    fun addInfo(info: Info) {
        infoList.add(info)
        rebuildInfoList()
    }

    /**
     * Adds a single info item to the list and refreshes the view.
     */
    fun addInfo(title: String, text: String?, divider: String = "：") {
        infoList.add(Info(title, text, divider))
        rebuildInfoList()
    }

    /**
     * Re-inflates the info list views based on the current `infoList`.
     * This is the key method for updating the info section.
     */
    private fun rebuildInfoList() {
        // Remove all views except the title, which is the first child (index 0)
        if (infoLinearLayout.childCount > 1) {
            infoLinearLayout.removeViews(1, infoLinearLayout.childCount - 1)
        }

        val endMarkTVLen = endMarkView.paint.measureText(endMarkView.text.toString()).toInt()

        infoList.forEach { infoItem ->
            val row = LinearLayout(c).apply {
                orientation = LinearLayout.HORIZONTAL
                val rowParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    if (hasEndMark) {
                        marginEnd = (floatSp2intPx(
                            innerMarginBetweenSp + endMarkMarginEndSp - innerMarginEndSp
                        ) + endMarkTVLen).coerceAtLeast(0)
                    }
                    topMargin = floatSp2intPx(textLineSpaceSp)
                }
                layoutParams = rowParams
            }

            val tvTitle = TextView(c).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                // ✅ **FIX**: Use the member variable
                textSize = this@InfoCard.infoTextSizeSp
                text = "${infoItem.title}："
            }

            val tvText = TextView(c).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                // ✅ **FIX**: Use the member variable
                textSize = this@InfoCard.infoTextSizeSp
                text = infoItem.text
            }

            row.addView(tvTitle)
            row.addView(tvText)
            infoLinearLayout.addView(row)
        }
    }


    // ========== BUILDER and DATA CLASS (Unchanged) ==========

    data class Info(val title: String, val text: String?, val divider: String = "：")

    private fun floatSp2intPx(value: Float): Int {
        return (value * spMultiply).toInt()
    }

    class Builder constructor(context: Context) {
        val c = context
        var spMultiply = 1f
        fun setSpMultiply(spMultiply: Float) = apply { this.spMultiply = spMultiply }
        var cardBackground: Drawable? = null
        fun setCardBackground(cardBackground: Drawable?) = apply { this.cardBackground = cardBackground }
        var outerMarginBottomSp = 12f
        fun setOuterMarginBottomSp(outerMarginBottomSp: Float) = apply { this.outerMarginBottomSp = outerMarginBottomSp }
        var outerMarginTopSp = 0f
        fun setOuterMarginTopSp(outerMarginTopSp: Float) = apply { this.outerMarginTopSp = outerMarginTopSp }
        var outerMarginStartSp = 0f
        fun setOuterMarginStartSp(outerMarginStartSp: Float) = apply { this.outerMarginStartSp = outerMarginStartSp }
        var outerMarginEndSp = 0f
        fun setOuterMarginEndSp(outerMarginEndSp: Float) = apply { this.outerMarginEndSp = outerMarginEndSp }
        var innerMarginBetweenSp = 12f
        fun setInnerMarginBetweenSp(innerMarginBetweenSp: Float) = apply { this.innerMarginBetweenSp = innerMarginBetweenSp }
        var innerMarginTopSp = 12f
        fun setInnerMarginTopSp(innerMarginTopSp: Float) = apply { this.innerMarginTopSp = innerMarginTopSp }
        var innerMarginBottomSp = 12f
        fun setInnerMarginBottomSp(innerMarginBottomSp: Float) = apply { this.innerMarginBottomSp = innerMarginBottomSp }
        var innerMarginStartSp = 12f
        fun setInnerMarginStartSp(innerMarginStartSp: Float) = apply { this.innerMarginStartSp = innerMarginStartSp }
        var innerMarginEndSp = 12f
        fun setInnerMarginEndSp(innerMarginEndSp: Float) = apply { this.innerMarginEndSp = innerMarginEndSp }
        var textLineSpaceSp = 1f
        fun setTextLineSpaceSp(textLineSpaceSp: Float) = apply { this.textLineSpaceSp = textLineSpaceSp }
        var layoutWidth = LayoutParams.MATCH_PARENT
        fun setLayoutWidth(layoutWidth: Int) = apply { this.layoutWidth = layoutWidth }
        var layoutHeight = LayoutParams.WRAP_CONTENT
        fun setLayoutHeight(layoutHeight: Int) = apply { this.layoutHeight = layoutHeight }
        fun setLayoutHeightSp(layoutHeightSp: Float) = apply { this.layoutHeight = (layoutHeightSp * spMultiply).toInt() }
        var hasIcon = true
        fun setHasIcon(hasIcon: Boolean) = apply { this.hasIcon = hasIcon }
        var iconPath = "🍓"
        fun setIcon(icon: String) = apply { this.iconPath = icon }
        var iconSize = 150
        @Deprecated("No longer used.")
        fun setIconTextSizeSp(com: Float) = apply { /* deprecated */ }
        fun setIconSize(iconSize: Int) = apply { this.iconSize = iconSize }
        var hasEndMark = false
        fun setHasEndMark(hasEndMark: Boolean) = apply { this.hasEndMark = hasEndMark }
        var endMark = "A"
        fun setEndMark(endMark: String) = apply { this.endMark = endMark }
        var endMarkTextSizeSp = 52f
        fun setEndMarkTextSizeSp(endMarkTextSizeSp: Float) = apply { this.endMarkTextSizeSp = endMarkTextSizeSp }
        var endMarkMarginEndSp = 24f
        fun setEndMarkMarginEndSp(endMarkMarginEndSp: Float) = apply { this.endMarkMarginEndSp = endMarkMarginEndSp }
        var endMarkMarginBottomSp = 18f
        fun setEndMarkMarginBottomSp(endMarkMarginBottomSp: Float) = apply { this.endMarkMarginBottomSp = endMarkMarginBottomSp }
        var title = "标题"
        fun setTitle(title: String) = apply { this.title = title }
        var titleTextSizeSp = 24f
        fun setTitleTextSizeSp(titleTextSizeSp: Float) = apply { this.titleTextSizeSp = titleTextSizeSp }
        fun setTitleMaxEms(titleMaxEms: Int) = apply { this.titleMaxEms = titleMaxEms }
        var titleMaxEms = 12
        fun setTitleMaxLines(titleMaxLines: Int) = apply { this.titleMaxLines = titleMaxLines }
        var titleMaxLines = 1
        fun setTitleEllipsize(titleEllipsize: TextUtils.TruncateAt) = apply { this.titleEllipsize = titleEllipsize }
        var titleEllipsize = TextUtils.TruncateAt.END
        fun setInfoTextSizeSp(infoTextSizeSp: Float) = apply { this.infoTextSizeSp = infoTextSizeSp }
        var strokeColor: Int? = null
        fun setStrokeColor(color: Int?) = apply { this.strokeColor = color }
        var infoTextSizeSp = 14f
        val infoList = ArrayList<Info>()
        fun addInfo(info: Info) = apply { this.infoList.add(info) }
        fun addInfo(title: String, text: String?, divider: String = "：") = apply { this.infoList.add(Info(title, text, divider)) }
        fun build(): InfoCard = InfoCard(this)
    }
}

