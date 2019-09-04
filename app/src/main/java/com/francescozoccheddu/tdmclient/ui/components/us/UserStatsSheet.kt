package com.francescozoccheddu.tdmclient.ui.components.us

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import com.francescozoccheddu.tdmclient.R
import com.francescozoccheddu.tdmclient.ui.utils.InOutImageButton
import com.francescozoccheddu.tdmclient.utils.commons.event
import com.francescozoccheddu.tdmclient.utils.commons.invoke

class UserStatsSheet @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    init {
        View.inflate(context, R.layout.uss, this)
    }

    private val profilePage = UserStatsProfilePage(this)
    private val leaderboardPage = UserStatsLeaderboardPage(this)

    private val closeButton = findViewById<InOutImageButton>(R.id.uss_close).apply {
        setOnClickListener {
            onCloseRequested()
        }
    }

    var onCloseRequested = event()

    fun onOpened() {
        closeButton.show()
    }

    fun onClosed() {
        profilePage.hideHelp()
        closeButton.hide()
    }

    var stats
        get() = profilePage.stats
        set(value) {
            profilePage.stats = value
        }

    var leaderboard
        get() = leaderboardPage.leaderboard
        set(value) {
            leaderboardPage.leaderboard = value
        }

}