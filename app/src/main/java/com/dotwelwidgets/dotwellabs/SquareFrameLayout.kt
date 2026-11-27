package com.dotwelwidgets.dotwellabs

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout

/**
 * A square tile surface for the category grid. The width is supplied by the
 * parent row (weighted two-column layout or a fixed single-column width), and
 * the height follows it so the rounded tiles stay square at any font scale.
 */
class SquareFrameLayout(context: Context, attrs: AttributeSet? = null) :
    FrameLayout(context, attrs) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        super.onMeasure(
            widthMeasureSpec,
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
        )
    }
}
