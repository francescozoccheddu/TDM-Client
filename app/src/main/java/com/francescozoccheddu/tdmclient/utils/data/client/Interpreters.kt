package com.francescozoccheddu.tdmclient.utils.data.client

import org.json.JSONArray
import org.json.JSONObject


interface Interpreter<RequestType, ResponseType> {

    companion object {
        val IDENTITY = object : Interpreter<Any?, Any?> {
            override fun interpretRequest(request: Any?) = request

            override fun interpretResponse(request: Any?, response: Any?) = response
        }
    }

    class UninterpretableResponseException : Exception()

    fun interpretRequest(request: RequestType): Any?

    fun interpretResponse(request: RequestType, response: Any?): ResponseType

}

abstract class SimpleInterpreter<RequestType, ResponseType> : Interpreter<RequestType, ResponseType> {

    override final fun interpretResponse(request: RequestType, response: Any?): ResponseType {
        return when (response) {
            is JSONObject -> interpretResponse(request, response)
            is JSONArray -> interpretResponse(request, response)
            is Boolean -> interpretResponse(request, response)
            is Int -> interpretResponse(request, response)
            is Long -> interpretResponse(request, response)
            is String -> interpretResponse(request, response)
            is Float -> interpretResponse(request, response)
            is Double -> interpretResponse(request, response)
            null -> interpretNullResponse(request)
            else -> throw RuntimeException("Unexpected response type")
        }
    }

    open fun interpretResponse(request: RequestType, response: JSONObject): ResponseType =
        throw Interpreter.UninterpretableResponseException()

    open fun interpretResponse(request: RequestType, response: JSONArray): ResponseType =
        throw Interpreter.UninterpretableResponseException()

    open fun interpretResponse(request: RequestType, response: Boolean): ResponseType =
        throw Interpreter.UninterpretableResponseException()

    open fun interpretResponse(request: RequestType, response: Int): ResponseType =
        throw Interpreter.UninterpretableResponseException()

    open fun interpretResponse(request: RequestType, response: Long): ResponseType =
        throw Interpreter.UninterpretableResponseException()

    open fun interpretResponse(request: RequestType, response: String): ResponseType =
        throw Interpreter.UninterpretableResponseException()

    open fun interpretResponse(request: RequestType, response: Float): ResponseType =
        throw Interpreter.UninterpretableResponseException()

    open fun interpretResponse(request: RequestType, response: Double): ResponseType =
        throw Interpreter.UninterpretableResponseException()

    open fun interpretNullResponse(request: RequestType): ResponseType =
        throw Interpreter.UninterpretableResponseException()

}

interface PollInterpreter<RequestType, ResponseType, DataType> :
    Interpreter<RequestType, ResponseType> {

    companion object {

        fun <RequestType, ResponseType> from(interpreter: Interpreter<RequestType, ResponseType>)
                : PollInterpreter<RequestType, ResponseType, ResponseType> =
            object : PollInterpreter<RequestType, ResponseType, ResponseType>,
                Interpreter<RequestType, ResponseType> by interpreter {

                override fun interpretData(response: ResponseType) = response

            }

        val IDENTITY = from(Interpreter.IDENTITY)
    }

    fun interpretTime(request: Server.Service<RequestType, ResponseType>.Request) = request.startTime

    fun interpretData(response: ResponseType): DataType

}

abstract class SimplePollInterpreter<RequestType, ResponseType, DataType> :
    SimpleInterpreter<RequestType, ResponseType>(),
    PollInterpreter<RequestType, ResponseType, DataType>