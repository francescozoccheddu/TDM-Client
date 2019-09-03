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

data class LeaderboardPosition(
    val id: Int,
    val name: String,
    val title: String,
    val avatarUrl: String,
    val score: Int,
    val level: Int
)

private val INTERPRETER =
    PollInterpreter.from(object : SimpleInterpreter<Int, List<LeaderboardPosition>>() {

        override fun interpretRequest(request: Int) = JSONObject().apply { put("size", request) }

        override fun interpretResponse(
            request: Int,
            statusCode: Int,
            response: JSONObject
        ) = try {
            parseJsonObjectList(response.getJSONArray("leaderboard"))
            {
                LeaderboardPosition(
                    it.getInt("id"),
                    it.getString("name"),
                    it.getString("title"),
                    it.getString("avatarUrl"),
                    it.getInt("score"),
                    it.getInt("level")
                )
            }
        } catch (_: Exception) {
            throw Interpreter.UninterpretableResponseException()
        }

    })

typealias LeaderboardService = Server.PollService<Int, List<LeaderboardPosition>, List<LeaderboardPosition>>

fun makeLeaderboardService(server: Server, size: Int = 10): LeaderboardService =
    server.PollService(
        SERVICE_ADDRESS,
        size,
        INTERPRETER
    ).apply {
        customRetryPolicy = DEFAULT_RETRY_POLICY
    }
