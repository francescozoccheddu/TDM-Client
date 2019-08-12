package com.francescozoccheddu.tdmclient.ui.bottomgroup


import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.RelativeLayout
import com.francescozoccheddu.animatorhelpers.ABFloat
import com.francescozoccheddu.tdmclient.R
import com.francescozoccheddu.tdmclient.utils.android.visible
import kotlinx.android.synthetic.main.bg_info.view.bg_info_bt
import kotlinx.android.synthetic.main.bg_info.view.bg_info_iv
import kotlinx.android.synthetic.main.bg_info.view.bg_info_pb
import kotlinx.android.synthetic.main.bg_info.view.bg_info_tv

class InfoComponent @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr), BottomGroup.Component {


    init {
        View.inflate(context, R.layout.bg_info, this)
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

    var text = ""
        set(value) {
            if (value != field) {
                field = value
                bg_info_tv.text = value
            }
        }

    var action: String? = null
        set(value) {
            if (value != field) {
                field = value
                bg_info_bt.apply {
                    visible = value != null
                    if (value != null)
                        text = value
                }
            }
        }

    var loading = true
        set(value) {
            if (value != field) {
                field = value
                bg_info_pb.visible = value
                bg_info_iv.visible = !value
            }
        }

    var icon: Int = R.drawable.ic_back
        set(value) {
            if (field != value) {
                field = value
                bg_info_iv.setImageResource(value)
            }
        }

    inline fun onAction(crossinline callback: () -> Unit) = bg_info_bt.setOnClickListener { callback() }

}