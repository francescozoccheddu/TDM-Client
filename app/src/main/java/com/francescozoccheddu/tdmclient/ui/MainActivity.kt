package com.francescozoccheddu.tdmclient.ui

import android.os.Bundle
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.francescozoccheddu.tdmclient.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style


class MainActivity : AppCompatActivity() {

    companion object {

        val MAP_BOUNDS = LatLngBounds.Builder()
            .include(LatLng(39.267498, 9.181226))
            .include(LatLng(39.176358, 9.054797))
            .build()

        const val MAP_STYLE_URI = "mapbox://styles/francescozz/cjx1wlf2l080f1cqmmhh4jbgi"

    }

    private lateinit var mapContainer: FrameLayout
    private lateinit var mapView: MapView
    private lateinit var map: MapboxMap

    private lateinit var fab: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity)

        // Map
        mapContainer = findViewById(R.id.map_container)
        mapView = findViewById(R.id.map)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync { map ->
            this.map = map
            map.setStyle(Style.Builder().fromUri(MAP_STYLE_URI)) { style ->
                map.setLatLngBoundsForCameraTarget(MAP_BOUNDS)
            }
        }

        // Fab
        fab = findViewById(R.id.fab)
        fab.setOnClickListener {
            fab.isExpanded = !fab.isExpanded

        }

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
