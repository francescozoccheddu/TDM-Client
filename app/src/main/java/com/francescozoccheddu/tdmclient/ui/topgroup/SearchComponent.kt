package com.francescozoccheddu.tdmclient.ui.topgroup


import android.animation.LayoutTransition
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.francescozoccheddu.tdmclient.R
import com.francescozoccheddu.tdmclient.data.PlaceQuerier
import com.francescozoccheddu.tdmclient.ui.utils.LocationSearchProvider
import com.francescozoccheddu.tdmclient.utils.android.visible
import kotlinx.android.synthetic.main.tg_search.view.tg_search_clear
import kotlinx.android.synthetic.main.tg_search.view.tg_search_close
import kotlinx.android.synthetic.main.tg_search.view.tg_search_loading
import kotlinx.android.synthetic.main.tg_search.view.tg_search_results
import kotlinx.android.synthetic.main.tg_search.view.tg_search_text

class SearchComponent @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {


    private val searchProvider = LocationSearchProvider()

    init {
        orientation = VERTICAL
        View.inflate(context, R.layout.tg_search, this)
        searchProvider.onLocationClick += {
            tg_search_text.text.clear()
            tg_search_text.clearFocus()
            onDestinationChosen?.invoke(it)
        }

        tg_search_results.apply {
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            adapter = searchProvider.adapter
        }

        tg_search_close.setOnClickListener {
            tg_search_text.clearFocus()
        }

        tg_search_clear.setOnClickListener {
            tg_search_text.text.clear()
        }

        searchProvider.onLoadingChange += { tg_search_loading.visible = it }

        tg_search_text.addTextChangedListener(object : TextWatcher {

            override fun afterTextChanged(s: Editable) {
                tg_search_clear.visible = s.length > 0
                searchProvider.query = s.toString()
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

        })

        tg_search_text.setOnFocusChangeListener { _, focused ->
            tg_search_close.isEnabled = focused
            if (!focused) {
                val service = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                service.hideSoftInputFromWindow(tg_search_text.windowToken, 0)
            }
            onFocusChanged?.invoke(focused)
        }

        layoutTransition = LayoutTransition().apply {
            enableTransitionType(LayoutTransition.CHANGING)
        }

    }

    fun clearTextFocus() {
        tg_search_text.clearFocus()
    }

    var onFocusChanged: ((Boolean) -> Unit)? = null

    var onDestinationChosen: ((PlaceQuerier.Location) -> Unit)? = null


    var location
        get() = searchProvider.userLocation
        set(value) {
            searchProvider.userLocation = value
        }

}