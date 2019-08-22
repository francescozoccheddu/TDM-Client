package com.francescozoccheddu.tdmclient.ui.components.us

import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import com.francescozoccheddu.tdmclient.R
import com.francescozoccheddu.tdmclient.utils.android.Timer
import com.francescozoccheddu.tdmclient.utils.android.visible
import com.robinhood.ticker.TickerView

class GainComponent(parent: View) {

    private companion object {
        private const val DURATION = 1f
    }

    private val countdown = Timer().Countdown().apply {
        time = DURATION
        runnable = Runnable { container.startAnimation(outAnimation) }
    }

    private val inAnimation = AnimationUtils.loadAnimation(parent.context, R.anim.us_gain_in).apply {
        setAnimationListener(object : Animation.AnimationListener {

            override fun onAnimationRepeat(p0: Animation?) {}

            override fun onAnimationEnd(p0: Animation?) {}

            override fun onAnimationStart(p0: Animation?) {
                container.visible = true
                countdown.pull()
            }

        })
    }

    private val outAnimation: Animation = AnimationUtils.loadAnimation(parent.context, R.anim.us_gain_out).apply {
        setAnimationListener(
            object : Animation.AnimationListener {

                override fun onAnimationRepeat(p0: Animation?) {}

                override fun onAnimationEnd(p0: Animation?) {
                    countdown.cancel()
                    container.visible = false
                }

                override fun onAnimationStart(p0: Animation?) {}

            })
    }

    private val container = parent.findViewById<View>(R.id.us_gain)
    private val text = parent.findViewById<TickerView>(R.id.us_gain_tv)

    fun notify(gain: Int) {
        text.text = gain.toString()
        if (!container.visible)
            container.startAnimation(inAnimation)
    }

}