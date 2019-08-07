package com.francescozoccheddu.tdmclient.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.francescozoccheddu.tdmclient.R
import com.francescozoccheddu.tdmclient.data.Geocoder
import com.francescozoccheddu.tdmclient.utils.commons.FuncEvent
import com.francescozoccheddu.tdmclient.utils.data.travelDuration
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import kotlin.math.roundToInt


class LocationSearchProvider(bounds: LatLngBounds) {

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

            fun bind(location: Geocoder.Location) {
                tvName.text = location.name
                ivIcon.setImageResource(
                    when (location.type) {
                        Geocoder.Location.Type.ADDRESS -> R.drawable.ic_map
                        Geocoder.Location.Type.PLACE -> R.drawable.ic_terrain
                        Geocoder.Location.Type.POI -> R.drawable.ic_place
                        Geocoder.Location.Type.UNKNOWN -> R.drawable.ic_place
                    }
                )
                updateDistance()
            }

            fun updateDistance() {
                val a = list[adapterPosition].point
                val b = userLocation
                if (b != null) {
                    val minutes = (travelDuration(a.distanceTo(b).toFloat()) / 60f).roundToInt()
                    tvDistance.text = if (minutes < 1) "<1m" else "${minutes}m"
                    tvDistance.visibility = View.VISIBLE
                }
                else
                    tvDistance.visibility = View.GONE
            }
        }

        private val list = mutableListOf<Geocoder.Location>()

        fun setList(list: Collection<Geocoder.Location>) {
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

    var userLocation: LatLng? = null
        set(value) {
            if (value != field) {
                field = value
                _adapter.updateDistances()
            }
        }

    private val geocoder = Geocoder(bounds).apply {
        onResult += { _adapter.setList(it.result!!) }
        onLoadingChange += { this@LocationSearchProvider.onLoadingChange(it.loading) }
    }

    var query: String = ""
        set(value) {
            field = value
            geocoder.query = value
        }

    private val _adapter = SearchListAdapter()
    val adapter: RecyclerView.Adapter<*> = _adapter

    val onLocationClick = FuncEvent<Geocoder.Location>()

    val onLoadingChange = FuncEvent<Boolean>()

    val loading = geocoder.loading

}