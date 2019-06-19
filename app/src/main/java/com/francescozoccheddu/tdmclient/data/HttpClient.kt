package com.francescozoccheddu.tdmclient.data

import android.content.Context
import com.android.volley.DefaultRetryPolicy
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.net.URI
import com.android.volley.Request as VolleyRequest
import com.android.volley.Response as VolleyResponse

class HttpClient(context: Context)
{

    private companion object
    {

        private val ROOT_URI = URI.create("http://192.168.1.2:8080")
        private const val REQUEST_TIMEOUT = 15 * 1000
        private val REQUEST_RETRY_POLICY = DefaultRetryPolicy(
            REQUEST_TIMEOUT,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )

        private fun getAbsoluteUrl(relativeUrl: String) = ROOT_URI.resolve(relativeUrl).normalize().toString()

        private class BaseRequest(
            private val request: JsonObjectRequest,
            override val callback: Callback?,
            override val body: JSONObject?,
            override val url: String
        ) : Request
        {

            override fun cancel() = request.cancel()
            override val canceled: Boolean get() = request.isCanceled
            override val finished: Boolean get() = result != null
            override val succeeded: Boolean get() = result is JSONObject
            override val exception: Exception get() = result as Exception
            override val response: JSONObject get() = result as JSONObject
            private var result: Any? = null

            fun finishWithException(exception: Exception)
            {
                result = exception
                callback?.onException(exception)
            }

            fun finishWithResult(response: JSONObject)
            {
                result = response
                callback?.onResponse(response)
            }

        }

    }

    interface Callback
    {
        fun onException(exception: Exception)
        {
        }

        fun onResponse(response: JSONObject)
        {
        }
    }

    interface Request
    {
        fun cancel()
        val callback: Callback?
        val body: JSONObject?
        val url: String
        val canceled: Boolean
        val finished: Boolean
        val succeeded: Boolean
        val exception: Exception
        val response: JSONObject
    }

    private val requestQueue = Volley.newRequestQueue(context)
    private val pendingRequests = mutableListOf<Request>()

    fun request(url: String, body: JSONObject?, callback: Callback?): Request
    {
        lateinit var request: BaseRequest
        val jsonRequest = JsonObjectRequest(VolleyRequest.Method.GET, getAbsoluteUrl(url), body,
            VolleyResponse.Listener<JSONObject>
            { response ->
                request.finishWithResult(response)
                pendingRequests -= request
            },
            VolleyResponse.ErrorListener
            { error ->
                request.finishWithException(error)
                pendingRequests -= request
            }
        )
        jsonRequest.retryPolicy = REQUEST_RETRY_POLICY
        requestQueue.add(jsonRequest)
        request = BaseRequest(jsonRequest, callback, body, url)
        pendingRequests += request
        return request
    }


}