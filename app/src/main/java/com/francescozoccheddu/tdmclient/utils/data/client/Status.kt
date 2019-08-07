package com.francescozoccheddu.tdmclient.utils.data.client

enum class Status {
    NETWORK_ERROR, NO_CONNECTION_ERROR, SURRENDED_ERROR, RESPONSE_ERROR, REQUEST_ERROR, SERVER_ERROR, RUNTIME_ERROR,
    SUCCESS, PENDING, CANCELED, PLANNED;

    val started
        get() = this != PLANNED

    val canceled
        get() = this == CANCELED

    val pending
        get() = this == PENDING

    val succeeded
        get() = this == SUCCESS

}

val Status.finished
    get() = !canceled && !pending

val Status.error
    get() = finished && !succeeded

