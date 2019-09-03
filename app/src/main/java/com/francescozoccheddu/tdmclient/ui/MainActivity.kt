package com.francescozoccheddu.tdmclient.ui

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.francescozoccheddu.tdmclient.R
import com.francescozoccheddu.tdmclient.ui.MainService.Companion.MAP_BOUNDS
import com.francescozoccheddu.tdmclient.ui.components.bg.RoutingController
import com.francescozoccheddu.tdmclient.ui.components.sb.SearchBarComponent
import com.francescozoccheddu.tdmclient.ui.components.us.UserStatsComponent
import com.francescozoccheddu.tdmclient.utils.android.Permissions
import com.francescozoccheddu.tdmclient.utils.android.dp
import com.francescozoccheddu.tdmclient.utils.android.hsv
import com.francescozoccheddu.tdmclient.utils.data.latLng
import com.francescozoccheddu.tdmclient.utils.data.latlng
import com.francescozoccheddu.tdmclient.utils.data.mapboxAccessToken
import com.francescozoccheddu.tdmclient.utils.data.point
import com.francescozoccheddu.tdmclient.utils.data.statsOrNull
import com.mapbox.core.constants.Constants.PRECISION_6
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Geometry
import com.mapbox.geojson.LineString
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.localization.LocalizationPlugin
import com.mapbox.mapboxsdk.style.expressions.Expression.color
import com.mapbox.mapboxsdk.style.expressions.Expression.exponential
import com.mapbox.mapboxsdk.style.expressions.Expression.get
import com.mapbox.mapboxsdk.style.expressions.Expression.heatmapDensity
import com.mapbox.mapboxsdk.style.expressions.Expression.interpolate
import com.mapbox.mapboxsdk.style.expressions.Expression.linear
import com.mapbox.mapboxsdk.style.expressions.Expression.literal
import com.mapbox.mapboxsdk.style.expressions.Expression.stop
import com.mapbox.mapboxsdk.style.expressions.Expression.subtract
import com.mapbox.mapboxsdk.style.expressions.Expression.zoom
import com.mapbox.mapboxsdk.style.layers.FillLayer
import com.mapbox.mapboxsdk.style.layers.HeatmapLayer
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.Property
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillColor
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillOpacity
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.heatmapColor
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.heatmapOpacity
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.heatmapRadius
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.heatmapWeight
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconColor
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconOpacity
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconOptional
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconSize
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineCap
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineColor
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineJoin
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineOpacity
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineWidth
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.textAllowOverlap
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.textAnchor
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.textColor
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.textField
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.textOpacity
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.textOptional
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.textRadialOffset
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.textSize
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigationOptions
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgressState
import kotlinx.android.synthetic.main.bg.bg_root
import kotlinx.android.synthetic.main.ma.ma_confetti
import kotlinx.android.synthetic.main.ma.ma_map
import kotlinx.android.synthetic.main.sb.sb_root
import kotlinx.android.synthetic.main.us.us_root
import nl.dionsegijn.konfetti.models.Shape
import nl.dionsegijn.konfetti.models.Size
import kotlin.math.roundToInt


class MainActivity : AppCompatActivity() {

    private companion object {

        private const val MAP_STYLE_URI = "mapbox://styles/francescozz/cjx1wlf2l080f1cqmmhh4jbgi"
        private const val MIN_ZOOM = 11.0
        private const val MAX_ZOOM = 20.0
        private const val MIN_HEATMAP_RADIUS_WIDTH_FACTOR = 0.03f
        private const val MAX_HEATMAP_RADIUS_WIDTH_FACTOR = 0.25f
        private const val SEARCH_ZOOM = 13.0
        private const val CAMERA_ANIMATION_DURATION = 1f

        private const val MB_IMAGE_DESTINATION = "image_destination"
        private const val MB_SOURCE_DESTINATION = "source_destination"
        private const val MB_LAYER_DESTINATION = "source_destination"
        private const val MB_SOURCE_COVERAGE_POINTS = "source_coverage_points"
        private const val MB_SOURCE_COVERAGE_QUADS = "source_coverage_quads"
        private const val MB_LAYER_COVERAGE_POINTS = "layer_coverage_points"
        private const val MB_LAYER_COVERAGE_QUADS = "layer_coverage_quads"
        private const val MB_SOURCE_DIRECTIONS = "source_directions"
        private const val MB_LAYER_DIRECTIONS = "layer_directions"
        private const val MB_IMAGE_POI = "image_poi"
        private const val MB_SOURCE_POIS = "source_pois"
        //private const val MB_LAYER_POIS_RADIUS = "layer_pois_radius"
        private const val MB_LAYER_POIS_POINT = "layer_pois_point"
        private const val MB_LAYER_LOWEST = "admin-0-boundary-disputed"

    }

