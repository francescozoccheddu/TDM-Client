package com.francescozoccheddu.tdmclient.utils.data.client

import java.net.URI


data class ServiceAddress(val uri: URI) {

    constructor(uri: String) : this(URI.create(uri))

    init {
        if (uri.scheme != null || uri.authority != null || uri.query != null || uri.fragment != null) {
            throw IllegalArgumentException("Service URI must contain only path")
        }
    }

    override fun toString() = uri.toString()

}

data class ServerAddress(val uri: URI) {

    constructor(uri: String) : this(URI.create(uri))

    init {
        val scheme = uri.scheme
        if (scheme?.toLowerCase() != "http") {
            throw IllegalArgumentException("Server URI scheme must be HTTP")
        }
        if (uri.authority == null) {
            throw IllegalArgumentException("Server URI must contain authority info")
        }
        if (uri.query != null || uri.fragment != null) {
            throw IllegalArgumentException("Server URI cannot contain query or fragment info")
        }
    }

    fun resolveService(serviceAddress: ServiceAddress) = uri.resolve(serviceAddress.uri).toString()

    override fun toString() = uri.toString()
}
