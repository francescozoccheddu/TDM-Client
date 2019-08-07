package com.francescozoccheddu.tdmclient.data

import com.francescozoccheddu.tdmclient.utils.commons.FuncEvent
import com.francescozoccheddu.tdmclient.utils.data.boundingBox
import com.francescozoccheddu.tdmclient.utils.data.latlng
import com.francescozoccheddu.tdmclient.utils.data.mapboxAccessToken
import com.mapbox.api.geocoding.v5.MapboxGeocoding
import com.mapbox.api.geocoding.v5.models.GeocodingResponse
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import java.util.*

class Geocoder(val bounds: LatLngBounds?) {

    private companion object {
        private const val MAX_RESULTS = 4
        private const val CACHE_SIZE = 100
    }

    data class Location(val name: String, val point: LatLng, val type: Type) {

        enum class Type {
            PLACE, ADDRESS, POI, UNKNOWN
        }
    }

    val builder = MapboxGeocoding.builder()
        .accessToken(mapboxAccessToken)
        .autocomplete(true)
        .limit(MAX_RESULTS)
        .languages(Locale.getDefault())
        .geocodingTypes("district", "place", "locality", "neighborhood", "address", "poi").apply {
            if (bounds != null)
                bbox(bounds.boundingBox)
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
                val cachedResult = cache[query]
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
        builder.query(query).build().enqueueCall(object : retrofit2.Callback<GeocodingResponse> {
            override fun onResponse(
                call: retrofit2.Call<GeocodingResponse>,
                response: retrofit2.Response<GeocodingResponse>
            ) {
                val list = response.body()!!.features()!!
                    .asSequence()
                    .sortedBy { it.relevance() }
                    .map {
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
                    }.toList()
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