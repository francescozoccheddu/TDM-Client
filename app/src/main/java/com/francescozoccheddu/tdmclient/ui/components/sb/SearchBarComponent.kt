package com.francescozoccheddu.tdmclient.ui.components.sb


import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.Transition
import androidx.transition.TransitionInflater
import androidx.transition.TransitionManager
import com.francescozoccheddu.tdmclient.R
import com.francescozoccheddu.tdmclient.data.PlaceQuerier
import com.francescozoccheddu.tdmclient.ui.utils.InOutImageButton
import com.francescozoccheddu.tdmclient.ui.utils.LocationSearchProvider
import com.francescozoccheddu.tdmclient.utils.android.setMargins
import com.francescozoccheddu.tdmclient.utils.android.visible
import kotlin.math.max
import kotlin.math.roundToInt

class SearchBarComponent(private val parent: ViewGroup) {

    private val transition = TransitionInflater
        .from(parent.context)
        .inflateTransition(R.transition.sb)
        .apply {
            addListener(object : Transition.TransitionListener {

                override fun onTransitionEnd(transition: Transition) {
                    updateVisibility()
                }

                override fun onTransitionResume(transition: Transition) {
                    updateVisibility()
                }

                override fun onTransitionPause(transition: Transition) {
                    updateVisibility()
                }

                override fun onTransitionCancel(transition: Transition) {
                    updateVisibility()
                }

                override fun onTransitionStart(transition: Transition) {
                    updateVisibility()
                }

            })
        }

    private fun updateVisibility() {
        visible = enabled || card.height > 0
    }

    private val searchProvider = LocationSearchProvider()
    private val editText = parent.findViewById<EditText>(R.id.tg_search_text)
    private val scrim = parent.findViewById<View>(R.id.sb_scrim)
    private val resultList = parent.findViewById<RecyclerView>(R.id.sb_search_results)
    private val card = parent.findViewById<CardView>(R.id.sb_card)
    private val content = parent.findViewById<View>(R.id.sb_content)

    init {

        scrim.setOnClickListener { clearFocus() }

        searchProvider.onLocationClick += {
            editText.text.clear()
            clearFocus()
            onDestinationChosen?.invoke(it)
        }

        searchProvider.onResultsChanged += { update() }

        val closeButton = parent.findViewById<ImageButton>(R.id.tg_search_close)
        val clearButton = parent.findViewById<InOutImageButton>(R.id.sb_clear)
        val progressBar = parent.findViewById<ProgressBar>(R.id.sb_loading)

        resultList.apply {
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            adapter = searchProvider.adapter
        }

        closeButton.setOnClickListener {
            clearFocus()
        }

        clearButton.setOnClickListener {
            editText.text.clear()
        }

        searchProvider.onLoadingChange += { progressBar.visible = it }

        editText.addTextChangedListener(object : TextWatcher {

            override fun afterTextChanged(s: Editable) {
                clearButton.shown = s.isNotEmpty()
                searchProvider.query = s.toString()
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

        })

        val closeAvd = SearchToBackAVD(closeButton)

        editText.setOnFocusChangeListener { _, nowFocused ->
            if (nowFocused && !enabled)
                clearFocus()
            if (!focused) {
                val service = parent.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                service.hideSoftInputFromWindow(editText.windowToken, 0)
            }
            closeButton.isEnabled = focused
            closeAvd.state = if (focused) SearchToBackAVD.State.BACK else SearchToBackAVD.State.SEARCH
            scrim.isClickable = focused
            update()
            onFocusChanged?.invoke(focused)
        }

        scrim.isClickable = false

    }

    val focused get() = editText.hasFocus() && enabled

    private fun update() {
        TransitionManager.beginDelayedTransition(parent, transition)
        resultList.visible = focused
        scrim.alpha = if (focused) 0.4f else 0f
        card.radius =
            if (enabled)
                parent.resources.getDimension(
                    if (focused)
                        R.dimen.sb_focused_radius
                    else
                        R.dimen.sb_radius
                )
            else 0f
        content.visible = enabled
        run {
            val targetMargins = parent.resources.getDimensionPixelOffset(R.dimen.sb_margin)
            val margins = parent.resources.getDimensionPixelOffset(
                if (focused) R.dimen.sb_focused_margin
                else R.dimen.sb_margin
            )
            val targetPadding = parent.resources.getDimension(R.dimen.sb_padding)
            val extraMargins = max(targetMargins - margins, 0) * 0.75f
            val padding = (targetPadding + extraMargins).roundToInt()
            card.setMargins(margins)
            content.setMargins(padding)
        }
        card.alpha = if (focused) 1f else 0.85f
        card.elevation =
            if (focused) parent.resources.getDimension(R.dimen.sb_focused_elevation)
            else if (enabled) parent.resources.getDimension(R.dimen.sb_elevation)
            else 0f
    }

    var enabled = false
        set(value) {
            if (value != field) {
                field = value
                editText.isFocusable = value
                editText.isFocusableInTouchMode = value
                clearFocus()
                update()
                updateVisibility()
            }
        }

    fun clearFocus() {
        editText.clearFocus()
    }

    var onFocusChanged: ((Boolean) -> Unit)? = null

    var visible = false
        private set(value) {
            if (value != field) {
                field = value
                onVisibilityChanged?.invoke(visible)
            }
        }

    var onVisibilityChanged: ((Boolean) -> Unit)? = null

    var onDestinationChosen: ((PlaceQuerier.Location) -> Unit)? = null

    var location
        get() = searchProvider.userLocation
        set(value) {
            searchProvider.userLocation = value
        }

}