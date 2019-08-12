package com.francescozoccheddu.tdmclient.ui.bottomgroup

class RoutingController(group: BottomGroup) {

    private val ui = BottomGroupController(group)

    init {
        ui.state = BottomGroupController.State.IDLE
        ui.onWalkIntent = {
            ui.state = BottomGroupController.State.CHOOSING_WALK_MODE
        }
        ui.onSelectRoutingMode = {
            ui.state = when (it) {
                WalkComponent.RoutingMode.NEARBY -> BottomGroupController.State.CHOOSING_DURATION
                WalkComponent.RoutingMode.DESTINATION -> BottomGroupController.State.PICKING_DESTINATION
            }
        }
        ui.onConfirmRouting = {
            ui.state = BottomGroupController.State.ROUTING
        }
        ui.onCancelRouting = {
            ui.state = BottomGroupController.State.IDLE
        }
        ui.onDestinationConfirmed = {
            ui.state = BottomGroupController.State.CHOOSING_DURATION
        }
    }

    enum class Problem {
        OFFLINE, UNLOCATABLE, LOCATING, PERMISSIONS_UNGRANTED
    }

    var problem: Problem? = null

}