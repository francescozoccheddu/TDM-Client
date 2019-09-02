package com.francescozoccheddu.tdmclient.data

import com.francescozoccheddu.tdmclient.utils.commons.dateParseISO
import com.francescozoccheddu.tdmclient.utils.data.client.Interpreter
import com.francescozoccheddu.tdmclient.utils.data.client.RetryPolicy
import com.francescozoccheddu.tdmclient.utils.data.client.Server
import com.francescozoccheddu.tdmclient.utils.data.client.SimplePollInterpreter
import com.mapbox.geojson.FeatureCollection
import org.json.JSONObject
import java.util.*

private const val SERVICE_ADDRESS = "getcoverage"
private val DEFAULT_RETRY_POLICY = RetryPolicy(3f)

enum class CoverageRetrieveMode {
    POINTS, QUADS
}

data class CoverageData(val data: FeatureCollection, val time: Date)

private val INTERPRETER = object : SimplePollInterpreter<CoverageRetrieveMode, CoverageData, FeatureCollection>() {

    override fun interpretData(response: CoverageData) = response.data

    override fun interpretRequest(request: CoverageRetrieveMode) = JSONObject().apply {
        put(
            "mode", when (request) {
                CoverageRetrieveMode.POINTS -> "points"
                CoverageRetrieveMode.QUADS -> "quads"
            }
        )
    }

    override fun interpretResponse(
        request: CoverageRetrieveMode,
        statusCode: Int,
        response: JSONObject
    ): CoverageData {
        try {
            val data = FeatureCollection.fromJson(response.getJSONObject("data").toString())
            val isotime = response.getString("time")
            return CoverageData(data, dateParseISO(isotime))
        } catch (_: Exception) {
            throw Interpreter.UninterpretableResponseException()
        }
    }

    override fun interpretTime(request: Server.Service<CoverageRetrieveMode, CoverageData>.Request) =
        request.response.time

}

typealias CoverageService = Server.AutoPollService<CoverageRetrieveMode, CoverageData, FeatureCollection>

fun makeCoverageService(server: Server) : CoverageService =
    server.AutoPollService(
        SERVICE_ADDRESS,
        CoverageRetrieveMode.POINTS,
        INTERPRETER
    ).apply {
        customRetryPolicy = DEFAULT_RETRY_POLICY
    }
