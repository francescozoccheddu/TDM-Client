package com.francescozoccheddu.tdmclient.data

import com.francescozoccheddu.tdmclient.utils.commons.ProcEvent
import com.francescozoccheddu.tdmclient.utils.data.RemoteValue
import com.francescozoccheddu.tdmclient.utils.data.client.Server
import com.mapbox.geojson.FeatureCollection
import com.mapbox.mapboxsdk.geometry.LatLng

class DataRetriever(server: Server, user: UserKey) {

    private companion object {
        private const val COVERAGE_INTERVAL_TIME = 2f
        private const val COVERAGE_EXPIRATION_TIME = 60f
        private const val AVATARS_EXPIRATION_TIME = 3600f
        private const val LEADERBOARD_EXPIRATION_TIME = 60f
        private const val LEADERBOARD_SIZE = 10
        private const val POI_INTERVAL_TIME = 10f
        private const val POI_EXPIRATION_TIME = 120f
    }

    val onCoveragePointDataChange = ProcEvent()
    val onCoverageQuadDataChange = ProcEvent()
    val onPoiDataChange = ProcEvent()

    private val coveragePointService = makeCoverageService(server).apply {
        pollRequest = CoverageRetrieveMode.POINTS
        expiration = COVERAGE_EXPIRATION_TIME
        onData += { onCoveragePointDataChange() }
        onExpire += onCoveragePointDataChange
    }
    private val coverageQuadService = makeCoverageService(server).apply {
        pollRequest = CoverageRetrieveMode.QUADS
        expiration = COVERAGE_EXPIRATION_TIME
        onData += { onCoverageQuadDataChange() }
        onExpire += onCoverageQuadDataChange
    }
    private val poiService = makePoiService(server, user).apply {
        expiration = POI_EXPIRATION_TIME
        onData += { onPoiDataChange() }
        onExpire += onPoiDataChange
    }
    private val routeService = makeRouteService(server)

    private val leaderboard = RemoteValue(makeLeaderboardService(server, LEADERBOARD_SIZE)).apply {
        expiration = LEADERBOARD_EXPIRATION_TIME
    }

    private val avatars = RemoteValue(makeAvatarsService(server)).apply {
        expiration = AVATARS_EXPIRATION_TIME
    }

    val poiData: FeatureCollection?
        get() = if (poiService.hasData && !poiService.expired) poiService.data else null

    val coveragePointData: FeatureCollection?
        get() = if (coveragePointService.hasData && !coveragePointService.expired) coveragePointService.data else null

    val coverageQuadData: FeatureCollection?
        get() = if (coverageQuadService.hasData && !coverageQuadService.expired) coverageQuadService.data else null

    fun requestRoute(
        from: LatLng,
        to: LatLng?,
        time: Float
    ) = routeService.Request(RouteRequest(from, to, time))

    fun getAvatars(forceUpdate: Boolean = false, callback: (AvatarSet?) -> Unit) {
        avatars.get(forceUpdate, callback)
    }

    fun getLeaderboard(forceUpdate: Boolean = false, callback: (List<User>?) -> Unit) {
        leaderboard.get(forceUpdate, callback)
    }

    var polling = false
        set(value) {
            if (value != field) {
                field = value
                val coveragePollPeriod = if (value) COVERAGE_INTERVAL_TIME else null
                coveragePointService.periodicPoll = coveragePollPeriod
                coverageQuadService.periodicPoll = coveragePollPeriod
                poiService.periodicPoll = if (value) POI_INTERVAL_TIME else null
            }
        }

}