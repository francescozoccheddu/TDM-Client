package com.francescozoccheddu.tdmclient.data

import com.francescozoccheddu.tdmclient.utils.data.client.Interpreter
import com.francescozoccheddu.tdmclient.utils.data.client.RetryPolicy
import com.francescozoccheddu.tdmclient.utils.data.client.Server
import com.francescozoccheddu.tdmclient.utils.data.client.SimpleInterpreter
import org.json.JSONObject

private const val SERVICE_ADDRESS = "setuser"
private val DEFAULT_RETRY_POLICY = RetryPolicy(2f)

data class EditUserData(
    val key: UserKey,
    val name: String?,
    val lastNotifiedLevel: Int?,
    val avatar: Int?
)

enum class EditUserResult {
    SUCCESS, PROFANITY_IN_NAME, LOCKED_AVATAR
}

private val INTERPRETER = object : SimpleInterpreter<EditUserData, EditUserResult>() {

    override fun interpretRequest(request: EditUserData) =
        JSONObject().apply {
            put("id", request.key.id)
            put("passkey", request.key.passkey)
            if (request.avatar != null)
                put("avatar", request.avatar)
            if (request.lastNotifiedLevel != null)
                put("lastNotifiedLevel", request.lastNotifiedLevel)
            if (request.name != null)
                put("name", request.name)
        }

    override fun interpretResponse(
        request: EditUserData,
        statusCode: Int,
        response: JSONObject
    ) = when (statusCode) {
        200 -> EditUserResult.SUCCESS
        else -> try {
            when (response.getJSONObject("error").getString("formError")) {
                "profanity" -> EditUserResult.PROFANITY_IN_NAME
                "locked avatar" -> EditUserResult.LOCKED_AVATAR
                else -> throw Interpreter.UninterpretableResponseException()
            }
        } catch (_: Exception) {
            throw Interpreter.UninterpretableResponseException()
        }
    }

}

typealias EditUserService = Server.Service<EditUserData, EditUserResult>

fun makeEditUserService(server: Server): EditUserService =
    server.Service(
        SERVICE_ADDRESS,
        INTERPRETER
    ).apply {
        customRetryPolicy = DEFAULT_RETRY_POLICY
    }

fun EditUserService.setName(key: UserKey, name: String) =
    this.Request(EditUserData(key, name, null, null))

fun EditUserService.setAvatar(key: UserKey, avatar: Int) =
    this.Request(EditUserData(key, null, null, avatar))

fun EditUserService.setLastNotifiedLevel(key: UserKey, lastNotifiedLevel: Int) =
    this.Request(EditUserData(key, null, lastNotifiedLevel, null))

fun EditUserData.editUserStats(stats: UserStats, avatars: List<String>? = null) = UserStats(
    stats.score,
    stats.level,
    stats.multiplier,
    stats.nextLevelScore,
    lastNotifiedLevel ?: stats.lastNotifiedLevel,
    name ?: stats.name,
    if (avatars != null && avatar != null) avatars[avatar] else stats.avatarUrl
)