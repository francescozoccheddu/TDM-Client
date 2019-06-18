package com.francescozoccheddu.tdmclient

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.Style

val cagliariBounds = LatLngBounds.Builder()
    .include(LatLng(39.267498, 9.181226))
    .include(LatLng(39.176358, 9.054797))
    .build()

const val mapStyleUrl = "mapbox://styles/francescozz/cjx1wlf2l080f1cqmmhh4jbgi"

class MainActivity : AppCompatActivity() {

    private lateinit var mapView: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync { mapboxMap ->
            mapboxMap.setStyle(Style.Builder().fromUrl(mapStyleUrl))
            {
                mapboxMap.setLatLngBoundsForCameraTarget(cagliariBounds)
                mapboxMap.setMinZoomPreference(10.0)
                mapboxMap.setMaxZoomPreference(18.0)
                val uiSettings = mapboxMap.uiSettings
                uiSettings.isLogoEnabled = false
                uiSettings.isAttributionEnabled = false
                uiSettings.isCompassEnabled = false
                uiSettings.isRotateGesturesEnabled = false
            }
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
