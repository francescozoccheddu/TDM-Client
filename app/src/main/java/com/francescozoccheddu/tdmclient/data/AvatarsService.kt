package com.francescozoccheddu.tdmclient.data

import com.francescozoccheddu.tdmclient.utils.data.client.Interpreter
import com.francescozoccheddu.tdmclient.utils.data.client.PollInterpreter
import com.francescozoccheddu.tdmclient.utils.data.client.RetryPolicy
import com.francescozoccheddu.tdmclient.utils.data.client.Server
import com.francescozoccheddu.tdmclient.utils.data.client.SimpleInterpreter
import com.francescozoccheddu.tdmclient.utils.data.parseJsonObjectList
import com.francescozoccheddu.tdmclient.utils.data.parseJsonStringList
import org.json.JSONObject

private const val SERVICE_ADDRESS = "getavatars"
private val DEFAULT_RETRY_POLICY = RetryPolicy(2f)

class AvatarSet(levels: List<AvatarLevel>, val images: List<String>) {

    val levels: List<AvatarLevel>

    init {
        this.levels = levels.sortedBy { it.level }
        var lastLevel: Int? = null
        for (level in this.levels) {
            if (level.level == lastLevel)
                throw IllegalArgumentException("Repeated levels")
            lastLevel = level.level
        }
        val maxAvatarLevel = this.levels.maxBy { it.maxAvatarIndex }
        if (maxAvatarLevel != null && maxAvatarLevel.maxAvatarIndex > images.lastIndex)
            throw IllegalArgumentException("Max avatar index cannot be greater than the last image index")
    }

    data class AvatarLevel(val level: Int, val maxAvatarIndex: Int)

    fun maxAvatarByLevel(level: Int): Int {
        val i = levels.binarySearchBy(level) { it.level }
        val ai = if (i > 0) i else (-i - 2)
        return if (ai < 0) 0 else levels[ai].maxAvatarIndex
    }

}

private val INTERPRETER = PollInterpreter.from(object : SimpleInterpreter<Unit, AvatarSet>() {

    override fun interpretRequest(request: Unit) = JSONObject()

    override fun interpretResponse(
        request: Unit,
        statusCode: Int,
        response: JSONObject
    ): AvatarSet {
        try {
            val levels = parseJsonObjectList(response.getJSONArray("levels")) {
                AvatarSet.AvatarLevel(it.getInt("level"), it.getInt("lastImage"))
            }
            val images = parseJsonStringList(response.getJSONArray("images"))
            return AvatarSet(levels, images)
        } catch (_: Exception) {
            throw Interpreter.UninterpretableResponseException()
        }
    }

})

typealias AvatarsService = Server.PollService<Unit, AvatarSet, AvatarSet>

fun makeAvatarsService(server: Server): AvatarsService =
    server.PollService(
        SERVICE_ADDRESS,
        Unit,
        INTERPRETER
    ).apply {
        customRetryPolicy = DEFAULT_RETRY_POLICY
    }
