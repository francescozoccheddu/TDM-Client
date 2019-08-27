package com.francescozoccheddu.tdmclient.utils.commons

import android.content.res.Resources
import com.francescozoccheddu.tdmclient.R
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToLong


fun dateDiff(date1: Date, date2: Date): Float {
    val diffMs = date2.time - date1.time
    return diffMs / 1000f
}

fun dateElapsed(date: Date): Float {
    return dateDiff(date, Date())
}

fun dateAdd(date: Date, time: Float): Date {
    return Date(date.time + (time * 1000).roundToLong())
}

private val dateFormat = SimpleDateFormat("yyyy-MM-dd")
private val datetimeFormat = SimpleDateFormat(dateFormat.toPattern() + "'T'HH:mm:ss")
private val datetimezoneFormat = SimpleDateFormat(datetimeFormat.toPattern() + "ZZZZZ").apply {
    timeZone = TimeZone.getDefault()
}

fun dateParseISO(date: String): Date {
    val builder = StringBuilder(date.trim())
    val format: SimpleDateFormat = when (builder.length) {
        9 -> {
            // YYYY-MM-DD
            dateFormat
        }
        19 -> {
            // YYYY-MM-DDTHH:MM:SS
            builder.setCharAt(10, 'T')
            datetimeFormat
        }
        26 -> {
            // YYYY-MM-DDTHH:MM:SS.mmmmmm
            builder.setCharAt(10, 'T')
            builder.delete(19, 26)
            datetimeFormat
        }
        25 -> {
            // YYYY-MM-DDTHH:MM:SS+HH:MM
            builder.setCharAt(10, 'T')
            datetimezoneFormat
        }
        32 -> {
            // YYYY-MM-DDTHH:MM:SS.mmmmmm+HH:MM
            builder.setCharAt(10, 'T')
            builder.delete(19, 26)
            datetimezoneFormat
        }
        else -> throw ParseException("Unexpected length", 0)
    }
    return format.parse(builder.toString())!!
}

val Date.iso get() = datetimezoneFormat.format(this)

fun toMillis(seconds: Float) = (seconds * 1000).roundToLong()


private val hrIntervalBuilder = StringBuilder(6)

fun hmIntervalString(minutes: Int): String {
    val tm = minutes
    val m = tm % 60
    val h = tm / 60
    val sb = hrIntervalBuilder
    sb.setLength(0)
    if (h > 0) {
        sb.append(h)
        sb.append('h')
    }
    if (m > 0) {
        if (sb.isNotEmpty())
            sb.append(' ')
        sb.append(m)
        sb.append('m')
    }
    return sb.toString()
}

fun hrIntervalString(resources: Resources, minutes: Int): String {
    return if (minutes < 60)
        resources.getQuantityString(R.plurals.date_minutes, minutes, minutes)
    else {
        val h = minutes / 60
        val m = minutes % 60
        val hs = resources.getQuantityString(R.plurals.date_hours, h, h)
        val ms = resources.getQuantityString(R.plurals.date_minutes, m, m)
        if (m != 0)
            resources.getString(R.string.date_format_hours_minutes, hs, ms)
        else
            hs
    }
}