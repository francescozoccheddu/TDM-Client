package com.francescozoccheddu.tdmclient.ui.bottomgroup


import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.RelativeLayout
import com.francescozoccheddu.animatorhelpers.ABFloat
import com.francescozoccheddu.tdmclient.R
import com.francescozoccheddu.tdmclient.utils.android.visible
import kotlinx.android.synthetic.main.bg_duration.view.bg_duration_cancel
import kotlinx.android.synthetic.main.bg_duration.view.bg_duration_confirm

class DurationComponent @JvmOverloads constructor(
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
                animationCallback?.invoke()
                animationCallback = null
            }
        }
        speed = 6f
    }

    private var animationCallback: (() -> Unit)? = null

    override fun animate(mode: BottomGroup.AnimationMode, callback: (() -> Unit)?) {
        this.animationCallback = callback
        animationAlpha = when (mode) {
            BottomGroup.AnimationMode.IN -> 1f
            BottomGroup.AnimationMode.OUT -> 0f
        }
    }

    inline fun onCancel(crossinline callback: () -> Unit) = bg_duration_cancel.setOnClickListener { callback() }
    inline fun onConfirm(crossinline callback: () -> Unit) = bg_duration_confirm.setOnClickListener { callback() }

}