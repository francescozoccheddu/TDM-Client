package com.francescozoccheddu.tdmclient.ui.components.us

import android.content.Context
import android.content.SharedPreferences
import android.view.ViewGroup
import com.francescozoccheddu.tdmclient.R
import com.francescozoccheddu.tdmclient.data.UserStats
import com.francescozoccheddu.tdmclient.utils.android.OverlayMotionLayout
import com.robinhood.ticker.TickerView
import kotlin.math.max

class UserStatsComponent(parent: ViewGroup) {

    private companion object {
        private const val MIN_NOTIFY_GAIN = 20

        val DEFAULT_PREFS_NAME = "${this::class.java.canonicalName}:lastNotifiedLevel"
        val DEFAULT_STATS_PREF_KEY = "${this::class.java.canonicalName}:lastNotifiedLevel"
    }

    private val root = parent.findViewById<OverlayMotionLayout>(R.id.us_root)
    private val gainComponent = GainComponent(parent)
    private val scoreText = parent.findViewById<TickerView>(R.id.us_score_tv)
    private val levelText = parent.findViewById<TickerView>(R.id.us_level_tv)
    private val sheet = parent.findViewById<UserStatsSheet>(R.id.us_sheet_root)

    init {
        parent.findViewById<UserStatsSheet>(R.id.us_sheet_root).onClose = this::requestClose
        root.addHitRect(R.id.us_score_root)
        root.addHitRect(R.id.us_level_root)
        root.addHitRect(R.id.us_gain_root)
        root.addHitRect(R.id.us_sheet_root)
        root.setTransition(R.id.us_cs_gone, R.id.us_cs_gone)
        root.transitionToEnd()
    }

    private fun updateCloseTransition() {
        root.getTransition(R.id.us_t_idle_to_expanded_sheet).setEnable(draggable && enabled)
        root.getTransition(R.id.us_t_gone_to_expanded_sheet).setEnable(draggable && !enabled)
    }

    private var draggable = true
        set(value) {
            root.getTransition(R.id.us_t_idle_to_expanded_gain).setEnable(value)
            root.getTransition(R.id.us_t_idle_to_expanded_score).setEnable(value)
            root.getTransition(R.id.us_t_idle_to_expanded_level).setEnable(value)
            updateCloseTransition()
        }

    private fun requestClose() {
        if (root.currentState == R.id.us_cs_expanded)
            root.transitionToState(
                if (enabled) R.id.us_cs_idle
                else R.id.us_cs_gone
            )
    }

    var enabled = false
        set(value) {
            if (value != field) {
                field = value
                draggable = value
                root.transitionToState(
                    if (value) R.id.us_cs_idle
                    else R.id.us_cs_gone
                )
            }
        }

    var stats = UserStats(0, 0, 1f, null, 0)
        set(value) {
            if (value != field) {
                val gain = value.score - field.score
                if (gain > MIN_NOTIFY_GAIN)
                    gainComponent.notify(gain)
                field = value
                sheet.stats = value
                scoreText.text = value.score.toString()
                levelText.text = (value.level + 1).toString()
                lastNotifiedLevel = max(value.lastNotifiedLevel, lastNotifiedLevel)
                if (value.level > lastNotifiedLevel) {
                    lastNotifiedLevel = value.level
                    // TODO Fireworks
                    onLevelNotified?.invoke(value.level)
                }
            }
        }

    fun loadLastNotifiedLevel(prefs: SharedPreferences, key: String = DEFAULT_STATS_PREF_KEY) {
        lastNotifiedLevel = prefs.getInt(key, 0)
    }

    fun saveLastNotifiedLevel(prefs: SharedPreferences.Editor, key: String = DEFAULT_STATS_PREF_KEY) {
        prefs.putInt(key, lastNotifiedLevel)
    }

    fun loadLastNotifiedLevel(
        context: Context,
        prefsName: String = DEFAULT_PREFS_NAME,
        key: String = DEFAULT_STATS_PREF_KEY
    ) {
        loadLastNotifiedLevel(context.getSharedPreferences(prefsName, Context.MODE_PRIVATE), key)
    }

    fun saveLastNotifiedLevel(
        context: Context,
        prefsName: String = DEFAULT_PREFS_NAME,
        key: String = DEFAULT_STATS_PREF_KEY
    ) {
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        saveLastNotifiedLevel(editor, key)
        editor.apply()
    }

    var lastNotifiedLevel = 0
        private set

    var onLevelNotified: ((Int) -> Unit)? = null

}