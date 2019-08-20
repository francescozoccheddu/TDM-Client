package com.francescozoccheddu.tdmclient.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import com.francescozoccheddu.tdmclient.R
import com.francescozoccheddu.tdmclient.utils.commons.event
import com.francescozoccheddu.tdmclient.utils.commons.invoke

class UserStatsSheet @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {


    init {
        View.inflate(context, R.layout.uss, this)
    }

    private val closeButton = findViewById<InOutImageButton>(R.id.uss_close).apply {
        setOnClickListener { onClose() }
    }

    var onClose = event()

    fun onOpened() {
        closeButton.show()
    }

    fun onClosed() {
        closeButton.hide()
    }


}