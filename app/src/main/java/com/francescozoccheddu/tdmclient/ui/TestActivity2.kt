package com.francescozoccheddu.tdmclient.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.francescozoccheddu.tdmclient.R
import com.francescozoccheddu.tdmclient.utils.commons.FixedSizeSortedQueue
import kotlinx.android.synthetic.main.testactivity2.cb_reversed
import kotlinx.android.synthetic.main.testactivity2.np_input
import kotlinx.android.synthetic.main.testactivity2.tv_queue

class TestActivity2 : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.testactivity2)
        np_input.minValue = 0
        np_input.maxValue = 10
        update()
    }

    private val queue = FixedSizeSortedQueue.by<Int>(10, false)

    private val input get() = np_input.value
    private val reversed get() = cb_reversed.isChecked

    fun update() {
        tv_queue.text = queue.print()
    }

    fun add(view: View) {
        queue.add(input, reversed)
        update()
    }

    fun remove(view: View) {
        val iterator = queue.mutableIterator(reversed)
        for (i in iterator)
            if (i == input)
                iterator.remove()
        update()
    }

    fun removeAll(view: View) {
        val iterator = queue.mutableIterator(reversed)
        for (i in iterator)
            iterator.remove()
        update()
    }


}