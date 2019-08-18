package com.francescozoccheddu.tdmclient.data

import com.francescozoccheddu.tdmclient.utils.data.client.Interpreter
import com.francescozoccheddu.tdmclient.utils.data.client.PollInterpreter
import com.francescozoccheddu.tdmclient.utils.data.client.RetryPolicy
import com.francescozoccheddu.tdmclient.utils.data.client.Server
import com.francescozoccheddu.tdmclient.utils.data.client.SimpleInterpreter
import com.mapbox.geojson.FeatureCollection
import org.json.JSONObject

private const val SERVICE_ADDRESS = "getpois"
private val DEFAULT_RETRY_POLICY = RetryPolicy(2f)

private val INTERPRETER = PollInterpreter.from(object : SimpleInterpreter<User, FeatureCollection>() {
    override fun interpretRequest(request: User): JSONObject? =
        JSONObject().apply {
            put("id", request.id)
            put("passkey", request.passkey)
            put("mode", "points")
        }

    override fun interpretResponse(request: User, response: JSONObject): FeatureCollection =
        try {
            FeatureCollection.fromJson(response.getJSONObject("pois").toString())
        } catch (_: Exception) {
            throw Interpreter.UninterpretableResponseException()
        }
})

typealias PoiService = Server.AutoPollService<User, FeatureCollection, FeatureCollection>

fun makePoiService(server: Server, user: User): PoiService =
    server.AutoPollService(
        SERVICE_ADDRESS,
        user,
        INTERPRETER
    ).apply {
        customRetryPolicy = DEFAULT_RETRY_POLICY
    }
