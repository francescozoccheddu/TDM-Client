package com.francescozoccheddu.tdmclient.ui.components.us

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import com.francescozoccheddu.tdmclient.R
import com.francescozoccheddu.tdmclient.data.UserStats
import com.francescozoccheddu.tdmclient.ui.utils.InOutImageButton
import com.francescozoccheddu.tdmclient.utils.commons.event
import com.francescozoccheddu.tdmclient.utils.commons.invoke
import com.robinhood.ticker.TickerView

class UserStatsSheet @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {


    init {
        View.inflate(context, R.layout.uss, this)
    }

    private val score = findViewById<TickerView>(R.id.uss_score_tv)
    private val level = findViewById<TickerView>(R.id.uss_level_tv)
    private val multiplier = findViewById<TickerView>(R.id.uss_multiplier_tv)
    private val nextLevel = findViewById<TickerView>(R.id.uss_next_level_tv)

    private val closeButton = findViewById<InOutImageButton>(R.id.uss_close).apply {
        setOnClickListener { onClose() }
    }

    var onClose = event()

    fun onOpened() {
        closeButton.show()
    }

    fun onClosed() {
        closeButton.hide()
    }

    var stats = UserStats(0, 0, 1f, null, 0)
        set(value) {
            if (value != field) {
                field = value
                score.text = value.score.toString()
                level.text = (value.level + 1).toString()
                multiplier.text = value.multiplier.toString()
                nextLevel.text = value.nextLevelScore.toString()
            }
        }

}