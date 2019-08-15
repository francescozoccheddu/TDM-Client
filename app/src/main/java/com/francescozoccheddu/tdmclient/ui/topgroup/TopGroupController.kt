package com.francescozoccheddu.tdmclient.ui.topgroup

import com.francescozoccheddu.tdmclient.data.PlaceQuerier

class TopGroupController(private val group: TopGroup) {

    init {
        group.search.onDestinationChosen = {
            onDestinationChosen?.invoke(it)
        }
    }

    enum class State {
        SEARCHING, SCORE, HIDDEN
    }

    var state = State.HIDDEN
        set(value) {
            if (value != field) {
                field = value
                group.state = when (value) {
                    State.SEARCHING -> if (group.state == TopGroup.State.SEARCHING) TopGroup.State.SEARCHING else TopGroup.State.SEARCH
                    State.SCORE -> TopGroup.State.SCORE
                    State.HIDDEN -> TopGroup.State.HIDDEN
                }
            }
        }

    var score
        get() = group.score.score
        set(value) {
            group.score.score = value
        }

    var onDestinationChosen: ((PlaceQuerier.Location) -> Unit)? = null

}