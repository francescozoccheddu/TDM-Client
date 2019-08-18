package com.francescozoccheddu.tdmclient.data

import android.location.Location
import com.francescozoccheddu.tdmclient.utils.commons.iso
import com.francescozoccheddu.tdmclient.utils.data.client.Interpreter
import com.francescozoccheddu.tdmclient.utils.data.client.RetryPolicy
import com.francescozoccheddu.tdmclient.utils.data.client.Server
import com.francescozoccheddu.tdmclient.utils.data.client.SimpleInterpreter
import com.francescozoccheddu.tdmclient.utils.data.json
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

private const val SERVICE_ADDRESS = "putmeasurements"
private val DEFAULT_RETRY_POLICY = RetryPolicy(4f)

data class Measurement(
    val altitude: Float, val humidity: Float,
    val pressure: Float, val temperature: Float,
    val fineDust150: Float, val fineDust200: Float
)

data class LocalizedMeasurement(val time: Date, val location: Location, val measurement: Measurement)
data class MeasurementPutRequest(val user: User, val measurements: Collection<LocalizedMeasurement>)


private val INTERPRETER = object : SimpleInterpreter<MeasurementPutRequest, Score>() {
    override fun interpretRequest(request: MeasurementPutRequest): JSONObject? =
        JSONObject().apply {
            put("id", request.user.id)
            put("passkey", request.user.passkey)
            put("measurements", JSONArray(request.measurements.map {
                JSONObject().apply {
                    put("time", it.time.iso)
                    put("location", it.location.json)
                    put("altitude", it.measurement.altitude)
                    put("humidity", it.measurement.humidity)
                    put("pressure", it.measurement.pressure)
                    put("temperature", it.measurement.temperature)
                    put("fineDust150", it.measurement.fineDust150)
                    put("fineDust200", it.measurement.fineDust200)
                }
            }))
        }

    override fun interpretResponse(request: MeasurementPutRequest, response: JSONObject): Score {
        try {
            return parseScore(response)
        } catch (_: Exception) {
            throw Interpreter.UninterpretableResponseException()
        }
    }
}

typealias MeasurementService = Server.Service<MeasurementPutRequest, Score>

fun makeMeasurementService(server: Server): MeasurementService =
    server.Service(
        SERVICE_ADDRESS,
        INTERPRETER
    ).apply {
        customRetryPolicy = DEFAULT_RETRY_POLICY
    }
