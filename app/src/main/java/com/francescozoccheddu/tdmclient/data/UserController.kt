package com.francescozoccheddu.tdmclient.data

import android.content.Context
import android.content.SharedPreferences
import com.francescozoccheddu.tdmclient.utils.commons.FuncEvent
import com.francescozoccheddu.tdmclient.utils.commons.ProcEvent
import com.francescozoccheddu.tdmclient.utils.data.client.Server
import com.francescozoccheddu.tdmclient.utils.data.client.Status
import java.util.*
import kotlin.math.max

class UserController(val key: UserKey, server: Server) {

    companion object {
        val DEFAULT_PREFS_NAME = "${this::class.java.canonicalName}:userStats"
        val DEFAULT_STATS_PREF_KEY = "${this::class.java.canonicalName}:userStats"
        const val MAX_SET_REQUESTS = 6
    }

    private lateinit var _stats: UserStats
    private var lastNotifiedLevel: Int = 0

    private val getService = makeUserService(server, key).apply {
        onData += {
            stats = it
            setLastNotifiedLevel()
        }
    }
    private val setService = makeEditUserService(server)

    enum class EditNameResult {
        SUCCESS, PROFANITY, REQUEST_FAILURE
    }

    fun setName(name: String, callback: (EditNameResult) -> Unit) {
        setService.setName(key, name).apply {
            onStatusChange += {
                if (!it.status.pending) {
                    if (it.hasResponse) {
                        if (hasStats && it.response == EditUserResult.SUCCESS)
                            submitStats(it.request.editUserStats(stats), it.startTime)
                        callback(
                            when (it.response) {
                                EditUserResult.SUCCESS -> EditNameResult.SUCCESS
                                EditUserResult.PROFANITY_IN_NAME -> EditNameResult.PROFANITY
                                EditUserResult.LOCKED_AVATAR -> EditNameResult.REQUEST_FAILURE
                            }
                        )
                    }
                    else callback(EditNameResult.REQUEST_FAILURE)
                }
            }
            start()
        }
    }

    fun setAvatar(avatar: Int, avatars: AvatarSet? = null, callback: (Boolean) -> Unit) {
        setService.setAvatar(key, avatar).apply {
            onStatusChange += {
                if (!it.status.pending) {
                    if (it.hasResponse) {
                        if (hasStats && it.response == EditUserResult.SUCCESS)
                            submitStats(
                                it.request.editUserStats(stats, avatars?.images),
                                it.startTime
                            )
                        callback(it.response == EditUserResult.SUCCESS)
                    }
                    else callback(false)
                }
            }
            start()
        }
    }

    fun notifyLevel(level: Int) {
        if (level > lastNotifiedLevel) {
            lastNotifiedLevel = level
            setLastNotifiedLevel()
        }
    }

    private fun setLastNotifiedLevel() {
        if (!hasStats || lastNotifiedLevel > stats.lastNotifiedLevel && lastNotifiedLevel <= stats.level) {
            if (setService.pendingRequests.size < MAX_SET_REQUESTS)
                setService.setLastNotifiedLevel(key, lastNotifiedLevel).apply {
                    onStatusChange += {
                        if (hasStats && it.status.succeeded)
                            submitStats(it.request.editUserStats(stats), it.startTime)
                    }
                    start()
                }
        }
    }

    var stats
        get() = _stats
        private set(value) {
            if (!hasStats || _stats != value) {
                _stats = value
                if (value.lastNotifiedLevel > lastNotifiedLevel)
                    lastNotifiedLevel = value.lastNotifiedLevel
                onStatsChange()
                if (value.level > lastNotifiedLevel)
                    onLevelUp(value.level)
            }
        }

    val hasStats get() = this::_stats.isInitialized

    val onStatsChange = ProcEvent()

    val onLevelUp = FuncEvent<Int>()

    fun submitStats(stats: UserStats, time: Date) {
        getService.submit(time, stats)
    }

    fun loadStats(prefs: SharedPreferences, key: String = DEFAULT_STATS_PREF_KEY) {
        if (!hasStats) {
            val stats = loadUserStats(prefs, "$key:stats")
            if (stats != null)
                this.stats = stats
        }
        val loadedLastNotifiedLevel =
            prefs.getInt("$key:${this::lastNotifiedLevel.name}", lastNotifiedLevel)
        lastNotifiedLevel = max(loadedLastNotifiedLevel, lastNotifiedLevel)
    }

    fun saveStats(prefs: SharedPreferences.Editor, key: String = DEFAULT_STATS_PREF_KEY) {
        if (hasStats)
            saveUserStats(prefs, stats, "$key:stats")
        prefs.putInt("$key:${this::lastNotifiedLevel.name}", lastNotifiedLevel)
    }

    fun loadStats(
        context: Context,
        prefsName: String = DEFAULT_PREFS_NAME,
        key: String = DEFAULT_STATS_PREF_KEY
    ) {
        if (!hasStats) {
            loadStats(context.getSharedPreferences(prefsName, Context.MODE_PRIVATE), key)
        }
    }

    fun saveStats(
        context: Context,
        prefsName: String = DEFAULT_PREFS_NAME,
        key: String = DEFAULT_STATS_PREF_KEY
    ) {
        if (hasStats) {
            val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            val editor = prefs.edit()
            saveStats(editor, key)
            editor.apply()
        }
    }

    fun requestStatsUpdate() {
        getService.poll()
    }

}