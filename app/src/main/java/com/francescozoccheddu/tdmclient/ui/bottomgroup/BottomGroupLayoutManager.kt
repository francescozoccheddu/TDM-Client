package com.francescozoccheddu.tdmclient.ui.bottomgroup

import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.transition.TransitionInflater
import androidx.transition.TransitionManager
import com.francescozoccheddu.knob.KnobView
import com.francescozoccheddu.tdmclient.R
import com.francescozoccheddu.tdmclient.utils.android.setMargins
import com.francescozoccheddu.tdmclient.utils.android.visible
import com.francescozoccheddu.tdmclient.utils.commons.Event
import com.francescozoccheddu.tdmclient.utils.commons.event
import com.francescozoccheddu.tdmclient.utils.commons.invoke
import com.francescozoccheddu.tdmclient.utils.data.snapDown
import com.francescozoccheddu.tdmclient.utils.data.snapUp
import kotlin.math.roundToInt

class BottomGroupLayoutManager(private val parent: ViewGroup) {

    private val transition = TransitionInflater
        .from(parent.context)
        .inflateTransition(R.transition.bg)

    interface ScrimComponent {
        var onDismiss: Event
    }

    interface WalkComponent {
        enum class RoutingMode {
            NEARBY, DESTINATION
        }

        var onChoose: ((RoutingMode) -> Unit)?
    }

    interface InfoComponent {
        var text: String
        var icon: Int
        var action: String?
        var onAction: Event
        var loading: Boolean
    }

    interface DurationComponent {
        var minTime: Float
        val time: Float
        var onCancel: Event
        var onConfirm: Event
    }

    interface ActionComponent {
        var onAction: Event
    }

    private companion object {

        const val MIN_DURATION_RANGE = 120
        const val MIN_DURATION_STEP = 5f

    }


    private val cardView = parent.findViewById<CardView>(R.id.bg_card)
    private val scrimView = parent.findViewById<View>(R.id.bg_scrim)
    private val actionView = parent.findViewById<View>(R.id.bg_action)
    private val infoView = parent.findViewById<View>(R.id.bg_info)
    private val walkView = parent.findViewById<View>(R.id.bg_walk)
    private val durationView = parent.findViewById<View>(R.id.bg_duration)

    val scrim = object : ScrimComponent {

        init {
            scrimView.apply {
                setOnClickListener {
                    onDismiss()
                }
            }
        }

        override var onDismiss = event()

    }

    val walk = object : WalkComponent {

        init {
            parent.findViewById<View>(R.id.bg_walk_destination)
                .setOnClickListener { onChoose?.invoke(WalkComponent.RoutingMode.DESTINATION) }
            parent.findViewById<View>(R.id.bg_walk_nearby)
                .setOnClickListener { onChoose?.invoke(WalkComponent.RoutingMode.NEARBY) }
        }

        override var onChoose: ((WalkComponent.RoutingMode) -> Unit)? = null

    }

    val info = object : InfoComponent {

        private val blinkAnimation = AnimationUtils.loadAnimation(parent.context, R.anim.blink)

        private val textView = parent.findViewById<TextView>(R.id.bg_info_tv)
        private val imageView = parent.findViewById<ImageView>(R.id.bg_info_iv)
        private val progressBar = parent.findViewById<ProgressBar>(R.id.bg_info_pb)
        private val button = parent.findViewById<Button>(R.id.bg_info_bt).apply {
            setOnClickListener { onAction() }
        }

        override var text = ""
            set(value) {
                if (value != field) {
                    field = value
                    textView.text = value
                    textView.startAnimation(blinkAnimation)
                }
            }

        override var action: String? = null
            set(value) {
                if (value != field) {
                    field = value
                    button.apply {
                        visible = value != null
                        if (value != null) {
                            text = value
                            startAnimation(blinkAnimation)
                        }
                    }
                }
            }

        override var loading = true
            set(value) {
                if (value != field) {
                    field = value
                    progressBar.visible = value
                    imageView.visible = !value
                }
            }

        override var icon: Int = R.drawable.ic_back
            set(value) {
                if (field != value) {
                    field = value
                    imageView.setImageResource(value)
                    imageView.startAnimation(blinkAnimation)

                }
            }

        override var onAction = event()

    }

