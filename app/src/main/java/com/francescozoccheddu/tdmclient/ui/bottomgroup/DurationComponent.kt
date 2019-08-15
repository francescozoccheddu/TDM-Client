package com.francescozoccheddu.tdmclient.ui.bottomgroup


import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.RelativeLayout
import com.francescozoccheddu.animatorhelpers.ABFloat
import com.francescozoccheddu.tdmclient.R
import com.francescozoccheddu.tdmclient.ui.utils.GroupStateManager
import com.francescozoccheddu.tdmclient.utils.android.visible
import kotlinx.android.synthetic.main.bg_duration.view.bg_duration_cancel
import kotlinx.android.synthetic.main.bg_duration.view.bg_duration_confirm
import kotlinx.android.synthetic.main.bg_duration.view.bg_duration_time
import kotlin.math.ceil
import kotlin.math.floor

class DurationComponent @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr), GroupStateManager.GroupComponent {

    companion object {

        const val MIN_DURATION_RANGE = 120
        const val STEP = 5f

    }

    init {
        View.inflate(context, R.layout.bg_duration, this)
    }

    private var animationAlpha by ABFloat(if (visible) 1f else 0f).apply {
        onUpdate = {
            alpha = it.value
            visible = it.value != 0f
            if (!it.running) {
                animationCallback?.invoke()
                animationCallback = null
            }
        }
        speed = 6f
    }

    private var animationCallback: (() -> Unit)? = null

    override fun animate(mode: GroupStateManager.GroupComponent.Mode, callback: (() -> Unit)?) {
        this.animationCallback = callback
        animationAlpha = when (mode) {
            GroupStateManager.GroupComponent.Mode.IN -> 1f
            GroupStateManager.GroupComponent.Mode.OUT -> 0f
        }
    }

    var minTime = 0f
        set(value) {
            println("MINTIME=$value")
            if (value < 0f)
                throw IllegalArgumentException("'${this::minTime.name}' cannot be negative")
            bg_duration_time.apply {
                minValue = ceil(value / STEP) * STEP
                maxValue = ceil((minValue + MIN_DURATION_RANGE) / 60f) * 60f
                startValue = floor(minValue / 60f) * 60f
            }
        }

    val time get() = bg_duration_time.value

    inline fun onCancel(crossinline callback: () -> Unit) = bg_duration_cancel.setOnClickListener { callback() }
    inline fun onConfirm(crossinline callback: () -> Unit) = bg_duration_confirm.setOnClickListener { callback() }

}