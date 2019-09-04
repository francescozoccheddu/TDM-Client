package com.francescozoccheddu.tdmclient.ui.components.us

import android.app.Activity
import android.content.Context
import android.graphics.drawable.Animatable2
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager
import com.francescozoccheddu.tdmclient.R
import com.francescozoccheddu.tdmclient.data.Leaderboard
import com.francescozoccheddu.tdmclient.data.UserStats
import com.francescozoccheddu.tdmclient.ui.MainService
import com.francescozoccheddu.tdmclient.ui.utils.InOutImageButton
import com.francescozoccheddu.tdmclient.utils.commons.event
import com.francescozoccheddu.tdmclient.utils.commons.invoke
import com.google.android.material.tabs.TabLayout
import me.everything.android.ui.overscroll.HorizontalOverScrollBounceEffectDecorator
import me.everything.android.ui.overscroll.VerticalOverScrollBounceEffectDecorator
import me.everything.android.ui.overscroll.adapters.ViewPagerOverScrollDecorAdapter

class UserStatsSheet @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    class LeaderboardPageFragment : Fragment() {

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ) = inflater.inflate(R.layout.uss_leaderboard, container, false)

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            val sheet = (context as Activity).findViewById<UserStatsSheet>(R.id.us_sheet_root)
            sheet.leaderboardPage = UserStatsLeaderboardPage(view)
            sheet.leaderboardPage.leaderboard = sheet.leaderboard
        }

    }

    class ProfilePageFragment : Fragment() {

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ) = inflater.inflate(R.layout.uss_profile, container, false)

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            val sheet = (context as Activity).findViewById<UserStatsSheet>(R.id.us_sheet_root)
            sheet.profilePage = UserStatsProfilePage(view)
            sheet.profilePage.stats = sheet.stats
        }

    }

    private inner class PagerAdapter(fm: FragmentManager) :
        FragmentStatePagerAdapter(fm) {
        override fun getCount(): Int = 2

        override fun getItem(position: Int): Fragment = when (position) {
            0 -> ProfilePageFragment()
            1 -> LeaderboardPageFragment()
            else -> throw IllegalArgumentException()
        }

        override fun getPageTitle(position: Int) = when (position) {
            0 -> resources.getString(R.string.uss_profile_page)
            1 -> resources.getString(R.string.uss_leaderboard_page)
            else -> null
        }

    }

    init {
        View.inflate(context, R.layout.uss, this)
        val pager = findViewById<ViewPager>(R.id.uss_pager).apply {
            adapter = PagerAdapter((context as AppCompatActivity).supportFragmentManager)
            HorizontalOverScrollBounceEffectDecorator(
                ViewPagerOverScrollDecorAdapter(this),
                VerticalOverScrollBounceEffectDecorator.DEFAULT_TOUCH_DRAG_MOVE_RATIO_FWD * 2f,
                VerticalOverScrollBounceEffectDecorator.DEFAULT_TOUCH_DRAG_MOVE_RATIO_BCK * 2f,
                VerticalOverScrollBounceEffectDecorator.DEFAULT_DECELERATE_FACTOR
            )
            addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                override fun onPageScrollStateChanged(state: Int) {
                }

                override fun onPageScrolled(
                    position: Int,
                    positionOffset: Float,
                    positionOffsetPixels: Int
                ) {
                }

                override fun onPageSelected(position: Int) {
                    when (position) {
                        0 -> requestUserStats()
                        1 -> requestLeaderboard()
                    }
                }

            })
        }
        findViewById<TabLayout>(R.id.uss_tabs).setupWithViewPager(pager)
        (findViewById<ImageView>(R.id.uss_coins).drawable as Animatable2).apply {
            start()
            registerAnimationCallback(object : Animatable2.AnimationCallback() {

                override fun onAnimationEnd(drawable: Drawable?) {
                    start()
                }

            })
        }
    }

    private lateinit var profilePage: UserStatsProfilePage
    private lateinit var leaderboardPage: UserStatsLeaderboardPage

    private val closeButton = findViewById<InOutImageButton>(R.id.uss_close).apply {
        setOnClickListener {
            onCloseRequested()
        }
    }

    var onCloseRequested = event()

    fun onOpened() {
        closeButton.show()
        requestLeaderboard()
        requestUserStats()
    }

    private fun requestLeaderboard() {
        MainService.instance?.dataRetriever?.getLeaderboard {
            if (it != null)
                leaderboard = it
        }
    }

    private fun requestUserStats() {
        MainService.instance?.userController?.requestStatsUpdate {
            if (it != null)
                stats = it
        }
    }

    fun onClosed() {
        profilePage.abortAll()
        closeButton.hide()
    }

    var stats = UserStats(0, 0, 1f, null, 0, "", "", "")
        set(value) {
            field = value
            if (this::profilePage.isInitialized)
                profilePage.stats = value
        }

    private var leaderboard: Leaderboard = emptyList()
        set(value) {
            field = value
            if (this::leaderboardPage.isInitialized)
                leaderboardPage.leaderboard = value
        }

}