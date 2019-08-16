package com.francescozoccheddu.tdmclient.ui.topgroup

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.cardview.widget.CardView
import com.francescozoccheddu.tdmclient.R
import com.francescozoccheddu.tdmclient.ui.utils.GroupStateManager
import kotlinx.android.synthetic.main.tg.view.tg_root
import kotlinx.android.synthetic.main.tg.view.tg_score
import kotlinx.android.synthetic.main.tg.view.tg_scrim
import kotlinx.android.synthetic.main.tg.view.tg_search


class TopGroup @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {


    enum class State(override val componentId: Int?) : GroupStateManager.GroupState {
        SCORE(R.id.tg_score),
        SEARCH(R.id.tg_search),
        SEARCHING(R.id.tg_search),
        HIDDEN(R.id.tg_score)
    }

    private val root: CardView
    private val stateManager: GroupStateManager<State>

    init {
        View.inflate(context, R.layout.tg, this)
        root = tg_root
        stateManager = GroupStateManager(root, State.HIDDEN)
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


