package com.francescozoccheddu.tdmclient.ui

import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.francescozoccheddu.tdmclient.R
import com.francescozoccheddu.tdmclient.utils.boundingBox
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mapbox.mapboxsdk.annotations.Marker
import com.mapbox.mapboxsdk.annotations.MarkerOptions
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

    private lateinit var mapView: MapView
    private lateinit var map: MapboxMap

    private lateinit var searchListView: RecyclerView
    private lateinit var searchProvider: LocationSearchProvider


    private fun setFabIcon(icon: Int) {
        fab.setImageDrawable(ContextCompat.getDrawable(this@MainActivity, icon))
    }

    private fun setFabColor(color: Int) {
        fab.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this@MainActivity, color))
    }

    private lateinit var fab: FloatingActionButton

    private lateinit var etSearch: EditText


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity)

        // Map
        mapView = findViewById(R.id.map)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync { map ->
            this.map = map
            map.setStyle(Style.Builder().fromUri(MAP_STYLE_URI)) { style ->
                map.setLatLngBoundsForCameraTarget(MAP_BOUNDS)
            }
            map.addOnMapClickListener {
                if (locationChooseMode)
                    setChoosenLocation(it, null)
                locationChooseMode
            }
        }

        // Fab
        val walktypeSheet = findViewById<View>(R.id.l_walktype)
        val durationSheet = findViewById<View>(R.id.l_duration)

        fab = findViewById(R.id.fab)
        fab.setOnClickListener {
            walktypeSheet.visibility = View.VISIBLE
            durationSheet.visibility = View.GONE
            fab.isExpanded = true
        }

        findViewById<MaterialButton>(R.id.bt_duration_ok).setOnClickListener {
            fab.isExpanded = false
        }

        findViewById<View>(R.id.li_walktype_destination).setOnClickListener {
            fab.isExpanded = false
        }

        findViewById<View>(R.id.li_walktype_nearby).setOnClickListener {
            walktypeSheet.visibility = View.GONE
            durationSheet.visibility = View.VISIBLE
        }

        // Search view
        searchProvider = LocationSearchProvider(MAP_BOUNDS.boundingBox)
        searchProvider.onLocationClick += {
            if (locationChooseMode)
                setChoosenLocation(it.point, it.name)
        }

        searchListView = findViewById(R.id.rv_search)
        searchListView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity, RecyclerView.VERTICAL, false)
            adapter = searchProvider.adapter
        }

        etSearch = findViewById(R.id.et_search)
        etSearch.addTextChangedListener(object : TextWatcher {

            override fun afterTextChanged(s: Editable) {
                searchProvider.query(s.toString())
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

        })

    }

    private fun setChoosenLocation(point: LatLng, name: String?) {
        marker?.remove()
        marker = map.addMarker(MarkerOptions().setPosition(point))
        // TODO Open bottom sheet
        if (name != null) {
            // TODO Set name in bottom sheet
        }
        else {
            // TODO Set loading in bottom sheet
            // TODO Start name query
            // TODO [When query returns] Set name in bottom sheet
        }
    }

    private fun removeChoosenLocation() {
        marker?.remove()
        // TODO Close bottom sheet
    }

    private var marker: Marker? = null

    private var locationChooseMode = true

    override fun onBackPressed() {
        if (fab.isExpanded)
            fab.isExpanded = false
        else
            super.onBackPressed()
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
