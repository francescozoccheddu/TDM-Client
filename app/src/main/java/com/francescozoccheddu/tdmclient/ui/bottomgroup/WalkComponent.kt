package com.francescozoccheddu.tdmclient.ui.bottomgroup


import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import com.francescozoccheddu.tdmclient.R
import kotlinx.android.synthetic.main.bg_walk.view.bg_walk_destination
import kotlinx.android.synthetic.main.bg_walk.view.bg_walk_nearby

class WalkComponent @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {


    init {
        orientation = VERTICAL
        View.inflate(context, R.layout.bg_walk, this)
    }


    enum class RoutingMode {
        NEARBY, DESTINATION
    }

    inline fun onChoose(crossinline callback: ((RoutingMode) -> Unit)) {
        bg_walk_nearby.setOnClickListener { callback(RoutingMode.NEARBY) }
        bg_walk_destination.setOnClickListener { callback(RoutingMode.DESTINATION) }
    }

}