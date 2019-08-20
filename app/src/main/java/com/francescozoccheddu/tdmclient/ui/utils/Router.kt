package com.francescozoccheddu.tdmclient.ui.utils

import com.francescozoccheddu.tdmclient.data.getDirections
import com.francescozoccheddu.tdmclient.ui.MainService
import com.francescozoccheddu.tdmclient.utils.data.client.Server
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.mapboxsdk.geometry.LatLng

class Router {

    var service: MainService? = null

    var running = false
        private set

    private var spotRequest: Server.Service<*, *>.Request? = null
    private lateinit var attachedRequest: Server.Service<*, *>.Request

    var onResult: ((DirectionsRoute?) -> Unit)? = null

    fun request(to: LatLng?, time: Float) {
        cancel()
        val service = this.service ?: throw IllegalStateException("'${this::service.name}' is null")
        running = true
        spotRequest = service.requestRoute(to, time).apply {
            attachedRequest = this
            onStatusChange += {
                if (!status.pending) {
                    spotRequest = null
                    if (status.succeeded)
                        getDirections(it.response, to != null) {
                            if (running && this@Router.attachedRequest == this) {
                                running = false
                                onResult?.invoke(it)
                            }
                        }
                    else if (running) {
                        running = false
                        onResult?.invoke(null)
                    }
                }
            }
            start()
        }
    }

    fun cancel() {
        if (spotRequest?.status?.pending == true) {
            spotRequest?.cancel()
            spotRequest = null
        }
        running = false
    }

}