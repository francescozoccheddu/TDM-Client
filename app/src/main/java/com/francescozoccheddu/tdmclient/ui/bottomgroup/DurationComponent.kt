package com.francescozoccheddu.tdmclient.ui.bottomgroup


import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.RelativeLayout
import com.francescozoccheddu.knob.KnobView
import com.francescozoccheddu.tdmclient.R
import com.francescozoccheddu.tdmclient.utils.data.snapDown
import com.francescozoccheddu.tdmclient.utils.data.snapUp
import kotlinx.android.synthetic.main.bg_duration.view.bg_duration_cancel
import kotlinx.android.synthetic.main.bg_duration.view.bg_duration_confirm
import kotlinx.android.synthetic.main.bg_duration.view.bg_duration_time
import kotlin.math.roundToInt

class DurationComponent @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {

    companion object {

        const val MIN_DURATION_RANGE = 120
        const val STEP = 5f

    }

    init {
        View.inflate(context, R.layout.bg_duration, this)

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

        bg_duration_time.thickText = object : KnobView.ThickTextProvider {
            override fun provide(view: KnobView, track: Int, thick: Int, value: Float) = provider(value)
        }
        bg_duration_time.labelText = object : KnobView.LabelTextProvider {
            override fun provide(view: KnobView, value: Float) = provider(value)
        }

    }

    var minTime = 0f
        set(value) {
            if (value < 0f)
                throw IllegalArgumentException("'${this::minTime.name}' cannot be negative")
            field = value
            bg_duration_time.apply {
                minValue = value.snapUp(STEP)
                maxValue = (minValue + MIN_DURATION_RANGE).snapUp(STEP)
                startValue = minValue.snapDown(60f)
            }
        }

    val time get() = bg_duration_time.value

    inline fun onCancel(crossinline callback: () -> Unit) = bg_duration_cancel.setOnClickListener { callback() }
    inline fun onConfirm(crossinline callback: () -> Unit) = bg_duration_confirm.setOnClickListener { callback() }

}