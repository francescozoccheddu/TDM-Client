package com.francescozoccheddu.tdmclient.data

import com.francescozoccheddu.tdmclient.utils.data.client.Interpreter
import com.francescozoccheddu.tdmclient.utils.data.client.RetryPolicy
import com.francescozoccheddu.tdmclient.utils.data.client.Server
import com.francescozoccheddu.tdmclient.utils.data.client.SimpleInterpreter
import com.francescozoccheddu.tdmclient.utils.data.json
import com.francescozoccheddu.tdmclient.utils.data.mapboxAccessToken
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.MapboxDirections
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.geometry.LatLng
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*


private const val SERVICE_ADDRESS = "getroute"
private val DEFAULT_RETRY_POLICY = RetryPolicy(6f)

data class RouteRequest(val from: LatLng, val to: LatLng?, val time: Float)

private val INTERPRETER = object : SimpleInterpreter<RouteRequest, List<Point>>() {

    override fun interpretRequest(request: RouteRequest) =
        JSONObject().apply {
            put("from", request.from.json)
            if (request.to != null)
                put("to", request.to.json)
            put("time", request.time)
        }

    override fun interpretResponse(request: RouteRequest, response: JSONObject): List<Point> {
        try {
            val jsonRoute = response.getJSONArray("routes").getJSONArray(0)
            if (jsonRoute.length() < 2)
                throw RuntimeException()
            return List(jsonRoute.length(), {
                val point = jsonRoute.getJSONObject(it).getJSONArray("point")
                Point.fromLngLat(point.getDouble(0), point.getDouble(1))
            })
        } catch (_: Exception) {
            println("ROUTE ERROR:\n${response.toString(4)}")
            throw Interpreter.UninterpretableResponseException()
        }
    }

}

typealias RouteRetriever = Server.Service<RouteRequest, List<Point>>

fun makeRouteRetriever(server: Server) =
    server.Service(
        SERVICE_ADDRESS,
        INTERPRETER
    ).apply {
        customRetryPolicy = DEFAULT_RETRY_POLICY
    }

private const val SPOT_SNAP_RADIUS = 200.0
private const val START_SNAP_RADIUS = 50.0
private const val END_SNAP_RADIUS = 50.0

fun getDirections(path: List<Point>, hasDestination: Boolean, callback: (DirectionsRoute?) -> Unit) {
    if (path.size < 1)
        throw IllegalArgumentException("Path size must be at least 2")

    MapboxDirections.builder().apply {
        accessToken(mapboxAccessToken)
        origin(path.first())
        destination(path.last())
        overview(DirectionsCriteria.OVERVIEW_FULL)
        profile(DirectionsCriteria.PROFILE_WALKING)
        language(Locale.getDefault())
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