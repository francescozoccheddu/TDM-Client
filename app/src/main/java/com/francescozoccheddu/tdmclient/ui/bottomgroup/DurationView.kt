package com.francescozoccheddu.tdmclient.ui.bottomgroup


import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.RelativeLayout
import com.francescozoccheddu.animatorhelpers.ABFloat
import com.francescozoccheddu.tdmclient.R
import com.francescozoccheddu.tdmclient.utils.android.visible

class DurationView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr), BottomGroup.Component {


    init {
        View.inflate(context, R.layout.bg_duration, this)
    }

    private var animationAlpha by ABFloat(if (visible) 1f else 0f).apply {
        onUpdate = {
            alpha = it.value
            visible = it.value != 0f
            if (!it.running) {
                callback?.invoke()
                callback = null
            }
        }
        speed = 6f
    }

    var callback: (() -> Unit)? = null

    override fun animate(mode: BottomGroup.AnimationMode, callback: (() -> Unit)?) {
        this.callback = callback
        animationAlpha = when (mode) {
            BottomGroup.AnimationMode.IN -> 1f
            BottomGroup.AnimationMode.OUT -> 0f
        }
    }

}