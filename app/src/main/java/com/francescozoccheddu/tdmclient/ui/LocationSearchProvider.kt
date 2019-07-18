package com.francescozoccheddu.tdmclient.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mapbox.api.geocoding.v5.MapboxGeocoding
import com.mapbox.api.geocoding.v5.models.CarmenFeature
import com.mapbox.api.geocoding.v5.models.GeocodingResponse
import com.mapbox.geojson.BoundingBox
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import java.util.*
import java.util.stream.Collectors


class LocationSearchProvider(boundingBox: BoundingBox) {

    private companion object {
        const val CACHE_SIZE = 100
        const val MAX_RESULTS = 4
        val COUNTRY = Locale.ITALY
        val LANGUAGE = Locale.ITALIAN
    }

    private class SearchListAdapter :
        RecyclerView.Adapter<SearchListAdapter.ViewHolder>() {

        private val list = mutableListOf<Location>()

        fun setList(list: Collection<Location>) {
            this.list.clear()
            this.list.addAll(list)
            notifyDataSetChanged()
        }

        private class ViewHolder(viewGroup: ViewGroup) : RecyclerView.ViewHolder(viewGroup) {
            private val tvText = viewGroup.findViewById<TextView>(com.francescozoccheddu.tdmclient.R.id.tv_text)
            private val ivIcon = viewGroup.findViewById<ImageView>(com.francescozoccheddu.tdmclient.R.id.iv_icon)

            fun bind(location: Location) {
                tvText.text = location.name
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(
                com.francescozoccheddu.tdmclient.R.layout.search_item,
                parent,
                false
            ) as ViewGroup
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(list[position])
        }

        override fun getItemCount() = list.size

    }

    private data class Location(val name: String, val point: Point)

    private val cache = object : LinkedHashMap<String, Collection<Location>>() {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Collection<Location>>?): Boolean {
            return size > CACHE_SIZE
        }
    }

    private val geocodingBuilder = MapboxGeocoding.builder()
        .accessToken(Mapbox.getAccessToken() ?: throw IllegalStateException("No access token registered"))
        .autocomplete(true)
        .country(COUNTRY)
        .limit(MAX_RESULTS)
        .languages(LANGUAGE)
        .geocodingTypes("district", "place", "locality", "neighborhood", "address", "poi")
        .bbox(boundingBox)


    private fun onResponse(query: String, results: List<CarmenFeature>) {
        val list = results.stream()
            .sorted(compareBy { c -> -c.relevance()!! })
            .map { Location(it.matchingText() ?: it.text()!!, it.center()!!) }
            .collect(Collectors.toList())
        cache[query] = list
        if (query == lastQuery) {
            _adapter.setList(list)
        }
    }

    private lateinit var lastQuery: String

    fun query(input: String, proximity: Point? = null) {
        var query = input.trim().toLowerCase()
        lastQuery = query
        if (query == "") {
            _adapter.setList(emptyList())
        }
        else {
            cache[query].let {
                if (it != null) {
                    _adapter.setList(it)
                }
                else {
                    if (proximity != null) {
                        geocodingBuilder.proximity(proximity)
                    }
                    geocodingBuilder.query(query).build().enqueueCall(object : retrofit2.Callback<GeocodingResponse> {
                        override fun onResponse(
                            call: retrofit2.Call<GeocodingResponse>,
                            response: retrofit2.Response<GeocodingResponse>
                        ) {
                            onResponse(query, response.body()!!.features())
                        }

                        override fun onFailure(call: retrofit2.Call<GeocodingResponse>, throwable: Throwable) {
                            print("LOC_SEARCH_PROV: Error=$throwable")
                        }
                    })
                }
            }
        }
    }

    private val _adapter = SearchListAdapter()
    val adapter: RecyclerView.Adapter<*> = _adapter

}