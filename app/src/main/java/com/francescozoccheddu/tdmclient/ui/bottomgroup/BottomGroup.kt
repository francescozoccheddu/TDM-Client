package com.francescozoccheddu.tdmclient.ui.bottomgroup

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.cardview.widget.CardView
import androidx.constraintlayout.motion.widget.MotionLayout
import com.francescozoccheddu.animatorhelpers.SpringColor
import com.francescozoccheddu.tdmclient.R
import com.francescozoccheddu.tdmclient.ui.GroupStateManager
import kotlinx.android.synthetic.main.bg.view.bg_action
import kotlinx.android.synthetic.main.bg.view.bg_duration
import kotlinx.android.synthetic.main.bg.view.bg_info
import kotlinx.android.synthetic.main.bg.view.bg_root
import kotlinx.android.synthetic.main.bg.view.bg_scrim
import kotlinx.android.synthetic.main.bg.view.bg_walk

class BottomGroup @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : MotionLayout(context, attrs, defStyleAttr) {


    enum class State(override val constraintSetId: Int, override val componentId: Int?) : GroupStateManager.GroupState {
        ACTION(R.id.bg_cs_action, R.id.bg_action),
        INFO(R.id.bg_cs_info, R.id.bg_info),
        WALK(R.id.bg_cs_walk, R.id.bg_walk),
        DURATION(R.id.bg_cs_duration, R.id.bg_duration),
        HIDDEN(R.id.bg_cs_hidden, null)
    }

    private val root: CardView
    private val stateManager: GroupStateManager<State>

    init {
        View.inflate(context, R.layout.bg, this)
        root = bg_root
        loadLayoutDescription(R.xml.bg_motion)
        stateManager = GroupStateManager(this, State.HIDDEN)
        stateManager.onTransitionCompleted = {
            if (stateManager.state == State.HIDDEN)
                _color.reach()
        }
    }

    var state: State
        get() = stateManager.state
        set(value) {
            stateManager.state = value
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
            if (stateManager.state == State.HIDDEN && !stateManager.running)
                _color.reach()
        }

    var modal
        get() = bg_scrim.isClickable
        set(value) {
            bg_scrim.isClickable = value
        }

    inline fun onDismiss(crossinline callback: (() -> Unit)) = bg_scrim.setOnClickListener { callback() }

}


