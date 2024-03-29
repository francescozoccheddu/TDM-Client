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
import com.francescozoccheddu.tdmclient.data.CoverageService
import com.francescozoccheddu.tdmclient.data.FakeSensor
import com.francescozoccheddu.tdmclient.data.Measurement
import com.francescozoccheddu.tdmclient.data.PoiService
import com.francescozoccheddu.tdmclient.data.RouteRequest
import com.francescozoccheddu.tdmclient.data.RouteService
import com.francescozoccheddu.tdmclient.data.SensorDriver
import com.francescozoccheddu.tdmclient.data.User
import com.francescozoccheddu.tdmclient.data.UserStats
import com.francescozoccheddu.tdmclient.data.makeCoverageService
import com.francescozoccheddu.tdmclient.data.makePoiService
import com.francescozoccheddu.tdmclient.data.makeRouteService
import com.francescozoccheddu.tdmclient.ui.utils.ServiceNotification
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
import kotlin.math.roundToLong


class MainService : Service() {

    companion object {

        private const val KILL_WITH_TASK = false
        private const val LOCATION_POLL_INTERVAL = 1f
        private const val LOCATION_POLL_MAX_WAIT = LOCATION_POLL_INTERVAL * 5
        private const val LOCATION_EXPIRATION_TIME = 30f
        private const val MEASURE_INTERVAL_TIME = 3f
        private const val COVERAGE_INTERVAL_TIME = 2f
        private const val COVERAGE_EXPIRATION_TIME = 60f
        private const val POI_INTERVAL_TIME = 10f
        private const val POI_EXPIRATION_TIME = 120f
        private const val SERVER_ADDRESS = "http://192.168.43.57:8080/"
        private val USER = User(0, "0")
        private const val STOP_ACTION = "IntentActionStop"
        private const val START_ACTIVITY_ACTION = "IntentActionStartActivity"

        val MAP_BOUNDS = LatLngBounds.Builder()
            .include(LatLng(39.1806426351715, 9.07530756367117))
            .include(LatLng(39.2828121980494, 9.18185143891843))
            .build()

        fun makeStopIntent(context: Context) = Intent(context, MainService::class.java).apply {
            action = STOP_ACTION
        }

        fun makeStartActivityIntent(context: Context) =
            Intent(context, MainService::class.java).apply {
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
    val onStatsChange = ProcEvent()
    val onCoveragePointDataChange = ProcEvent()
    val onCoverageQuadDataChange = ProcEvent()
    val onPoiDataChange = ProcEvent()

    val coveragePointData: FeatureCollection?
        get() = if (coveragePointService.hasData && !coveragePointService.expired) coveragePointService.data else null

    val coverageQuadData: FeatureCollection?
        get() = if (coverageQuadService.hasData && !coverageQuadService.expired) coverageQuadService.data else null

    val poiData: FeatureCollection?
        get() = if (poiService.hasData && !poiService.expired) poiService.data else null

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
                val coveragePollPeriod = if (value && bound) COVERAGE_INTERVAL_TIME else null
                coveragePointService.periodicPoll = coveragePollPeriod
                coverageQuadService.periodicPoll = coveragePollPeriod
                poiService.periodicPoll = if (value && bound) POI_INTERVAL_TIME else null
                sensorDriver.pushing = value
                onOnlineChange()
            }
        }

    var notifyLevel: Int
        get() = sensorDriver.notifyLevel
        set(value) {
            sensorDriver.notifyLevel = value
        }

    val userStats: UserStats?
        get() = if (sensorDriver.hasStats) sensorDriver.stats else null

    fun requestStatsUpdate() {
        sensorDriver.requestStatsUpdate()
    }

    fun requestRoute(to: LatLng?, time: Float): Server.Service<RouteRequest, List<Point>>.Request {
        val from = location
        if (from != null)
            return routeService.Request(RouteRequest(from.latLng, to, time))
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
    private lateinit var coveragePointService: CoverageService
    private lateinit var coverageQuadService: CoverageService
    private lateinit var poiService: PoiService
    private lateinit var routeService: RouteService
    private lateinit var notification: ServiceNotification
    private var bound = false
        set(value) {
            if (value != field) {
                field = value
                notification.foreground = !value
                val coveragePollPeriod = if (value && online) COVERAGE_INTERVAL_TIME else null
                coveragePointService.periodicPoll = coveragePollPeriod
                coverageQuadService.periodicPoll = coveragePollPeriod
                poiService.periodicPoll = POI_INTERVAL_TIME
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

        // Prepare services
        run {
            coveragePointService = makeCoverageService(server).apply {
                pollRequest = CoverageRetrieveMode.POINTS
                expiration = COVERAGE_EXPIRATION_TIME
                onData += { onCoveragePointDataChange() }
                onExpire += onCoveragePointDataChange
            }
            coverageQuadService = makeCoverageService(server).apply {
                pollRequest = CoverageRetrieveMode.QUADS
                expiration = COVERAGE_EXPIRATION_TIME
                onData += { onCoverageQuadDataChange() }
                onExpire += onCoverageQuadDataChange
            }
            poiService = makePoiService(server, USER).apply {
                expiration = POI_EXPIRATION_TIME
                onData += { onPoiDataChange() }
                onExpire += onPoiDataChange
            }
            routeService = makeRouteService(server)
        }

        // Prepare sensor
        run {
            val fakeMeasurement = Measurement(100f, 50f, 50f, 20f, 50f, 50f)
            sensorDriver = SensorDriver(
                server,
                USER,
                FakeSensor(fakeMeasurement)
            ).apply {
                onStatsChange += this@MainService.onStatsChange
                onReachableChange += { connected = sensorDriver.reachable && online }
                measureInterval = MEASURE_INTERVAL_TIME
                loadStats(this@MainService)
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
            requestStatsUpdate()
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
        sensorDriver.saveStats(this)
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
        coveragePointService.periodicPoll = null
        coverageQuadService.periodicPoll = null
        poiService.periodicPoll = null
        sensorDriver.measuring = false
        sensorDriver.pushing = false
        server.cancelAll()
        sensorDriver.saveStats(this)
    }

}