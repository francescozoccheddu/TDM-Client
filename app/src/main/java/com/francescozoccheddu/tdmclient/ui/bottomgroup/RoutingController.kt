package com.francescozoccheddu.tdmclient.ui.bottomgroup

import com.francescozoccheddu.tdmclient.data.Geocoder
import com.francescozoccheddu.tdmclient.ui.Router
import com.francescozoccheddu.tdmclient.utils.android.Timer
import com.francescozoccheddu.tdmclient.utils.commons.ProcEvent
import com.francescozoccheddu.tdmclient.utils.data.latLng
import com.francescozoccheddu.tdmclient.utils.data.point
import com.francescozoccheddu.tdmclient.utils.data.travelDuration
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.mapboxsdk.geometry.LatLng

class RoutingController(group: BottomGroup) {

    private val ui = BottomGroupController(group)
    private val router = Router()

    init {
        router.onResult = {
            route = it
            updateState()
            if (it == null) {
                ui.state = BottomGroupController.State.ROUTING_FAILED
                failureNotificationCountdown.pull()
            }
        }
        ui.onWalkIntent = {
            ui.state = BottomGroupController.State.CHOOSING_WALK_MODE
        }
        ui.onSelectRoutingMode = {
            if (it == WalkComponent.RoutingMode.DESTINATION)
                pickingDestination = true
            else
                ui.minTime = 10f
            ui.state = when (it) {
                WalkComponent.RoutingMode.NEARBY -> BottomGroupController.State.CHOOSING_DURATION
                WalkComponent.RoutingMode.DESTINATION -> BottomGroupController.State.PICKING_DESTINATION
            }
        }
        ui.onConfirmRouting = {
            ui.state = BottomGroupController.State.ROUTING
            if (service != null)
                router.request(destination, ui.time)
            else
                router.onResult?.invoke(null)
        }
        ui.onRetryRouting = ui.onConfirmRouting
        ui.onCancelRouting = {
            cancelRouting(true)
            ui.state = BottomGroupController.State.IDLE
        }
        ui.onDestinationConfirmed = {
            val a = this.service?.location?.latLng
            val b = this.destination
            if (a != null && b != null)
                ui.minTime = travelDuration(a.distanceTo(b).toFloat()) / 60f
            else
                ui.minTime = 10f
            ui.state = BottomGroupController.State.CHOOSING_DURATION
        }
    }

    var route: DirectionsRoute? = null
        private set(value) {
            if (value != field) {
                field = value
                onRouteChanged()
            }
        }

    var destination: LatLng? = null
        private set(value) {
            if (value != field) {
                field = value
                updateState()
                onDestinationChanged()
            }
        }

    fun setDestination(destination: LatLng, name: String, exactName: Boolean) {
        if (destination != this.destination) {
            this.destination = destination
            ui.destinationName = name
            if (!exactName) {
                Geocoder.reverse(destination.point) {
                    if (it != null && destination == this.destination)
                        ui.destinationName = it
                }
            }
        }
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
        router.cancel()
        if (evenIfCompleted)
            route = null
    }

    val onPickingDestinationChanged = ProcEvent()
    val onDestinationChanged = ProcEvent()
    val onRouteChanged = ProcEvent()

    var service
        get() = router.service
        set(value) {
            router.service = value
        }

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
        else if (router.running) BottomGroupController.State.ROUTING
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