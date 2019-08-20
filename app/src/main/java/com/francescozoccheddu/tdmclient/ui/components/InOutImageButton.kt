package com.francescozoccheddu.tdmclient.ui.components

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageButton

class InOutImageButton @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ImageButton(context, attrs, defStyleAttr) {

    private val avd = InOutAVD(this)

    fun show() {
        shown = true
    }

    fun hide() {
        shown = false
    }

    var shown
        get() = avd.state == InOutAVD.State.VISIBLE
        set(value) {
            avd.state = if (value) InOutAVD.State.VISIBLE else InOutAVD.State.GONE
        }

}