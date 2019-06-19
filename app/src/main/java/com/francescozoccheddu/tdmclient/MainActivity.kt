package com.francescozoccheddu.tdmclient

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.francescozoccheddu.tdmclient.data.HttpClient
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.Style
import org.json.JSONObject


class MainActivity : AppCompatActivity()
{

    companion object
    {

        val cagliariBounds = LatLngBounds.Builder()
            .include(LatLng(39.267498, 9.181226))
            .include(LatLng(39.176358, 9.054797))
            .build()

        const val mapStyleUrl = "mapbox://styles/francescozz/cjx1wlf2l080f1cqmmhh4jbgi"
    }

    private lateinit var mapView: MapView

    override fun onCreate(savedInstanceState: Bundle?)
    {
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

        val client = HttpClient(this)
        val json = """{"id":0}"""
        client.request("getcoverage", JSONObject(json), object : HttpClient.Callback
        {

            override fun onException(exception: Exception)
            {
                Toast.makeText(this@MainActivity, exception.toString(), Toast.LENGTH_LONG).show()
            }

            override fun onResponse(response: JSONObject)
            {
                Toast.makeText(this@MainActivity, response.toString(), Toast.LENGTH_LONG).show()
            }

        })

    }

    public override fun onStart()
    {
        super.onStart()
        mapView.onStart()
    }

    public override fun onResume()
    {
        super.onResume()
        mapView.onResume()
    }

    public override fun onPause()
    {
        super.onPause()
        mapView.onPause()
    }

    public override fun onStop()
    {
        super.onStop()
        mapView.onStop()
    }

    override fun onLowMemory()
    {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy()
    {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle)
    {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

}
