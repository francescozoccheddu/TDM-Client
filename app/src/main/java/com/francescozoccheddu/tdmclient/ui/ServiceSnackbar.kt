package com.francescozoccheddu.tdmclient.ui

import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.francescozoccheddu.tdmclient.R
import com.francescozoccheddu.tdmclient.utils.commons.ProcEvent
import com.google.android.material.snackbar.Snackbar

class ServiceSnackbar(val layout: CoordinatorLayout) {

    enum class State {
        LOCATING, UNLOCATABLE, PERMISSIONS_UNGRANTED, ROUTING, OFFLINE
    }

    private var snackbar: Snackbar? = null

    private fun setStateSnackbar() {
        val state = this.state
        if (state != null)
            snackbar = when (state) {
                State.LOCATING -> Snackbar.make(layout, R.string.snackbar_locating, Snackbar.LENGTH_INDEFINITE)
                State.UNLOCATABLE -> Snackbar.make(layout, R.string.snackbar_unlocatable, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.snackbar_action_unlocatable) {
                        this.state = null
                        onLocationEnableRequest()
                    }
                State.PERMISSIONS_UNGRANTED -> Snackbar.make(
                    layout, R.string.snackbar_permissions_ungranted, Snackbar.LENGTH_INDEFINITE
                )
                    .setAction(R.string.snackbar_action_permissions_ungranted) {
                        this.state = null
                        onPermissionAskRequest()
                    }
                State.ROUTING -> Snackbar.make(layout, R.string.snackbar_routing, Snackbar.LENGTH_INDEFINITE)
                State.OFFLINE -> Snackbar.make(layout, R.string.snackbar_offline, Snackbar.LENGTH_INDEFINITE)
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

    fun notifyRoutingFailure(retryCallback: ((Unit) -> Unit)?) {
        if (!routingFailure) {
            routingFailure = true
        }
    }

}