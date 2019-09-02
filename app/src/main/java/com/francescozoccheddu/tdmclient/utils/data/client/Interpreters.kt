package com.francescozoccheddu.tdmclient.utils.data.client

import org.json.JSONArray
import org.json.JSONObject


interface Interpreter<RequestType, ResponseType> {

    companion object {
        val IDENTITY = object : Interpreter<Any?, Any?> {
            override fun interpretRequest(request: Any?) = request

            override fun interpretResponse(
                request: Any?,
                statusCode: Int,
                response: Any?
            ) = response
        }
    }

    class UninterpretableResponseException : Exception()

    fun interpretRequest(request: RequestType): Any?

    fun interpretResponse(request: RequestType, statusCode: Int, response: Any?): ResponseType

}

abstract class SimpleInterpreter<RequestType, ResponseType> :
    Interpreter<RequestType, ResponseType> {

    final override fun interpretResponse(
        request: RequestType,
        statusCode: Int,
        response: Any?
    ): ResponseType {
        return when (response) {
            is JSONObject -> interpretResponse(request, statusCode, response)
            is JSONArray -> interpretResponse(request, statusCode, response)
            is Boolean -> interpretResponse(request, statusCode, response)
            is Int -> interpretResponse(request, statusCode, response)
            is Long -> interpretResponse(request, statusCode, response)
            is String -> interpretResponse(request, statusCode, response)
            is Float -> interpretResponse(request, statusCode, response)
            is Double -> interpretResponse(request, statusCode, response)
            null -> interpretNullResponse(request, statusCode)
            else -> throw RuntimeException("Unexpected response type")
        }
    }

    open fun interpretResponse(
        request: RequestType,
        statusCode: Int,
        response: JSONObject
    ): ResponseType =
        throw Interpreter.UninterpretableResponseException()

    open fun interpretResponse(
        request: RequestType,
        statusCode: Int,
        response: JSONArray
    ): ResponseType =
        throw Interpreter.UninterpretableResponseException()

    open fun interpretResponse(
        request: RequestType,
        statusCode: Int,
        response: Boolean
    ): ResponseType =
        throw Interpreter.UninterpretableResponseException()

    open fun interpretResponse(request: RequestType, statusCode: Int, response: Int): ResponseType =
        throw Interpreter.UninterpretableResponseException()

    open fun interpretResponse(
        request: RequestType,
        statusCode: Int,
        response: Long
    ): ResponseType =
        throw Interpreter.UninterpretableResponseException()

    open fun interpretResponse(
        request: RequestType,
        statusCode: Int,
        response: String
    ): ResponseType =
        throw Interpreter.UninterpretableResponseException()

    open fun interpretResponse(
        request: RequestType,
        statusCode: Int,
        response: Float
    ): ResponseType =
        throw Interpreter.UninterpretableResponseException()

    open fun interpretResponse(
        request: RequestType,
        statusCode: Int,
        response: Double
    ): ResponseType =
        throw Interpreter.UninterpretableResponseException()

    open fun interpretNullResponse(request: RequestType, statusCode: Int): ResponseType =
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

    fun interpretTime(request: Server.Service<RequestType, ResponseType>.Request) =
        request.startTime

    fun interpretData(response: ResponseType): DataType

}

abstract class SimplePollInterpreter<RequestType, ResponseType, DataType> :
    SimpleInterpreter<RequestType, ResponseType>(),
    PollInterpreter<RequestType, ResponseType, DataType>