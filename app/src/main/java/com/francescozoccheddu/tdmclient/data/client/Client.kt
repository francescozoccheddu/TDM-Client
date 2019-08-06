package com.francescozoccheddu.tdmclient.data.client

import android.content.Context
import com.android.volley.AuthFailureError
import com.android.volley.ClientError
import com.android.volley.DefaultRetryPolicy
import com.android.volley.NetworkError
import com.android.volley.NoConnectionError
import com.android.volley.ParseError
import com.android.volley.ServerError
import com.android.volley.TimeoutError
import com.android.volley.VolleyError
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.francescozoccheddu.tdmclient.utils.FuncEvent
import com.francescozoccheddu.tdmclient.utils.ProcEvent
import com.francescozoccheddu.tdmclient.utils.dateElapsed
import com.francescozoccheddu.tdmclientservice.Timer
import org.json.JSONException
import org.json.JSONObject
import java.nio.charset.Charset
import java.nio.charset.IllegalCharsetNameException
import java.nio.charset.UnsupportedCharsetException
import java.util.*
import kotlin.math.roundToInt
import com.android.volley.Request as VolleyRequest
import com.android.volley.Response as VolleyResponse

class Server(context: Context, val address: ServerAddress) {

    constructor(context: Context, address: String) : this(context, ServerAddress(address))

    fun cancelIf(predicate: (Service<*, *>.Request) -> Boolean) {
        val iterator = _pendingRequests.iterator()
        while (iterator.hasNext()) {
            val request = iterator.next()
            if (predicate(request)) {
                iterator.remove()
                request.cancel()
            }
        }
    }

    fun cancelAll() = cancelIf { true }

    val onRequestStatusChanged = FuncEvent<Service<*, *>.Request>()

    private val _pendingRequests = mutableSetOf<Service<*, *>.Request>()
    val pendingRequests = Collections.unmodifiableSet(_pendingRequests)

    private fun requestStatusChanged(request: Service<*, *>.Request) {
        if (request.status.pending)
            _pendingRequests += request
        else
            _pendingRequests -= request
        onRequestStatusChanged(request)
    }

    var retryPolicy = RetryPolicy()

    private val nativeQueue = Volley.newRequestQueue(context)

    private class Nullable<Type>(val value: Type)

    inner open class Service<RequestType, ResponseType>(
        val address: ServiceAddress,
        open val interpreter: Interpreter<RequestType, ResponseType>
    ) {

        constructor(
            address: String,
            interpreter: Interpreter<RequestType, ResponseType>
        ) : this(ServiceAddress(address), interpreter)

        fun cancelIf(predicate: (Request) -> Boolean) {
            val iterator = _pendingRequests.iterator()
            while (iterator.hasNext()) {
                val request = iterator.next()
                if (predicate(request)) {
                    iterator.remove()
                    request.cancel()
                }
            }
        }

        fun cancelAll() = cancelIf { true }

        val onRequestStatusChanged = FuncEvent<Request>()

        private val _pendingRequests = mutableSetOf<Request>()
        val pendingRequests = Collections.unmodifiableSet(_pendingRequests)

        protected open fun requestStatusChanged(request: Request) {
            this@Server.requestStatusChanged(request)
            if (request.status.pending)
                _pendingRequests += request
            else
                _pendingRequests -= request
            onRequestStatusChanged(request)
        }

        val retryPolicy
            get() = customRetryPolicy ?: this@Server.retryPolicy

        var customRetryPolicy: RetryPolicy? = null

        inner class Request(
            val request: RequestType,
            val retryPolicy: RetryPolicy = this@Service.retryPolicy
        ) {

            private val nativeRequest: VolleyRequest<JSONObject>

            val hasResponse get() = _response != null

            val response: ResponseType
                get() = _response?.value ?: throw IllegalStateException("Pending")

            private var _response: Nullable<ResponseType>? = null

            var status = Status.PLANNED
                private set(value) {
                    field = value
                    this@Service.requestStatusChanged(this)
                    onStatusChange(this)
                }

            var startTime: Date = Date()
                get() = if (!status.started) throw IllegalStateException("Not started") else field
                private set

            var endTime: Date = Date()
                get() = if (status.pending) throw IllegalStateException("Pending") else field
                private set

            private fun trySetResponse(body: JSONObject) {
                try {
                    _response = Nullable(interpreter.interpretResponse(request, body))
                } catch (_: Interpreter.UninterpretableResponseException) {
                }
            }

            init {
                nativeRequest = JsonObjectRequest(
                    VolleyRequest.Method.POST,
                    this@Server.address.resolveService(this@Service.address),
                    this@Service.interpreter.interpretRequest(request),
                    VolleyResponse.Listener<JSONObject>
                    {
                        endTime = Date()
                        trySetResponse(it)
                        if (hasResponse) {
                            status = Status.SUCCESS
                        }
                        else {
                            status = Status.RESPONSE_ERROR
                        }
                    },
                    VolleyResponse.ErrorListener
                    {
                        endTime = Date()
                        val networkResponse = it.networkResponse
                        if (networkResponse?.data != null) {
                            var body: JSONObject? = null
                            try {
                                val charsetName = HttpHeaderParser.parseCharset(networkResponse.headers, "utf-8")
                                val charset = Charset.forName(charsetName)
                                body = JSONObject(String(networkResponse.data, charset))
                            } catch (_: JSONException) {
                            } catch (_: IllegalCharsetNameException) {
                            } catch (_: UnsupportedCharsetException) {
                            }
                            if (body != null)
                                trySetResponse(body)
                        }
                        status = when (it) {
                            is AuthFailureError, is ClientError -> Status.REQUEST_ERROR
                            is NetworkError -> Status.NETWORK_ERROR
                            is ParseError -> Status.RESPONSE_ERROR
                            is NoConnectionError -> Status.NO_CONNECTION_ERROR
                            is ServerError -> Status.SERVER_ERROR
                            is TimeoutError -> Status.SURRENDED_ERROR
                            is VolleyError -> Status.REQUEST_ERROR
                            else -> Status.RUNTIME_ERROR
                        }
                    }
                )
                nativeRequest.retryPolicy = DefaultRetryPolicy(
                    (retryPolicy.timeout * 1000).roundToInt(),
                    retryPolicy.attempts - 1,
                    retryPolicy.backoffMultiplier
                )
            }


            fun start() {
                if (status.started)
                    throw IllegalArgumentException("Already started")
                this@Server.nativeQueue.add(nativeRequest)
                startTime = Date()
                status = Status.PENDING
            }

            fun cancel() {
                if (!status.pending)
                    throw IllegalStateException("Not pending")
                nativeRequest.cancel()
                endTime = Date()
                status = Status.CANCELED
            }

            val onStatusChange = FuncEvent<Request>()

        }
    }

