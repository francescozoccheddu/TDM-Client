package com.francescozoccheddu.tdmclient.data

import android.location.Location
import com.francescozoccheddu.tdmclient.utils.data.client.Interpreter
import com.francescozoccheddu.tdmclient.utils.data.client.RetryPolicy
import com.francescozoccheddu.tdmclient.utils.data.client.Server
import com.francescozoccheddu.tdmclient.utils.data.client.SimpleInterpreter
import com.francescozoccheddu.tdmclient.utils.data.json
import com.francescozoccheddu.tdmclient.utils.data.mapboxAccessToken
import com.francescozoccheddu.tdmclient.utils.data.point
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.MapboxDirections
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*


private const val SERVICE_ADDRESS = "getroute"
private val DEFAULT_RETRY_POLICY = RetryPolicy(6f)

data class RouteRequest(val from: Location, val to: Location?, val time: Float)

private val INTERPRETER = object : SimpleInterpreter<RouteRequest, List<Point>>() {

    override fun interpretRequest(request: RouteRequest) =
        JSONObject().apply {
            put("from", request.from.json)
            if (request.to != null)
                put("to", request.from.json)
            put("time", request.time)
        }

    override fun interpretResponse(request: RouteRequest, response: JSONObject): List<Point> {
        try {
            val jsonRoute = response.getJSONArray("routes").getJSONArray(0)
            return List(
                jsonRoute.length() + if (request.to == null) 1 else 0,
                { i ->
                    if (i == jsonRoute.length())
                        request.from.point
                    else {
                        val point = jsonRoute.getJSONObject(i).getJSONArray("point")
                        Point.fromLngLat(point.getDouble(0), point.getDouble(1))
                    }
                })
        } catch (_: Exception) {
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

private const val SPOT_SNAP_RADIUS = 200
private const val ENDPOINT_SNAP_RADIUS = 50

fun getDirections(origin: Point, path: List<Point>, callback: (DirectionsRoute?) -> Unit) {
    if (path.isEmpty())
        throw IllegalArgumentException("Empty path")

    MapboxDirections.builder().apply {
        accessToken(mapboxAccessToken)
        origin(origin)
        destination(path.last())
        overview(DirectionsCriteria.OVERVIEW_FULL)
        profile(DirectionsCriteria.PROFILE_WALKING)
        language(Locale.getDefault())
        radiuses(*DoubleArray(path.size + 1) {
            if (it == 0 || it == path.size)
                ENDPOINT_SNAP_RADIUS.toDouble()
            else
                SPOT_SNAP_RADIUS.toDouble()
        })

        for (i in 0 until path.lastIndex)
            addWaypoint(path[i])

        build().enqueueCall(object : Callback<DirectionsResponse> {
            override fun onResponse(call: Call<DirectionsResponse>, response: Response<DirectionsResponse>) {
                val routes = response.body()?.routes()
                if (routes != null && routes.size > 0)
                    callback(routes[0])
                else
                    callback(null)
            }

            override fun onFailure(call: Call<DirectionsResponse>, throwable: Throwable) {
                callback(null)
            }
        })
    }
}