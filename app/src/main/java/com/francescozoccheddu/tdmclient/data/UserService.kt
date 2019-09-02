package com.francescozoccheddu.tdmclient.data

import com.francescozoccheddu.tdmclient.utils.data.client.Interpreter
import com.francescozoccheddu.tdmclient.utils.data.client.PollInterpreter
import com.francescozoccheddu.tdmclient.utils.data.client.RetryPolicy
import com.francescozoccheddu.tdmclient.utils.data.client.Server
import com.francescozoccheddu.tdmclient.utils.data.client.SimpleInterpreter
import org.json.JSONObject

private const val SERVICE_ADDRESS = "getuser"
private val DEFAULT_RETRY_POLICY = RetryPolicy(2f)

private val INTERPRETER = PollInterpreter.from(object : SimpleInterpreter<UserKey, UserStats>() {

    override fun interpretRequest(request: UserKey) =
        JSONObject().apply {
            put("id", request.id)
            put("passkey", request.passkey)
        }

    override fun interpretResponse(
        request: UserKey,
        statusCode: Int,
        response: JSONObject
    ): UserStats {
        try {
            return parseUserStats(response)
        } catch (_: Exception) {
            throw Interpreter.UninterpretableResponseException()
        }
    }

})

typealias UserService = Server.PollService<UserKey, UserStats, UserStats>

fun makeUserService(server: Server, userKey: UserKey): UserService =
    server.PollService(
        SERVICE_ADDRESS,
        userKey,
        INTERPRETER
    ).apply {
        customRetryPolicy = DEFAULT_RETRY_POLICY
    }
