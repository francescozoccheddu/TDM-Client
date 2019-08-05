package com.francescozoccheddu.tdmclient.ui

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.location.Location
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import com.francescozoccheddu.tdmclient.R
import com.francescozoccheddu.tdmclient.ui.MainActivity
import com.francescozoccheddu.tdmclientservice.ConnectivityStatusReceiver
import com.francescozoccheddu.tdmclientservice.LocationStatusReceiver
import com.francescozoccheddu.tdmclientservice.Timer
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.android.core.location.LocationEngineResult
import kotlin.math.roundToLong


class MainService : Service() {

    companion object {

        private const val NOTIFICATION_CHANNEL = "ForegroundServiceChannel"
        private const val KILL_WITH_APP = false
        private const val ENABLE_KILL_BUTTON = false
        private const val FOREGROUND_NOTIFICATION_ID = 1
        private const val DEVICE_LOST_NOTIFICATION_ID = 2
        private const val LOCATION_POLL_INTERVAL = 1f
        private const val LOCATION_POLL_MAX_WAIT = LOCATION_POLL_INTERVAL * 5
        private const val LOCATION_EXPIRATION_TIME = 15f
        private const val MEASURE_INTERVAL_TIME = 2f
        private const val CONNECTIVITY_RETRY_WAIT = 5f
        private const val PREFS_SCORE_KEY = "score"
        private const val PREFS_NAME = "ClientServicePreferences"

        fun bind(context: Context, connection: ServiceConnection) {
            val intent = Intent(context, MainService::class.java)
            context.startService(intent)
            if (!context.bindService(intent, connection, Context.BIND_ADJUST_WITH_ACTIVITY))
                throw RuntimeException("Unable to connect to service")
        }

        fun sensorLostConnection(context: Context) {
            if (context.stopService(Intent(context, MainService::class.java))) {
                with(NotificationManagerCompat.from(context)) {
                    notify(
                        DEVICE_LOST_NOTIFICATION_ID,
                        NotificationCompat.Builder(context, NOTIFICATION_CHANNEL)
                            .setContentTitle("TDM Client")
                            .setContentText("Connessione col dispositivo persa. Premi per riprovare.")
                            .setSmallIcon(R.drawable.ic_launcher_foreground)
                            .setAutoCancel(true)
                            .build()
                    )
                }
            }
        }

    }


    interface Callbacks {

        fun onScoreChange(service: MainService)

        fun onLocationChange(service: MainService)

        fun onConnectivityChange(service: MainService)

        fun onLocationProviderChange(service: MainService)

    }

    inner class Binding : Binder() {
        val service get() = this@MainService
    }

    var callbacks: Callbacks? = null

    var location: Location? = null
        private set(value) {
            if (value != field) {
                field = value
                callbacks?.onLocationChange(this)
            }
        }

    var locatable = true
        private set(value) {
            if (value != field) {
                field = value
                callbacks?.onLocationProviderChange(this)
            }
        }

    var online = true
        private set(value) {
            if (value != field) {
                field = value
                callbacks?.onConnectivityChange(this)
            }
        }

    var score = 0
        private set(value) {
            if (value != field) {
                field = value
                callbacks?.onScoreChange(this)
            }
        }

    fun requestScoreUpdate() {

    }

    private val locationCallback = object : LocationEngineCallback<LocationEngineResult> {
        override fun onSuccess(result: LocationEngineResult?) {
            val location = result?.lastLocation
            if (location != null) {
                this@MainService.location = location
                locationExpirationCountdown.pull()
            }
        }

        override fun onFailure(exception: Exception) {}
    }

    private lateinit var notification: Notification
    private lateinit var locationExpirationCountdown: Timer.Countdown
    private lateinit var measureTicker: Timer.Ticker
    private val connectivityStatusReceiver = ConnectivityStatusReceiver()
    private val locationStatusReceiver = LocationStatusReceiver()
    private lateinit var locationEngine: LocationEngine

    private fun setForeground(enabled: Boolean) {
        if (enabled)
            startForeground(FOREGROUND_NOTIFICATION_ID, notification)
        else
            stopForeground(true)
    }

    @SuppressLint("MissingPermission")
    override fun onCreate() {
        super.onCreate()

        // Prepare notification
        run {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                    .createNotificationChannel(
                        NotificationChannel(
                            NOTIFICATION_CHANNEL,
                            "Foreground Service Channel",
                            NotificationManager.IMPORTANCE_DEFAULT
                        )
                    )
            }

            val notificationIntent = Intent(this, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)

            notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL)
                .setContentTitle("Title")
                .setContentText("Content")
                .setContentIntent(pendingIntent)
                .build()

            setForeground(true)
        }

        // Prepare callbacks
        run {
            connectivityStatusReceiver.register(this) { online = it }
            locationStatusReceiver.register(this) { locatable = it }

            locationEngine = LocationEngineProvider.getBestLocationEngine(this)
            val request = LocationEngineRequest
                .Builder((LOCATION_POLL_INTERVAL * 1000).roundToLong())
                .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                .setMaxWaitTime((LOCATION_POLL_MAX_WAIT * 1000).roundToLong())
                .build()
            locationEngine.requestLocationUpdates(request, locationCallback, mainLooper)
        }

        // Prepare timers
        run {
            val timer = Timer()
            locationExpirationCountdown = timer.Countdown().apply {
                time = LOCATION_EXPIRATION_TIME
                runnable = Runnable {
                    location = null
                }
            }
            measureTicker = timer.Ticker().apply {
                tickInterval = MEASURE_INTERVAL_TIME
                runnable = Runnable {

                }
            }
        }

        // Update results
        run {
            online = ConnectivityStatusReceiver.isOnline(this)
            locatable = LocationStatusReceiver.isEnabled(this)

            locationEngine.getLastLocation(locationCallback)

            score = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getInt(PREFS_SCORE_KEY, score)
            requestScoreUpdate()
        }

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onRebind(intent: Intent?) {
        super.onRebind(intent)
        setForeground(false)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        callbacks = null
        setForeground(true)
        return true
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        if (KILL_WITH_APP)
            stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? {
        setForeground(false)
        return Binding()
    }

    override fun onDestroy() {
        super.onDestroy()
        locationExpirationCountdown.cancel()
        measureTicker.running = false
        locationEngine.removeLocationUpdates(locationCallback)
        connectivityStatusReceiver.unregister(this)
        locationStatusReceiver.unregister(this)
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putInt(PREFS_SCORE_KEY, score)
        }
    }

}