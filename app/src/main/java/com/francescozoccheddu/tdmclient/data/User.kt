package com.francescozoccheddu.tdmclient.data

import android.content.SharedPreferences
import org.json.JSONObject

data class User(val id: Int, val passkey: String)

data class UserStats(
    val score: Int,
    val level: Int,
    val multiplier: Float,
    val nextLevelScore: Int?,
    val lastNotifiedLevel: Int
)

fun parseUserStats(json: JSONObject) = UserStats(
    json.getInt("score"),
    json.getInt("level"),
    json.getDouble("multiplier").toFloat(),
    if (json.isNull("nextLevelScore")) null else json.getInt("nextLevelScore"),
    json.getInt("lastNotifiedLevel")
)

fun saveUserStats(prefs: SharedPreferences.Editor, userStats: UserStats, keyPrefix: String) {
    prefs.putInt("$keyPrefix:${UserStats::score.name}", userStats.score)
    prefs.putInt("$keyPrefix:${UserStats::level.name}", userStats.level)
    prefs.putFloat("$keyPrefix:${UserStats::multiplier.name}", userStats.multiplier)
    prefs.putInt("$keyPrefix:${UserStats::nextLevelScore.name}", userStats.nextLevelScore ?: -1)
    prefs.putInt("$keyPrefix:${UserStats::lastNotifiedLevel.name}", userStats.lastNotifiedLevel)
}

fun loadUserStats(prefs: SharedPreferences, keyPrefix: String): UserStats? {
    val score = prefs.getInt("$keyPrefix:${UserStats::score.name}", -1)
    val level = prefs.getInt("$keyPrefix:${UserStats::level.name}", -1)
    val multiplier = prefs.getFloat("$keyPrefix:${UserStats::multiplier.name}", -1f)
    val nextLevelScore = prefs.getInt("$keyPrefix:${UserStats::nextLevelScore.name}", -1)
    val lastNotifiedLevel = prefs.getInt("$keyPrefix:${UserStats::lastNotifiedLevel.name}", -1)
    return if (score < 0 || level < 0 || multiplier < 0 || lastNotifiedLevel < 0)
        null
    else
        UserStats(score, level, multiplier, if (nextLevelScore < 0) null else nextLevelScore, lastNotifiedLevel)
}