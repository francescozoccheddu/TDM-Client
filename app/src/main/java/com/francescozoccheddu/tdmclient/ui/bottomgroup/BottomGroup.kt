package com.francescozoccheddu.tdmclient.ui.bottomgroup

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.cardview.widget.CardView
import androidx.core.view.setPadding
import com.francescozoccheddu.animatorhelpers.SpringColor
import com.francescozoccheddu.animatorhelpers.SpringFloat
import com.francescozoccheddu.animatorhelpers.SpringInt
import com.francescozoccheddu.tdmclient.R
import com.francescozoccheddu.tdmclient.ui.utils.GroupStateManager
import kotlinx.android.synthetic.main.bg.view.bg_action
import kotlinx.android.synthetic.main.bg.view.bg_duration
import kotlinx.android.synthetic.main.bg.view.bg_info
import kotlinx.android.synthetic.main.bg.view.bg_root
import kotlinx.android.synthetic.main.bg.view.bg_scrim
import kotlinx.android.synthetic.main.bg.view.bg_walk

class BottomGroup @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {


    enum class State(override val componentId: Int?) : GroupStateManager.GroupState {
        ACTION(R.id.bg_action),
        INFO(R.id.bg_info),
        WALK(R.id.bg_walk),
        DURATION(R.id.bg_duration),
        HIDDEN(null)
    }

    private val root: CardView
    private val stateManager: GroupStateManager<State>

    init {
        View.inflate(context, R.layout.bg, this)
        root = bg_root
        stateManager = GroupStateManager(root, State.HIDDEN)
    }

    private var cornerRadius by SpringFloat(resources.getDimension(R.dimen.fab_size) / 2f).apply {
        onUpdate = {
            root.radius = it.value
        }
        acceleration = 20f
        maxVelocity = 200f
    }

    private var padding by SpringInt(0).apply {
        onUpdate = {
            setPadding(it.value)
        }
        acceleration = 20f
        maxVelocity = 200f
    }

    var state: State
        get() = stateManager.state
        set(value) {
            stateManager.state = value
            cornerRadius = when (value) {
                State.ACTION, State.HIDDEN -> resources.getDimension(R.dimen.fab_size) / 2f
                State.INFO -> resources.getDimension(R.dimen.snackbar_corner_radius)
                State.WALK, State.DURATION -> resources.getDimension(R.dimen.sheet_corner_radius)
            }
            padding = when (state) {
                State.ACTION, State.HIDDEN -> 100
                State.INFO -> 50
                State.WALK, State.DURATION -> 30
            }

            root.layoutParams = root.layoutParams
        }

    val action = bg_action
    val duration = bg_duration
    val info = bg_info
    val walk = bg_walk

    private val _color = SpringColor(root.cardBackgroundColor.defaultColor).apply {
        onUpdate = { root.setCardBackgroundColor(it.value) }
        acceleration = 100f
        maxVelocity = 1000f
    }

    var color
        get() = _color.target
        set(value) {
            _color.value = value
        }

    var modal
        get() = bg_scrim.isClickable
        set(value) {
            bg_scrim.isClickable = value
        }

    inline fun onDismiss(crossinline callback: (() -> Unit)) = bg_scrim.setOnClickListener { callback() }

}


