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

private val INTERPRETER = PollInterpreter.from(object : SimpleInterpreter<UserKey, FeatureCollection>() {
    override fun interpretRequest(request: UserKey): JSONObject? =
        JSONObject().apply {
            put("id", request.id)
            put("passkey", request.passkey)
            put("mode", "points")
        }

    override fun interpretResponse(
        request: UserKey,
        statusCode: Int,
        response: JSONObject
    ) =
        try {
            FeatureCollection.fromJson(response.getJSONObject("pois").toString())
        } catch (_: Exception) {
            throw Interpreter.UninterpretableResponseException()
        }
})

typealias PoiService = Server.AutoPollService<UserKey, FeatureCollection, FeatureCollection>

fun makePoiService(server: Server, userKey: UserKey): PoiService =
    server.AutoPollService(
        SERVICE_ADDRESS,
        userKey,
        INTERPRETER
    ).apply {
        customRetryPolicy = DEFAULT_RETRY_POLICY
    }
