package com.francescozoccheddu.tdmclient.utils.data

import androidx.annotation.FloatRange
import com.francescozoccheddu.tdmclient.data.UserController
import com.francescozoccheddu.tdmclient.data.UserStats
import com.francescozoccheddu.tdmclient.utils.commons.dateElapsed
import com.francescozoccheddu.tdmclient.utils.data.client.Server
import org.json.JSONArray
import org.json.JSONObject

fun travelDuration(@FloatRange(from = 0.0) distance: Float) = distance / 0.8f

fun <Type> parseJsonList(array: JSONArray, parser: (Any) -> Type) =
    List(array.length()) { parser(array.get(it)) }

fun parseJsonStringList(array: JSONArray) = parseJsonList(array) { it as String }

fun <Type> parseJsonObjectList(array: JSONArray, parser: (JSONObject) -> Type) =
    parseJsonList(array) { parser(it as JSONObject) }

class RemoteValue<RequestType, ResponseType, DataType>(val service: Server.PollService<RequestType, ResponseType, DataType>) {

    var expiration: Float? = null
        set(value) {
            if (value != null && value < 0.0f) {
                throw IllegalArgumentException("Value must be positive")
            }
            field = value
        }

    init {
        service.onData += { notify(it) }
        service.onRequestStatusChanged += {
            if (service.pendingRequests.isEmpty())
                notify(null)
        }
    }

    private fun notify(data: DataType?) {
        val callbacksCopy = callbacks.toList()
        callbacks.clear()
        callbacksCopy.forEach { it(data) }
    }

    private val callbacks = mutableListOf<(DataType?) -> Unit>()

    fun get(forceUpdate: Boolean = false, callback: ((DataType?) -> Unit)? = null) {
        val expiration = this.expiration
        if (forceUpdate || !service.hasData || (expiration != null && dateElapsed(service.time) > expiration)) {
            if (callback != null)
                callbacks.add(callback)
            if (service.pendingRequests.isEmpty())
                service.poll()
        }
        else
            callback?.invoke(service.data)
    }
}

val UserController.statsOrNull: UserStats? get() = if (hasStats) stats else null
