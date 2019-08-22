package com.francescozoccheddu.tdmclient.ui.utils

import android.widget.ImageView
import com.francescozoccheddu.tdmclient.R

class InOutAVD(view: ImageView) : AVD<InOutAVD.State>(view,
    State.GONE
) {

    enum class State(override val state: Int) : AVD.State {
        VISIBLE(R.attr.state_visible), GONE(R.attr.state_gone)
    }

}