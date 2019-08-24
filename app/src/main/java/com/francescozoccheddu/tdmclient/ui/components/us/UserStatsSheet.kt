package com.francescozoccheddu.tdmclient.ui.components.us

import android.animation.LayoutTransition
import android.content.Context
import android.graphics.drawable.Animatable2
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.text.HtmlCompat
import com.francescozoccheddu.tdmclient.R
import com.francescozoccheddu.tdmclient.data.UserStats
import com.francescozoccheddu.tdmclient.ui.utils.InOutImageButton
import com.francescozoccheddu.tdmclient.utils.android.visible
import com.francescozoccheddu.tdmclient.utils.commons.event
import com.francescozoccheddu.tdmclient.utils.commons.invoke
import com.robinhood.ticker.TickerView

class UserStatsSheet @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val nextLevelRoot: View

    init {
        View.inflate(context, R.layout.uss, this)
        findViewById<ViewGroup>(R.id.uss_values_root).apply {
            layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
            layoutTransition = layoutTransition
        }
        findViewById<View>(R.id.uss_score_root).setOnClickListener { toggleHelp(scoreHelp) }
        findViewById<View>(R.id.uss_level_root).setOnClickListener { toggleHelp(levelHelp) }
        findViewById<View>(R.id.uss_multiplier_root).setOnClickListener { toggleHelp(multiplierHelp) }
        nextLevelRoot = findViewById<View>(R.id.uss_next_level_root).apply {
            setOnClickListener { toggleHelp(nextLevelHelp) }
        }
        (findViewById<ImageView>(R.id.uss_coins).drawable as Animatable2).apply {
            start()
            registerAnimationCallback(object : Animatable2.AnimationCallback() {

                override fun onAnimationEnd(drawable: Drawable?) {
                    start()
                }

            })
        }
    }

    private val scoreHelp = findViewById<TextView>(R.id.uss_score_help)
    private val levelHelp = findViewById<TextView>(R.id.uss_level_help)
    private val multiplierHelp = findViewById<TextView>(R.id.uss_multiplier_help)
    private val nextLevelHelp = findViewById<TextView>(R.id.uss_next_level_help)


    private fun toggleHelp(view: View) {
        showHelp(if (view.visible) null else view)
    }

    private fun showHelp(view: View?) {
        scoreHelp.visible = scoreHelp == view
        levelHelp.visible = levelHelp == view
        multiplierHelp.visible = multiplierHelp == view
        nextLevelHelp.visible = nextLevelHelp == view
    }

    private val scoreText = findViewById<TickerView>(R.id.uss_score_tv)
    private val levelText = findViewById<TickerView>(R.id.uss_level_tv)
    private val multiplierText = findViewById<TickerView>(R.id.uss_multiplier_tv)
    private val nextLevelText = findViewById<TickerView>(R.id.uss_next_level_tv)

    private val closeButton = findViewById<InOutImageButton>(R.id.uss_close).apply {
        setOnClickListener {
            showHelp(null)
            onClose()
        }
    }

    var onClose = event()

    fun onOpened() {
        closeButton.show()
    }

    fun onClosed() {
        showHelp(null)
        closeButton.hide()
    }

    var stats = UserStats(0, 0, 1f, null, 0)
        set(value) {
            if (value != field) {
                field = value
                scoreText.text = value.score.toString()
                levelText.text = (value.level + 1).toString()
                multiplierText.text = value.multiplier.toString()
                val nextLevelScore = value.nextLevelScore
                nextLevelRoot.visible = nextLevelScore != null
                if (nextLevelScore != null) {
                    nextLevelText.text = nextLevelScore.toString()
                    nextLevelHelp.text = HtmlCompat.fromHtml(
                        resources.getString(
                            R.string.uss_next_level_help,
                            nextLevelScore - value.score,
                            value.level + 2
                        ),
                        HtmlCompat.FROM_HTML_MODE_LEGACY
                    )
                }
                else
                    nextLevelHelp.visible = false
            }
        }

}