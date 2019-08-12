package com.francescozoccheddu.tdmclient.ui.bottomgroup


import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import com.francescozoccheddu.animatorhelpers.ABFloat
import com.francescozoccheddu.tdmclient.R
import com.francescozoccheddu.tdmclient.utils.android.visible
import kotlinx.android.synthetic.main.bg_walk.view.bg_walk_destination
import kotlinx.android.synthetic.main.bg_walk.view.bg_walk_nearby

class WalkComponent @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), BottomGroup.Component {

    init {
        orientation = VERTICAL
        View.inflate(context, R.layout.bg_walk, this)

        bg_walk_nearby.setOnClickListener { onChoose?.invoke(RoutingMode.NEARBY) }
        bg_walk_destination.setOnClickListener { onChoose?.invoke(RoutingMode.DESTINATION) }

    }

    private var animationAlpha by ABFloat(if (visible) 1f else 0f).apply {
        onUpdate = {
            alpha = it.value
            visible = it.value != 0f
            if (!it.running) {
                callback?.invoke()
                callback = null
            }
        }
        speed = 6f
    }

    var callback: (() -> Unit)? = null

    override fun animate(mode: BottomGroup.AnimationMode, callback: (() -> Unit)?) {
        this.callback = callback
        animationAlpha = when (mode) {
            BottomGroup.AnimationMode.IN -> 1f
            BottomGroup.AnimationMode.OUT -> 0f
        }
    }

    enum class RoutingMode {
        NEARBY, DESTINATION
    }

    var onChoose: ((RoutingMode) -> Unit)? = null

}