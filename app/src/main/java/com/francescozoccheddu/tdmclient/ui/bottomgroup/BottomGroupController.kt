package com.francescozoccheddu.tdmclient.ui.bottomgroup

import androidx.core.content.ContextCompat
import com.francescozoccheddu.tdmclient.R

class BottomGroupController(val group: BottomGroup) {

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

    private fun getString(resId: Int) = group.resources.getString(resId)
    private fun getColor(resId: Int) = ContextCompat.getColor(group.context, resId)

    var state: State = State.HIDDEN
        set(value) {
            if (value != field) {
                field = value
                updateState()
            }
        }

    private fun updateState() {
        group.mode = when (state) {
            State.PICKING_DESTINATION, State.LOCATING, State.UNLOCATABLE, State.PERMISSIONS_UNGRANTED,
            State.ROUTING, State.OFFLINE, State.OUTSIDE_AREA, State.ROUTING_FAILED -> BottomGroup.Mode.INFO
            State.HIDDEN -> BottomGroup.Mode.HIDDEN
            State.IDLE, State.ROUTED -> BottomGroup.Mode.ACTION
            State.CONFIRMING_DESTINATION -> if (destinationName != null) BottomGroup.Mode.INFO else BottomGroup.Mode.ACTION
            State.CHOOSING_DURATION -> BottomGroup.Mode.DURATION
            State.CHOOSING_WALK_MODE -> BottomGroup.Mode.WALK
        }
        group.color = when (state) {
            State.UNLOCATABLE, State.PERMISSIONS_UNGRANTED, State.OFFLINE,
            State.ROUTING_FAILED -> getColor(R.color.backgroundError)
            State.LOCATING, State.ROUTING, State.IDLE, State.CHOOSING_WALK_MODE,
            State.CHOOSING_DURATION, State.PICKING_DESTINATION -> getColor(R.color.background)
            State.HIDDEN -> group.color
            State.OUTSIDE_AREA, State.ROUTED -> getColor(R.color.backgroundWarning)
            State.CONFIRMING_DESTINATION -> getColor(R.color.backgroundOk)
        }
        group.info.text = when (state) {
            State.PICKING_DESTINATION -> getString(R.string.snackbar_picking)
            State.LOCATING -> getString(R.string.snackbar_locating)
            State.UNLOCATABLE -> getString(R.string.snackbar_unlocatable)
            State.PERMISSIONS_UNGRANTED -> getString(R.string.snackbar_permissions_ungranted)
            State.ROUTING -> getString(R.string.snackbar_routing)
            State.OFFLINE -> getString(R.string.snackbar_offline)
            State.OUTSIDE_AREA -> getString(R.string.snackbar_outside_area)
            State.ROUTING_FAILED -> getString(R.string.snackbar_routing_failed)
            State.CONFIRMING_DESTINATION -> destinationName ?: group.info.text
            else -> group.info.text
        }
        group.info.loading = when (state) {
            State.LOCATING, State.ROUTING -> true
            else -> false
        }
        group.info.icon = when (state) {
            State.UNLOCATABLE, State.OUTSIDE_AREA -> R.drawable.ic_unlocatable
            State.OFFLINE -> R.drawable.ic_offline
            State.PERMISSIONS_UNGRANTED, State.ROUTING_FAILED -> R.drawable.ic_warning
            State.CONFIRMING_DESTINATION, State.PICKING_DESTINATION -> R.drawable.ic_place
            else -> group.info.icon
        }
        group.modal = when (state) {
            State.CHOOSING_DURATION, State.CHOOSING_WALK_MODE -> true
            else -> false
        }
        group.info.action = when (state) {
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
    var onSelectRoutingMode: ((WalkComponent.RoutingMode) -> Unit)? = null
    var onWalkIntent: (() -> Unit)? = null
    var onLocationEnableIntent: (() -> Unit)? = null
    var onPermissionGrantIntent: (() -> Unit)? = null
    var onDestinationConfirmed: (() -> Unit)? = null

    init {
        group.action.onClick { if (state == State.IDLE) onWalkIntent?.invoke() }
        group.onDismiss { if (state == State.CHOOSING_DURATION || state == State.CHOOSING_WALK_MODE) onCancelRouting?.invoke() }
        group.duration.onCancel { if (state == State.CHOOSING_DURATION) onCancelRouting?.invoke() }
        group.duration.onConfirm { if (state == State.CHOOSING_DURATION) onConfirmRouting?.invoke() }
        group.info.onAction {
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
        group.walk.onChoose { if (state == State.CHOOSING_WALK_MODE) onSelectRoutingMode?.invoke(it) }
        updateState()
    }

}