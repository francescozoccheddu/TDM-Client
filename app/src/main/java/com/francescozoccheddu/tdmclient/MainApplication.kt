package com.francescozoccheddu.tdmclient

import android.app.Application
import com.mapbox.mapboxsdk.Mapbox

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Mapbox.getInstance(applicationContext, getString(R.string.mapbox_access_token))
    }

}