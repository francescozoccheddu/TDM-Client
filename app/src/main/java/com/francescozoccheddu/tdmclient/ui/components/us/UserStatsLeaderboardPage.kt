package com.francescozoccheddu.tdmclient.ui.components.us

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.francescozoccheddu.tdmclient.R
import com.francescozoccheddu.tdmclient.data.Leaderboard
import com.francescozoccheddu.tdmclient.data.LeaderboardPosition
import com.francescozoccheddu.tdmclient.utils.android.getStyledString
import com.squareup.picasso.Picasso
import me.everything.android.ui.overscroll.VerticalOverScrollBounceEffectDecorator
import me.everything.android.ui.overscroll.adapters.RecyclerViewOverScrollDecorAdapter


class UserStatsLeaderboardPage(parent: View) {

    private inner class PositionListAdapter :
        RecyclerView.Adapter<PositionListAdapter.ViewHolder>() {

        private inner class ViewHolder(viewGroup: ViewGroup) : RecyclerView.ViewHolder(viewGroup) {

            private val tvName =
                viewGroup.findViewById<TextView>(R.id.uss_leaderboard_item_name)
            private val ivAvatar =
                viewGroup.findViewById<ImageView>(R.id.uss_leaderboard_item_avatar)
            private val tvTitle =
                viewGroup.findViewById<TextView>(R.id.uss_leaderboard_item_title)
            private val tvLevel =
                viewGroup.findViewById<TextView>(R.id.uss_leaderboard_item_level)
            private val tvScore =
                viewGroup.findViewById<TextView>(R.id.uss_leaderboard_item_score)
            private val tvPosition =
                viewGroup.findViewById<TextView>(R.id.uss_leaderboard_item_position)

            fun bind(position: LeaderboardPosition) {
                tvLevel.text =
                    tvLevel.resources.getStyledString(
                        R.string.uss_leaderboard_level,
                        position.level + 1
                    )
                tvTitle.text = position.title
                tvName.text = position.name
                tvScore.text =
                    tvScore.resources.getStyledString(
                        R.string.uss_leaderboard_score,
                        position.score
                    )
                Picasso.get().load(position.avatarUrl).into(ivAvatar)
                val position = adapterPosition
                tvPosition.text = "${position + 1}°"
                val resources = ivAvatar.resources
                val bodySize = resources.getDimension(
                    when (position) {
                        0 -> com.francescozoccheddu.tdmclient.R.dimen.uss_leaderboard_fst_body_size
                        1 -> com.francescozoccheddu.tdmclient.R.dimen.uss_leaderboard_snd_body_size
                        2 -> com.francescozoccheddu.tdmclient.R.dimen.uss_leaderboard_trd_body_size
                        else -> com.francescozoccheddu.tdmclient.R.dimen.uss_leaderboard_nth_body_size
                    }
                )
                ivAvatar.layoutParams.apply {
                    val avatarSize = resources.getDimensionPixelSize(
                        when (position) {
                            0 -> com.francescozoccheddu.tdmclient.R.dimen.uss_leaderboard_fst_avatar_size
                            1 -> com.francescozoccheddu.tdmclient.R.dimen.uss_leaderboard_snd_avatar_size
                            2 -> com.francescozoccheddu.tdmclient.R.dimen.uss_leaderboard_trd_avatar_size
                            else -> com.francescozoccheddu.tdmclient.R.dimen.uss_leaderboard_nth_avatar_size
                        }
                    )
                    height = avatarSize
                }
                tvScore.textSize = bodySize
                tvName.textSize = bodySize
            }

        }

        private val list = mutableListOf<LeaderboardPosition>()

        fun setList(list: Collection<LeaderboardPosition>) {
            this.list.clear()
            this.list.addAll(list)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(
                R.layout.uss_leaderboard_item, parent, false
            ) as ViewGroup
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(list[position])
        }

        override fun getItemCount() = list.size

    }

    private val listAdapter = PositionListAdapter()

    init {
        parent.findViewById<RecyclerView>(R.id.uss_leaderboard_list)
            .apply {
                layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
                adapter = listAdapter
                VerticalOverScrollBounceEffectDecorator(
                    RecyclerViewOverScrollDecorAdapter(this),
                    VerticalOverScrollBounceEffectDecorator.DEFAULT_TOUCH_DRAG_MOVE_RATIO_FWD * 2f,
                    VerticalOverScrollBounceEffectDecorator.DEFAULT_TOUCH_DRAG_MOVE_RATIO_BCK * 2f,
                    VerticalOverScrollBounceEffectDecorator.DEFAULT_DECELERATE_FACTOR
                )
            }
    }

    var leaderboard: Leaderboard = emptyList()
        set(value) {
            if (value != field) {
                field = value
                listAdapter.setList(value)
            }
        }

}