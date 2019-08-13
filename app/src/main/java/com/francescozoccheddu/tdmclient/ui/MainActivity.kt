package com.francescozoccheddu.tdmclient.ui

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.francescozoccheddu.tdmclient.R
import com.francescozoccheddu.tdmclient.ui.MainService.Companion.MAP_BOUNDS
import com.francescozoccheddu.tdmclient.ui.bottomgroup.RoutingController
import com.francescozoccheddu.tdmclient.utils.data.point
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.expressions.Expression.get
import com.mapbox.mapboxsdk.style.expressions.Expression.heatmapDensity
import com.mapbox.mapboxsdk.style.expressions.Expression.interpolate
import com.mapbox.mapboxsdk.style.expressions.Expression.linear
import com.mapbox.mapboxsdk.style.expressions.Expression.literal
import com.mapbox.mapboxsdk.style.expressions.Expression.rgba
import com.mapbox.mapboxsdk.style.expressions.Expression.stop
import com.mapbox.mapboxsdk.style.expressions.Expression.subtract
import com.mapbox.mapboxsdk.style.expressions.Expression.zoom
import com.mapbox.mapboxsdk.style.layers.HeatmapLayer
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.heatmapColor
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.heatmapIntensity
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.heatmapOpacity
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.heatmapRadius
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.heatmapWeight
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconIgnorePlacement
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconOptional
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.textAllowOverlap
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import kotlinx.android.synthetic.main.ma.ma_bg
import kotlinx.android.synthetic.main.ma.ma_map

class MainActivity : AppCompatActivity() {

    private companion object {

        private const val MAP_STYLE_URI = "mapbox://styles/francescozz/cjx1wlf2l080f1cqmmhh4jbgi"
        private const val MB_IMAGE_DESTINATION = "image_destination"
        private const val MB_SOURCE_DESTINATION = "source_destination"
        private const val MB_LAYER_DESTINATION = "source_destination"
        private const val MB_SOURCE_COVERAGE = "source_coverage"
        private const val MB_LAYER_COVERAGE = "layer_coverage"

    }

