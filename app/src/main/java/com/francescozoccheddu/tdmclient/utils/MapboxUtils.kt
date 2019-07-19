package com.francescozoccheddu.tdmclient.utils

import com.mapbox.geojson.BoundingBox
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds

val Point.latlng get() = LatLng(latitude(), longitude())

val BoundingBox.latlngBounds get() = LatLngBounds.Builder().include(southwest().latlng).include(northeast().latlng).build()

val LatLngBounds.boundingBox get() = BoundingBox.fromLngLats(lonWest, latSouth, lonEast, latNorth)

val LatLng.point get() = Point.fromLngLat(longitude, latitude)

val mapboxAccessToken get() = Mapbox.getAccessToken() ?: throw IllegalStateException("No access token registered")