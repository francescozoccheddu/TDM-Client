package com.francescozoccheddu.tdmclient.ui.utils

import android.widget.ImageView

open class AVD<Type>(
    private val view: ImageView,
    initialState: Type
) where Type : Enum<Type>, Type : AVD.State {

    private val stateLists = run {
        val enums = initialState::class.java.enumConstants!!
        Array(enums.size) { i ->
            IntArray(enums.size) { j ->
                val state = (enums[j] as State).state
                if (j == i) state else -state
            }
        }
    }

    interface State {
        val state: Int
    }

    private val Type.stateList
        get() = stateLists[ordinal]

    init {
        view.setImageState(initialState.stateList, false)
    }

    var state = initialState
        set(value) {
            if (value != field) {
                field = value
                view.setImageState(value.stateList, true)
            }
        }

}