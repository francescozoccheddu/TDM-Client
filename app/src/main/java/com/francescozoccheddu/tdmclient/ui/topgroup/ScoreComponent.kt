package com.francescozoccheddu.tdmclient.ui.topgroup


import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.RelativeLayout
import com.francescozoccheddu.animatorhelpers.ABFloat
import com.francescozoccheddu.tdmclient.ui.utils.GroupStateManager
import com.francescozoccheddu.tdmclient.utils.android.visible
import com.robinhood.ticker.TickerUtils
import kotlinx.android.synthetic.main.tg_score.view.tg_score_tv


class ScoreComponent @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr), GroupStateManager.GroupComponent {


    init {
        View.inflate(context, com.francescozoccheddu.tdmclient.R.layout.tg_score, this)
        tg_score_tv.apply {
            setCharacterLists(TickerUtils.provideNumberList())
            text = score.toString()
            animationInterpolator = OvershootInterpolator()
        }
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

    override fun animate(mode: GroupStateManager.GroupComponent.Mode, callback: (() -> Unit)?) {
        this.animationCallback = callback
        animationAlpha = when (mode) {
            GroupStateManager.GroupComponent.Mode.IN -> 1f
            GroupStateManager.GroupComponent.Mode.OUT -> 0f
        }
    }

    var score = 0
        set(value) {
            if (value != field) {
                field = value
                tg_score_tv.text = value.toString()
            }
        }

}