    val duration = object : DurationComponent {

        private val knob = parent.findViewById<KnobView>(R.id.bg_duration_time)

        init {

            val provider = object {

                private val builder = StringBuilder(6)

                operator fun invoke(value: Float): String {
                    val tm = value.roundToInt()
                    val m = tm % 60
                    val h = tm / 60
                    val sb = builder
                    sb.setLength(0)
                    if (h > 0) {
                        sb.append(h)
                        sb.append('h')
                    }
                    if (m > 0) {
                        if (sb.isNotEmpty())
                            sb.append(' ')
                        sb.append(m)
                        sb.append('m')
                    }
                    return sb.toString()
                }

            }

            knob.thickText = object : KnobView.ThickTextProvider {
                override fun provide(view: KnobView, track: Int, thick: Int, value: Float) = provider(value)
            }
            knob.labelText = object : KnobView.LabelTextProvider {
                override fun provide(view: KnobView, value: Float) = provider(value)
            }

            parent.findViewById<View>(R.id.bg_duration_cancel).setOnClickListener { onCancel() }
            parent.findViewById<View>(R.id.bg_duration_confirm).setOnClickListener { onConfirm() }

        }

        override var minTime = 0f
            set(value) {
                if (value < 0f)
                    throw IllegalArgumentException("'${this::minTime.name}' cannot be negative")
                field = value
                knob.apply {
                    minValue = value.snapUp(MIN_DURATION_STEP)
                    maxValue = (minValue + MIN_DURATION_RANGE).snapUp(MIN_DURATION_STEP)
                    startValue = minValue.snapDown(60f)
                }
            }

        override val time get() = knob.value

        override var onCancel = event()
        override var onConfirm = event()

    }

    val action = object : ActionComponent {

        init {
            parent.findViewById<View>(R.id.bg_action).apply {
                setOnClickListener {
                    onAction()
                }
            }
        }

        override var onAction = event()

    }

    enum class State {
        ACTION, INFO, WALK, DURATION, HIDDEN
    }


    var state = State.HIDDEN
        set(value) {
            if (value != field) {
                field = value
                update()
            }
        }

    private fun update() {
        TransitionManager.beginDelayedTransition(parent, transition)
        actionView.visible = false
        infoView.visible = false
        walkView.visible = false
        durationView.visible = false
        when (state) {
            State.ACTION -> actionView
            State.INFO -> infoView
            State.WALK -> walkView
            State.DURATION -> durationView
            State.HIDDEN -> null
        }?.visible = true
        scrimView.isClickable = when (state) {
            State.WALK, State.DURATION -> true
            else -> false
        }
        scrimView.alpha = when (state) {
            State.DURATION -> 0.5f
            State.WALK -> 0.35f
            else -> 0f
        }
        cardView.setCardBackgroundColor(color)
        cardView.radius = when (state) {
            State.ACTION -> parent.resources.getDimension(R.dimen.bg_action_size) / 2f
            State.DURATION, State.WALK -> parent.resources.getDimension(R.dimen.bg_walk_duration_radius)
            State.INFO -> parent.resources.getDimension(R.dimen.bg_info_radius)
            State.HIDDEN -> 0f
        }
        cardView.setMargins(
            when (state) {
                State.ACTION, State.INFO -> parent.resources.getDimensionPixelOffset(R.dimen.bg_action_margin)
                State.WALK -> parent.resources.getDimensionPixelOffset(R.dimen.bg_walk_margin)
                State.DURATION -> parent.resources.getDimensionPixelOffset(R.dimen.bg_duration_margin)
                State.HIDDEN ->
                    (parent.resources.getDimension(R.dimen.bg_action_margin)
                            + parent.resources.getDimension(R.dimen.bg_action_size) / 2f).roundToInt()
            }
        )
        cardView.elevation = when (state) {
            State.ACTION -> parent.resources.getDimension(R.dimen.bg_action_elevation)
            State.HIDDEN -> 0f
            else -> parent.resources.getDimension(R.dimen.bg_no_action_elevation)
        }
    }

    fun reach() {
        TransitionManager.endTransitions(parent)
    }

    var color = cardView.cardBackgroundColor.defaultColor
        set(value) {
            if (value != field) {
                field = value
                update()
            }
        }

}


