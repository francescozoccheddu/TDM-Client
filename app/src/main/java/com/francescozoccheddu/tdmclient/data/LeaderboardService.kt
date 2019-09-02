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

private val INTERPRETER = PollInterpreter.from(object : SimpleInterpreter<Unit, List<User>>() {

    override fun interpretRequest(request: Unit) = JSONObject()

    override fun interpretResponse(
        request: Unit,
        statusCode: Int,
        response: JSONObject
    ) = try {
        parseJsonObjectList(response.getJSONArray("leaderboard"))
        { User(it.getInt("id"), parseUserStats(it.getJSONObject("user"))) }
    } catch (_: Exception) {
        throw Interpreter.UninterpretableResponseException()
    }

})

typealias LeaderboardService = Server.PollService<Unit, List<User>, List<User>>

fun makeLeaderboardService(server: Server): LeaderboardService =
    server.PollService(
        SERVICE_ADDRESS,
        Unit,
        INTERPRETER
    ).apply {
        customRetryPolicy = DEFAULT_RETRY_POLICY
    }
