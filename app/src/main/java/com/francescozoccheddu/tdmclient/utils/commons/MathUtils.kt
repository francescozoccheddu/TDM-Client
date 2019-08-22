package com.francescozoccheddu.tdmclient.utils.commons

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round

fun Float.snap(snap: Float) = round(this / snap) * snap

fun Float.snapUp(snap: Float) = ceil(this / snap) * snap

fun Float.snapDown(snap: Float) = floor(this / snap) * snap