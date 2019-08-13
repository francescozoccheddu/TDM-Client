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
import com.francescozoccheddu.tdmclient.data.CoverageRetrieveMode
import com.francescozoccheddu.tdmclient.data.CoverageRetriever
import com.francescozoccheddu.tdmclient.data.FakeSensor
import com.francescozoccheddu.tdmclient.data.RouteRequest
import com.francescozoccheddu.tdmclient.data.RouteRetriever
import com.francescozoccheddu.tdmclient.data.SensorDriver
import com.francescozoccheddu.tdmclient.data.makeCoverageRetriever
import com.francescozoccheddu.tdmclient.data.makeRouteRetriever
import com.francescozoccheddu.tdmclient.utils.android.ConnectivityStatusReceiver
import com.francescozoccheddu.tdmclient.utils.android.LocationStatusReceiver
import com.francescozoccheddu.tdmclient.utils.android.Timer
import com.francescozoccheddu.tdmclient.utils.commons.ProcEvent
import com.francescozoccheddu.tdmclient.utils.data.client.Server
import com.francescozoccheddu.tdmclient.utils.data.latLng
import com.francescozoccheddu.tdmclient.utils.data.latlngBounds
import com.francescozoccheddu.tdmclient.utils.data.point
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.turf.TurfJoins
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
        private val STOP_ACTION = "IntentActionStop"
        private val START_ACTIVITY_ACTION = "IntentActionStartActivity"

        val MAP_BOUNDS = LatLngBounds.Builder()
            .include(LatLng(39.267498, 9.181226))
            .include(LatLng(39.176358, 9.054797))
            .build()

        fun makeStopIntent(context: Context) = Intent(context, MainService::class.java).apply {
            action = STOP_ACTION
        }

        fun makeStartActivityIntent(context: Context) = Intent(context, MainService::class.java).apply {
            action = START_ACTIVITY_ACTION
        }

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

    val onLocationChange = ProcEvent()
    val onLocatableChange = ProcEvent()
    val onConnectedChange = ProcEvent()
    val onOnlineChange = ProcEvent()
    val onScoreChange = ProcEvent()
    val onCoverageDataChange = ProcEvent()

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
                onLocationChange()
            }
        }

    var locatable = false
        private set(value) {
            if (value != field) {
                field = value
                onLocatableChange()
            }
        }

    var connected = false
        private set(value) {
            if (value != field) {
                field = value
                onConnectedChange()
            }
        }

    var online = false
        private set(value) {
            if (value != field) {
                field = value
                connected = sensorDriver.reachable && value
                coverageRetriever.periodicPoll = if (value && bound) COVERAGE_INTERVAL_TIME else null
                sensorDriver.pushing = value
                onOnlineChange()
            }
        }

    var score = 0
        private set(value) {
            if (value != field) {
                field = value
                onScoreChange()
            }
        }

    fun requestScoreUpdate() {
        sensorDriver.requestScoreUpdate()
    }

    fun requestRoute(to: Location?, time: Float, callback: (RouteRequest, List<Point>?) -> Unit) {
        val from = location
        if (from != null) {
            routeRetriever.Request(RouteRequest(from, to, time)).apply {
                onStatusChange += {
                    if (it.status.succeeded)
                        callback(it.request, it.response)
                    else if (!it.status.pending)
                        callback(it.request, null)
                }
                start()
            }
        }
        else
            throw IllegalStateException("'${this::location.name}' is null")
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
    private lateinit var routeRetriever: RouteRetriever
    private lateinit var notification: ServiceNotification
    private var bound = false
        set(value) {
            if (value != field) {
                field = value
                notification.foreground = !value
                coverageRetriever.periodicPoll = if (value && online) COVERAGE_INTERVAL_TIME else null
            }
        }
    private val districts: FeatureCollection? by lazy {
        try {
            val json = assets.open("map.geojson").bufferedReader().use {
                it.readText()
            }
            FeatureCollection.fromJson(json)
        } catch (_: Exception) {
            null
        }
    }

    fun getDistrictName(point: LatLng): String? {
        val districts = this.districts
        if (districts != null) {
            val bbox = districts.bbox()
            if (bbox == null || bbox.latlngBounds.contains(point)) {
                val gpoint = point.point
                val feature = districts.features()?.firstOrNull {
                    val geometry = it.geometry()
                    geometry is Polygon && TurfJoins.inside(gpoint, geometry)
                }
                return feature?.properties()?.get("name")?.asString
            }
        }
        return null
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

        // Prepare retrievers
        run {
            coverageRetriever = makeCoverageRetriever(server).apply {
                pollRequest = COVERAGE_RETRIEVE_MODE
                expiration = COVERAGE_EXPIRATION_TIME
                onData += { onCoverageDataChange() }
                onExpire += { onCoverageDataChange() }
            }

            routeRetriever = makeRouteRetriever(server)
        }

        // Prepare sensor
        run {
            val fakeMeasurement = SensorDriver.Measurement(100f, 50f, 50f, 20f, 50f, 50f)
            sensorDriver = SensorDriver(
                server,
                USER,
                FakeSensor(fakeMeasurement)
            ).apply {
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
        when (intent?.action) {
            STOP_ACTION -> stopSelf()
            START_ACTIVITY_ACTION -> {
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            }
        }
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