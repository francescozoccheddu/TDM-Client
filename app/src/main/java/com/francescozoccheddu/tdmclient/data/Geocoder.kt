package com.francescozoccheddu.tdmclient.data

import com.francescozoccheddu.tdmclient.ui.MainService
import com.francescozoccheddu.tdmclient.utils.commons.FuncEvent
import com.francescozoccheddu.tdmclient.utils.data.boundingBox
import com.francescozoccheddu.tdmclient.utils.data.latlng
import com.francescozoccheddu.tdmclient.utils.data.mapboxAccessToken
import com.mapbox.api.geocoding.v5.MapboxGeocoding
import com.mapbox.api.geocoding.v5.models.GeocodingResponse
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.geometry.LatLng
import retrofit2.Call
import retrofit2.Response
import java.util.*

class Geocoder {

    companion object {
        private const val MAX_RESULTS = 4
        private const val CACHE_SIZE = 100

        private val FORWARD_BUILDER = MapboxGeocoding.builder()
            .accessToken(mapboxAccessToken)
            .autocomplete(true)
            .limit(MAX_RESULTS)
            .languages(Locale.ITALIAN)
            .country(Locale.ITALY)
            .geocodingTypes("district", "place", "locality", "neighborhood", "address", "poi")
            .bbox(MainService.MAP_BOUNDS.boundingBox)

        private val REVERSE_BUILDER = MapboxGeocoding.builder()
            .accessToken(mapboxAccessToken)
            .languages(Locale.ITALIAN)
            .country(Locale.ITALY)

        fun reverse(point: Point, callback: (String?) -> Unit) {
            REVERSE_BUILDER.query(point).build().enqueueCall(object : retrofit2.Callback<GeocodingResponse> {
                override fun onFailure(call: Call<GeocodingResponse>, t: Throwable) {
                    callback(null)
                }

                override fun onResponse(call: Call<GeocodingResponse>, response: Response<GeocodingResponse>) {
                    val features = response.body()?.features()
                    if (features != null && features.isNotEmpty())
                        callback(features[0].text())
                    else callback(null)
                }

            })
        }

    }

    data class Location(val name: String, val point: LatLng, val type: Type) {

        enum class Type {
            PLACE, ADDRESS, POI, UNKNOWN
        }
    }


    private val cache = object : LinkedHashMap<String, List<Location>>() {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<Location>>?) =
            size > CACHE_SIZE
    }

    var query: String = ""
        set(value) {
            val clean = value.trim().toLowerCase()
            if (clean != field) {
                field = clean
                val cachedResult = if (clean.isEmpty()) emptyList() else cache[query]
                if (cachedResult == null)
                    request(clean)
                else
                    onCurrentQueryEnd(cachedResult)
            }
        }

    var result: Collection<Location>? = null

    var loading = false
        set(value) {
            if (value != field) {
                field = value
                onLoadingChange(this)
            }
        }

    private fun request(query: String) {
        loading = true
        result = null
        FORWARD_BUILDER.query(query).build().enqueueCall(object : retrofit2.Callback<GeocodingResponse> {
            override fun onResponse(
                call: retrofit2.Call<GeocodingResponse>,
                response: retrofit2.Response<GeocodingResponse>
            ) {
                val list = response.body()?.features()
                    ?.asSequence()
                    ?.sortedBy { it.relevance() }
                    ?.map {
                        var type = Location.Type.UNKNOWN
                        for (typeDesc in it.placeType()!!) {
                            type = when (typeDesc.trim().toLowerCase()) {
                                "poi" -> Location.Type.POI
                                "address" -> Location.Type.ADDRESS
                                "place" -> Location.Type.PLACE
                                else -> type
                            }
                        }
                        Location(
                            it.matchingText() ?: it.text()!!,
                            it.center()!!.latlng,
                            type
                        )
                    }?.toList() ?: emptyList()
                cache[query] = list
                if (query == this@Geocoder.query)
                    onCurrentQueryEnd(list)
            }

            override fun onFailure(call: retrofit2.Call<GeocodingResponse>, throwable: Throwable) {
                if (query == this@Geocoder.query)
                    onCurrentQueryEnd(null)
            }
        })
    }


    fun onCurrentQueryEnd(result: List<Location>?) {
        loading = false
        if (result != null) {
            this.result = result
            onResult(this)
        }
        else {
            this.result = null
            onFailure(this)
        }
    }

    fun retry() {
        if (query.isNotEmpty() && result == null)
            request(query)
    }

    val onResult = FuncEvent<Geocoder>()
    val onFailure = FuncEvent<Geocoder>()
    val onLoadingChange = FuncEvent<Geocoder>()

}