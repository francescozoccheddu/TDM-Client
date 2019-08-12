package com.francescozoccheddu.tdmclient.ui.bottomgroup

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.motion.widget.MotionLayout
import com.francescozoccheddu.animatorhelpers.SpringColor
import com.francescozoccheddu.tdmclient.R
import kotlinx.android.synthetic.main.bg.view.ag_action
import kotlinx.android.synthetic.main.bg.view.ag_duration
import kotlinx.android.synthetic.main.bg.view.ag_info
import kotlinx.android.synthetic.main.bg.view.ag_root
import kotlinx.android.synthetic.main.bg.view.ag_walk

class BottomGroup @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : MotionLayout(context, attrs, defStyleAttr) {


    interface Component {

        fun animate(mode: AnimationMode, callback: (() -> Unit)? = null)

    }


    init {
        View.inflate(context, R.layout.bg, this)
        loadLayoutDescription(R.xml.bg_motion)
        setTransitionListener(object : TransitionListener {

            override fun onTransitionTrigger(ml: MotionLayout?, from: Int, to: Boolean, progress: Float) {}

            override fun onTransitionStarted(ml: MotionLayout?, from: Int, to: Int) {}

            override fun onTransitionChange(ml: MotionLayout?, from: Int, to: Int, progress: Float) {}

            override fun onTransitionCompleted(ml: MotionLayout?, state: Int) {
                val mode = fromState(state)
                if (mode == this@BottomGroup.mode) {
                    changingLayout = false
                    ag_root.isClickable = isClickable(mode)
                }
                if (mode == Mode.HIDDEN)
                    _color.reach()
                updateAnimation()
            }

        })
        setTransition(Mode.ACTION.state, Mode.ACTION.state)
        transitionToEnd()
    }

    enum class Mode {
        ACTION, INFO, WALK, DURATION, HIDDEN
    }

    private val Mode.state
        get() = when (this) {
            Mode.ACTION -> R.id.cs_ag_action
            Mode.INFO -> R.id.cs_ag_info
            Mode.WALK -> R.id.cs_ag_walk
            Mode.DURATION -> R.id.cs_ag_duration
            Mode.HIDDEN -> R.id.cs_ag_hidden
        }

    private fun fromState(state: Int) = when (state) {
        R.id.cs_ag_action -> Mode.ACTION
        R.id.cs_ag_info -> Mode.INFO
        R.id.cs_ag_walk -> Mode.WALK
        R.id.cs_ag_duration -> Mode.DURATION
        R.id.cs_ag_hidden -> Mode.HIDDEN
        else -> throw IllegalArgumentException("Not a state")
    }

    private val actionView = setOf<View>(ag_action)
    private val infoView = setOf<View>(ag_info, ag_action)
    private val walkView = setOf<View>(ag_walk)
    private val durationView = setOf<View>(ag_duration)
    private val hiddenView = emptySet<View>()

    private val Mode.views
        get() = when (this) {
            Mode.ACTION -> actionView
            Mode.INFO -> infoView
            Mode.WALK -> walkView
            Mode.DURATION -> durationView
            Mode.HIDDEN -> hiddenView
        }

    enum class AnimationMode {
        IN, OUT
    }

    private var views = actionView.toMutableSet<View>()

    private fun updateAnimation() {
        if (progress != 0f && progress != 1f)
            return
        run {
            // Remove
            val exViews = views - mode.views
            val nViews = views - exViews
            for (view in exViews)
                (view as Component).animate(AnimationMode.OUT) {
                    views.remove(view)
                    updateAnimation()
                }
            for (view in nViews)
                (view as Component).animate(AnimationMode.IN)
            if (exViews.isNotEmpty())
                return
        }
        run {
            // Transition
            if (currentState != mode.state) {
                changingLayout = true
                transitionToState(mode.state)
                return
            }
        }
        run {
            // Add
            val nViews = mode.views - views
            for (view in nViews)
                views.add(view.apply {
                    (this as Component).animate(AnimationMode.IN)
                })
        }
    }

    private var changingLayout = false

    private fun isClickable(mode: Mode) =
        mode == Mode.ACTION || (mode == Mode.INFO && clickableSnackbar)

    var mode: Mode = Mode.ACTION
        set(value) {
            if (value != field) {
                field = value
                ag_root.isClickable = ag_root.isClickable && isClickable(value)
                updateAnimation()
            }
        }

    private val _color = SpringColor(ag_root.cardBackgroundColor.defaultColor).apply {
        onUpdate = { ag_root.setCardBackgroundColor(it.value) }
        acceleration = 100f
        maxVelocity = 1000f
    }

    var color
        get() = _color.target
        set(value) {
            _color.value = value
            if (mode == Mode.HIDDEN && !changingLayout)
                _color.reach()
        }

    var clickableSnackbar = true
        set(value) {
            if (value != field) {
                field = value
                if (mode == Mode.INFO && !changingLayout)
                    ag_root.isClickable = isClickable(mode)
            }
        }

}