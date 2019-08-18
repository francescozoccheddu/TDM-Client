package com.francescozoccheddu.tdmclient.data

import android.content.SharedPreferences
import org.json.JSONObject

data class User(val id: Int, val passkey: String)

data class Score(
    val score: Int,
    val level: Int,
    val multiplier: Float,
    val nextLevelScore: Int?,
    val lastNotifiedLevel: Int
)

fun parseScore(json: JSONObject) = Score(
    json.getInt("score"),
    json.getInt("level"),
    json.getDouble("multiplier").toFloat(),
    if (json.isNull("nextLevelScore")) null else json.getInt("nextLevelScore"),
    json.getInt("lastNotifiedLevel")
)

fun saveScoreToPrefs(prefs: SharedPreferences.Editor, score: Score, keyPrefix: String) {
    prefs.putInt("$keyPrefix:${Score::score.name}", score.score)
    prefs.putInt("$keyPrefix:${Score::level.name}", score.level)
    prefs.putFloat("$keyPrefix:${Score::multiplier.name}", score.multiplier)
    prefs.putInt("$keyPrefix:${Score::nextLevelScore.name}", score.nextLevelScore ?: -1)
    prefs.putInt("$keyPrefix:${Score::lastNotifiedLevel.name}", score.lastNotifiedLevel)
}

fun loadScoreFromPrefs(prefs: SharedPreferences, keyPrefix: String): Score? {
    val score = prefs.getInt("$keyPrefix:${Score::score.name}", -1)
    val level = prefs.getInt("$keyPrefix:${Score::level.name}", -1)
    val multiplier = prefs.getFloat("$keyPrefix:${Score::multiplier.name}", -1f)
    val nextLevelScore = prefs.getInt("$keyPrefix:${Score::nextLevelScore.name}", -1)
    val lastNotifiedLevel = prefs.getInt("$keyPrefix:${Score::lastNotifiedLevel.name}", -1)
    if (score < 0 || level < 0 || multiplier < 0 || lastNotifiedLevel < 0)
        return null
    else
        return Score(score, level, multiplier, if (nextLevelScore < 0) null else nextLevelScore, lastNotifiedLevel)
}