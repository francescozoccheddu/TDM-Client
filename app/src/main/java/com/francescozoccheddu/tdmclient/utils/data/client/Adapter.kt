package com.francescozoccheddu.tdmclient.utils.data.client

import com.android.volley.NetworkResponse
import com.android.volley.ParseError
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyLog
import com.android.volley.toolbox.HttpHeaderParser
import com.francescozoccheddu.tdmclient.utils.commons.Nullable
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset

class VolleyAdapterRequest<ResponseType>(
    url: String,
    private val requestBody: String,
    private val interpreter: (Int, Any?) -> ResponseType,
    private val listener: Response.Listener<ResponseType>,
    errorListener: Response.ErrorListener
) : Request<ResponseType>(Method.POST, url, errorListener) {


    companion object {

        const val CHARSET = "utf-8"

        fun toJSON(any: Any?): String {
            return when (any) {
                is Int, is Long, is Double, is Boolean,
                is JSONArray, is JSONObject -> any.toString()
                null -> "null"
                is String -> "\"$any\""
                else -> throw IllegalArgumentException("Not a JSON type")
            }
        }

        fun fromJSON(json: String): Any? {
            val j = json.trim()
            run {
                // Object
                try {
                    return JSONObject(j)
                } catch (_: JSONException) {
                }
            }
            run {
                // List
                try {
                    return JSONArray(j)
                } catch (_: JSONException) {
                }
            }
            run {
                // Constant
                if (j == "true")
                    return true
                if (j == "false")
                    return false
                if (j == "null")
                    return null
            }
            run {
                // Number
                val prim = j.toIntOrNull()
                    ?: j.toLongOrNull()
                    ?: j.toFloatOrNull()
                    ?: j.toDoubleOrNull()
                if (prim != null)
                    return prim
            }
            run {
                // String
                if (j.length >= 2 && j[0] == '"' && j[j.lastIndex] == '"')
                    return j.substring(1, j.lastIndex)
            }
            throw JSONException("Not a JSON string")
        }

        fun <ResponseType> parseResponse(
            response: NetworkResponse,
            interpreter: (Int, Any?) -> ResponseType
        ): Nullable<ResponseType>? {
            return try {
                val charset = Charset.forName(HttpHeaderParser.parseCharset(response.headers, CHARSET))
                val json = String(response.data, charset)
                Nullable(interpreter(response.statusCode, fromJSON(json)))
            } catch (e: UnsupportedEncodingException) {
                null
            } catch (je: JSONException) {
                null
            } catch (ue: Interpreter.UninterpretableResponseException) {
                null
            }
        }

    }

    private val lock = Any()
    private var canceled = false

    override fun cancel() {
        super.cancel()
        synchronized(lock) {
            canceled = true
        }
    }

    override fun deliverResponse(response: ResponseType) {
        synchronized(lock) {
            if (!canceled)
                listener.onResponse(response)
        }
    }

    override fun parseNetworkResponse(response: NetworkResponse): Response<ResponseType> {
        val parsedResponse = parseResponse(response, interpreter)
        if (parsedResponse != null)
            return Response.success(parsedResponse.value, HttpHeaderParser.parseCacheHeaders(response))
        else
            return Response.error(ParseError())
    }

    override fun getHeaders() = mutableMapOf(
        "Accept" to "application/json",
        "Accept-Charset" to CHARSET
    )

    @Deprecated("Use {@link #getBodyContentType()}. ")
    override fun getPostBodyContentType(): String {
        return bodyContentType
    }


    @Deprecated("Use {@link #getBody()}. ")
    override fun getPostBody(): ByteArray? {
        return body
    }

    override fun getBodyContentType(): String {
        return "application/json; charset=$CHARSET"
    }

    override fun getBody(): ByteArray? {
        return try {
            requestBody.toByteArray(charset(CHARSET))
        } catch (uee: UnsupportedEncodingException) {
            VolleyLog.wtf("Unsupported Encoding while trying to get body bytes using %s", CHARSET)
            null
        }
    }

}