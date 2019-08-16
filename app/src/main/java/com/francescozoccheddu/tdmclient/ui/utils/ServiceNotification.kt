package com.francescozoccheddu.tdmclient.ui.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.francescozoccheddu.tdmclient.R
import com.francescozoccheddu.tdmclient.ui.MainActivity
import com.francescozoccheddu.tdmclient.ui.MainService
import com.francescozoccheddu.tdmclient.utils.android.addAction
import com.francescozoccheddu.tdmclient.utils.android.makeActivityIntent
import com.francescozoccheddu.tdmclient.utils.android.setContentText
import com.francescozoccheddu.tdmclient.utils.android.setContentTitle

class ServiceNotification(val service: MainService) {

    companion object {

        private const val NOTIFICATION_CHANNEL = "MainServiceNotificationChannel"
        private const val FOREGROUND_NOTIFICATION_ID = 1
        private const val ENABLE_KILL_BUTTON = true
        private const val SENSOR_LOST_NOTIFICATION_ID = 2

        private fun notify(context: Context, id: Int, notification: Notification) {
            NotificationManagerCompat.from(context).notify(id, notification)
        }

        private fun NotificationCompat.Builder.addStopServiceAction() =
            if (ENABLE_KILL_BUTTON)
                addAction(
                    R.drawable.ic_launcher_foreground, R.string.notification_action_kill,
                    PendingIntent.getService(
                        mContext, 0, MainService.makeStopIntent(mContext),
                        PendingIntent.FLAG_UPDATE_CURRENT
                    )
                )
            else this

        private fun makeBuilder(context: Context) =
            NotificationCompat.Builder(context,
                NOTIFICATION_CHANNEL
            )
                .setContentIntent(makeActivityIntent(context, MainActivity::class.java))
                .setContentTitle(R.string.notification_title)
                .setSmallIcon(R.drawable.ic_launcher_foreground)

        fun notifySensorConnectionLost(context: Context) {
            notify(
                context,
                SENSOR_LOST_NOTIFICATION_ID,
                makeBuilder(context)
                    .setContentText(R.string.notification_content_device_lost)
                    .build()
            )
        }

    }

    init {
        service.onConnectedChange += this::update
        service.onOnlineChange += this::update
        service.onLocatableChange += this::update
        service.onLocationChange += this::update
        update()
    }

    private val builder get() = makeBuilder(
        service
    )

    private val okNotification by lazy {
        builder
            .setContentText(R.string.notification_content_ok)
            .addStopServiceAction()
            .build()
    }

    private val unknownLocationNotification by lazy {
        builder
            .setContentText(R.string.notification_content_unknown_location)
            .addStopServiceAction()
            .build()
    }

    private val unlocatableNotification by lazy {
        builder
            .setContentText(R.string.notification_content_unlocatable)
            .addStopServiceAction()
            .addAction(
                R.drawable.ic_launcher_foreground,
                R.string.notification_action_unlocatable,
                PendingIntent.getActivity(service, 0, Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), 0)
            )
            .build()
    }

    private val offlineNotification by lazy {
        builder
            .setContentText(R.string.notification_content_offline)
            .addStopServiceAction()
            .build()
    }

    private val unreachableNotification by lazy {
        builder
            .setContentText(R.string.notification_content_unreachable)
            .addStopServiceAction()
            .build()
    }


    enum class NotificationType {
        OK, UNKNOWN_LOCATION, UNLOCATABLE, OFFLINE, UNREACHABLE
    }

    private var notificationType: NotificationType =
        NotificationType.OK
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
                    service.startForeground(FOREGROUND_NOTIFICATION_ID, notification)
                else
                    service.stopForeground(true)
            }
        }

}