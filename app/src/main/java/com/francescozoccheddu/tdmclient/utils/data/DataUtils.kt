package com.francescozoccheddu.tdmclient.utils.data

import androidx.annotation.FloatRange
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
        service.onRequestStatusChanged += { request ->
            if (!request.status.pending) {
                if (request.status.succeeded)
                    callbacks.forEach { it(service.data) }
                else
                    callbacks.forEach { it(null) }
            }
        }
    }

    private val callbacks = mutableListOf<(DataType?) -> Unit>()

    fun get(forceUpdate: Boolean = false, callback: (DataType?) -> Unit) {
        val expiration = this.expiration
        if (forceUpdate || !service.hasData || (expiration != null && dateElapsed(service.time) > expiration)) {
            callbacks.add(callback)
            if (service.pendingRequests.isEmpty())
                service.poll()
        }
        else {
            callback(service.data)
        }
    }
}