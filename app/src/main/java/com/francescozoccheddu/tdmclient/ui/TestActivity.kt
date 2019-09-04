package com.francescozoccheddu.tdmclient.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.francescozoccheddu.tdmclient.R
import com.francescozoccheddu.tdmclient.data.LeaderboardPosition
import com.francescozoccheddu.tdmclient.data.UserStats
import com.francescozoccheddu.tdmclient.ui.components.us.UserStatsSheet

class TestActivity : AppCompatActivity() {

    private lateinit var sheet: UserStatsSheet

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.test_activity)
        sheet = findViewById(R.id.sheet)
        sheet.stats =
            UserStats(69, 2, 1.2f, 1500, 2, "Aldo cazzi", "https://robohash.org/e", "Muratore")
        sheet.leaderboard = listOf(
            LeaderboardPosition(0, "Zero", "Cazzone", "https://robohash.org/e", 495, 4),
            LeaderboardPosition(1, "Uno", "Cazzone", "https://robohash.org/c", 354, 4),
            LeaderboardPosition(2, "Due", "Cazzone", "https://robohash.org/d", 233, 3),
            LeaderboardPosition(3, "Tre", "Cazzone", "https://robohash.org/a", 221, 3),
            LeaderboardPosition(4, "Quattro", "Cazzone", "https://robohash.org/c", 109, 2),
            LeaderboardPosition(5, "Cinque", "Cazzone", "https://robohash.org/a", 87, 1),
            LeaderboardPosition(5, "Cinque", "Cazzone", "https://robohash.org/a", 87, 1),
            LeaderboardPosition(5, "Cinque", "Cazzone", "https://robohash.org/a", 87, 1),
            LeaderboardPosition(5, "Cinque", "Cazzone", "https://robohash.org/a", 87, 1),
            LeaderboardPosition(5, "Cinque", "Cazzone", "https://robohash.org/a", 87, 1),
            LeaderboardPosition(5, "Cinque", "Cazzone", "https://robohash.org/a", 87, 1),
            LeaderboardPosition(5, "Cinque", "Cazzone", "https://robohash.org/a", 87, 1),
            LeaderboardPosition(5, "Cinque", "Cazzone", "https://robohash.org/a", 87, 1)

        )
        sheet.onOpened()
    }
}
