package com.francescozoccheddu.tdmclient.ui.utils

import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import com.francescozoccheddu.tdmclient.utils.android.visible

class GroupStateManager<StateType : GroupStateManager.GroupState>(
    private val layout: ViewGroup,
    initialState: StateType
) {

    interface GroupState {

        val componentId: Int?

    }

    init {
        setView(initialState.componentId)
    }

    private fun setView(componentId: Int?) {
        TransitionManager.beginDelayedTransition(layout)
        layout.children.forEach { it.visible = false }
        componentId?.view?.visible = true
    }


    private val Int.view get() = layout.findViewById<View>(this)

    var state: StateType = initialState
        set(value) {
            if (value != field) {
                println(value)
                setView(value.componentId)
            }
        }

}