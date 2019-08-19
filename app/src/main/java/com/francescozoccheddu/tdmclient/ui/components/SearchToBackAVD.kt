package com.francescozoccheddu.tdmclient.ui.components

import android.widget.ImageView
import com.francescozoccheddu.tdmclient.R
import com.francescozoccheddu.tdmclient.ui.utils.AVD

class SearchToBackAVD(view: ImageView) : AVD<SearchToBackAVD.State>(view, State.SEARCH) {

    enum class State(override val state: Int) : AVD.State {
        BACK(R.attr.sb_state_back), SEARCH(R.attr.sb_state_search)
    }

}