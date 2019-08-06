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
import com.francescozoccheddu.tdmclient.R
import com.francescozoccheddu.tdmclient.data.operation.FakeSensor
import com.francescozoccheddu.tdmclient.data.operation.SensorDriver
import com.francescozoccheddu.tdmclient.ui.MainActivity
import com.francescozoccheddu.tdmclient.utils.FuncEvent
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
        private const val MEASURE_INTERVAL_TIME = 3f
        private val USER = SensorDriver.User(0, "0")

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


    inner class Binding : Binder() {
        val service get() = this@MainService
    }

    val onLocationChange = FuncEvent<MainService>()
    val onLocatableChange = FuncEvent<MainService>()
    val onOnlineChange = FuncEvent<MainService>()
    val onScoreChange = FuncEvent<MainService>()

    var location: Location? = null
        private set(value) {
            if (value != field) {
                field = value
                if (value != null)
                    sensorDriver.location = value
                sensorDriver.measuring = value != null
                onLocationChange(this)
            }
        }

    var locatable = false
        private set(value) {
            if (value != field) {
                field = value
                onLocatableChange(this)
            }
        }

    var online = false
        private set(value) {
            if (value != field) {
                field = value
                sensorDriver.pushing = value
                onOnlineChange(this)
            }
        }

    var score = 0
        private set(value) {
            if (value != field) {
                field = value
                onScoreChange(this)
            }
        }

    fun requestScoreUpdate() {
        sensorDriver.requestScoreUpdate()
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
    private val connectivityStatusReceiver = ConnectivityStatusReceiver()
    private val locationStatusReceiver = LocationStatusReceiver()
    private lateinit var locationEngine: LocationEngine
    private lateinit var sensorDriver: SensorDriver

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

        // Prepare timer
        run {
            val timer = Timer()
            locationExpirationCountdown = timer.Countdown().apply {
                time = LOCATION_EXPIRATION_TIME
                runnable = Runnable {
                    location = null
                }
            }
        }

        // Prepare sensor
        run {
            val fakeMeasurement = SensorDriver.Measurement(100f, 50f, 50f, 20f, 50f, 50f)
            sensorDriver = SensorDriver(this, USER, FakeSensor(fakeMeasurement))
            sensorDriver.onScoreChange += { score = it.score }
            sensorDriver.onConnectionChange += { online = it.reachable && ConnectivityStatusReceiver.isOnline(this) }
            sensorDriver.measureInterval = MEASURE_INTERVAL_TIME
            sensorDriver.loadScore(this)
        }

        // Prepare callbacks
        run {
            connectivityStatusReceiver.register(this) { online = it && sensorDriver.reachable }
            locationStatusReceiver.register(this) { locatable = it }

            locationEngine = LocationEngineProvider.getBestLocationEngine(this)
            val request = LocationEngineRequest
                .Builder((LOCATION_POLL_INTERVAL * 1000).roundToLong())
                .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                .setMaxWaitTime((LOCATION_POLL_MAX_WAIT * 1000).roundToLong())
                .build()
            locationEngine.requestLocationUpdates(request, locationCallback, mainLooper)
        }

        // Update results
        run {
            online = ConnectivityStatusReceiver.isOnline(this)
            locatable = LocationStatusReceiver.isEnabled(this)
            locationEngine.getLastLocation(locationCallback)
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
        locationEngine.removeLocationUpdates(locationCallback)
        connectivityStatusReceiver.unregister(this)
        locationStatusReceiver.unregister(this)
        sensorDriver.measuring = false
        sensorDriver.pushing = false
        sensorDriver.cancelAll()
        sensorDriver.saveScore(this)
    }

}