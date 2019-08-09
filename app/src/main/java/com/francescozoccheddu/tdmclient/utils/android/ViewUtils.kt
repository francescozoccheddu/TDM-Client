package com.francescozoccheddu.tdmclient.utils.android

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.view.View
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton

var View.visible
    get() = visibility == View.VISIBLE
    set(value) {
        visibility = if (value) View.VISIBLE else View.GONE
    }

fun NotificationCompat.Builder.setContentTitle(resId: Int) =
    setContentTitle(mContext.resources.getString(resId))

fun NotificationCompat.Builder.setContentText(resId: Int) =
    setContentText(mContext.resources.getString(resId))

fun <ActivityType : Activity> makeActivityIntent(context: Context, clazz: Class<ActivityType>) =
    PendingIntent.getActivity(context, 0, Intent(context, clazz).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }, PendingIntent.FLAG_UPDATE_CURRENT)

fun NotificationCompat.Builder.addAction(icon: Int, title: Int, intent: PendingIntent) =
    addAction(icon, mContext.resources.getString(title), intent)

fun ImageView.setImageDrawable(icon: Int) {
    setImageDrawable(ContextCompat.getDrawable(context, icon))
}

fun FloatingActionButton.setBackgroundColor(color: Int) {
    backgroundTintList = ColorStateList.valueOf(color)
}

fun FloatingActionButton.setBackgroundColorRes(color: Int) {
    setBackgroundColor(ContextCompat.getColor(context, color))
}