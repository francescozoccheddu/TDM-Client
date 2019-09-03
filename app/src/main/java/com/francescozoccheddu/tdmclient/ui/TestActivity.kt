package com.francescozoccheddu.tdmclient.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.francescozoccheddu.tdmclient.R
import com.francescozoccheddu.tdmclient.ui.components.us.UserStatsSheet

class TestActivity : AppCompatActivity() {

    private lateinit var sheet: UserStatsSheet

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.test_activity)
        sheet = findViewById(R.id.sheet)
        sheet.onOpened()
    }
}
