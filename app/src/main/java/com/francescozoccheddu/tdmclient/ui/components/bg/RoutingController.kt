package com.francescozoccheddu.tdmclient.ui.components.bg

import android.content.Context
import android.view.ViewGroup
import com.francescozoccheddu.tdmclient.data.PlaceQuerier
import com.francescozoccheddu.tdmclient.ui.utils.Router
import com.francescozoccheddu.tdmclient.utils.android.Timer
import com.francescozoccheddu.tdmclient.utils.commons.ProcEvent
import com.francescozoccheddu.tdmclient.utils.commons.hrIntervalString
import com.francescozoccheddu.tdmclient.utils.data.latLng
import com.francescozoccheddu.tdmclient.utils.data.point
import com.francescozoccheddu.tdmclient.utils.data.travelDuration
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.services.android.navigation.R
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress
import com.mapbox.services.android.navigation.v5.utils.ManeuverUtils
import kotlin.math.roundToInt

class RoutingController(parent: ViewGroup, private val context: Context) {

    private val ui = BottomGroupController(parent)
    private val router = Router(context)

    init {
        router.onResult = { to, steps ->
            route = steps
            updateState()
            if (steps == null) {
                ui.state = BottomGroupController.State.ROUTING_FAILED
                failureNotificationCountdown.pull()
            }
            else if (destination == null)
                destination = to
        }
        ui.onWalkIntent = {
            ui.state = BottomGroupController.State.CHOOSING_WALK_MODE
        }
        ui.onSelectRoutingMode = {
            if (it == BottomGroupLayoutManager.WalkComponent.RoutingMode.DESTINATION)
                pickingDestination = true
            else
                ui.minTime = 10f
            ui.state = when (it) {
                BottomGroupLayoutManager.WalkComponent.RoutingMode.NEARBY -> BottomGroupController.State.CHOOSING_DURATION
                BottomGroupLayoutManager.WalkComponent.RoutingMode.DESTINATION -> BottomGroupController.State.PICKING_DESTINATION
            }
        }
        ui.onConfirmRouting = {
            pickingDestination = false
            ui.state = BottomGroupController.State.ROUTING
            if (service != null)
                router.request(destination, ui.time * 60f)
            else
                router.onResult?.invoke(null, null)
        }
        ui.onRetryRouting = {
            failureNotificationCountdown.cancel()
            if (destination != null)
                ui.onConfirmRouting?.invoke()
            else
                updateState()
        }

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

    var onLocationEnableIntent
        get() = ui.onLocationEnableIntent
        set(value) {
            ui.onLocationEnableIntent = value
        }

    var onPermissionGrantIntent
        get() = ui.onPermissionGrantIntent
        set(value) {
            ui.onPermissionGrantIntent = value
        }

    var route: DirectionsRoute? = null
        private set(value) {
            if (value != field) {
                field = value
                ui.maneuverInstruction = null
                ui.maneuverIcon = null
                ui.maneuverInfo = null
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
                PlaceQuerier.reverse(destination.point) {
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
        failureNotificationCountdown.cancel()
        pickingDestination = false
        destination = null
        router.cancel()
        if (evenIfCompleted)
            route = null
        updateState()
    }

    fun cancelRouting() {
        cancelRouting(true)
    }

    fun updateRoutingInstructions(progress: RouteProgress) {
        val banner = progress.bannerInstruction()?.primary
        if (banner != null)
            ui.maneuverInstruction = banner.text
        val leg = progress.currentLegProgress()
        val step = leg?.upComingStep() ?: leg?.currentStep()
        if (step != null)
            ui.maneuverIcon = ManeuverUtils.getManeuverResource(step)
        ui.maneuverInfo =
            hrIntervalString(context.resources, (progress.durationRemaining() / 60.0).roundToInt())
    }

    fun completeRouting() {
        completedNotificationCountdown.pull()
        cancelRouting(true)
        updateState()
    }

    val onPickingDestinationChanged = ProcEvent()
    val onDestinationChanged = ProcEvent()
    val onRouteChanged = ProcEvent()

    var service
        get() = router.service
        set(value) {
            router.service = value
            R.drawable.ic_maneuver_arrive
        }

    enum class Problem {
        OFFLINE, UNLOCATABLE, LOCATING, PERMISSIONS_UNGRANTED, OUTSIDE_AREA, UNBOUND
    }

    private val completedNotificationCountdown: Timer.Countdown

    private val failureNotificationCountdown: Timer.Countdown

    init {
        val timer = Timer()
        completedNotificationCountdown = timer.Countdown().apply {
            runnable = Runnable {
                updateState()
            }
            time = 2f
        }
        failureNotificationCountdown = timer.Countdown().apply {
            runnable = Runnable {
                destination = null
                updateState()
            }
            time = 4f
        }
    }


    private fun updateState() {
        val problem = this.problem
        ui.state =
            if (!enabled)
                BottomGroupController.State.HIDDEN
            else if (failureNotificationCountdown.running)
                BottomGroupController.State.ROUTING_FAILED
            else if (completedNotificationCountdown.running)
                BottomGroupController.State.ROUTE_COMPLETED
            else if (problem != null)
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
            else when (ui.state) {
                BottomGroupController.State.CHOOSING_DURATION, BottomGroupController.State.CHOOSING_WALK_MODE -> ui.state
                else -> BottomGroupController.State.IDLE
            }
    }

    fun onBack() =
        if (router.running
            || pickingDestination
            || failureNotificationCountdown.running
            || ui.state == BottomGroupController.State.CHOOSING_WALK_MODE
            || ui.state == BottomGroupController.State.CHOOSING_DURATION
        ) {
            ui.state = BottomGroupController.State.IDLE
            cancelRouting(false)
            true
        }
        else false


    var problem: Problem? =
        Problem.UNBOUND
        set(value) {
            if (value != field) {
                field = value
                if (value != null) {
                    if (failureNotificationCountdown.running) {
                        failureNotificationCountdown.cancel()
                        destination = null
                    }
                    completedNotificationCountdown.cancel()
                    cancelRouting(false)
                }
                updateState()
            }
        }

    var enabled = true
        set(value) {
            if (field != value) {
                field = value
                updateState()
            }
        }

}