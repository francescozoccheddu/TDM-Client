package com.francescozoccheddu.tdmclient.ui

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.location.Location
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.francescozoccheddu.tdmclient.data.client.Server
import com.francescozoccheddu.tdmclient.data.operation.CoverageRetrieveMode
import com.francescozoccheddu.tdmclient.data.operation.CoverageRetriever
import com.francescozoccheddu.tdmclient.data.operation.FakeSensor
import com.francescozoccheddu.tdmclient.data.operation.SensorDriver
import com.francescozoccheddu.tdmclient.data.operation.makeCoverageRetriever
import com.francescozoccheddu.tdmclient.utils.FuncEvent
import com.francescozoccheddu.tdmclient.utils.latLng
import com.francescozoccheddu.tdmclientservice.ConnectivityStatusReceiver
import com.francescozoccheddu.tdmclientservice.LocationStatusReceiver
import com.francescozoccheddu.tdmclientservice.Timer
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import org.json.JSONObject
import kotlin.math.roundToLong


class MainService : Service() {

    companion object {

        private const val KILL_WITH_TASK = false
        private const val LOCATION_POLL_INTERVAL = 1f
        private const val LOCATION_POLL_MAX_WAIT = LOCATION_POLL_INTERVAL * 5
        private const val LOCATION_EXPIRATION_TIME = 15f
        private const val MEASURE_INTERVAL_TIME = 3f
        private const val COVERAGE_INTERVAL_TIME = 10f
        private const val COVERAGE_EXPIRATION_TIME = 60f
        private val COVERAGE_RETRIEVE_MODE = CoverageRetrieveMode.POINTS
        private const val SERVER_ADDRESS = "http://192.168.43.57:8080/"
        private val USER = SensorDriver.User(0, "0")

        val MAP_BOUNDS = LatLngBounds.Builder()
            .include(LatLng(39.267498, 9.181226))
            .include(LatLng(39.176358, 9.054797))
            .build()

        fun bind(context: Context, connection: ServiceConnection) {
            val intent = Intent(context, MainService::class.java)
            context.startService(intent)
            if (!context.bindService(intent, connection, Context.BIND_ADJUST_WITH_ACTIVITY))
                throw RuntimeException("Unable to connect to service")
        }

        fun sensorLostConnection(context: Context) {
            if (context.stopService(Intent(context, MainService::class.java)))
                ServiceNotification.notifySensorConnectionLost(context)
        }

    }


    inner class Binding : Binder() {
        val service get() = this@MainService
    }

    val onLocationChange = FuncEvent<MainService>()
    val onLocatableChange = FuncEvent<MainService>()
    val onConnectedChange = FuncEvent<MainService>()
    val onOnlineChange = FuncEvent<MainService>()
    val onScoreChange = FuncEvent<MainService>()
    val onCoverageDataChange = FuncEvent<MainService>()

    val coverageData: JSONObject?
        get() = if (coverageRetriever.hasData && !coverageRetriever.expired) coverageRetriever.data else null

    val insideMeasurementArea
        get() = run {
            val loc = location
            loc != null && MAP_BOUNDS.contains(loc.latLng)
        }

    var location: Location? = null
        private set(value) {
            if (value != field) {
                field = value
                if (value != null)
                    sensorDriver.location = value
                sensorDriver.measuring = insideMeasurementArea
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

    var connected = false
        private set(value) {
            if (value != field) {
                field = value
                onConnectedChange(this)
            }
        }

    var online = false
        private set(value) {
            if (value != field) {
                field = value
                connected = sensorDriver.reachable && value
                coverageRetriever.periodicPoll = if (value && bound) COVERAGE_INTERVAL_TIME else null
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

    private lateinit var locationExpirationCountdown: Timer.Countdown
    private val connectivityStatusReceiver = ConnectivityStatusReceiver()
    private val locationStatusReceiver = LocationStatusReceiver()
    private lateinit var locationEngine: LocationEngine
    private lateinit var server: Server
    private lateinit var sensorDriver: SensorDriver
    private lateinit var coverageRetriever: CoverageRetriever
    private lateinit var notification: ServiceNotification
    private var bound = false
        set(value) {
            if (value != field) {
                field = value
                notification.foreground = !value
                coverageRetriever.periodicPoll = if (value && online) COVERAGE_INTERVAL_TIME else null
            }
        }

    @SuppressLint("MissingPermission")
    override fun onCreate() {
        super.onCreate()

        // Prepare notification
        run {
            notification = ServiceNotification(this)
            notification.registerChannel()
            notification.foreground = true
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

        server = Server(this, SERVER_ADDRESS)

        // Prepare retriever
        coverageRetriever = makeCoverageRetriever(server).apply {
            pollRequest = COVERAGE_RETRIEVE_MODE
            expiration = COVERAGE_EXPIRATION_TIME
            onData += { onCoverageDataChange(this@MainService) }
            onExpire += { onCoverageDataChange(this@MainService) }
        }

        // Prepare sensor
        run {
            val fakeMeasurement = SensorDriver.Measurement(100f, 50f, 50f, 20f, 50f, 50f)
            sensorDriver = SensorDriver(server, USER, FakeSensor(fakeMeasurement)).apply {
                onScoreChange += { this@MainService.score = it.score }
                onReachableChange += { connected = it.reachable && online }
                measureInterval = MEASURE_INTERVAL_TIME
                loadScore(this@MainService)
            }
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

        // Update results
        run {
            connected = ConnectivityStatusReceiver.isOnline(this)
            locatable = LocationStatusReceiver.isEnabled(this)
            locationEngine.getLastLocation(locationCallback)
            requestScoreUpdate()
        }

        if (!PermissionsManager.areLocationPermissionsGranted(this)) {
            Log.e(this::class.java.name, "Location access permissions are not granted")
            stopSelf()
        }

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onRebind(intent: Intent?) {
        super.onRebind(intent)
        bound = true
    }

    override fun onUnbind(intent: Intent?): Boolean {
        bound = false
        sensorDriver.saveScore(this)
        return true
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        if (KILL_WITH_TASK)
            stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? {
        bound = true
        return Binding()
    }

    override fun onDestroy() {
        super.onDestroy()
        locationExpirationCountdown.cancel()
        locationEngine.removeLocationUpdates(locationCallback)
        connectivityStatusReceiver.unregister(this)
        locationStatusReceiver.unregister(this)
        coverageRetriever.periodicPoll = null
        sensorDriver.measuring = false
        sensorDriver.pushing = false
        server.cancelAll()
        sensorDriver.saveScore(this)
    }

}