package com.francescozoccheddu.tdmclient.utils

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


fun dateDiff(date1: Date, date2: Date, timeUnit: TimeUnit = TimeUnit.SECONDS): Long {
    val diffMs = date2.getTime() - date1.getTime()
    return timeUnit.convert(diffMs, TimeUnit.MILLISECONDS)
}

fun dateElapsed(date: Date, timeUnit: TimeUnit = TimeUnit.SECONDS): Long {
    return dateDiff(date, Date(), timeUnit)
}

fun dateAdd(date: Date, time: Long, timeUnit: TimeUnit = TimeUnit.SECONDS): Date {
    return Date(date.time + timeUnit.toMillis(time))
}

private val dateFormat = SimpleDateFormat("yyyy-MM-dd")
private val datetimeFormat = SimpleDateFormat(dateFormat.toPattern() + "'T'HH:mm:ss")
private val datetimezoneFormat = SimpleDateFormat(datetimeFormat.toPattern() + "ZZZZZ")

fun dateParseISO(date: String): Date {
    var builder = StringBuilder(date.trim())
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