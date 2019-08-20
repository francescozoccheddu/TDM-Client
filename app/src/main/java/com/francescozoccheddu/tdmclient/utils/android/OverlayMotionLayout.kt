package com.francescozoccheddu.tdmclient.utils.android

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.constraintlayout.motion.widget.MotionLayout
import kotlin.math.roundToInt

class OverlayMotionLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : MotionLayout(context, attrs, defStyleAttr) {

    val hitRects = mutableSetOf<View>()

    fun addHitRect(id: Int) {
        hitRects.add(findViewById(id))
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val rect = Rect()
        return if (event != null) {
            val starting = when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> true
                else -> false
            }
            if (!starting || hitRects.any {
                    it.getHitRect(rect)
                    rect.contains(event.x.roundToInt(), event.y.roundToInt())
                })
                super.onTouchEvent(event)
            else false
        }
        else false
    }

}