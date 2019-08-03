package com.francescozoccheddu.tdmclient.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.francescozoccheddu.tdmclient.R
import com.francescozoccheddu.tdmclient.utils.FuncEvent
import com.francescozoccheddu.tdmclient.utils.ProcEvent
import com.francescozoccheddu.tdmclient.utils.latlng
import com.francescozoccheddu.tdmclient.utils.mapboxAccessToken
import com.mapbox.api.geocoding.v5.MapboxGeocoding
import com.mapbox.api.geocoding.v5.models.CarmenFeature
import com.mapbox.api.geocoding.v5.models.GeocodingResponse
import com.mapbox.geojson.BoundingBox
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.geometry.LatLng
import java.util.*
import java.util.stream.Collectors
import kotlin.math.roundToInt


class LocationSearchProvider(boundingBox: BoundingBox) {

    private companion object {
        private const val CACHE_SIZE = 100
        private const val MAX_RESULTS = 4
        private val COUNTRY = Locale.ITALY
        private val LANGUAGE = Locale.ITALIAN
        private const val SECONDS_PER_METER = 0.8

        private fun makeLocation(name: String, point: LatLng, typeDescs: Iterable<String>): Location {
            var type = Location.Type.UNKNOWN
            for (typeDesc in typeDescs) {
                type = when (typeDesc.trim().toLowerCase()) {
                    "poi" -> Location.Type.POI
                    "address" -> Location.Type.ADDRESS
                    "place" -> Location.Type.PLACE
                    else -> type
                }
            }
            return Location(name, point, type)
        }

    }

    private inner class SearchListAdapter : RecyclerView.Adapter<SearchListAdapter.ViewHolder>() {

        private val recyclerViews = mutableSetOf<RecyclerView>()

        override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
            super.onAttachedToRecyclerView(recyclerView)
            recyclerViews += recyclerView
        }

        override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
            super.onDetachedFromRecyclerView(recyclerView)
            recyclerViews -= recyclerView
        }

        private inner class ViewHolder(viewGroup: ViewGroup) : RecyclerView.ViewHolder(viewGroup) {

            init {
                viewGroup.setOnClickListener {
                    onLocationClick(list[adapterPosition])
                }
            }

            private val tvName = viewGroup.findViewById<TextView>(R.id.tv_name)
            private val ivIcon = viewGroup.findViewById<ImageView>(R.id.iv_icon)
            private val tvDistance = viewGroup.findViewById<TextView>(R.id.tv_distance)

            fun bind(location: Location) {
                tvName.text = location.name
                ivIcon.setImageResource(
                    when (location.type) {
                        Location.Type.ADDRESS -> R.drawable.ic_map
                        Location.Type.PLACE -> R.drawable.ic_terrain
                        Location.Type.POI -> R.drawable.ic_place
                        Location.Type.UNKNOWN -> R.drawable.ic_place
                    }
                )
                updateDistance()
            }

            fun updateDistance() {
                val a = list[adapterPosition].point
                val b = userLocation
                if (b != null) {
                    val minutes = ((a.distanceTo(b) * SECONDS_PER_METER) / 60f).roundToInt()
                    tvDistance.text = if (minutes < 1) "<1m" else "${minutes}m"
                    tvDistance.visibility = View.VISIBLE
                }
                else
                    tvDistance.visibility = View.GONE
            }
        }

        private val list = mutableListOf<Location>()

        fun setList(list: Collection<Location>) {
            this.list.clear()
            this.list.addAll(list)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(
                R.layout.search_item,
                parent, false
            ) as ViewGroup
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(list[position])
        }

        override fun getItemCount() = list.size

        fun updateDistances() {
            for (recyclerView in recyclerViews) {
                for (i in 0 until _adapter.itemCount) {
                    val viewHolder = recyclerView.findViewHolderForAdapterPosition(i)
                    if (viewHolder is ViewHolder)
                        viewHolder.updateDistance()
                }
            }
        }

    }

    data class Location(val name: String, val point: LatLng, val type: Type) {

        enum class Type {
            PLACE, ADDRESS, POI, UNKNOWN
        }
    }

    private val cache = object : LinkedHashMap<String, Collection<Location>>() {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Collection<Location>>?): Boolean {
            return size > CACHE_SIZE
        }
    }

    private val geocodingBuilder = MapboxGeocoding.builder()
        .accessToken(mapboxAccessToken)
        .autocomplete(true)
        .country(COUNTRY)
        .limit(MAX_RESULTS)
        .languages(LANGUAGE)
        .geocodingTypes("district", "place", "locality", "neighborhood", "address", "poi")
        .bbox(boundingBox)


    private fun onResponse(query: String, results: List<CarmenFeature>) {
        val list = results.stream()
            .sorted(compareBy { c -> -c.relevance()!! })
            .map { makeLocation(it.matchingText() ?: it.text()!!, it.center()!!.latlng, it.placeType()!!) }
            .collect(Collectors.toList())
        cache[query] = list
        if (query == lastQuery) {
            _adapter.setList(list)
            onQueryCompleted()
        }
    }

    var userLocation: LatLng? = null
        set(value) {
            if (value != field) {
                field = value
                _adapter.updateDistances()
            }
        }

    private lateinit var lastQuery: String

    fun query(input: String, proximity: Point? = null) {
        var query = input.trim().toLowerCase()
        lastQuery = query
        if (query == "") {
            _adapter.setList(emptyList())
            onQueryCompleted()
        }
        else {
            cache[query].let {
                if (it != null) {
                    _adapter.setList(it)
                    onQueryCompleted()
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
                            if (query == lastQuery)
                                onQueryFailed()
                        }
                    })
                }
            }
        }
    }

    private val _adapter = SearchListAdapter()
    val adapter: RecyclerView.Adapter<*> = _adapter

    val onLocationClick = FuncEvent<Location>()

    val onQueryCompleted = ProcEvent()

    val onQueryFailed = ProcEvent()

}