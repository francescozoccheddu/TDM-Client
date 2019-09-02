package com.francescozoccheddu.tdmclient.data

import com.francescozoccheddu.tdmclient.utils.data.client.Interpreter
import com.francescozoccheddu.tdmclient.utils.data.client.PollInterpreter
import com.francescozoccheddu.tdmclient.utils.data.client.RetryPolicy
import com.francescozoccheddu.tdmclient.utils.data.client.Server
import com.francescozoccheddu.tdmclient.utils.data.client.SimpleInterpreter
import com.francescozoccheddu.tdmclient.utils.data.parseJsonObjectList
import org.json.JSONObject

private const val SERVICE_ADDRESS = "getleaderboard"
private val DEFAULT_RETRY_POLICY = RetryPolicy(3f)

private val INTERPRETER = PollInterpreter.from(object : SimpleInterpreter<Int, List<User>>() {

    override fun interpretRequest(request: Int) = JSONObject().apply { put("size", request) }

    override fun interpretResponse(
        request: Int,
        statusCode: Int,
        response: JSONObject
    ) = try {
        parseJsonObjectList(response.getJSONArray("leaderboard"))
        { User(it.getInt("id"), parseUserStats(it.getJSONObject("user"))) }
    } catch (_: Exception) {
        throw Interpreter.UninterpretableResponseException()
    }

})

typealias LeaderboardService = Server.PollService<Int, List<User>, List<User>>

fun makeLeaderboardService(server: Server, size: Int = 10): LeaderboardService =
    server.PollService(
        SERVICE_ADDRESS,
        size,
        INTERPRETER
    ).apply {
        customRetryPolicy = DEFAULT_RETRY_POLICY
    }
