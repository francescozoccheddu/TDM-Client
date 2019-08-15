package com.francescozoccheddu.tdmclient.ui.topgroup

import android.animation.LayoutTransition
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.cardview.widget.CardView
import androidx.constraintlayout.motion.widget.MotionLayout
import com.francescozoccheddu.tdmclient.R
import com.francescozoccheddu.tdmclient.ui.utils.GroupStateManager
import kotlinx.android.synthetic.main.tg.view.tg_root
import kotlinx.android.synthetic.main.tg.view.tg_score
import kotlinx.android.synthetic.main.tg.view.tg_scrim
import kotlinx.android.synthetic.main.tg.view.tg_search


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

    private val layoutChangingTransition = LayoutTransition().apply {
        enableTransitionType(LayoutTransition.CHANGING)
        disableTransitionType(LayoutTransition.CHANGE_APPEARING)
        disableTransitionType(LayoutTransition.CHANGE_DISAPPEARING)
        disableTransitionType(LayoutTransition.DISAPPEARING)
        disableTransitionType(LayoutTransition.APPEARING)
        setDuration(200L)
        setInterpolator(LayoutTransition.CHANGING, DecelerateInterpolator())
    }

    init {
        View.inflate(context, R.layout.tg, this)
        root = tg_root
        loadLayoutDescription(R.xml.tg_motion)
        stateManager = GroupStateManager(this, State.HIDDEN)
        stateManager.onTransitionCompleted = {
            if (stateManager.state == State.SEARCHING || stateManager.state == State.SEARCH && layoutTransition == null)
                root.layoutTransition = layoutChangingTransition
        }
        tg_search.onFocusChanged = {
            if (state == State.SEARCHING || state == State.SEARCH) {
                state = if (it) State.SEARCHING else State.SEARCH
            }
        }
        tg_scrim.setOnClickListener {
            if (state == State.SEARCHING)
                tg_search.clearTextFocus()
        }
    }

    val search = tg_search
    val score = tg_score

    var state: State
        get() = stateManager.state
        set(value) {
            if (value != state && value != State.SEARCH && value != State.SEARCHING)
                root.layoutTransition = null
            tg_scrim.isClickable = value == State.SEARCHING
            stateManager.state = value
        }

}


