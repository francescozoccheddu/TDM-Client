package com.francescozoccheddu.tdmclient.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.francescozoccheddu.tdmclient.R
import com.francescozoccheddu.tdmclient.ui.MainService.Companion.MAP_BOUNDS
import com.francescozoccheddu.tdmclient.utils.android.visible
import com.francescozoccheddu.tdmclient.utils.data.point
import com.mapbox.geojson.FeatureCollection
import com.mapbox.mapboxsdk.geometry.LatLng
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
import kotlinx.android.synthetic.main.activity.cl_root
import kotlinx.android.synthetic.main.activity.fab
import kotlinx.android.synthetic.main.activity.mv_map
import kotlinx.android.synthetic.main.activity.v_fabSheetDuration
import kotlinx.android.synthetic.main.activity.v_fabSheetWalkMode
import kotlinx.android.synthetic.main.bar.et_search
import kotlinx.android.synthetic.main.bar.l_search_bar
import kotlinx.android.synthetic.main.bar.pb_search
import kotlinx.android.synthetic.main.bar.rv_search
import kotlinx.android.synthetic.main.sheet_duration.bt_duration_ok
import kotlinx.android.synthetic.main.sheet_walktype.li_walktype_destination
import kotlinx.android.synthetic.main.sheet_walktype.li_walktype_nearby

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
    private lateinit var searchProvider: LocationSearchProvider
    private lateinit var snackbar: ServiceSnackbar
    private val permissions = Permissions(this)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity)

        // Map
        mv_map.apply {
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
                    ) { style ->
                        enableLocationComponent(style)
                        setLatLngBoundsForCameraTarget(MAP_BOUNDS)
                    }
                    addOnMapClickListener {
                        if (destinationPickEnabled)
                            destination = it
                        destinationPickEnabled
                    }
                }
            }
        }
        // Fab
        run {

            fab.setOnClickListener {
                if (route != null)
                    route = null
                else if (destinationPickEnabled) {
                    if (destination != null) {
                        fabSheetMode = FabSheetMode.WALK_DURATION
                        fab.isExpanded = true
                    }
                    else {
                        destination = null
                        destinationPickEnabled = false
                        fab.isExpanded = false
                    }
                }
                else {
                    fabSheetMode = FabSheetMode.WALK_MODE
                    fab.isExpanded = true
                }
            }

            bt_duration_ok.setOnClickListener {
                fab.isExpanded = false
            }

            li_walktype_destination.setOnClickListener {
                destination = null
                fab.isExpanded = false
                destinationPickEnabled = true
            }

            li_walktype_nearby.setOnClickListener {
                fabSheetMode = FabSheetMode.WALK_DURATION
            }

        }
        // Search view
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

        snackbar = ServiceSnackbar(cl_root)
        snackbar.onLocationEnableRequest += {
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            Toast.makeText(this, R.string.toast_location_settings_overlay, Toast.LENGTH_LONG).show()
        }
        snackbar.onPermissionAskRequest += {
            if (permissions.canAsk)
                permissions.ask(this::onPermissionsChanged)
            else
                permissions.openSettings()
        }

    }

    private fun updateSnackbar() {

    }

    private var route: Any? = null
        set(value) {
            field = value
            updateFab()
        }

    private enum class FabSheetMode {
        WALK_MODE, WALK_DURATION
    }

    private var fabSheetMode = FabSheetMode.WALK_MODE
        set(value) {
            field = value
            v_fabSheetWalkMode.visible = value == FabSheetMode.WALK_MODE
            v_fabSheetDuration.visible = value == FabSheetMode.WALK_DURATION
        }

    private fun updateFab() {
    }

    private var destination: LatLng? = null
        set(value) {
            field = value
            map.getStyle { style ->
                val source = style.getSource(MB_SOURCE_DESTINATION)
                if (source is GeoJsonSource) {
                    if (value == null)
                        source.setGeoJson(null as FeatureCollection?)
                    else
                        source.setGeoJson(value.point)
                }
            }
            updateFab()
        }


    private var destinationPickEnabled = false
        set(value) {
            field = value
            l_search_bar.visible = value
            updateFab()
        }

    private var userLocation: LatLng? = null
        set(value) {
            if (field != value) {
                if (value != null && MAP_BOUNDS.contains(value))
                    field = value
                else
                    field = null
                searchProvider.userLocation = field
            }
        }

    private fun onPermissionsChanged(granted: Boolean) {
        if (granted) {
            if (snackbar.state == ServiceSnackbar.State.PERMISSIONS_UNGRANTED) {
                snackbar.state = null
                updateSnackbar()
            }

        }
        else
            snackbar.state = ServiceSnackbar.State.PERMISSIONS_UNGRANTED

    }

    @SuppressWarnings("MissingPermission")
    private fun enableLocationComponent(style: Style) {
        val locationComponent = map.locationComponent

        val locationComponentActivationOptions =
            LocationComponentActivationOptions.builder(this, style)
                .useDefaultLocationEngine(true)
                .build()

        locationComponent.activateLocationComponent(locationComponentActivationOptions)
        locationComponent.setLocationComponentEnabled(true)
        locationComponent.setCameraMode(CameraMode.NONE)
        locationComponent.setRenderMode(RenderMode.COMPASS)
    }

    override fun onBackPressed() {
        if (fab.isExpanded)
            fab.isExpanded = false
        else if (et_search.hasFocus())
            et_search.clearFocus()
        else if (destinationPickEnabled) {
            if (destination != null)
                destination = null
            else
                destinationPickEnabled = false
        }
        else
            super.onBackPressed()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        this.permissions.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    public override fun onStart() {
        super.onStart()
        mv_map.onStart()
    }

    public override fun onResume() {
        super.onResume()
        if (permissions.granted)
            onPermissionsChanged(true)
        else if (snackbar.state != ServiceSnackbar.State.PERMISSIONS_UNGRANTED) {
            if (permissions.canAsk)
                permissions.ask(this::onPermissionsChanged)
            else
                snackbar.state = ServiceSnackbar.State.PERMISSIONS_UNGRANTED
        }
        mv_map.onResume()
    }

    public override fun onPause() {
        super.onPause()
        mv_map.onPause()
    }

    public override fun onStop() {
        super.onStop()
        mv_map.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mv_map.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mv_map.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mv_map.onSaveInstanceState(outState)
    }

}
