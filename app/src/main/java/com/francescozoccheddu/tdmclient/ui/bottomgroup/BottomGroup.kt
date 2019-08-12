package com.francescozoccheddu.tdmclient.ui.bottomgroup

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.cardview.widget.CardView
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.content.ContextCompat
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

        root.setOnClickListener { if (_cardClickable) onClick?.invoke() }
        bg_scrim.setOnClickListener { onDismiss?.invoke() }
    }

    private val rippleDrawable = run {
        val outValue = TypedValue()
        getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
        ContextCompat.getDrawable(context, outValue.resourceId)
    }

    private var _cardClickable = false
        set(value) {
            if (value != field) {
                field = value
                root.foreground = if (value) rippleDrawable else null
            }
        }

    val action = bg_action
    val duration = bg_duration
    val info = bg_info
    val walk = bg_walk

    enum class Mode {
        ACTION, INFO, WALK, DURATION, HIDDEN
    }

    private val Mode.state
        get() = when (this) {
            Mode.ACTION -> R.id.bg_cs_action
            Mode.INFO -> R.id.bg_cs_info
            Mode.WALK -> R.id.bg_cs_walk
            Mode.DURATION -> R.id.bg_cs_duration
            Mode.HIDDEN -> R.id.bg_cs_hidden
        }

    private fun fromState(state: Int) = when (state) {
        R.id.bg_cs_action -> Mode.ACTION
        R.id.bg_cs_info -> Mode.INFO
        R.id.bg_cs_walk -> Mode.WALK
        R.id.bg_cs_duration -> Mode.DURATION
        R.id.bg_cs_hidden -> Mode.HIDDEN
        else -> throw IllegalArgumentException("Not a state")
    }

    private val actionView = setOf<View>(action)
    private val infoView = setOf<View>(info, action)
    private val walkView = setOf<View>(walk)
    private val durationView = setOf<View>(duration)
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

    private var views = actionView.toMutableSet()

    private var changingLayout = false

    private fun updateAnimation() {
        if (progress != 0f && progress != 1f)
            return
        changingLayout = true
        _cardClickable = false
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
                _cardClickable = false
                transitionToState(mode.state)
                return
            }
            else {
                _cardClickable = clickableCard
                changingLayout = false
                if (mode == Mode.HIDDEN)
                    _color.reach()
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

    var clickableCard = false
        set(value) {
            if (value != field) {
                field = value
                if (!changingLayout)
                    _cardClickable = value
            }
        }

    var modal
        get() = bg_scrim.isClickable
        set(value) {
            bg_scrim.isClickable = value
        }

    var onClick: (() -> Unit)? = null
    var onDismiss: (() -> Unit)? = null
}


