package com.francescozoccheddu.tdmclient.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import com.francescozoccheddu.tdmclient.R
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager

class Permissions(val activity: Activity) {


    private fun notify(granted: Boolean) {
        val callback = this.callback
        this.callback = null
        callback?.invoke(granted)
    }

    private var callback: ((Boolean) -> Unit)? = null

    val manager = PermissionsManager(object : PermissionsListener {

        override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {}

        override fun onPermissionResult(granted: Boolean) {
            notify(granted)
        }

    })

    val granted get() = PermissionsManager.areLocationPermissionsGranted(activity)

    val canAsk get() = activity.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)

    fun openSettings() {
        activity.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.fromParts("package", activity.packageName, null))
        )
        Toast.makeText(activity, R.string.toast_permissions_settings_overlay, Toast.LENGTH_LONG).show()
    }

    fun ask(callback: (Boolean) -> Unit) {
        if (this.callback == null) {
            this.callback = callback
            if (granted)
                notify(true)
            else
                manager.requestLocationPermissions(activity)
        }
    }

    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        manager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

}