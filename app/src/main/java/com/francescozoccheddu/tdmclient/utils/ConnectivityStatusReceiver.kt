package com.francescozoccheddu.tdmclientservice

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager


class ConnectivityStatusReceiver : Receiver<Boolean>() {

    companion object {

        fun isOnline(context: Context): Boolean {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkInfo = connectivityManager.activeNetworkInfo
            return networkInfo != null && networkInfo.isConnected
        }

    }

    override val intentFilter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)

    override fun onReceive(context: Context, intent: Intent) = isOnline(context)

}