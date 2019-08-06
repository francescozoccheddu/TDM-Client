package com.francescozoccheddu.tdmclient.ui

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.francescozoccheddu.tdmclient.R

class ServiceNotification(val service: MainService) {

    companion object {

        private const val NOTIFICATION_CHANNEL = "MainServiceNotificationChannel"
        private const val FOREGROUND_NOTIFICATION_ID = 1
        private const val ENABLE_KILL_BUTTON = false
        private const val SENSOR_LOST_NOTIFICATION_ID = 2

        private fun notify(context: Context, id: Int, notification: Notification) {
            NotificationManagerCompat.from(context).notify(id, notification)
        }

        fun notifySensorConnectionLost(context: Context) {
            notify(
                context, SENSOR_LOST_NOTIFICATION_ID,
                NotificationCompat.Builder(context, NOTIFICATION_CHANNEL)
                    .setContentTitle("TDM Client")
                    .setContentText("Device lost")
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .build()
            )
        }

    }

    init {
        val listener = { service: MainService -> update() }
        service.onConnectedChange += listener
        service.onOnlineChange += listener
        service.onLocatableChange += listener
        service.onLocationChange += listener
        update()
    }

    private val okNotification by lazy {
        NotificationCompat.Builder(service, NOTIFICATION_CHANNEL)
            .setContentTitle("TDM Client")
            .setContentText("OK")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()
    }
    private val unknownLocationNotification by lazy {
        NotificationCompat.Builder(service, NOTIFICATION_CHANNEL)
            .setContentTitle("TDM Client")
            .setContentText("Unknown")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()
    }
    private val unlocatableNotification by lazy {
        NotificationCompat.Builder(service, NOTIFICATION_CHANNEL)
            .setContentTitle("TDM Client")
            .setContentText("Unlocatable")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()
    }
    private val offlineNotification by lazy {
        NotificationCompat.Builder(service, NOTIFICATION_CHANNEL)
            .setContentTitle("TDM Client")
            .setContentText("Offline")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()
    }
    private val unreachableNotification by lazy {
        NotificationCompat.Builder(service, NOTIFICATION_CHANNEL)
            .setContentTitle("TDM Client")
            .setContentText("Unreachable")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()
    }


    enum class NotificationType {
        OK, UNKNOWN_LOCATION, UNLOCATABLE, OFFLINE, UNREACHABLE
    }

    private var notificationType: NotificationType = NotificationType.OK
        set(value) {
            if (value != field) {
                field = value
                if (foreground)
                    notify(FOREGROUND_NOTIFICATION_ID, notification)
            }
        }

    private val notification
        get() = when (notificationType) {
            NotificationType.OK -> okNotification
            NotificationType.UNKNOWN_LOCATION -> unknownLocationNotification
            NotificationType.UNLOCATABLE -> unlocatableNotification
            NotificationType.OFFLINE -> offlineNotification
            NotificationType.UNREACHABLE -> unreachableNotification
        }

    fun registerChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            (service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(
                    NotificationChannel(
                        NOTIFICATION_CHANNEL,
                        service.resources.getString(R.string.service_notification_channel),
                        NotificationManager.IMPORTANCE_DEFAULT
                    )
                )
        }
    }

    fun notifySensorConnectionLost() {
        notifySensorConnectionLost(service)
    }

    private fun notify(id: Int, notification: Notification) {
        notify(service, id, notification)
    }

    private fun update() {
        service.apply {
            notificationType =
                if (connected) {
                    if (locatable) {
                        if (location != null)
                            NotificationType.OK
                        else
                            NotificationType.UNKNOWN_LOCATION
                    }
                    else NotificationType.UNLOCATABLE
                }
                else {
                    if (online)
                        NotificationType.UNREACHABLE
                    else
                        NotificationType.OFFLINE
                }
        }
    }

    var foreground = false
        set(value) {
            if (value != field) {
                field = value
                if (value)
                    notify(FOREGROUND_NOTIFICATION_ID, notification)
            }
        }

}