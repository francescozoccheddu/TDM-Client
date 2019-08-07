package com.francescozoccheddu.tdmclient.data

import android.location.Location
import com.francescozoccheddu.tdmclient.utils.data.client.Interpreter
import com.francescozoccheddu.tdmclient.utils.data.client.RetryPolicy
import com.francescozoccheddu.tdmclient.utils.data.client.Server
import com.francescozoccheddu.tdmclient.utils.data.json
import com.mapbox.geojson.Point
import org.json.JSONObject

private const val SERVICE_ADDRESS = "getroute"
private val DEFAULT_RETRY_POLICY = RetryPolicy(6f)

data class RouteRequest(val from: Location, val to: Location?, val time: Float)

private val INTERPRETER = object : Interpreter<RouteRequest, Collection<Point>> {

    override fun interpretRequest(request: RouteRequest) =
        JSONObject().apply {
            put("from", request.from.json)
            if (request.to != null)
                put("to", request.from.json)
            put("time", request.time)
        }

    override fun interpretResponse(request: RouteRequest, response: JSONObject): Collection<Point> {
        try {
            val jsonRoute = response.getJSONArray("routes").getJSONArray(0)
            return List(jsonRoute.length(), { i ->
                val point = jsonRoute.getJSONObject(i).getJSONArray("point")
                Point.fromLngLat(point.getDouble(0), point.getDouble(1))
            })
        } catch (_: Exception) {
            throw Interpreter.UninterpretableResponseException()
        }

    }

}

typealias RouteRetriever = Server.Service<RouteRequest, Collection<Point>>

fun makeRouteRetriever(server: Server) =
    server.Service(
        SERVICE_ADDRESS,
        INTERPRETER
    ).apply {
        customRetryPolicy = DEFAULT_RETRY_POLICY
    }
