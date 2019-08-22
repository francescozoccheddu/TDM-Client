package com.francescozoccheddu.tdmclient.data

import com.francescozoccheddu.tdmclient.utils.data.client.Interpreter
import com.francescozoccheddu.tdmclient.utils.data.client.PollInterpreter
import com.francescozoccheddu.tdmclient.utils.data.client.RetryPolicy
import com.francescozoccheddu.tdmclient.utils.data.client.Server
import com.francescozoccheddu.tdmclient.utils.data.client.SimpleInterpreter
import org.json.JSONObject

private const val SERVICE_ADDRESS = "getuser"
private val DEFAULT_RETRY_POLICY = RetryPolicy(2f)

data class UserGetRequest(val user: User, val notifyLevel: Int? = null)

private val INTERPRETER = PollInterpreter.from(object : SimpleInterpreter<UserGetRequest, UserStats>() {

    override fun interpretRequest(request: UserGetRequest) =
        JSONObject().apply {
            put("id", request.user.id)
            put("passkey", request.user.passkey)
            val level = request.notifyLevel
            if (level != null)
                put("notifyLevel", request.notifyLevel)
        }

    override fun interpretResponse(request: UserGetRequest, response: JSONObject): UserStats {
        try {
            return parseUserStats(response)
        } catch (_: Exception) {
            throw Interpreter.UninterpretableResponseException()
        }
    }

})

typealias UserService = Server.PollService<UserGetRequest, UserStats, UserStats>

fun makeUserService(server: Server, user: User): UserService =
    server.PollService(
        SERVICE_ADDRESS,
        UserGetRequest(user),
        INTERPRETER
    ).apply {
        customRetryPolicy = DEFAULT_RETRY_POLICY
    }
