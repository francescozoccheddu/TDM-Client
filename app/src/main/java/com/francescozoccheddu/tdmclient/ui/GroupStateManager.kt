package com.francescozoccheddu.tdmclient.ui

import android.view.View
import androidx.constraintlayout.motion.widget.MotionLayout

class GroupStateManager<StateType : GroupStateManager.GroupState>(
    private val layout: MotionLayout,
    initialState: StateType
) {

    interface GroupState {

        val constraintSetId: Int
        val componentId: Int?

    }

    interface GroupComponent {

        enum class Mode {
            IN, OUT
        }

        fun animate(mode: Mode, callback: (() -> Unit)? = null)

    }

    init {
        layout.setTransitionListener(object : MotionLayout.TransitionListener {

            override fun onTransitionTrigger(ml: MotionLayout?, from: Int, to: Boolean, progress: Float) {}

            override fun onTransitionStarted(ml: MotionLayout?, from: Int, to: Int) {}

            override fun onTransitionChange(ml: MotionLayout?, from: Int, to: Int, progress: Float) {}

            override fun onTransitionCompleted(ml: MotionLayout?, state: Int) {
                updateAnimation()
            }

        })
        layout.setTransition(initialState.constraintSetId, initialState.constraintSetId)
        layout.transitionToEnd()
    }

    private val Int.view get() = layout.findViewById<View>(this) as GroupComponent

    var state: StateType = initialState
        set(value) {
            if (value != field) {
                field = value
                updateAnimation()
            }
        }

    private var currentComponentId: Int? = null

    var running = false
        private set

    var onTransitionCompleted: (() -> Unit)? = null

    private fun updateAnimation() {
        if (layout.progress != 0f && layout.progress != 1f)
            return
        running = true
        // Remove
        val componentIdSnapshot = currentComponentId
        if (state.componentId != componentIdSnapshot && componentIdSnapshot != null) {
            componentIdSnapshot.view.animate(GroupComponent.Mode.OUT) {
                if (currentComponentId == componentIdSnapshot)
                    currentComponentId = null
                updateAnimation()
            }
        }
        else {
            // Transition
            if (layout.currentState != state.constraintSetId)
                layout.transitionToState(state.constraintSetId)
            else {
                running = false
                currentComponentId = state.componentId
                currentComponentId?.view?.animate(GroupComponent.Mode.IN)
                onTransitionCompleted?.invoke()
            }
        }
    }

}