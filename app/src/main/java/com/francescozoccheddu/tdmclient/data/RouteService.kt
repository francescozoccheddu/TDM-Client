package com.francescozoccheddu.tdmclient.data

import com.francescozoccheddu.tdmclient.utils.data.client.Interpreter
import com.francescozoccheddu.tdmclient.utils.data.client.RetryPolicy
import com.francescozoccheddu.tdmclient.utils.data.client.Server
import com.francescozoccheddu.tdmclient.utils.data.client.SimpleInterpreter
import com.francescozoccheddu.tdmclient.utils.data.json
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.geometry.LatLng
import org.json.JSONObject


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

typealias RouteService = Server.Service<RouteRequest, List<Point>>

fun makeRouteService(server: Server) : RouteService =
    server.Service(
        SERVICE_ADDRESS,
        INTERPRETER
    ).apply {
        customRetryPolicy = DEFAULT_RETRY_POLICY
    }