    private lateinit var map: MapboxMap
    //private lateinit var searchProvider: LocationSearchProvider
    //private lateinit var snackbar: ServiceSnackbar
    private val permissions = Permissions(this)
    private lateinit var routingController: RoutingController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ma)

        routingController = RoutingController(ma_bg)
        routingController.onDestinationChanged += {
            if (this::map.isInitialized) {
                map.getStyle {
                    val source = it.getSource(MB_SOURCE_DESTINATION) as GeoJsonSource
                    val geometry = routingController.destination?.point
                    //source.setGeoJson(Feature.fromGeometry(geometry))
                }
            }
        }

        // Map
        ma_map.apply {
            onCreate(savedInstanceState)
            getMapAsync { map ->
                this@MainActivity.map = map.apply {
                    setStyle(
                        Style.Builder()
                            .fromUri(MAP_STYLE_URI)
                            .withImage(MB_IMAGE_DESTINATION, resources.getDrawable(R.drawable.ic_somewhere, null))
                            .withSource(GeoJsonSource(MB_SOURCE_DESTINATION))
                            .withLayer(
                                SymbolLayer(MB_LAYER_DESTINATION, MB_SOURCE_DESTINATION)
                                    .withProperties(
                                        iconAllowOverlap(true),
                                        textAllowOverlap(true),
                                        iconImage(MB_IMAGE_DESTINATION),
                                        iconIgnorePlacement(true),
                                        iconOptional(false)
                                    )
                            ).withSource(GeoJsonSource(MB_SOURCE_COVERAGE))
                            .withLayer(
                                HeatmapLayer(MB_LAYER_COVERAGE, MB_SOURCE_COVERAGE)
                                    .withProperties(
                                        heatmapColor(
                                            interpolate(
                                                linear(), heatmapDensity(),
                                                literal(0.0), rgba(6, 50, 255, 0),
                                                literal(0.2), rgba(12, 105, 255, 0.5),
                                                literal(1.0), rgba(48, 210, 255, 1.0)
                                            )
                                        ),
                                        heatmapIntensity(
                                            interpolate(
                                                linear(), zoom(),
                                                stop(11, 1),
                                                stop(15, 3)
                                            )
                                        ),
                                        heatmapRadius(
                                            interpolate(
                                                linear(), zoom(),
                                                stop(11, 30),
                                                stop(15, 100)
                                            )
                                        ),
                                        heatmapOpacity(
                                            interpolate(
                                                linear(), zoom(),
                                                stop(12, 0.5),
                                                stop(15, 0)
                                            )
                                        ),
                                        heatmapWeight(
                                            subtract(literal(1f), get("coverage"))
                                        )
                                    )
                            )
                    ) {
                        setLatLngBoundsForCameraTarget(MAP_BOUNDS)
                        if (permissions.granted)
                            enableLocationComponent(it)
                        val source = it.getSource(MB_SOURCE_DESTINATION) as GeoJsonSource
                        val geometry = routingController.destination?.point
                        //source.setGeoJson(Feature.fromGeometry(geometry))
                    }

                    addOnMapClickListener { click ->
                        if (MainService.MAP_BOUNDS.contains(click)) {
                            val name = service?.getDistrictName(click)
                            Toast.makeText(this@MainActivity, name, Toast.LENGTH_SHORT).show()
                        }
                        routingController.pickingDestination
                    }
                }
            }
        }
        /*
        // Search component
        run {

            searchProvider = LocationSearchProvider(MAP_BOUNDS)
            searchProvider.onLocationClick += {
                if (destinationPickEnabled)
                    destination = it.point
                et_search.clearFocus()
            }

            rv_search.apply {
                layoutManager = LinearLayoutManager(this@MainActivity, RecyclerView.VERTICAL, false)
                adapter = searchProvider.adapter
            }

            val ibSearchClose = findViewById<ImageButton>(R.id.ib_search_close)
            val ibSearchClear = findViewById<ImageButton>(R.id.ib_search_clear)

            ibSearchClose.setOnClickListener {
                et_search.clearFocus()
            }

            ibSearchClear.setOnClickListener {
                et_search.text.clear()
            }

            searchProvider.onLoadingChange += { pb_search.visible = it }

            et_search.addTextChangedListener(object : TextWatcher {

                override fun afterTextChanged(s: Editable) {
                    pb_search.visible = false
                    ibSearchClear.visibility = if (s.length > 0) View.VISIBLE else View.GONE
                    searchProvider.query = s.toString()
                }

                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

            })

            et_search.setOnFocusChangeListener { _, focused ->
                rv_search.visibility = if (focused) View.VISIBLE else View.GONE
                ibSearchClose.visibility = if (focused) View.VISIBLE else View.GONE
                if (!focused) {
                    val service = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    service.hideSoftInputFromWindow(et_search.windowToken, 0)
                }
            }
        }

        snackbar = ServiceSnackbar(cl_root).apply {

            onLocationEnableRequest += {
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            onPermissionAskRequest += {
                if (permissions.canAsk)
                    permissions.ask(this@MainActivity::onPermissionsChanged)
                else
                    permissions.openSettings()
            }
            onRoutingAbortRequest += {
                routing = false
            }

        }

        */


    }

    private fun updateRouting() {
        routingController.problem = if (!permissions.granted)
            RoutingController.Problem.PERMISSIONS_UNGRANTED
        else {
            val service = this.service
            if (service == null)
                RoutingController.Problem.UNBOUND
            else if (!service.locatable)
                RoutingController.Problem.UNLOCATABLE
            else if (!service.online)
                RoutingController.Problem.OFFLINE
            else if (service.location == null)
                RoutingController.Problem.LOCATING
            else if (!service.insideMeasurementArea)
                RoutingController.Problem.OUTSIDE_AREA
            else null
        }
    }

    private fun onLocationChanged() {
        updateRouting()
    }

    private fun onLocatableChange() {
        updateRouting()
    }

    private fun onOnlineChange() {
        updateRouting()
    }

    private fun onScoreChange() {
    }

    private fun onCoverageDataChange() {

    }

    private var service: MainService? = null
        set(value) {
            if (value != field) {
                val old = field
                field = value
                if (old != null) {
                    old.onLocationChange -= this::onLocationChanged
                    old.onLocatableChange -= this::onLocatableChange
                    old.onOnlineChange -= this::onOnlineChange
                    old.onScoreChange -= this::onScoreChange
                    old.onCoverageDataChange -= this::onCoverageDataChange
                }
                if (value != null) {
                    value.onLocationChange += this::onLocationChanged
                    value.onLocatableChange += this::onLocatableChange
                    value.onOnlineChange += this::onOnlineChange
                    value.onScoreChange += this::onScoreChange
                    value.onCoverageDataChange += this::onCoverageDataChange
                    onLocationChanged()
                    onLocatableChange()
                    onOnlineChange()
                    onScoreChange()
                    onCoverageDataChange()
                }
                updateRouting()
            }
        }

    private val serviceConnection = object : ServiceConnection {

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
        }

        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = (binder as MainService.Binding).service
        }

    }

    private fun onPermissionsChanged(granted: Boolean) {
        if (granted) {
            if (this::map.isInitialized)
                map.getStyle { enableLocationComponent(it) }
            MainService.bind(this, serviceConnection)
        }
        updateRouting()
    }

    @SuppressWarnings("MissingPermission")
    private fun enableLocationComponent(style: Style) {
        map.locationComponent.apply {
            if (!isLocationComponentActivated) {
                activateLocationComponent(
                    LocationComponentActivationOptions
                        .builder(this@MainActivity, style)
                        .useDefaultLocationEngine(true)
                        .build()
                )
                setLocationComponentEnabled(true)
                setCameraMode(CameraMode.NONE)
                setRenderMode(RenderMode.COMPASS)
            }
        }
    }

    override fun onBackPressed() {
        if (routingController.onBack())
            return
        super.onBackPressed()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        this.permissions.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    public override fun onStart() {
        super.onStart()
        ma_map.onStart()
    }

    public override fun onResume() {
        super.onResume()
        if (permissions.granted)
            onPermissionsChanged(true)
        else if (routingController.problem != RoutingController.Problem.PERMISSIONS_UNGRANTED) {
            if (permissions.canAsk)
                permissions.ask(this::onPermissionsChanged)
            else
                updateRouting()
        }
        ma_map.onResume()
    }

    public override fun onPause() {
        super.onPause()
        if (service != null) {
            service = null
            unbindService(serviceConnection)
        }
        ma_map.onPause()
    }

    public override fun onStop() {
        super.onStop()
        ma_map.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        ma_map.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        ma_map.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        ma_map.onSaveInstanceState(outState)
    }

}
