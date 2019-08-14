package com.francescozoccheddu.tdmclient.ui.bottomgroup

import com.francescozoccheddu.tdmclient.utils.android.Timer
import com.francescozoccheddu.tdmclient.utils.commons.ProcEvent
import com.mapbox.mapboxsdk.geometry.LatLng
import org.json.JSONObject

class RoutingController(group: BottomGroup) {

    private val ui = BottomGroupController(group)

    init {
        ui.onWalkIntent = {
            ui.state = BottomGroupController.State.CHOOSING_WALK_MODE
        }
        ui.onSelectRoutingMode = {
            ui.state = when (it) {
                WalkComponent.RoutingMode.NEARBY -> BottomGroupController.State.CHOOSING_DURATION
                WalkComponent.RoutingMode.DESTINATION -> BottomGroupController.State.PICKING_DESTINATION
            }
            if (it == WalkComponent.RoutingMode.DESTINATION) {
                pickingDestination = true
            }
        }
        ui.onConfirmRouting = {
            ui.state = BottomGroupController.State.ROUTING
        }
        ui.onCancelRouting = {
            cancelRouting(true)
            ui.state = BottomGroupController.State.IDLE
        }
        ui.onDestinationConfirmed = {
            ui.state = BottomGroupController.State.CHOOSING_DURATION
        }
    }

    var route: JSONObject? = null
        private set

    private var pendingRoute: Any? = null

    var destination: LatLng? = null
        private set(value) {
            if (value != field) {
                field = value
                updateState()
                onDestinationChanged()
            }
        }

    fun setDestination(destination: LatLng, name: String, exactName: Boolean) {
        this.destination = destination
        ui.destinationName = name
    }

    fun removeDestination() {
        destination = null
    }

    var pickingDestination = false
        private set(value) {
            if (value != field) {
                field = value
                onPickingDestinationChanged()
            }
        }

    private fun cancelRouting(evenIfCompleted: Boolean) {
        pickingDestination = false
        destination = null
        pendingRoute = null
        if (evenIfCompleted)
            route = null
    }

    val onPickingDestinationChanged = ProcEvent()
    val onDestinationChanged = ProcEvent()

    enum class Problem {
        OFFLINE, UNLOCATABLE, LOCATING, PERMISSIONS_UNGRANTED, OUTSIDE_AREA, UNBOUND
    }

    private val failureNotificationCountdown = Timer().Countdown().apply {
        runnable = Runnable {
            destination = null
            updateState()
        }
        time = 4f
    }

    private fun updateState() {
        if (failureNotificationCountdown.running) {
            destination = null
            failureNotificationCountdown.cancel()
        }
        val problem = this.problem
        ui.state = if (problem != null)
            when (problem) {
                Problem.OFFLINE -> BottomGroupController.State.OFFLINE
                Problem.UNLOCATABLE -> BottomGroupController.State.UNLOCATABLE
                Problem.LOCATING -> BottomGroupController.State.LOCATING
                Problem.PERMISSIONS_UNGRANTED -> BottomGroupController.State.PERMISSIONS_UNGRANTED
                Problem.OUTSIDE_AREA -> BottomGroupController.State.OUTSIDE_AREA
                Problem.UNBOUND -> BottomGroupController.State.HIDDEN
            }
        else if (pickingDestination) {
            if (destination != null)
                BottomGroupController.State.CONFIRMING_DESTINATION
            else
                BottomGroupController.State.PICKING_DESTINATION
        }
        else if (route != null) BottomGroupController.State.ROUTED
        else if (pendingRoute != null) BottomGroupController.State.ROUTING
        else BottomGroupController.State.IDLE
    }

    fun onBack(): Boolean {
        return false
    }

    var problem: Problem? = Problem.UNBOUND
        set(value) {
            if (value != field) {
                field = value
                if (value != null)
                    cancelRouting(false)
                updateState()
            }
        }

}