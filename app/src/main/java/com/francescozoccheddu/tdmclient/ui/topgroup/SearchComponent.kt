package com.francescozoccheddu.tdmclient.ui.topgroup


import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.RelativeLayout
import com.francescozoccheddu.animatorhelpers.ABFloat
import com.francescozoccheddu.tdmclient.R
import com.francescozoccheddu.tdmclient.ui.GroupStateManager
import com.francescozoccheddu.tdmclient.utils.android.visible

class SearchComponent @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), GroupStateManager.GroupComponent {


    init {
        orientation = VERTICAL
        View.inflate(context, R.layout.tg_search, this)
    }

    private var animationAlpha by ABFloat(if (visible) 1f else 0f).apply {
        onUpdate = {
            alpha = it.value
            visible = it.value != 0f
            if (!it.running) {
                animationCallback?.invoke()
                animationCallback = null
            }
        }
        speed = 6f
    }

    private var animationCallback: (() -> Unit)? = null

    override fun animate(mode: GroupStateManager.GroupComponent.Mode, callback: (() -> Unit)?) {
        this.animationCallback = callback
        animationAlpha = when (mode) {
            GroupStateManager.GroupComponent.Mode.IN -> 1f
            GroupStateManager.GroupComponent.Mode.OUT -> 0f
        }
    }

}