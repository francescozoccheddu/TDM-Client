package com.francescozoccheddu.tdmclient.utils.android

import android.os.Handler
import android.os.Looper
import com.francescozoccheddu.tdmclient.utils.commons.dateElapsed
import com.francescozoccheddu.tdmclient.utils.commons.toMillis
import java.util.*
import kotlin.math.max

class Timer(looper: Looper = Looper.myLooper()!!) {

    private val handler = Handler(looper)

    inner class Task {

        private val internalRunnable = Runnable {
            if (running) {
                running = false
                runnable?.run()
            }
        }

        private lateinit var _scheduleTime: Date
        private var _scheduleDelay = 0f

        val scheduleTime
            get() = if (running)
                _scheduleTime
            else
                throw IllegalStateException("Not measuring")

        val scheduleDelay
            get() = if (running)
                _scheduleDelay
            else
                throw IllegalStateException("Not measuring")

        val elapsed get() = dateElapsed(scheduleTime)
        val remaining get() = max(scheduleDelay - elapsed, 0f)

        var running = false
            private set

        var runnable: Runnable? = null

        fun schedule(delay: Float) {
            if (delay <= 0f)
                throw IllegalArgumentException("Delay must be positive")
            if (running)
                throw IllegalStateException("Already measuring")
            running = true
            _scheduleDelay = delay
            _scheduleTime = Date()
            handler.postDelayed(internalRunnable, toMillis(delay))
        }

        fun reschedule(delay: Float) {
            if (delay <= 0f)
                throw IllegalArgumentException("Delay must be positive")
            if (!running)
                throw IllegalStateException("Not measuring")
            val newDelay = delay - elapsed
            _scheduleDelay = delay
            handler.removeCallbacks(internalRunnable)
            if (delay > 0)
                handler.postDelayed(internalRunnable, toMillis(newDelay))
            else
                internalRunnable.run()
        }

        fun cancel() {
            if (running) {
                running = false
                handler.removeCallbacks(internalRunnable)
            }
        }

    }

    inner class Countdown {

        private val task = Task()

        var runnable: Runnable?
            get() = task.runnable
            set(value) {
                task.runnable = value
            }

        var time: Float = 10f
            set(value) {
                if (value != field) {
                    if (value <= 0f)
                        throw IllegalArgumentException("'${this::time.name}' must be positive")
                    field = value
                    if (running)
                        task.reschedule(value)
                }
            }

        val running
            get() = task.running

        val elapsed
            get() = task.elapsed

        val remaining
            get() = task.remaining

        fun pull() {
            task.cancel()
            task.schedule(time)
        }

        fun cancel() {
            task.cancel()
        }

    }

    inner class Ticker {

        private val task = Task().apply {
            runnable = Runnable { tick() }
        }

        var runnable: Runnable? = null

        var tickInterval: Float = 10f
            set(value) {
                if (value != field) {
                    if (value <= 0f)
                        throw IllegalArgumentException("'${this::tickInterval.name}' must be positive")
                    field = value
                    reschedule()
                }
            }

        private fun reschedule() {
            task.cancel()
            if (running) {
                val last = lastTick
                val delay = if (last == null) 0f
                else tickInterval - dateElapsed(last)
                if (delay > 0f)
                    task.schedule(delay)
                else
                    tick()
            }
        }

        val timeSinceLastTick: Float
            get() {
                val last = lastTick
                return if (last == null) Float.POSITIVE_INFINITY else dateElapsed(
                    last
                )
            }

        var running = false
            set(value) {
                if (value != field) {
                    field = value
                    reschedule()
                }
            }

        var lastTick: Date? = null
            private set

        fun tick() {
            runnable?.run()
            notifyTick()
        }

        fun notifyTick() {
            lastTick = Date()
            reschedule()
        }

    }

}