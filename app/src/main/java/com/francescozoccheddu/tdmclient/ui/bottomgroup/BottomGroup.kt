package com.francescozoccheddu.tdmclient.ui.bottomgroup

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.cardview.widget.CardView
import androidx.constraintlayout.motion.widget.MotionLayout
import com.francescozoccheddu.animatorhelpers.SpringColor
import com.francescozoccheddu.tdmclient.R
import com.francescozoccheddu.tdmclient.utils.android.getNavigationBarHeight
import kotlinx.android.synthetic.main.bg.view.bg_action
import kotlinx.android.synthetic.main.bg.view.bg_duration
import kotlinx.android.synthetic.main.bg.view.bg_info
import kotlinx.android.synthetic.main.bg.view.bg_navbar
import kotlinx.android.synthetic.main.bg.view.bg_root
import kotlinx.android.synthetic.main.bg.view.bg_scrim
import kotlinx.android.synthetic.main.bg.view.bg_walk

class BottomGroup @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : MotionLayout(context, attrs, defStyleAttr) {


    interface Component {

        fun animate(mode: AnimationMode, callback: (() -> Unit)? = null)

    }

    var mode: Mode = Mode.HIDDEN
        set(value) {
            if (value != field) {
                field = value
                updateAnimation()
            }
        }

    private val root: CardView

    init {
        View.inflate(context, R.layout.bg, this)
        bg_navbar.layoutParams.height = getNavigationBarHeight(context)
        root = bg_root
        loadLayoutDescription(R.xml.bg_motion)
        setTransitionListener(object : TransitionListener {

            override fun onTransitionTrigger(ml: MotionLayout?, from: Int, to: Boolean, progress: Float) {}

            override fun onTransitionStarted(ml: MotionLayout?, from: Int, to: Int) {}

            override fun onTransitionChange(ml: MotionLayout?, from: Int, to: Int, progress: Float) {}

            override fun onTransitionCompleted(ml: MotionLayout?, state: Int) {
                updateAnimation()
            }

        })
        setTransition(mode.state, mode.state)
        transitionToEnd()

        bg_scrim.setOnClickListener { onDismiss?.invoke() }
    }

    val action = bg_action
    val duration = bg_duration
    val info = bg_info
    val walk = bg_walk

    enum class Mode(val state: Int) {
        ACTION(R.id.bg_cs_action), INFO(R.id.bg_cs_info),
        WALK(R.id.bg_cs_walk), DURATION(R.id.bg_cs_duration), HIDDEN(R.id.bg_cs_hidden)
    }

    private val Mode.view: View?
        get() = when (this) {
            Mode.ACTION -> action
            Mode.INFO -> info
            Mode.WALK -> walk
            Mode.DURATION -> duration
            Mode.HIDDEN -> null
        }

    enum class AnimationMode {
        IN, OUT
    }

    private var currentView: View? = null

    private var changingLayout = false

    private fun updateAnimation() {
        if (progress != 0f && progress != 1f)
            return
        changingLayout = true
        // Remove
        val currentCurrentView = currentView
        if (mode.view != currentView && currentCurrentView != null) {
            (currentView as Component).animate(AnimationMode.OUT) {
                if (currentView == currentCurrentView)
                    currentView = null
                updateAnimation()
            }
        }
        else {
            // Transition
            if (currentState != mode.state)
                transitionToState(mode.state)
            else {
                changingLayout = false
                if (mode == Mode.HIDDEN)
                    _color.reach()
                currentView = mode.view
                val currentCurrentView = currentView
                if (currentCurrentView != null)
                    (currentCurrentView as Component).animate(AnimationMode.IN)
            }
        }
    }

    private val _color = SpringColor(root.cardBackgroundColor.defaultColor).apply {
        onUpdate = { root.setCardBackgroundColor(it.value) }
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

    var modal
        get() = bg_scrim.isClickable
        set(value) {
            bg_scrim.isClickable = value
        }

    var onDismiss: (() -> Unit)? = null
}


