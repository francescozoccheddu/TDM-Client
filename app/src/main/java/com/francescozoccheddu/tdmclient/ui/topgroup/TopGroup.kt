package com.francescozoccheddu.tdmclient.ui.topgroup

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.cardview.widget.CardView
import androidx.constraintlayout.motion.widget.MotionLayout
import com.francescozoccheddu.tdmclient.R
import com.francescozoccheddu.tdmclient.ui.GroupStateManager
import kotlinx.android.synthetic.main.tg.view.tg_root


class TopGroup @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : MotionLayout(context, attrs, defStyleAttr) {


    enum class State(override val constraintSetId: Int, override val componentId: Int?) : GroupStateManager.GroupState {
        SCORE(R.id.tg_cs_score, R.id.tg_score),
        SEARCH(R.id.tg_cs_search, R.id.tg_search),
        SEARCHING(R.id.tg_cs_searching, R.id.tg_search),
        HIDDEN(R.id.tg_cs_hidden, R.id.tg_score)
    }

    private val root: CardView
    private val stateManager: GroupStateManager<State>

    init {
        View.inflate(context, R.layout.tg, this)
        root = tg_root
        loadLayoutDescription(R.xml.tg_motion)
        stateManager = GroupStateManager(this, State.HIDDEN)

    }

    var state: State
        get() = stateManager.state
        set(value) {
            stateManager.state = value
        }

}


