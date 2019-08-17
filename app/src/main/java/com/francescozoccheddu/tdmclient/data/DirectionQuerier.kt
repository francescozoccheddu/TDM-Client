package com.francescozoccheddu.tdmclient.data

import com.francescozoccheddu.tdmclient.utils.data.languageLocale
import com.francescozoccheddu.tdmclient.utils.data.mapboxAccessToken
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.MapboxDirections
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

private const val SPOT_SNAP_RADIUS = 500.0
private const val START_SNAP_RADIUS = 50.0
private const val END_SNAP_RADIUS = 50.0

fun getDirections(path: List<Point>, hasDestination: Boolean, callback: (DirectionsRoute?) -> Unit) {
    if (path.size < 1)
        throw IllegalArgumentException("Path size must be at least 2")

    MapboxDirections.builder().apply {
        accessToken(mapboxAccessToken)

        origin(path.first())
        destination(path.last())
        alternatives(false)
        overview(DirectionsCriteria.OVERVIEW_FULL)
        profile(DirectionsCriteria.PROFILE_WALKING)
        steps(false)
        language(languageLocale)
        radiuses(*DoubleArray(path.size) {
            if (it == 0)
                START_SNAP_RADIUS
            else if (hasDestination && it == path.lastIndex)
                END_SNAP_RADIUS
            else
                SPOT_SNAP_RADIUS
        })

        for (i in 1 until path.lastIndex)
            addWaypoint(path[i])

        build().enqueueCall(object : Callback<DirectionsResponse> {
            override fun onResponse(call: Call<DirectionsResponse>, response: Response<DirectionsResponse>) {
                val routes = response.body()?.routes()
                if (routes != null && routes.size > 0)
                    callback(routes[0])
                else {
                    println("DIRECTIONS ERROR:\n${response.body()}")
                    callback(null)
                }
            }

            override fun onFailure(call: Call<DirectionsResponse>, throwable: Throwable) {
                println("DIRECTIONS ERROR:\n$throwable")
                callback(null)
            }
        })
    }

}