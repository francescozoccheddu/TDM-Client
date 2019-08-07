package com.francescozoccheddu.tdmclient.data

import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import android.os.Looper
import com.francescozoccheddu.tdmclient.utils.android.Timer
import com.francescozoccheddu.tdmclient.utils.commons.FixedSizeSortedQueue
import com.francescozoccheddu.tdmclient.utils.commons.FuncEvent
import com.francescozoccheddu.tdmclient.utils.commons.dateElapsed
import com.francescozoccheddu.tdmclient.utils.commons.iso
import com.francescozoccheddu.tdmclient.utils.data.client.Interpreter
import com.francescozoccheddu.tdmclient.utils.data.client.PollInterpreter
import com.francescozoccheddu.tdmclient.utils.data.client.RetryPolicy
import com.francescozoccheddu.tdmclient.utils.data.client.Server
import com.francescozoccheddu.tdmclient.utils.data.client.error
import com.francescozoccheddu.tdmclient.utils.data.json
import org.json.JSONArray
import org.json.JSONObject
import java.util.*
import kotlin.math.max

class SensorDriver(server: Server, val user: User, val sensor: Sensor, looper: Looper = Looper.myLooper()!!) {

    companion object {
        const val DEFAULT_PREFS_NAME = "tdmclient:SensorDriver:SharedPreferences"
        const val DEFAULT_SCORE_PREF_KEY = "$DEFAULT_PREFS_NAME.score"

        private const val MAX_SCORE_REQUESTS = 4
        private const val SCORE_SERVICE_ADDRESS = "getuser"
        private val SCORE_SERVICE_RETRY_POLICY = RetryPolicy(2f)

        private const val MEASUREMENT_SERVICE_ADDRESS = "putmeasurements"
        private val MEASUREMENT_SERVICE_RETRY_POLICY = RetryPolicy(4f)

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

    data class User(val id: Int, val passkey: String)
    data class Measurement(
        val altitude: Float, val humidity: Float,
        val pressure: Float, val temperature: Float,
        val fineDust150: Float, val fineDust200: Float
    )

    private data class LocalizedMeasurement(val time: Date, val location: Location, val measurement: Measurement)
    private data class MeasurementPutRequest(val user: User, val measurements: Collection<LocalizedMeasurement>)

    private val queue = FixedSizeSortedQueue.by(MAX_QUEUE_SIZE, true) { value: LocalizedMeasurement -> value.time }

    private val scoreService =
        server.PollService(SCORE_SERVICE_ADDRESS, user, PollInterpreter.from(object : Interpreter<User, Int> {
            override fun interpretRequest(request: User): JSONObject? =
                JSONObject().apply {
                    put("id", request.id)
                    put("passkey", request.passkey)
                }

            override fun interpretResponse(request: User, response: JSONObject): Int {
                try {
                    return response["score"] as Int
                } catch (_: Exception) {
                    throw Interpreter.UninterpretableResponseException()
                }
            }
        })).apply {
            onData += { score = it }
            customRetryPolicy = SCORE_SERVICE_RETRY_POLICY
        }
    private val measurementService =
        server.Service(MEASUREMENT_SERVICE_ADDRESS, object : Interpreter<MeasurementPutRequest, Int> {
            override fun interpretRequest(request: MeasurementPutRequest): JSONObject? =
                JSONObject().apply {
                    put("id", request.user.id)
                    put("passkey", request.user.passkey)
                    put("measurements", JSONArray(request.measurements.map {
                        JSONObject().apply {
                            put("time", it.time.iso)
                            put("location", it.location.json)
                            put("altitude", it.measurement.altitude)
                            put("humidity", it.measurement.humidity)
                            put("pressure", it.measurement.pressure)
                            put("temperature", it.measurement.temperature)
                            put("fineDust150", it.measurement.fineDust150)
                            put("fineDust200", it.measurement.fineDust200)
                        }
                    }))
                }

            override fun interpretResponse(request: MeasurementPutRequest, response: JSONObject): Int {
                try {
                    return response.getInt("score")
                } catch (_: Exception) {
                    throw Interpreter.UninterpretableResponseException()
                }
            }
        }).apply {
            onRequestStatusChanged += {
                if (it.status.succeeded) {
                    scoreService.submit(it.startTime, it.response)
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
            customRetryPolicy = MEASUREMENT_SERVICE_RETRY_POLICY
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

    var score = 0
        private set(value) {
            if (value != field || !hasScore) {
                hasScore = true
                field = value
                onScoreChange(this)
            }
        }

    var reachable = true
        private set(value) {
            if (value != field) {
                field = value
                onReachableChange(this)
            }
        }

    private var hasScore = false

    val onScoreChange = FuncEvent<SensorDriver>()

    val onReachableChange = FuncEvent<SensorDriver>()

    fun loadScore(prefs: SharedPreferences, key: String = DEFAULT_SCORE_PREF_KEY) {
        if (!hasScore && prefs.contains(key))
            score = prefs.getInt(key, score)
    }

    fun saveScore(prefs: SharedPreferences.Editor, key: String = DEFAULT_SCORE_PREF_KEY) {
        if (hasScore) {
            prefs.putInt(key, score)
        }
    }

    fun loadScore(context: Context, prefsName: String = DEFAULT_PREFS_NAME, key: String = DEFAULT_SCORE_PREF_KEY) {
        if (!hasScore) {
            loadScore(context.getSharedPreferences(prefsName, Context.MODE_PRIVATE), key)
        }
    }

    fun saveScore(context: Context, prefsName: String = DEFAULT_PREFS_NAME, key: String = DEFAULT_SCORE_PREF_KEY) {
        if (hasScore) {
            val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            val editor = prefs.edit()
            saveScore(editor, key)
            editor.apply()
        }
    }

    fun requestScoreUpdate() {
        if (scoreService.pendingRequests.size >= MAX_SCORE_REQUESTS)
            scoreService.pendingRequests.minBy { it.startTime }?.cancel()
        scoreService.poll()
    }

    fun cancelAll() {
        scoreService.cancelAll()
        measurementService.cancelAll()
    }

}