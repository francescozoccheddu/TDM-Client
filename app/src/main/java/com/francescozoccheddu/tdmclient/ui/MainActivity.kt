package com.francescozoccheddu.tdmclient.ui

import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.francescozoccheddu.knob.KnobView
import com.francescozoccheddu.tdmclient.R
import com.francescozoccheddu.tdmclient.utils.data.client.Server
import com.francescozoccheddu.tdmclient.data.CoverageRetrieveMode
import com.francescozoccheddu.tdmclient.data.makeCoverageRetriever
import com.francescozoccheddu.tdmclient.ui.MainService.Companion.MAP_BOUNDS
import com.francescozoccheddu.tdmclient.utils.data.point
import com.francescozoccheddu.tdmclient.utils.android.visible
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
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
import kotlinx.android.synthetic.main.activity.mv_map
import kotlinx.android.synthetic.main.bar.pb_search
import kotlinx.android.synthetic.main.sheet_duration.bt_duration_ok
import kotlinx.android.synthetic.main.sheet_walktype.li_walktype_destination
import kotlinx.android.synthetic.main.sheet_walktype.li_walktype_nearby

class MainActivity : AppCompatActivity(), PermissionsListener {

    private companion object {

        private const val MAP_STYLE_URI = "mapbox://styles/francescozz/cjx1wlf2l080f1cqmmhh4jbgi"
        private const val MB_IMAGE_DESTINATION = "image_destination"
        private const val MB_SOURCE_DESTINATION = "source_destination"
        private const val MB_LAYER_DESTINATION = "source_destination"
        private const val MB_SOURCE_COVERAGE = "source_coverage"
        private const val MB_LAYER_COVERAGE = "layer_coverage"
    }

    private lateinit var map: MapboxMap

    private lateinit var rvSearchList: RecyclerView
    private lateinit var searchProvider: LocationSearchProvider

    private lateinit var kvDuration: KnobView
    private lateinit var vFabSheetWalkMode: View
    private lateinit var vFabSheetWalkDuration: View
    private lateinit var fab: FloatingActionButton
    private lateinit var etSearch: EditText
    private lateinit var vgSearchBar: ViewGroup

    private lateinit var permissionManager: PermissionsManager

    private fun setFabIcon(icon: Int) {
        fab.setImageDrawable(ContextCompat.getDrawable(this@MainActivity, icon))
    }

    private fun setFabColor(color: Int) {
        fab.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this@MainActivity, color))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity)

        //Retriever
        val server = Server(this, "http://localhost:8080/")
        val service = makeCoverageRetriever(server)
        service.pollRequest = CoverageRetrieveMode.POINTS
        service.onData += { data ->
            val json = data.toString()
            map.getStyle { style ->
                val oldSource = style.getSource("coverage")
                if (oldSource is GeoJsonSource)
                    oldSource.setGeoJson(json)
                else
                    style.addSource(GeoJsonSource("coverage", json))
            }
        }

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
                        service.periodicPoll = 2f
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

        vgSearchBar = findViewById(R.id.l_search_bar)

        vFabSheetWalkMode = findViewById(R.id.v_fabSheetWalkMode)
        vFabSheetWalkDuration = findViewById(R.id.v_fabSheetDuration)

        fab = findViewById(R.id.fab)
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

        // Search view
        searchProvider = LocationSearchProvider(MAP_BOUNDS)
        searchProvider.onLocationClick += {
            if (destinationPickEnabled)
                destination = it.point
            etSearch.clearFocus()
        }

        rvSearchList = findViewById(R.id.rv_search)
        rvSearchList.apply {
            layoutManager = LinearLayoutManager(this@MainActivity, RecyclerView.VERTICAL, false)
            adapter = searchProvider.adapter
        }

        val ibSearchClose = findViewById<ImageButton>(R.id.ib_search_close)
        val ibSearchClear = findViewById<ImageButton>(R.id.ib_search_clear)

        ibSearchClose.setOnClickListener {
            etSearch.clearFocus()
        }

        ibSearchClear.setOnClickListener {
            etSearch.text.clear()
        }

        etSearch = findViewById(R.id.et_search)

        searchProvider.onLoadingChange += { pb_search.visible = it }

        etSearch.addTextChangedListener(object : TextWatcher {

            override fun afterTextChanged(s: Editable) {
                pb_search.visible = false
                ibSearchClear.visibility = if (s.length > 0) View.VISIBLE else View.GONE
                searchProvider.query = s.toString()
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

        })

        etSearch.setOnFocusChangeListener { _, focused ->
            rvSearchList.visibility = if (focused) View.VISIBLE else View.GONE
            ibSearchClose.visibility = if (focused) View.VISIBLE else View.GONE
            if (!focused) {
                val service = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                service.hideSoftInputFromWindow(etSearch.windowToken, 0)
            }
        }


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
            when (value) {
                FabSheetMode.WALK_MODE -> {
                    vFabSheetWalkDuration.visibility = View.GONE
                    vFabSheetWalkMode.visibility = View.VISIBLE
                }
                FabSheetMode.WALK_DURATION -> {
                    vFabSheetWalkMode.visibility = View.GONE
                    vFabSheetWalkDuration.visibility = View.VISIBLE
                }
            }
        }

    private fun updateFab() {
        var show = true
        if (route != null) {
            setFabIcon(R.drawable.ic_cancel)
            setFabColor(R.color.fab_cancel)
        }
        if (destinationPickEnabled) {
            if (destination != null) {
                setFabIcon(R.drawable.ic_done)
                setFabColor(R.color.fab_ok)
            }
            else {
                setFabIcon(R.drawable.ic_cancel)
                setFabColor(R.color.fab_cancel)
            }
        }
        else {
            if (PermissionsManager.areLocationPermissionsGranted(this)) {
                setFabIcon(R.drawable.ic_walk)
                setFabColor(R.color.colorPrimary)
            }
            else {
                show = false
            }
        }
        if (show) {
            if (!fab.isOrWillBeShown())
                fab.show()
        }
        else {
            if (!fab.isOrWillBeHidden())
                fab.hide()
        }
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
            if (value) {
                vgSearchBar.visibility = View.VISIBLE
            }
            else {
                vgSearchBar.visibility = View.GONE
            }
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

    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
        Toast.makeText(this, R.string.user_location_permission_explanation, Toast.LENGTH_LONG).show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        permissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted)
            map.getStyle { enableLocationComponent(it) }
    }

    @SuppressWarnings("MissingPermission")
    private fun enableLocationComponent(style: Style) {
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            val locationComponent = map.locationComponent

            val locationComponentActivationOptions =
                LocationComponentActivationOptions.builder(this, style)
                    .useDefaultLocationEngine(false)
                    .build()

            locationComponent.activateLocationComponent(locationComponentActivationOptions)
            locationComponent.setLocationComponentEnabled(true)
            locationComponent.setCameraMode(CameraMode.NONE)
            locationComponent.setRenderMode(RenderMode.COMPASS)
        }
        else {
            permissionManager = PermissionsManager(this)
            permissionManager.requestLocationPermissions(this)
        }
    }

    override fun onBackPressed() {
        if (fab.isExpanded)
            fab.isExpanded = false
        else if (etSearch.hasFocus())
            etSearch.clearFocus()
        else if (destinationPickEnabled) {
            if (destination != null)
                destination = null
            else
                destinationPickEnabled = false
        }
        else
            super.onBackPressed()
    }

    public override fun onStart() {
        super.onStart()
        mv_map.onStart()
    }

    public override fun onResume() {
        super.onResume()
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
