package com.francescozoccheddu.tdmclient.data.operation

import com.francescozoccheddu.tdmclient.data.client.Interpreter
import com.francescozoccheddu.tdmclient.data.client.PollInterpreter
import com.francescozoccheddu.tdmclient.data.client.RetryPolicy
import com.francescozoccheddu.tdmclient.data.client.Server
import com.francescozoccheddu.tdmclient.utils.dateParseISO
import org.json.JSONObject
import java.util.*

private const val SERVICE_ADDRESS = "getcoverage"
private val DEFAULT_RETRY_POLICY = RetryPolicy(3f)

enum class CoverageRetrieveMode {
    POINTS, QUADS
}

data class CoverageData(val data: JSONObject, val time: Date)

private val INTERPRETER = object : PollInterpreter<CoverageRetrieveMode, CoverageData, JSONObject> {

    override fun interpretData(response: CoverageData): JSONObject {
        return response.data
    }

    override fun interpretRequest(request: CoverageRetrieveMode): JSONObject? {
        val root = JSONObject()
        root.put(
            "mode", when (request) {
                CoverageRetrieveMode.POINTS -> "points"
                CoverageRetrieveMode.QUADS -> "quads"
            }
        )
        return root
    }

    override fun interpretResponse(request: CoverageRetrieveMode, response: JSONObject): CoverageData {
        try {
            val data = response["data"] as JSONObject
            val isotime = response["time"] as String
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
    server.AutoPollService(SERVICE_ADDRESS, CoverageRetrieveMode.POINTS, INTERPRETER).apply {
        customRetryPolicy = DEFAULT_RETRY_POLICY
    }
