package com.francescozoccheddu.tdmclient.ui.components.us

import android.view.ViewGroup
import com.airbnb.lottie.LottieAnimationView
import com.francescozoccheddu.tdmclient.R
import com.francescozoccheddu.tdmclient.data.UserStats
import com.francescozoccheddu.tdmclient.utils.android.OverlayMotionLayout
import com.francescozoccheddu.tdmclient.utils.android.hsv
import com.robinhood.ticker.TickerView
import nl.dionsegijn.konfetti.KonfettiView
import nl.dionsegijn.konfetti.models.Shape
import nl.dionsegijn.konfetti.models.Size

class UserStatsComponent(parent: ViewGroup) {

    private companion object {
        private const val MIN_NOTIFY_GAIN = 20
    }

    private val root = parent.findViewById<OverlayMotionLayout>(R.id.us_root)
    private val gainComponent = GainComponent(parent)
    private val scoreText = parent.findViewById<TickerView>(R.id.us_score_tv)
    private val levelText = parent.findViewById<TickerView>(R.id.us_level_tv)
    private val sheet = parent.findViewById<UserStatsSheet>(R.id.us_sheet_root)
    private val fireworks = parent.findViewById<LottieAnimationView>(R.id.us_fireworks)

    init {
        parent.findViewById<UserStatsSheet>(R.id.us_sheet_root).onCloseRequested = this::requestClose
        root.addHitRect(R.id.us_score_root)
        root.addHitRect(R.id.us_level_root)
        root.addHitRect(R.id.us_gain_root)
        root.addHitRect(R.id.us_sheet_root)
        root.setTransition(R.id.us_cs_gone, R.id.us_cs_gone)
        root.transitionToEnd()
    }

    private fun updateCloseTransition() {
        root.getTransition(R.id.us_t_idle_to_expanded_sheet).setEnable(draggable && enabled)
        root.getTransition(R.id.us_t_gone_to_expanded_sheet).setEnable(draggable && !enabled)
    }

    private var draggable = true
        set(value) {
            root.getTransition(R.id.us_t_idle_to_expanded_gain).setEnable(value)
            root.getTransition(R.id.us_t_idle_to_expanded_score).setEnable(value)
            root.getTransition(R.id.us_t_idle_to_expanded_level).setEnable(value)
            updateCloseTransition()
        }

    private fun requestClose() {
        if (root.currentState == R.id.us_cs_expanded)
            root.transitionToState(
                if (enabled) R.id.us_cs_idle
                else R.id.us_cs_gone
            )
    }

    var enabled = false
        set(value) {
            if (value != field) {
                field = value
                draggable = value
                root.transitionToState(
                    if (value) R.id.us_cs_idle
                    else R.id.us_cs_gone
                )
            }
        }


    var stats = UserStats(0, 0, 1f, null, 0, "", "")
        set(value) {
            if (value != field) {
                val gain = value.score - field.score
                if (gain > MIN_NOTIFY_GAIN)
                    gainComponent.notify(gain)
                field = value
                sheet.stats = value
                scoreText.text = value.score.toString()
                levelText.text = (value.level + 1).toString()
            }
        }

    private val tempLocation = IntArray(2)

    fun levelUpParty(confetti: KonfettiView) {
        fireworks.playAnimation()
        fireworks.getLocationOnScreen(tempLocation)
        var x = tempLocation[0].toFloat() + fireworks.width / 2f
        var y = tempLocation[1].toFloat() + fireworks.height / 2f
        confetti.getLocationOnScreen(tempLocation)
        x -= tempLocation[0]
        y -= tempLocation[1]
        confetti.build()
            .addColors(hsv(0f, 1f, 1f), hsv(30f, 1f, 1f), hsv(60f, 1f, 1f), hsv(330f, 1f, 1f))
            .setDirection(0.0, 359.0)
            .setSpeed(0f, 10f)
            .setFadeOutEnabled(true)
            .setTimeToLive(5000L)
            .addShapes(Shape.RECT, Shape.CIRCLE)
            .addSizes(Size(5))
            .setPosition(x, y)
            .burst(300)
    }

}