    open inner class PollService<RequestType, ResponseType, DataType>(
        address: ServiceAddress,
        var pollRequest: RequestType,
        override val interpreter: PollInterpreter<RequestType, ResponseType, DataType>
    ) : Service<RequestType, ResponseType>(address, interpreter) {

        constructor(
            address: String,
            pollRequest: RequestType,
            interpreter: PollInterpreter<RequestType, ResponseType, DataType>
        ) : this(ServiceAddress(address), pollRequest, interpreter)


        var time = Date()
            get() = if (hasData) field else throw IllegalStateException("No data")
            private set

        val data
            get() = _data?.value ?: throw IllegalStateException("No data")

        val hasData
            get() = _data != null

        private var _data: Nullable<DataType>? = null

        val onData = FuncEvent<DataType>()

        protected open fun dataSubmitted() {}

        protected open fun polled() {}

        fun poll() {
            if (cancelPendingRequestsOnPoll)
                cancelAll()
            Request(pollRequest).start()
            polled()
        }

        var cancelPendingRequestsOnPoll = false

        fun submit(time: Date, data: DataType) {
            if (!hasData || time > this.time) {
                this.time = time
                this._data = Nullable(data)
                dataSubmitted()
                onData(data)
            }
        }

        override fun requestStatusChanged(request: Request) {
            super.requestStatusChanged(request)
            if (request.status.succeeded)
                submit(interpreter.interpretTime(request), interpreter.interpretData(request.response))
        }

    }

    open inner class AutoPollService<RequestType, ResponseType, DataType>(
        address: ServiceAddress,
        pollRequest: RequestType,
        interpreter: PollInterpreter<RequestType, ResponseType, DataType>,
        timer: Timer = Timer()
    ) : PollService<RequestType, ResponseType, DataType>(address, pollRequest, interpreter) {

        constructor(
            address: String,
            pollRequest: RequestType,
            interpreter: PollInterpreter<RequestType, ResponseType, DataType>,
            timer: Timer = Timer()
        ) : this(
            ServiceAddress(address), pollRequest, interpreter, timer
        )

        private val pollTicker = timer.Ticker().apply {
            runnable = Runnable { poll() }
        }

        private val expirationCountdown = timer.Countdown().apply {
            runnable = Runnable { updateExpiration() }
        }

        private fun updateExpiration() {
            expirationCountdown.cancel()
            val expiration = this.expiration
            if (hasData && expiration != null) {
                val wait = expiration - dateElapsed(time)
                if (wait > 0) {
                    expired = false
                    expirationCountdown.time = wait
                    expirationCountdown.pull()
                }
                else if (!expired) {
                    expired = true
                    onExpire()
                }
            }
            else {
                expired = false
            }
        }

        var expiration: Float? = null
            set(value) {
                if (value != null && value < 0.0f) {
                    throw IllegalArgumentException("Value must be positive")
                }
                field = value
                updateExpiration()
            }

        var periodicPoll: Float? = null
            set(value) {
                if (value != field) {
                    field = value
                    if (value != null) {
                        pollTicker.tickInterval = value
                        pollTicker.running = true
                    }
                    else
                        pollTicker.running = false
                }
            }

        var expired = false
            private set

        val onExpire = ProcEvent()

        override fun dataSubmitted() {
            super.dataSubmitted()
            updateExpiration()
        }

        override fun polled() {
            super.polled()
            pollTicker.notifyTick()
        }

    }


}

interface Interpreter<RequestType, ResponseType> {

    companion object {
        val IDENTITY = object : Interpreter<JSONObject?, JSONObject> {
            override fun interpretRequest(request: JSONObject?) = request
            override fun interpretResponse(request: JSONObject?, response: JSONObject) = response
        }
    }

    class UninterpretableResponseException : Exception()

    fun interpretRequest(request: RequestType): JSONObject?

    fun interpretResponse(request: RequestType, response: JSONObject): ResponseType

}

interface PollInterpreter<RequestType, ResponseType, DataType> : Interpreter<RequestType, ResponseType> {

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

data class RetryPolicy(val timeout: Float = 5f, val attempts: Int = 1, val backoffMultiplier: Float = 1.0f) {
    init {
        if (timeout < 1.0f)
            throw IllegalArgumentException("Timeout must be at least 1 second")
        if (attempts < 1)
            throw IllegalArgumentException("Attempts count must be at least 1")
        if (backoffMultiplier < 1.0f)
            throw IllegalArgumentException("Backoff multiplier must be greater than 1")
    }
}