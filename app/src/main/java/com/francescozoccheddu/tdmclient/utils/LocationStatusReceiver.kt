package com.francescozoccheddu.tdmclientservice

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager

class LocationStatusReceiver : Receiver<Boolean>() {

    companion object {

        fun isEnabled(context: Context): Boolean {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }

    }

    override val intentFilter = IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)

    override fun onReceive(context: Context, intent: Intent) = isEnabled(context)

}