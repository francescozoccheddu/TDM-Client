package com.francescozoccheddu.tdmclient.ui.components.us

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
import com.francescozoccheddu.tdmclient.ui.utils.InOutImageButton
import com.francescozoccheddu.tdmclient.utils.commons.ProcEvent
import com.francescozoccheddu.tdmclient.utils.commons.event
import com.francescozoccheddu.tdmclient.utils.commons.invoke
import me.everything.android.ui.overscroll.HorizontalOverScrollBounceEffectDecorator
import me.everything.android.ui.overscroll.VerticalOverScrollBounceEffectDecorator
import me.everything.android.ui.overscroll.adapters.ViewPagerOverScrollDecorAdapter

class UserStatsSheet @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    class LeaderboardPageFragment : Fragment() {

        lateinit var sheet: UserStatsSheet

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            val root = inflater.inflate(R.layout.uss_leaderboard, container, false)
            sheet.leaderboardPage = UserStatsLeaderboardPage(root)
            sheet.leaderboardPage.leaderboard = sheet.leaderboard
            return root
        }
    }

    class ProfilePageFragment : Fragment() {

        lateinit var sheet: UserStatsSheet

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            val root = inflater.inflate(R.layout.uss_profile, container, false)
            sheet.profilePage = UserStatsProfilePage(root)
            sheet.profilePage.stats = sheet.stats
            return root
        }
    }

    private inner class PagerAdapter(fm: FragmentManager) :
        FragmentStatePagerAdapter(fm) {
        override fun getCount(): Int = 2

        override fun getItem(position: Int): Fragment = when (position) {
            0 -> ProfilePageFragment().apply { sheet = this@UserStatsSheet }
            1 -> LeaderboardPageFragment().apply { sheet = this@UserStatsSheet }
            else -> throw IllegalArgumentException()
        }
    }

    init {
        View.inflate(context, R.layout.uss, this)
        findViewById<ViewPager>(R.id.uss_pager).apply {
            adapter = PagerAdapter((context as AppCompatActivity).supportFragmentManager)
            HorizontalOverScrollBounceEffectDecorator(
                ViewPagerOverScrollDecorAdapter(this),
                VerticalOverScrollBounceEffectDecorator.DEFAULT_TOUCH_DRAG_MOVE_RATIO_FWD * 4f,
                VerticalOverScrollBounceEffectDecorator.DEFAULT_TOUCH_DRAG_MOVE_RATIO_BCK * 4f,
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
                        0 -> onUserStatsRequested()
                        1 -> onLeaderboardRequested()
                    }
                }

            })
        }
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
        onUserStatsRequested()
        onLeaderboardRequested()
    }

    fun onClosed() {
        profilePage.hideHelp()
        closeButton.hide()
    }

    val onUserStatsRequested = ProcEvent()
    val onLeaderboardRequested = ProcEvent()

    var stats = UserStats(0, 0, 1f, null, 0, "", "", "")
        set(value) {
            field = value
            if (this::profilePage.isInitialized)
                profilePage.stats = value
        }

    var leaderboard: Leaderboard = emptyList()
        set(value) {
            field = value
            if (this::leaderboardPage.isInitialized)
                leaderboardPage.leaderboard = value
        }

}