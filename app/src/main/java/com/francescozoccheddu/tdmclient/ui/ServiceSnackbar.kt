package ui

import android.widget.ImageView
import android.widget.ProgressBar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import com.francescozoccheddu.tdmclient.R
import com.francescozoccheddu.tdmclient.utils.android.addView
import com.francescozoccheddu.tdmclient.utils.android.setBackgroundColorRes
import com.francescozoccheddu.tdmclient.utils.commons.ProcEvent
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import kotlin.math.roundToInt


class ServiceSnackbar(val layout: CoordinatorLayout) {

    private companion object {
        private fun Snackbar.addIcon(icon: Int): Snackbar {
            val item = ImageView(context)
            item.setImageResource(icon)
            return addView(item)
        }

        private fun Snackbar.addLoading(): Snackbar {
            val item = ProgressBar(context)
            val size = context.resources.getDimension(R.dimen.snackbar_loading_size).roundToInt()
            return addView(item, size)
        }

        private fun Snackbar.setColorRes(color: Int): Snackbar {
            val foreground = ContextCompat.getColor(context, R.color.foreground)
            setTextColor(foreground)
            setActionTextColor(foreground)
            setBackgroundColorRes(color)
            return this
        }
    }

    enum class State {
        LOCATING, UNLOCATABLE, PERMISSIONS_UNGRANTED, ROUTING, OFFLINE, OUTSIDE_AREA
    }

    private fun make(text: Int, transient: Boolean = false) =
        Snackbar.make(layout, text, if (transient) Snackbar.LENGTH_SHORT else Snackbar.LENGTH_INDEFINITE)

    private var snackbar: Snackbar? = null

    private fun setStateSnackbar() {
        val state = this.state
        if (state != null)
            snackbar = when (state) {
                State.LOCATING -> make(R.string.snackbar_locating)
                    .setColorRes(R.color.colorPrimary)
                    .addLoading()
                State.UNLOCATABLE -> make(R.string.snackbar_unlocatable)
                    .setColorRes(R.color.semantic_error)
                    .setAction(R.string.snackbar_action_unlocatable) {
                        this.state = null
                        onLocationEnableRequest()
                    }
                State.PERMISSIONS_UNGRANTED -> make(R.string.snackbar_permissions_ungranted)
                    .setColorRes(R.color.semantic_error)
                    .setAction(R.string.snackbar_action_permissions_ungranted) {
                        this.state = null
                        onPermissionAskRequest()
                    }
                State.ROUTING -> make(R.string.snackbar_routing)
                    .setColorRes(R.color.colorPrimary)
                    .addLoading()
                State.OFFLINE -> make(R.string.snackbar_offline)
                    .setColorRes(R.color.semantic_error)
                State.OUTSIDE_AREA -> make(R.string.snackbar_outside_area)
                    .setColorRes(R.color.semantic_error)
            }.apply {
                show()
            }
        else
            snackbar?.dismiss()
    }

    var state: State? = null
        set(value) {
            if (value != field) {
                field = value
                if (!routingFailure)
                    setStateSnackbar()
            }
        }

    val onPermissionAskRequest = ProcEvent()

    val onLocationEnableRequest = ProcEvent()

    private var routingFailure = false

    fun notifyRoutingFailure(retryCallback: (() -> Unit)?) {
        routingFailure = true
        snackbar = make(R.string.snackbar_routing_failed, true).apply {
            addCallback(object : BaseTransientBottomBar.BaseCallback<Snackbar>() {

                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                    if (snackbar == transientBottomBar) {
                        routingFailure = false
                        setStateSnackbar()
                    }
                }

            })
            setColorRes(R.color.semantic_error)
            if (retryCallback != null)
                setAction(R.string.snackbar_routing_failed) { retryCallback() }
            show()
        }
    }

}