package com.francescozoccheddu.tdmclient.ui.components

import android.view.ViewGroup
import android.widget.ImageButton
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.constraintlayout.motion.widget.TransitionAdapter
import com.francescozoccheddu.tdmclient.R

class UserStatsComponent(parent: ViewGroup) {

    private companion object {
        private const val CLOSE_BUTTON_ENABLE_PROGRESS = 0.9f
    }

    private val root = parent.findViewById<MotionLayout>(R.id.us_root)

    init {
        val closeButton = parent.findViewById<ImageButton>(R.id.uss_close).apply {
            setOnClickListener { root.transitionToState(R.id.us_cs_idle) }
        }
        val closeButtonAvd = CrossAVD(closeButton)

        root.setTransitionListener(object : TransitionAdapter() {

            override fun onTransitionChange(motionLayout: MotionLayout?, startId: Int, endId: Int, progress: Float) {
                closeButtonAvd.state = if (endId == R.id.us_cs_expanded) {
                    if (startId == R.id.us_cs_expanded || progress >= CLOSE_BUTTON_ENABLE_PROGRESS)
                        CrossAVD.State.VISIBLE
                    else
                        CrossAVD.State.GONE
                } else if (startId == R.id.us_cs_expanded) {
                    if (endId == R.id.us_cs_expanded || progress <= 1f - CLOSE_BUTTON_ENABLE_PROGRESS)
                        CrossAVD.State.VISIBLE
                    else
                        CrossAVD.State.GONE
                } else CrossAVD.State.GONE
            }

            override fun onTransitionCompleted(motionLayout: MotionLayout?, currentId: Int) {
                closeButtonAvd.state =
                    if (currentId == R.id.us_cs_expanded) CrossAVD.State.VISIBLE
                    else CrossAVD.State.GONE
            }

        })
    }

    var enabled = true
    var score = 0
    var level = 0
    var multiplier = 1f
    var nextLevel: Int? = null
    var onLevelNotified: ((Int) -> Unit)? = null

}