package com.francescozoccheddu.tdmclient.ui.bottomgroup

import androidx.core.content.ContextCompat
import com.francescozoccheddu.tdmclient.R
import com.francescozoccheddu.tdmclient.utils.android.Timer

class BottomGroupController(val group: BottomGroup) {

    enum class State {
        LOCATING, UNLOCATABLE, PERMISSIONS_UNGRANTED, ROUTING, HIDDEN,
        OFFLINE, OUTSIDE_AREA, PICKING_DESTINATION, CONFIRMING_DESTINATION, IDLE,
        CHOOSING_WALK_MODE, CHOOSING_DURATION, ROUTED
    }

    var destinationName: String? = null
        set(value) {
            if (value != field) {
                field = value
                if (state == State.CONFIRMING_DESTINATION)
                    updateState()
            }
        }

    private fun getString(resId: Int) = group.resources.getString(resId)
    private fun getColor(resId: Int) = ContextCompat.getColor(group.context, resId)

    private val notifyCountdown = Timer().Countdown().apply {
        time = 5f
        runnable = Runnable { routingFailed = false }
    }

    private var routingFailed = false
        set(value) {
            if (value != field) {
                field = value
                if (value)
                    notifyCountdown.pull()
                updateState()
            }
        }

    var state: State = State.HIDDEN
        set(value) {
            if (value != field) {
                field = value
                updateState()
            }
        }

    private fun updateState() {
        if (routingFailed && state != State.HIDDEN) {
            group.mode = BottomGroup.Mode.INFO
            group.color = getColor(R.color.backgroundError)
            group.info.text = getString(R.string.snackbar_routing_failed)
            group.info.icon = R.drawable.ic_warning
            group.modal = false
        }
        else {
            group.mode = when (state) {
                State.LOCATING, State.UNLOCATABLE, State.PERMISSIONS_UNGRANTED,
                State.ROUTING, State.OFFLINE, State.OUTSIDE_AREA -> BottomGroup.Mode.INFO
                State.HIDDEN -> BottomGroup.Mode.HIDDEN
                State.PICKING_DESTINATION, State.IDLE, State.ROUTED -> BottomGroup.Mode.ACTION
                State.CONFIRMING_DESTINATION -> if (destinationName != null) BottomGroup.Mode.INFO else BottomGroup.Mode.ACTION
                State.CHOOSING_DURATION -> BottomGroup.Mode.DURATION
                State.CHOOSING_WALK_MODE -> BottomGroup.Mode.WALK
            }
            group.color = when (state) {
                State.UNLOCATABLE, State.PERMISSIONS_UNGRANTED, State.OFFLINE -> getColor(R.color.backgroundError)
                State.LOCATING, State.ROUTING, State.IDLE, State.CHOOSING_WALK_MODE,
                State.CHOOSING_DURATION -> getColor(R.color.background)
                State.HIDDEN -> group.color
                State.OUTSIDE_AREA, State.PICKING_DESTINATION, State.ROUTED -> getColor(R.color.backgroundWarning)
                State.CONFIRMING_DESTINATION -> getColor(R.color.backgroundOk)
            }
            group.clickableInfo = when (state) {
                State.UNLOCATABLE, State.PERMISSIONS_UNGRANTED, State.ROUTING, State.CONFIRMING_DESTINATION -> true
                else -> false
            }
            group.info.text = when (state) {
                State.LOCATING -> getString(R.string.snackbar_locating)
                State.UNLOCATABLE -> getString(R.string.snackbar_unlocatable)
                State.PERMISSIONS_UNGRANTED -> getString(R.string.snackbar_permissions_ungranted)
                State.ROUTING -> getString(R.string.snackbar_routing)
                State.OFFLINE -> getString(R.string.snackbar_offline)
                State.OUTSIDE_AREA -> getString(R.string.snackbar_outside_area)
                State.CONFIRMING_DESTINATION
                -> destinationName ?: group.info.text
                else -> group.info.text
            }
            group.info.loading = when (state) {
                State.LOCATING, State.ROUTING -> true
                else -> false
            }
            group.info.icon = when (state) {
                State.UNLOCATABLE -> R.drawable.ic_unlocatable
                State.PERMISSIONS_UNGRANTED, State.OFFLINE, State.OUTSIDE_AREA -> R.drawable.ic_warning
                State.CONFIRMING_DESTINATION -> R.drawable.ic_place
                else -> group.info.icon
            }
            group.modal = when (state) {
                State.CHOOSING_DURATION, State.CHOOSING_WALK_MODE -> true
                else -> false
            }
        }
    }

    var onRetryRouting: (() -> Unit)? = null
    var onConfirmRouting: (() -> Unit)? = null
    var onCancelRouting: (() -> Unit)? = null
    var onSelectRoutingMode: ((WalkComponent.RoutingMode) -> Unit)? = null
    var onWalkIntent: (() -> Unit)? = null
    var onLocationEnableIntent: (() -> Unit)? = null
    var onPermissionGrantIntent: (() -> Unit)? = null
    var onDestinationConfirmed: (() -> Unit)? = null

    fun notifyRoutingFailure() {
        routingFailed = true
    }

    init {
        updateState()
        group.onClick = {
            if (routingFailed)
                onRetryRouting?.invoke()
            else {
                when (state) {
                    State.UNLOCATABLE -> onLocationEnableIntent?.invoke()
                    State.PERMISSIONS_UNGRANTED -> onPermissionGrantIntent?.invoke()
                    State.ROUTING, State.PICKING_DESTINATION, State.ROUTED -> onCancelRouting?.invoke()
                    State.CONFIRMING_DESTINATION -> onDestinationConfirmed?.invoke()
                    State.IDLE -> onWalkIntent?.invoke()
                }
            }
        }
        group.onDismiss = { onCancelRouting?.invoke() }
        group.duration.onCancel = { onCancelRouting?.invoke() }
        group.duration.onConfirm = { onConfirmRouting?.invoke() }
        group.walk.onChoose = { onSelectRoutingMode?.invoke(it) }
    }

}