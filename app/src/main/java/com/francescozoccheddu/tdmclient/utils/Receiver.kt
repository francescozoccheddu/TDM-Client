package com.francescozoccheddu.tdmclientservice

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

abstract class Receiver<Type> {

    private val receiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            listener?.invoke(this@Receiver.onReceive(context!!, intent!!))
        }

    }

    val registered get() = listener != null

    private var listener: ((Type) -> Unit)? = null

    fun register(context: Context, listener: (Type) -> Unit) {
        if (this.listener != null)
            throw IllegalStateException("Broadcast receiver is already registered")
        this.listener = listener
        context.registerReceiver(receiver, intentFilter)
    }

    fun unregister(context: Context) {
        if (listener == null)
            throw IllegalStateException("Broadcast receiver is not registered")
        this.listener = null
        context.unregisterReceiver(receiver)
    }

    protected abstract fun onReceive(context: Context, intent: Intent): Type

    protected abstract val intentFilter: IntentFilter

}