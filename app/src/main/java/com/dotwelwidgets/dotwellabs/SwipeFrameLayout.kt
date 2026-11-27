package com.dotwelwidgets.dotwellabs

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.FrameLayout
import kotlin.math.abs

/**
 * Lets the category sheet preview be swiped horizontally without stealing
 * vertical scrolling from its parent ScrollView. A gesture is claimed only when
 * horizontal movement clearly dominates, then the swipe direction is reported
 * once on release.
 */
class SwipeFrameLayout(context: Context, attrs: AttributeSet? = null) :
    FrameLayout(context, attrs) {

    var onSwipe: ((direction: Int) -> Unit)? = null

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var downX = 0f
    private var downY = 0f
    private var horizontal = false
    private var moved = false

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.x
                downY = ev.y
                horizontal = false
                moved = false
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = ev.x - downX
                val dy = ev.y - downY
                if (!horizontal &&
                    abs(dx) > touchSlop &&
                    abs(dx) > abs(dy) * 1.3f
                ) {
                    horizontal = true
                    moved = true
                    return true
                }
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL,
            -> {
                horizontal = false
            }
        }
        return false
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> return true
            MotionEvent.ACTION_MOVE -> {
                if (abs(ev.x - downX) > touchSlop) moved = true
                return true
            }
            MotionEvent.ACTION_UP -> {
                val dx = ev.x - downX
                if (moved && abs(dx) > touchSlop * 2f) {
                    onSwipe?.invoke(if (dx < 0f) 1 else -1)
                } else {
                    performClick()
                }
                horizontal = false
                moved = false
            }
            MotionEvent.ACTION_CANCEL -> {
                horizontal = false
                moved = false
            }
        }
        return true
    }

    override fun performClick(): Boolean = super.performClick()
}
