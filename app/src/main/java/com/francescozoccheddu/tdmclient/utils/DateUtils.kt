package com.francescozoccheddu.tdmclient.utils

import java.util.*
import java.util.concurrent.TimeUnit


fun dateDiff(date1: Date, date2: Date, timeUnit: TimeUnit = TimeUnit.SECONDS): Long
{
    val diffMs = date2.getTime() - date1.getTime()
    return timeUnit.convert(diffMs, TimeUnit.MILLISECONDS)
}

fun dateElapsed(date: Date, timeUnit: TimeUnit = TimeUnit.SECONDS): Long
{
    return dateDiff(date, Date(), timeUnit)
}

fun dateAdd(date: Date, time: Long, timeUnit: TimeUnit = TimeUnit.SECONDS): Date
{
    return Date(date.time + timeUnit.toMillis(time))
}