    private lateinit var map: MapboxMap
    private val permissions = Permissions(this)
    private lateinit var routingController: RoutingController
    private lateinit var searchBarComponent: SearchBarComponent
    private lateinit var userStatsComponent: UserStatsComponent
    private lateinit var navigation: MapboxNavigation

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ma)

        searchBarComponent = SearchBarComponent(sb_root).apply {
            onDestinationChosen = {
                if (this@MainActivity::map.isInitialized)
                    map.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(it.point, SEARCH_ZOOM),
                        (CAMERA_ANIMATION_DURATION * 1000).roundToInt()
                    )
                routingController.setDestination(it.point, it.name, true)
            }
            onFocusChanged = { routingController.enabled = !searchBarComponent.focused }
            onVisibilityChanged = {
                updateStatsComponent()
            }
        }

        userStatsComponent = UserStatsComponent(us_root)

        routingController = RoutingController(bg_root, this).apply {
            onDestinationChanged += {
                setSource(MB_SOURCE_DESTINATION, routingController.destination?.point)
            }
            onRouteChanged += {
                if (this@MainActivity::map.isInitialized) {
                    val style = map.style
                    if (style != null)
                        setDirectionsLine(style)
                }
            }
            onLocationEnableIntent = {
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            onPermissionGrantIntent = {
                if (permissions.canAsk)
                    permissions.ask(this@MainActivity::onPermissionsChanged)
                else
                    permissions.openSettings()
            }
            onPickingDestinationChanged += {
                updateSearchBarComponent()
            }
            onRouteCompleted += {
                ma_confetti.apply {
                    build()
                        .addColors(
                            hsv(0f, 1f, 1f),
                            hsv(180f, 1f, 1f),
                            hsv(120f, 1f, 1f),
                            hsv(60f, 1f, 1f)
                        )
                        .setDirection(0.0, 180.0)
                        .setSpeed(0f, 7f)
                        .setFadeOutEnabled(true)
                        .setTimeToLive(5000L)
                        .addShapes(Shape.RECT, Shape.CIRCLE)
                        .addSizes(Size(6))
                        .setPosition(0f, width.toFloat(), -100f)
                        .streamFor(100, 1000)
                }
            }
        }

        navigation = MapboxNavigation(
            this@MainActivity,
            mapboxAccessToken,
            MapboxNavigationOptions.builder().navigationNotification(null).build()
        ).apply {

            addProgressChangeListener { _, routeProgress ->
                if (routeProgress.currentState() == RouteProgressState.ROUTE_ARRIVED)
                    routingController.completeRouting()
                else
                    routingController.updateRoutingInstructions(routeProgress)
            }
            addMilestoneEventListener { routeProgress, _, _ ->
                if (routeProgress.currentState() == RouteProgressState.ROUTE_ARRIVED)
                    routingController.completeRouting()
                else
                    routingController.updateRoutingInstructions(routeProgress)
            }
            addNavigationEventListener { running ->
                if (!running)
                    routingController.cancelRouting()
            }
        }

        // Map
        ma_map.apply {
            onCreate(savedInstanceState)
            getMapAsync { map ->
                this@MainActivity.map = map.apply {
                    val iconColor =
                        ContextCompat.getColor(this@MainActivity, R.color.backgroundDark)
                    val routeColor = ContextCompat.getColor(this@MainActivity, R.color.background)
                    val heatmapMinRadius = width * MIN_HEATMAP_RADIUS_WIDTH_FACTOR
                    val heatmapMaxRadius = width * MAX_HEATMAP_RADIUS_WIDTH_FACTOR
                    setStyle(
                        Style.Builder()
                            .fromUri(MAP_STYLE_URI)
                            .withImage(
                                MB_IMAGE_DESTINATION,
                                resources.getDrawable(R.drawable.place, null), true
                            )
                            .withImage(
                                MB_IMAGE_POI,
                                resources.getDrawable(R.drawable.poi, null), true
                            )
                            .withSource(GeoJsonSource(MB_SOURCE_POIS))
                            .withSource(GeoJsonSource(MB_SOURCE_COVERAGE_POINTS))
                            .withSource(GeoJsonSource(MB_SOURCE_COVERAGE_QUADS))
                            .withSource(GeoJsonSource(MB_SOURCE_DESTINATION))
                            .withSource(GeoJsonSource(MB_SOURCE_DIRECTIONS))
                            .withLayerAbove(
                                FillLayer(MB_LAYER_COVERAGE_QUADS, MB_SOURCE_COVERAGE_QUADS)
                                    .withProperties(
                                        fillColor(
                                            interpolate(
                                                linear(), subtract(literal(1f), get("coverage")),
                                                stop(0.0, color(hsv(210f, 1f, 1f, 0f))),
                                                stop(1.0, color(hsv(190f, 1f, 1f, 1f)))
                                            )
                                        ),
                                        fillOpacity(
                                            interpolate(
                                                linear(), zoom(),
                                                stop(12, 0),
                                                stop(15, 0.5)
                                            )
                                        )
                                    ),
                                MB_LAYER_LOWEST
                            )
                            /*.withLayerAbove(
                                CircleLayer(MB_LAYER_POIS_RADIUS, MB_SOURCE_POIS).withProperties(
                                    fillColor(
                                        rgba(48, 210, 255, 1.0)
                                    ),
                                    circleRadius(
                                        get("radius")
                                    )
                                ),
                                MB_LAYER_COVERAGE_QUADS
                            )*/
                            .withLayerAbove(
                                LineLayer(MB_LAYER_DIRECTIONS, MB_SOURCE_DIRECTIONS).withProperties(
                                    lineCap(Property.LINE_CAP_ROUND),
                                    lineJoin(Property.LINE_JOIN_ROUND),
                                    lineColor(routeColor),
                                    lineWidth(
                                        interpolate(
                                            linear(), zoom(),
                                            stop(MIN_ZOOM, 0.5f.dp),
                                            stop(MAX_ZOOM, 4f.dp)
                                        )
                                    ),
                                    lineOpacity(
                                        interpolate(
                                            linear(), zoom(),
                                            stop(MIN_ZOOM, 0.75f),
                                            stop(MAX_ZOOM, 0.85f)
                                        )
                                    )
                                ),
                                MB_LAYER_COVERAGE_QUADS
                            )
                            .withLayerAbove(
                                HeatmapLayer(MB_LAYER_COVERAGE_POINTS, MB_SOURCE_COVERAGE_POINTS)
                                    .withProperties(
                                        heatmapColor(
                                            interpolate(
                                                linear(), heatmapDensity(),
                                                stop(0.0, color(hsv(230f, 1f, 1f, 0f))),
                                                stop(0.2, color(hsv(220f, 1f, 1f, 0.7f))),
                                                stop(0.4, color(hsv(210f, 1f, 1f, 0.8f))),
                                                stop(0.6, color(hsv(200f, 1f, 1f, 0.9f))),
                                                stop(0.8, color(hsv(190f, 1f, 1f, 1f))),
                                                stop(1.0, color(hsv(180f, 1f, 1f, 1f)))
                                            )
                                        ),
                                        heatmapRadius(
                                            interpolate(
                                                exponential(1.5), zoom(),
                                                stop(MIN_ZOOM, heatmapMinRadius),
                                                stop(15, heatmapMaxRadius)
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
                                    ),
                                //MB_LAYER_POIS_RADIUS
                                MB_LAYER_DIRECTIONS
                            )
                            .withLayer(
                                SymbolLayer(MB_LAYER_POIS_POINT, MB_SOURCE_POIS)
                                    .withProperties(
                                        iconAllowOverlap(false),
                                        textAllowOverlap(false),
                                        iconImage(MB_IMAGE_POI),
                                        iconColor(iconColor),
                                        iconOpacity(
                                            interpolate(
                                                linear(), zoom(),
                                                stop(MIN_ZOOM, 0.65f),
                                                stop(MAX_ZOOM, 0.85f)
                                            )
                                        ),
                                        iconSize(
                                            interpolate(
                                                linear(), zoom(),
                                                stop(MIN_ZOOM, 0.75f),
                                                stop(MAX_ZOOM, 1f)
                                            )
                                        ),
                                        iconOptional(false),
                                        textOptional(true),
                                        textField(get("score")),
                                        textSize(
                                            interpolate(
                                                linear(), zoom(),
                                                stop(MIN_ZOOM, 3f.dp),
                                                stop(MAX_ZOOM, 6f.dp)
                                            )
                                        ),
                                        textAnchor(Property.TEXT_ANCHOR_TOP),
                                        textRadialOffset(0.5f.dp),
                                        textOpacity(
                                            interpolate(
                                                linear(), zoom(),
                                                stop(MIN_ZOOM, 0.5f),
                                                stop(MAX_ZOOM, 0.7f)
                                            )
                                        ),
                                        textColor(iconColor)
                                    )
                            )
                            .withLayer(
                                SymbolLayer(MB_LAYER_DESTINATION, MB_SOURCE_DESTINATION)
                                    .withProperties(
                                        iconAllowOverlap(false),
                                        iconImage(MB_IMAGE_DESTINATION),
                                        iconColor(iconColor),
                                        iconOpacity(
                                            interpolate(
                                                linear(), zoom(),
                                                stop(MIN_ZOOM, 0.85f),
                                                stop(MAX_ZOOM, 0.95f)
                                            )
                                        ),
                                        iconSize(
                                            interpolate(
                                                linear(), zoom(),
                                                stop(MIN_ZOOM, 0.75f),
                                                stop(MAX_ZOOM, 1f)
                                            )
                                        )
                                    )
                            )
                    ) { style ->
                        LocalizationPlugin(ma_map, map, style).apply {
                            matchMapLanguageWithDeviceDefault()
                        }
                        setMaxZoomPreference(MAX_ZOOM)
                        setMinZoomPreference(MIN_ZOOM)
                        setLatLngBoundsForCameraTarget(MAP_BOUNDS)
                        cameraPosition =
                            CameraPosition.Builder().target(MAP_BOUNDS.center).zoom(12.0).build()
                        if (permissions.granted)
                            enableLocationComponent(style)
                        setSource(
                            style,
                            MB_SOURCE_DESTINATION,
                            routingController.destination?.point
                        )
                        setDirectionsLine(style)
                        setSource(
                            style,
                            MB_SOURCE_COVERAGE_POINTS,
                            service?.dataRetriever?.coveragePointData
                        )
                        setSource(
                            style,
                            MB_SOURCE_COVERAGE_QUADS,
                            service?.dataRetriever?.coverageQuadData
                        )
                        setSource(style, MB_SOURCE_POIS, service?.dataRetriever?.poiData)
                        addOnMapClickListener { click ->
                            if (routingController.pickingDestination && MAP_BOUNDS.contains(
                                    click
                                )
                            ) {
                                val screenPoint = map.projection.toScreenLocation(click)
                                val features =
                                    map.queryRenderedFeatures(screenPoint, MB_LAYER_DESTINATION)
                                if (features.isNotEmpty()) {
                                    routingController.removeDestination()
                                    true
                                }
                                else {
                                    val name = service?.getDistrictName(click)
                                    if (name != null) {
                                        routingController.setDestination(click, name, false)
                                        true
                                    }
                                    else false
                                }
                            }
                            else false
                        }
                    }
                }
            }
        }
    }

    private fun setDirectionsLine(style: Style) {
        val directions = routingController.route
        val geometry = if (directions != null) LineString.fromPolyline(
            directions.geometry()!!,
            PRECISION_6
        )
        else null
        if (geometry != null) {
            navigation.startNavigation(directions!!)
            val builder = LatLngBounds.Builder()
            geometry.coordinates().forEach {
                builder.include(it.latlng)
            }
            map.animateCamera(
                CameraUpdateFactory.newLatLngBounds(
                    builder.build(),
                    resources.getDimensionPixelSize(R.dimen.map_bounds_padding)
                ), (CAMERA_ANIMATION_DURATION * 1000).roundToInt()
            )
        }
        else
            navigation.stopNavigation()
        setSource(style, MB_SOURCE_DIRECTIONS, geometry)
    }

    private fun GeoJsonSource.clear() {
        setGeoJson(null as FeatureCollection?)
    }

    private fun setSource(style: Style, sourceId: String, data: FeatureCollection?) {
        val source = style.getSource(sourceId)
        if (source is GeoJsonSource)
            source.setGeoJson(data)
    }

    private fun setSource(sourceId: String, data: FeatureCollection?) {
        if (this@MainActivity::map.isInitialized) {
            val style = map.style
            if (style != null)
                setSource(style, sourceId, data)
        }
    }

    private fun setSource(style: Style, sourceId: String, data: Geometry?) {
        val source = style.getSource(sourceId)
        if (source is GeoJsonSource)
            if (data == null)
                source.clear()
            else
                source.setGeoJson(data)
    }

    private fun setSource(sourceId: String, data: Geometry?) {
        if (this@MainActivity::map.isInitialized) {
            val style = map.style
            if (style != null)
                setSource(style, sourceId, data)
        }
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
        updateSearchBarComponent()
    }

    private fun updateSearchBarComponent() {
        searchBarComponent.enabled = routingController.pickingDestination
    }

    private fun updateStatsComponent() {
        userStatsComponent.enabled =
            !searchBarComponent.visible && service?.userController?.hasStats == true
    }

    private fun onLocationChanged() {
        updateRouting()
        searchBarComponent.location = service?.location?.latLng
    }

    private fun onLocatableChange() {
        updateRouting()
        searchBarComponent.enabled = service?.locatable == true
    }

    private fun onOnlineChange() {
        updateRouting()
    }

    private var lastNotifiedLevel = 0

    private fun onLevelUp(level: Int) {
        if (level > lastNotifiedLevel) {
            lastNotifiedLevel = level
            userStatsComponent.levelUpParty(ma_confetti)
            service?.userController?.notifyLevel(level)
        }
    }

    private fun onStatsChange() {
        val stats = service?.userController?.statsOrNull
        if (stats != null)
            userStatsComponent.stats = stats
        updateStatsComponent()
    }

    private fun onCoveragePointDataChange() {
        setSource(MB_SOURCE_COVERAGE_POINTS, service?.dataRetriever?.coveragePointData)
    }

    private fun onCoverageQuadDataChange() {
        setSource(MB_SOURCE_COVERAGE_QUADS, service?.dataRetriever?.coverageQuadData)
    }

    private fun onPoiDataChange() {
        setSource(MB_SOURCE_POIS, service?.dataRetriever?.poiData)
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
                    old.userController.onStatsChange -= this::onStatsChange
                    old.dataRetriever.onCoveragePointDataChange -= this::onCoveragePointDataChange
                    old.dataRetriever.onCoverageQuadDataChange -= this::onCoverageQuadDataChange
                    old.dataRetriever.onPoiDataChange -= this::onPoiDataChange
                    old.userController.onLevelUp -= this::onLevelUp
                }
                if (value != null) {
                    value.onLocationChange += this::onLocationChanged
                    value.onLocatableChange += this::onLocatableChange
                    value.onOnlineChange += this::onOnlineChange
                    value.userController.onStatsChange += this::onStatsChange
                    value.dataRetriever.onCoveragePointDataChange += this::onCoveragePointDataChange
                    value.dataRetriever.onCoverageQuadDataChange += this::onCoverageQuadDataChange
                    value.dataRetriever.onPoiDataChange += this::onPoiDataChange
                    value.userController.onLevelUp += this::onLevelUp
                    value.userController.notifyLevel(lastNotifiedLevel)
                    onLocationChanged()
                    onLocatableChange()
                    onOnlineChange()
                    onStatsChange()
                    onCoveragePointDataChange()
                    onCoverageQuadDataChange()
                    onPoiDataChange()
                }
                routingController.service = value
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
                isLocationComponentEnabled = true
                cameraMode = CameraMode.NONE
                renderMode = RenderMode.COMPASS
            }
        }
    }

    override fun onBackPressed() {
        if (searchBarComponent.focused)
            searchBarComponent.clearFocus()
        else if (routingController.onBack())
            return
        else super.onBackPressed()
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
        navigation.onDestroy()
        ma_map.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        ma_map.onSaveInstanceState(outState)
    }

}
