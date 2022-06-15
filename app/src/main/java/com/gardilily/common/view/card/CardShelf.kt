package com.gardilily.common.view.card

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.LinearLayout

open class CardShelf internal constructor(
	builder: Builder
) : LinearLayout(builder.c) {

	constructor(context: Context) : this(Builder(context))

	val c = context

	var spMultiply = builder.spMultiply
	var shelfBackground = builder.shelfBackground
	var outerMarginBottomSp = builder.outerMarginBottomSp
	var outerMarginTopSp = builder.outerMarginTopSp
	var outerMarginStartSp = builder.outerMarginStartSp
	var outerMarginEndSp = builder.outerMarginEndSp
	var innerMarginBetweenSp = builder.innerMarginBetweenSp
	var innerPaddingTopSp = builder.innerPaddingTopSp
	var innerPaddingBottomSp = builder.innerPaddingBottomSp
	var innerPaddingStartSp = builder.innerPaddingStartSp
	var innerPaddingEndSp = builder.innerPaddingEndSp
	val layoutWidth = builder.layoutWidth
	val layoutHeight = builder.layoutHeight
	var cardPerRow = builder.cardPerRow
	//var rowHeight = builder.rowHeight

	init {
		val params = LayoutParams(
			layoutWidth,
			layoutHeight
		)
		params.marginStart = floatSp2intPx(outerMarginStartSp)
		params.marginEnd = floatSp2intPx(outerMarginEndSp)
		params.topMargin = floatSp2intPx(outerMarginTopSp)
		params.bottomMargin = floatSp2intPx(outerMarginBottomSp)

		this.layoutParams = params

		this.orientation = VERTICAL

		this.setPadding(
			floatSp2intPx(innerPaddingStartSp),
			floatSp2intPx(innerPaddingTopSp),
			floatSp2intPx(innerPaddingEndSp),
			floatSp2intPx(innerPaddingBottomSp)
		)


		if (shelfBackground != null) {
			this.shelfBackground = shelfBackground
		}

		this.isClickable = false
	}

	class Builder constructor(context: Context) {
		val c = context

		var spMultiply = 1f
		fun setSpMultiply(spMultiply: Float) = apply {
			this.spMultiply = spMultiply
		}

		var shelfBackground: Drawable? = null
		fun setShelfBackground(shelfBackground: Drawable?) = apply {
			this.shelfBackground = shelfBackground
		}

		var outerMarginBottomSp = 12f
		fun setOuterMarginBottomSp(outerMarginBottomSp: Float) = apply {
			this.outerMarginBottomSp = outerMarginBottomSp
		}

		var outerMarginTopSp = 0f
		fun setOuterMarginTopSp(outerMarginTopSp: Float) = apply {
			this.outerMarginTopSp = outerMarginTopSp
		}

		var outerMarginStartSp = 0f
		fun setOuterMarginStartSp(outerMarginStartSp: Float) = apply {
			this.outerMarginStartSp = outerMarginStartSp
		}

		var outerMarginEndSp = 0f
		fun setOuterMarginEndSp(outerMarginEndSp: Float) = apply {
			this.outerMarginEndSp = outerMarginEndSp
		}

		var innerMarginBetweenSp = 12f
		fun setInnerMarginBetweenSp(innerMarginBetweenSp: Float) = apply {
			this.innerMarginBetweenSp = innerMarginBetweenSp
		}

		var innerPaddingTopSp = 12f
		fun setInnerPaddingTopSp(innerPaddingTopSp: Float) = apply {
			this.innerPaddingTopSp = innerPaddingTopSp
		}

		var innerPaddingBottomSp = 12f
		fun setInnerPaddingBottomSp(innerPaddingBottomSp: Float) = apply {
			this.innerPaddingBottomSp = innerPaddingBottomSp
		}

		var innerPaddingStartSp = 12f
		fun setInnerPaddingStartSp(innerPaddingStartSp: Float) = apply {
			this.innerPaddingStartSp = innerPaddingStartSp
		}

		var innerPaddingEndSp = 12f
		fun setInnerPaddingEndSp(innerPaddingEndSp: Float) = apply {
			this.innerPaddingEndSp = innerPaddingEndSp
		}

		var layoutWidth = LayoutParams.MATCH_PARENT
		fun setLayoutWidth(layoutWidth: Int) = apply {
			this.layoutWidth = layoutWidth
		}

		var layoutHeight = LayoutParams.WRAP_CONTENT
		fun setLayoutHeight(layoutHeight: Int) = apply {
			this.layoutHeight = layoutHeight
		}
		fun setLayoutHeightSp(layoutHeightSp: Float) = apply {
			this.layoutHeight = (layoutHeightSp * spMultiply).toInt()
		}

		var cardPerRow = 3
		fun setCardPerRow(cardPerRow: Int) = apply {
			this.cardPerRow = cardPerRow
		}

		//var rowHeight = LayoutParams.WRAP_CONTENT
		//fun setRowHeight(rowHeight: Int) = apply {
		//	this.rowHeight = rowHeight
		//}
		//fun setRowHeightSp(rowHeightSp: Float) = apply {
		//	this.rowHeight = (rowHeightSp * spMultiply).toInt()
		//}

		fun build(): CardShelf = CardShelf(this)
	}

	private var cardRowLayout: LinearLayout? = null
	private var cardCount = 0

	fun addCard(card: View) {
		fun generateRowLayout(): LinearLayout {
			val params = LayoutParams(
				LayoutParams.MATCH_PARENT,
				LayoutParams.WRAP_CONTENT
				//rowHeight
			)

			if (cardRowLayout != null)
				params.topMargin = floatSp2intPx(innerMarginBetweenSp)

			val ret = LinearLayout(c)
			ret.layoutParams = params
			ret.orientation = HORIZONTAL

			return ret
		}

		if (cardCount % cardPerRow == 0) {
			cardRowLayout = generateRowLayout()
			this.addView(cardRowLayout)
		}

		val cardParams = LayoutParams(
			0,
			card.layoutParams.height
		)

		cardParams.weight = 1f

		if (cardCount % cardPerRow > 0)
			cardParams.marginStart = floatSp2intPx(innerMarginBetweenSp / 2)
		if (cardCount % cardPerRow < cardPerRow - 1)
			cardParams.marginEnd = floatSp2intPx(innerMarginBetweenSp / 2)

		card.layoutParams = cardParams

		cardRowLayout!!.addView(card)

		cardCount++
	}

	fun fillLastRow() {
		if (cardRowLayout == null)
			return

		while (cardCount % cardPerRow > 0) {
			val emptyView = View(c)
			val params = LayoutParams(
				0,
				0
			)
			params.weight = 1f

			if (cardCount % cardPerRow > 0)
				params.marginStart = floatSp2intPx(innerMarginBetweenSp / 2)
			if (cardCount % cardPerRow < cardPerRow - 1)
				params.marginEnd = floatSp2intPx(innerMarginBetweenSp / 2)

			emptyView.layoutParams = params

			cardRowLayout!!.addView(emptyView)
			cardCount++
		}

	}

	/**
	 * 将 Sp 转换为 Px 单位。并将原浮点数变为整数。
	 *
	 * @param value - 以 sp 为单位的长度
	 * @return 以 px 为单位的长度
	 */
	private fun floatSp2intPx(value: Float): Int {
		return (value * spMultiply).toInt()
	}
}
