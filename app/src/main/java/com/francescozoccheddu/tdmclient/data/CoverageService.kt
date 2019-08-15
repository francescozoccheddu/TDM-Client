package com.francescozoccheddu.tdmclient.data

import com.francescozoccheddu.tdmclient.utils.commons.dateParseISO
import com.francescozoccheddu.tdmclient.utils.data.client.Interpreter
import com.francescozoccheddu.tdmclient.utils.data.client.RetryPolicy
import com.francescozoccheddu.tdmclient.utils.data.client.Server
import com.francescozoccheddu.tdmclient.utils.data.client.SimplePollInterpreter
import org.json.JSONObject
import java.util.*

private const val SERVICE_ADDRESS = "getcoverage"
private val DEFAULT_RETRY_POLICY = RetryPolicy(3f)

enum class CoverageRetrieveMode {
    POINTS, QUADS
}

data class CoverageData(val data: JSONObject, val time: Date)

private val INTERPRETER = object : SimplePollInterpreter<CoverageRetrieveMode, CoverageData, JSONObject>() {

    override fun interpretData(response: CoverageData): JSONObject {
        return response.data
    }

    override fun interpretRequest(request: CoverageRetrieveMode) = JSONObject().apply {
        put(
            "constraintSetId", when (request) {
                CoverageRetrieveMode.POINTS -> "points"
                CoverageRetrieveMode.QUADS -> "quads"
            }
        )
    }

    override fun interpretResponse(request: CoverageRetrieveMode, response: JSONObject): CoverageData {
        try {
            val data = response.getJSONObject("data")
            val isotime = response.getString("time")
            return CoverageData(data, dateParseISO(isotime))
        } catch (_: Exception) {
            throw Interpreter.UninterpretableResponseException()
        }
    }

    override fun interpretTime(request: Server.Service<CoverageRetrieveMode, CoverageData>.Request): Date {
        return request.response.time
    }

}

typealias CoverageRetriever = Server.AutoPollService<CoverageRetrieveMode, CoverageData, JSONObject>

fun makeCoverageRetriever(server: Server) =
    server.AutoPollService(
        SERVICE_ADDRESS,
        CoverageRetrieveMode.POINTS,
        INTERPRETER
    ).apply {
        customRetryPolicy = DEFAULT_RETRY_POLICY
    }
