package com.francescozoccheddu.tdmclient.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.francescozoccheddu.tdmclient.R
import com.francescozoccheddu.tdmclient.ui.topgroup.TopGroup
import kotlinx.android.synthetic.main.activity_test.ma_tg
import kotlin.random.Random

class TestActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)
    }

    fun transition(view: View) {
        ma_tg.state = TopGroup.State.values()[Random.nextInt(4)]
        println(ma_tg.state)
    }

}

