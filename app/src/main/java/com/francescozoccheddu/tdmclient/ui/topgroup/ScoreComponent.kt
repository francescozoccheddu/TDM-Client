package com.francescozoccheddu.tdmclient.ui.topgroup


import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.RelativeLayout
import com.robinhood.ticker.TickerUtils
import kotlinx.android.synthetic.main.tg_score.view.tg_score_tv


class ScoreComponent @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {


    init {
        View.inflate(context, com.francescozoccheddu.tdmclient.R.layout.tg_score, this)
        tg_score_tv.apply {
            setCharacterLists(TickerUtils.provideNumberList())
            text = score.toString()
            animationInterpolator = OvershootInterpolator()
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