package com.francescozoccheddu.tdmclient.ui.bottomgroup


import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import com.francescozoccheddu.tdmclient.R

class ActionComponent @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {


    init {
        View.inflate(context, R.layout.bg_action, this)
    }

    inline fun onClick(crossinline callback: () -> Unit) = setOnClickListener { callback() }

}