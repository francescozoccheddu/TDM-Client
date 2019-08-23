package com.francescozoccheddu.tdmclient.ui.utils

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.francescozoccheddu.tdmclient.R
import com.francescozoccheddu.tdmclient.data.PlaceQuerier
import com.francescozoccheddu.tdmclient.utils.commons.FuncEvent
import com.francescozoccheddu.tdmclient.utils.commons.ProcEvent
import com.francescozoccheddu.tdmclient.utils.commons.snap
import com.francescozoccheddu.tdmclient.utils.data.travelDuration
import com.mapbox.mapboxsdk.geometry.LatLng


class LocationSearchProvider {

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

            private val tvName = viewGroup.findViewById<TextView>(R.id.sb_item_name)
            private val ivIcon = viewGroup.findViewById<ImageView>(R.id.sb_item_icon)
            private val tvDistance = viewGroup.findViewById<TextView>(R.id.sb_item_distance)

            fun bind(location: PlaceQuerier.Location) {
                tvName.text = location.name
                ivIcon.setImageResource(
                    when (location.type) {
                        PlaceQuerier.Location.Type.ADDRESS -> R.drawable.sb_map
                        PlaceQuerier.Location.Type.PLACE -> R.drawable.sb_terrain
                        PlaceQuerier.Location.Type.POI -> R.drawable.place
                        PlaceQuerier.Location.Type.UNKNOWN -> R.drawable.place
                    }
                )
                updateDistance()
            }

            fun updateDistance() {
                val a = list[adapterPosition].point
                val b = userLocation
                if (b != null) {
                    tvDistance.text = run {
                        val tm = (travelDuration(a.distanceTo(b).toFloat()) / 60f)
                        val m = tm.snap(if (tm < 3) 1f else if (tm < 30f) 5f else 15f).toInt()
                        val h = m / 60
                        if (h > 0) "${h}h ${m % 60}m"
                        else if (m > 0) "${m}m"
                        else "<1m"
                    }
                    tvDistance.visibility = View.VISIBLE
                }
                else
                    tvDistance.visibility = View.GONE
            }
        }

        private val list = mutableListOf<PlaceQuerier.Location>()

        fun setList(list: Collection<PlaceQuerier.Location>) {
            this.list.clear()
            this.list.addAll(list)
            notifyDataSetChanged()
            onResultsChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(
                R.layout.sb_item,
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

    private val geocoder = PlaceQuerier().apply {
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

    val hasResults get() = _adapter.itemCount > 0

    val onLocationClick = FuncEvent<PlaceQuerier.Location>()

    val onLoadingChange = FuncEvent<Boolean>()

    val loading = geocoder.loading

    val onResultsChanged = ProcEvent()

}