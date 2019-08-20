package com.francescozoccheddu.tdmclient.ui.components

import android.view.ViewGroup
import com.francescozoccheddu.tdmclient.R
import com.francescozoccheddu.tdmclient.utils.android.OverlayMotionLayout

class UserStatsComponent(parent: ViewGroup) {

    private val root = parent.findViewById<OverlayMotionLayout>(R.id.us_root)

    init {
        parent.findViewById<UserStatsSheet>(R.id.us_sheet_root).onClose = this::requestClose
        root.addHitRect(R.id.us_score_root)
        root.addHitRect(R.id.us_level_root)
        root.addHitRect(R.id.us_gain_root)
        root.addHitRect(R.id.us_sheet_root)
        root.setTransition(R.id.us_cs_idle, R.id.us_cs_idle)
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

    var enabled = true
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

    var score = 0
    var level = 0
    var multiplier = 1f
    var nextLevel: Int? = null
    var onLevelNotified: ((Int) -> Unit)? = null

}