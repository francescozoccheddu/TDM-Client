package com.francescozoccheddu.tdmclient.ui.components.us

import android.animation.LayoutTransition
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.francescozoccheddu.tdmclient.R
import com.francescozoccheddu.tdmclient.data.UserStats
import com.francescozoccheddu.tdmclient.utils.android.getStyledString
import com.francescozoccheddu.tdmclient.utils.android.visible
import com.robinhood.ticker.TickerView
import com.squareup.picasso.Picasso

class UserStatsProfilePage(parent: View) {

    private val nextLevelRoot: View

    init {
        parent.findViewById<ViewGroup>(R.id.uss_profile_root).apply {
            layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
            layoutTransition = layoutTransition
        }
        parent.findViewById<View>(R.id.uss_profile_score_root)
            .setOnClickListener { toggleHelp(scoreHelp) }
        parent.findViewById<View>(R.id.uss_profile_level_root)
            .setOnClickListener { toggleHelp(levelHelp) }
        parent.findViewById<View>(R.id.uss_profile_multiplier_root)
            .setOnClickListener { toggleHelp(multiplierHelp) }
        nextLevelRoot = parent.findViewById<View>(R.id.uss_profile_next_level_root).apply {
            setOnClickListener { toggleHelp(nextLevelHelp) }
        }

    }

    private val scoreHelp = parent.findViewById<TextView>(R.id.uss_profile_score_help)
    private val levelHelp = parent.findViewById<TextView>(R.id.uss_profile_level_help)
    private val multiplierHelp = parent.findViewById<TextView>(R.id.uss_profile_multiplier_help)
    private val nextLevelHelp = parent.findViewById<TextView>(R.id.uss_profile_next_level_help)


    private fun toggleHelp(view: View) {
        showHelp(if (view.visible) null else view)
    }

    private fun showHelp(view: View?) {
        scoreHelp.visible = scoreHelp == view
        levelHelp.visible = levelHelp == view
        multiplierHelp.visible = multiplierHelp == view
        nextLevelHelp.visible = nextLevelHelp == view
    }

    fun hideHelp() {
        showHelp(null)
    }

    private val scoreText = parent.findViewById<TickerView>(R.id.uss_profile_score_tv)
    private val levelText = parent.findViewById<TickerView>(R.id.uss_profile_level_tv)
    private val multiplierText = parent.findViewById<TickerView>(R.id.uss_profile_multiplier_tv)
    private val nextLevelText = parent.findViewById<TickerView>(R.id.uss_profile_next_level_tv)
    private val nameText = parent.findViewById<TextView>(R.id.uss_profile_name)
    private val avatarImage = parent.findViewById<ImageView>(R.id.uss_profile_avatar)
    private val titleText = parent.findViewById<TextView>(R.id.uss_profile_title)

    var stats = UserStats(0, 0, 1f, null, 0, "", "", "")
        set(value) {
            if (value != field) {
                field = value
                nameText.text = value.name
                titleText.text = value.title
                Picasso.get().load(value.avatarUrl).into(avatarImage)
                scoreText.text = value.score.toString()
                levelText.text = (value.level + 1).toString()
                multiplierText.text = value.multiplier.toString()
                val nextLevelScore = value.nextLevelScore
                nextLevelRoot.visible = nextLevelScore != null
                if (nextLevelScore != null) {
                    nextLevelText.text = nextLevelScore.toString()
                    nextLevelHelp.text = nextLevelHelp.resources.getStyledString(
                        R.string.uss_next_level_help,
                        nextLevelScore - value.score,
                        value.level + 2
                    )
                }
                else
                    nextLevelHelp.visible = false
            }
        }

}