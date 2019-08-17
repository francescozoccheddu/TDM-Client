package com.francescozoccheddu.tdmclient.utils.android

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.text.TextUtilsCompat
import androidx.core.view.ViewCompat
import androidx.core.view.setMargins
import androidx.core.view.setPadding
import com.google.android.material.snackbar.Snackbar
import java.util.*
import kotlin.math.roundToInt


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

fun View.setBackgroundColorRes(color: Int) {
    setBackgroundColor(ContextCompat.getColor(context, color))
}

private val Snackbar.textView
    get() =
        this.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)

fun Snackbar.addView(view: View, size: Int = LinearLayout.LayoutParams.WRAP_CONTENT): Snackbar {
    val contentLay = textView.parent as ViewGroup
    view.layoutParams = LinearLayout.LayoutParams(size, size).apply {
        gravity = Gravity.CENTER
    }
    if (TextUtilsCompat.getLayoutDirectionFromLocale(Locale.getDefault()) == ViewCompat.LAYOUT_DIRECTION_LTR)
        contentLay.addView(view, 0)
    else
        contentLay.addView(view)
    return this
}

fun Snackbar.setBackgroundColor(color: Int) = this.apply { view.setBackgroundColor(color) }

fun Snackbar.setBackgroundColorRes(color: Int) = this.apply { view.setBackgroundColorRes(color) }

fun Snackbar.setTextColorRes(color: Int) = textView.setTextColor(ContextCompat.getColor(context, color))

fun Snackbar.setActionTextColorRes(color: Int) = setActionTextColor(ContextCompat.getColor(context, color))

fun View.setPaddingRes(dimens: Int) = setPadding(context.resources.getDimension(dimens).roundToInt())

fun getNavigationBarHeight(context: Context): Int {
    val resources = context.resources
    val orientation = resources.configuration.orientation
    val id = resources.getIdentifier(
        if (orientation == Configuration.ORIENTATION_PORTRAIT)
            "navigation_bar_height"
        else
            "navigation_bar_height_landscape",
        "dimen",
        "android"
    )
    return if (id > 0) resources.getDimensionPixelSize(id) else 0
}

fun getStatusBarHeight(context: Context): Int {
    val resources = context.resources
    val id = resources.getIdentifier("status_bar_height", "dimen", "android")
    return if (id > 0) resources.getDimensionPixelSize(id) else 0
}

fun View.setMargins(margins: Int) {
    (layoutParams as ViewGroup.MarginLayoutParams).setMargins(margins)
}

fun View.setMargins(left: Int, top: Int, right: Int, bottom: Int) {
    (layoutParams as ViewGroup.MarginLayoutParams).setMargins(left, top, right, bottom)
}