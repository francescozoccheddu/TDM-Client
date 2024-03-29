package com.francescozoccheddu.tdmclient.data

import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import com.francescozoccheddu.tdmclient.utils.android.Timer
import com.francescozoccheddu.tdmclient.utils.commons.FixedSizeSortedQueue
import com.francescozoccheddu.tdmclient.utils.commons.ProcEvent
import com.francescozoccheddu.tdmclient.utils.commons.dateElapsed
import com.francescozoccheddu.tdmclient.utils.data.client.Server
import com.francescozoccheddu.tdmclient.utils.data.client.error
import java.util.*
import kotlin.math.max

class SensorDriver(server: Server, val user: User, val sensor: Sensor) {

    companion object {
        val DEFAULT_PREFS_NAME = "${this::class.java.canonicalName}:userStats"
        val DEFAULT_STATS_PREF_KEY = "${this::class.java.canonicalName}:userStats"

        private const val MAX_STATS_REQUESTS = 4

        private const val MAX_MEASUREMENTS_REQUESTS = 4
        private const val MAX_UNREACHABLE_ATTEMPTS = 3
        private const val MAX_BATCH_HOLD_TIME = 10f
        private const val MAX_BATCH_SIZE = 10
        private const val MAX_PUT_REQUEST_SIZE = 50
        private const val MAX_QUEUE_HOLD_TIME = 30f
        private const val MAX_QUEUE_SIZE = 100
        private const val MIN_REQUEST_DELAY = 1f
        private const val PRIORITIZE_OLDEST = true
    }

    interface Sensor {
        fun measure(): Measurement
    }

    private val queue = FixedSizeSortedQueue.by(MAX_QUEUE_SIZE, true) { value: LocalizedMeasurement -> value.time }

    private val userService = makeUserService(server, user).apply {
        onData += { stats = it }
    }
    private val measurementService = makeMeasurementService(server).apply {
        onRequestStatusChanged += {
            if (it.status.succeeded) {
                userService.submit(it.startTime, it.response)
                reachable = true
                failureCount = 0
            }
            else if (it.status.error) {
                failureCount++
                if (failureCount > MAX_UNREACHABLE_ATTEMPTS)
                    reachable = false
            }

            if (!it.status.pending) {
                if (!it.status.succeeded)
                    queue.addLocalized(it.request.measurements.filter {
                        dateElapsed(it.time) < MAX_QUEUE_HOLD_TIME
                    })
                updateBatch()
            }
        }
    }

    private var failureCount = 0

    private fun updateBatch() {
        run {
            val iterator = queue.mutableIterator(true)
            for (item in iterator)
                if (dateElapsed(item.time) >= MAX_QUEUE_HOLD_TIME)
                    iterator.remove()
                else break
        }
        if (pushing) {
            fun batchWaitTime(): Float? {
                val oldestTime = queue.last?.time
                return if (oldestTime == null) null
                else max(MAX_BATCH_HOLD_TIME - dateElapsed(oldestTime), 0f)
            }

            fun canPush() = measurementService.pendingRequests.size < MAX_MEASUREMENTS_REQUESTS
                    && !requestCountdown.running

            fun shouldPush() = queue.length >= MAX_BATCH_SIZE || batchWaitTime() == 0f
            while (canPush() && shouldPush()) {
                val measurements = mutableListOf<LocalizedMeasurement>()
                val iterator = queue.mutableIterator(PRIORITIZE_OLDEST)
                for (item in iterator) {
                    measurements.add(item)
                    iterator.remove()
                    if (measurements.size >= MAX_PUT_REQUEST_SIZE)
                        break
                }
                measurementService.Request(
                    MeasurementPutRequest(
                        user,
                        measurements
                    )
                ).start()
                requestCountdown.pull()
            }
            run {
                val wait = batchWaitTime()
                if (wait != null && wait > 0f && queue.length < MAX_BATCH_SIZE) {
                    countdown.cancel()
                    countdown.time = wait
                    countdown.pull()
                }
            }
        }
    }

    private val requestCountdown: Timer.Countdown
    private val countdown: Timer.Countdown
    private val ticker: Timer.Ticker

    init {
        val timer = Timer()
        requestCountdown = timer.Countdown().apply {
            time = MIN_REQUEST_DELAY
            runnable = Runnable {
                updateBatch()
            }
        }
        countdown = timer.Countdown().apply {
            runnable = Runnable {
                updateBatch()
            }
        }
        ticker = timer.Ticker().apply {
            runnable = Runnable {
                queue.add(
                    LocalizedMeasurement(
                        Date(),
                        location,
                        sensor.measure()
                    )
                )
                updateBatch()
            }
        }
    }

    var measuring
        get() = ticker.running
        set(value) {
            if (value && !this::location.isInitialized)
                throw IllegalStateException("'${this::location.name}' has not been initialized")
            ticker.running = value
        }

    var pushing = false
        set(value) {
            if (value != field) {
                field = value
                if (!value)
                    countdown.cancel()
                updateBatch()
            }
        }

    lateinit var location: Location

    var measureInterval
        get() = ticker.tickInterval
        set(value) {
            ticker.tickInterval = value
        }

    val hasStats get() = this::_userStats.isInitialized

    private lateinit var _userStats: UserStats

    var notifyLevel = 0
        set(value) {
            if (value != field) {
                field = value
                userService.pollRequest = UserGetRequest(user, value)
                requestStatsUpdate()
            }
        }

    var stats
        get() = _userStats
        private set(value) {
            if (!hasStats || value != _userStats) {
                _userStats = value
                onStatsChange()
            }
        }

    var reachable = true
        private set(value) {
            if (value != field) {
                field = value
                onReachableChange()
            }
        }

    val onStatsChange = ProcEvent()

    val onReachableChange = ProcEvent()

    fun loadStats(prefs: SharedPreferences, key: String = DEFAULT_STATS_PREF_KEY) {
        if (!hasStats) {
            val stats = loadUserStats(prefs, key)
            if (stats != null)
                this.stats = stats
        }
    }

    fun saveStats(prefs: SharedPreferences.Editor, key: String = DEFAULT_STATS_PREF_KEY) {
        if (hasStats)
            saveUserStats(prefs, stats, key)
    }

    fun loadStats(context: Context, prefsName: String = DEFAULT_PREFS_NAME, key: String = DEFAULT_STATS_PREF_KEY) {
        if (!hasStats) {
            loadStats(context.getSharedPreferences(prefsName, Context.MODE_PRIVATE), key)
        }
    }

    fun saveStats(context: Context, prefsName: String = DEFAULT_PREFS_NAME, key: String = DEFAULT_STATS_PREF_KEY) {
        if (hasStats) {
            val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            val editor = prefs.edit()
            saveStats(editor, key)
            editor.apply()
        }
    }

    fun requestStatsUpdate() {
        if (userService.pendingRequests.size >= MAX_STATS_REQUESTS)
            userService.pendingRequests.minBy { it.startTime }?.cancel()
        userService.poll()
    }

    fun cancelAll() {
        userService.cancelAll()
        measurementService.cancelAll()
    }

}