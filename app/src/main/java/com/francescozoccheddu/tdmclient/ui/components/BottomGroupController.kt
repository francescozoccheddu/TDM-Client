package com.francescozoccheddu.tdmclient.ui.components

import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.francescozoccheddu.tdmclient.R

class BottomGroupController(private val parent: ViewGroup) {

    private val layout = BottomGroupLayoutManager(parent)

    enum class State {
        LOCATING, UNLOCATABLE, PERMISSIONS_UNGRANTED, ROUTING, HIDDEN,
        OFFLINE, OUTSIDE_AREA, PICKING_DESTINATION, CONFIRMING_DESTINATION, IDLE,
        CHOOSING_WALK_MODE, CHOOSING_DURATION, ROUTED, ROUTING_FAILED
    }

    var destinationName: String? = null
        set(value) {
            if (value != field) {
                field = value
                if (state == State.CONFIRMING_DESTINATION)
                    updateState()
            }
        }

    private fun getString(resId: Int) = parent.resources.getString(resId)
    private fun getColor(resId: Int) = ContextCompat.getColor(parent.context, resId)

    var state: State =
        State.HIDDEN
        set(value) {
            if (value != field) {
                field = value
                updateState()
            }
        }

    val time get() = layout.duration.time

    var minTime
        get() = layout.duration.minTime
        set(value) {
            layout.duration.minTime = value
        }

    private fun updateState() {
        layout.state = when (state) {
            State.PICKING_DESTINATION, State.LOCATING, State.UNLOCATABLE, State.PERMISSIONS_UNGRANTED,
            State.CONFIRMING_DESTINATION, State.ROUTING, State.OFFLINE, State.OUTSIDE_AREA,
            State.ROUTING_FAILED, State.ROUTED -> BottomGroupLayoutManager.State.INFO
            State.HIDDEN -> BottomGroupLayoutManager.State.HIDDEN
            State.IDLE -> BottomGroupLayoutManager.State.ACTION
            State.CHOOSING_DURATION -> BottomGroupLayoutManager.State.DURATION
            State.CHOOSING_WALK_MODE -> BottomGroupLayoutManager.State.WALK
        }
        layout.color = when (state) {
            State.UNLOCATABLE, State.PERMISSIONS_UNGRANTED, State.OFFLINE,
            State.ROUTING_FAILED -> getColor(R.color.backgroundError)
            State.LOCATING, State.ROUTING, State.IDLE, State.CHOOSING_WALK_MODE,
            State.CHOOSING_DURATION, State.PICKING_DESTINATION, State.ROUTED -> getColor(R.color.background)
            State.HIDDEN -> layout.color
            State.OUTSIDE_AREA -> getColor(R.color.backgroundWarning)
            State.CONFIRMING_DESTINATION -> getColor(R.color.backgroundOk)
        }
        layout.info.text = when (state) {
            State.PICKING_DESTINATION -> getString(R.string.snackbar_picking)
            State.LOCATING -> getString(R.string.snackbar_locating)
            State.UNLOCATABLE -> getString(R.string.snackbar_unlocatable)
            State.PERMISSIONS_UNGRANTED -> getString(R.string.snackbar_permissions_ungranted)
            State.ROUTING -> getString(R.string.snackbar_routing)
            State.OFFLINE -> getString(R.string.snackbar_offline)
            State.OUTSIDE_AREA -> getString(R.string.snackbar_outside_area)
            State.ROUTING_FAILED -> getString(R.string.snackbar_routing_failed)
            State.ROUTED -> getString(R.string.snackbar_routed)
            State.CONFIRMING_DESTINATION -> destinationName ?: getString(R.string.snackbar_unknown_place)
            else -> layout.info.text
        }
        layout.info.loading = when (state) {
            State.LOCATING, State.ROUTING -> true
            else -> false
        }
        layout.info.icon = when (state) {
            State.UNLOCATABLE, State.OUTSIDE_AREA -> R.drawable.ic_unlocatable
            State.OFFLINE -> R.drawable.ic_offline
            State.PERMISSIONS_UNGRANTED, State.ROUTING_FAILED -> R.drawable.ic_warning
            State.CONFIRMING_DESTINATION, State.PICKING_DESTINATION -> R.drawable.ic_place
            State.ROUTED -> R.drawable.ic_directions
            else -> layout.info.icon
        }
        layout.info.action = when (state) {
            State.UNLOCATABLE -> getString(R.string.snackbar_action_unlocatable)
            State.PERMISSIONS_UNGRANTED -> getString(R.string.snackbar_action_permissions_ungranted)
            State.ROUTING -> getString(R.string.snackbar_action_routing)
            State.PICKING_DESTINATION -> getString(R.string.snackbar_action_picking)
            State.CONFIRMING_DESTINATION -> getString(R.string.snackbar_action_destinated)
            State.ROUTED -> getString(R.string.snackbar_action_routed)
            State.ROUTING_FAILED -> getString(R.string.snackbar_action_routing_failed)
            else -> null
        }
    }

    var onRetryRouting: (() -> Unit)? = null
    var onConfirmRouting: (() -> Unit)? = null
    var onCancelRouting: (() -> Unit)? = null
    var onSelectRoutingMode: ((BottomGroupLayoutManager.WalkComponent.RoutingMode) -> Unit)? = null
    var onWalkIntent: (() -> Unit)? = null
    var onLocationEnableIntent: (() -> Unit)? = null
    var onPermissionGrantIntent: (() -> Unit)? = null
    var onDestinationConfirmed: (() -> Unit)? = null

    init {
        layout.action.onAction =
            { if (state == State.IDLE) onWalkIntent?.invoke() }
        layout.scrim.onDismiss =
            { if (state == State.CHOOSING_DURATION || state == State.CHOOSING_WALK_MODE) onCancelRouting?.invoke() }
        layout.duration.onCancel =
            { if (state == State.CHOOSING_DURATION) onCancelRouting?.invoke() }
        layout.duration.onConfirm =
            { if (state == State.CHOOSING_DURATION) onConfirmRouting?.invoke() }
        layout.info.onAction = {
            when (state) {
                State.UNLOCATABLE -> onLocationEnableIntent?.invoke()
                State.PERMISSIONS_UNGRANTED -> onPermissionGrantIntent?.invoke()
                State.ROUTING -> onCancelRouting?.invoke()
                State.PICKING_DESTINATION -> onCancelRouting?.invoke()
                State.CONFIRMING_DESTINATION -> onDestinationConfirmed?.invoke()
                State.ROUTED -> onCancelRouting?.invoke()
                State.ROUTING_FAILED -> onRetryRouting?.invoke()
            }
        }
        layout.walk.onChoose =
            { if (state == State.CHOOSING_WALK_MODE) onSelectRoutingMode?.invoke(it) }
        updateState()
    }

}