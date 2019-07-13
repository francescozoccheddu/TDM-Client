package com.francescozoccheddu.tdmclient

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.francescozoccheddu.tdmclient.data.client.Server
import com.francescozoccheddu.tdmclient.data.retrieve.CoverageRetriever
import com.francescozoccheddu.tdmclient.data.retrieve.makeCoverageRetriever
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.expressions.Expression.heatmapDensity
import com.mapbox.mapboxsdk.style.expressions.Expression.interpolate
import com.mapbox.mapboxsdk.style.expressions.Expression.linear
import com.mapbox.mapboxsdk.style.expressions.Expression.literal
import com.mapbox.mapboxsdk.style.expressions.Expression.rgba
import com.mapbox.mapboxsdk.style.expressions.Expression.stop
import com.mapbox.mapboxsdk.style.expressions.Expression.zoom
import com.mapbox.mapboxsdk.style.layers.HeatmapLayer
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.heatmapColor
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.heatmapIntensity
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.heatmapOpacity
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.heatmapRadius


class MainActivity : AppCompatActivity() {

    companion object {

        val MAP_BOUNDS = LatLngBounds.Builder()
            .include(LatLng(39.267498, 9.181226))
            .include(LatLng(39.176358, 9.054797))
            .build()

        const val MAP_STYLE_URL = "mapbox://styles/francescozz/cjx1wlf2l080f1cqmmhh4jbgi"
        const val COVERAGE_SOURCE_ID = "coverage"
        const val COVERAGE_LAYER_ID = "coverage"
    }

    private lateinit var mapView: MapView
    private lateinit var map: MapboxMap

    private lateinit var server: Server
    private lateinit var coverageService: CoverageRetriever

    private lateinit var sheet: BottomSheetBehavior<LinearLayout>
    private lateinit var fab: ExtendedFloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity)

        mapView = findViewById(R.id.map)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync { map ->
            this.map = map
            map.setStyle(Style.Builder().fromUrl(MAP_STYLE_URL)) { style ->
                map.setLatLngBoundsForCameraTarget(MAP_BOUNDS)
                map.setMinZoomPreference(10.0)
                map.setMaxZoomPreference(18.0)
                val uiSettings = map.uiSettings
                uiSettings.isLogoEnabled = false
                uiSettings.isAttributionEnabled = false
                uiSettings.isCompassEnabled = false
                uiSettings.isRotateGesturesEnabled = false
                addHeatmap(style)
            }
        }

        server = Server(this, "http://localhost:8080")
        coverageService = makeCoverageRetriever(server)
        coverageService.onRequestStatusChanged += {
            if (!it.status.pending) {
                Toast.makeText(this@MainActivity, it.status.toString(), Toast.LENGTH_SHORT).show()
            }
        }
        coverageService.onData += {
            val message = "${coverageService.time}\n${it.toString().substring(0, 20)}..."
            Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
        }

        sheet = BottomSheetBehavior.from(findViewById(R.id.sheet))
        fab = findViewById(R.id.fab)


        val shrinkCallback = Runnable { fab.shrink() }
        fab.setOnClickListener {
            fab.shrink()
            fab.removeCallbacks(shrinkCallback)
            fab.hide()
            sheet.state = BottomSheetBehavior.STATE_EXPANDED
        }

        sheet.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {

            override fun onStateChanged(sheet: View, state: Int) {
                if (state == BottomSheetBehavior.STATE_HIDDEN) {
                    fab.extend(false)
                    fab.show()
                    fab.postDelayed(shrinkCallback, 2000)
                }
            }

            override fun onSlide(sheet: View, offset: Float) {}

        })

        sheet.state = BottomSheetBehavior.STATE_HIDDEN
        fab.postDelayed(shrinkCallback, 2000)
    }


    fun addHeatmap(style: Style) {
        val layer = HeatmapLayer(COVERAGE_LAYER_ID, COVERAGE_SOURCE_ID)
        layer.maxZoom = 15f
        layer.setProperties(
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
            )
/*            heatmapWeight(
                get("coverage")
            )*/
        )
        style.addLayerBelow(layer, "aerialway")
    }

    public override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    public override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    public override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    public override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

}
