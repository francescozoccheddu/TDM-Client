package com.francescozoccheddu.tdmclient.utils.data

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import com.mapbox.geojson.BoundingBox
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import org.json.JSONArray


val Point.latlng get() = LatLng(latitude(), longitude())

val BoundingBox.latlngBounds get() = LatLngBounds.Builder().include(southwest().latlng).include(northeast().latlng).build()

val LatLngBounds.boundingBox get() = BoundingBox.fromLngLats(lonWest, latSouth, lonEast, latNorth)

val LatLng.point get() = Point.fromLngLat(longitude, latitude)

val Location.latLng get() = LatLng(this)

val Location.json get() = JSONArray(arrayOf(longitude, latitude))

val mapboxAccessToken get() = Mapbox.getAccessToken() ?: throw IllegalStateException("No access token registered")

fun isLocationEnabled(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        lm.isLocationEnabled
    }
    else {
        val mode = Settings.Secure.getInt(
            context.contentResolver, Settings.Secure.LOCATION_MODE,
            Settings.Secure.LOCATION_MODE_OFF
        )
        mode != Settings.Secure.LOCATION_MODE_OFF
    }
}