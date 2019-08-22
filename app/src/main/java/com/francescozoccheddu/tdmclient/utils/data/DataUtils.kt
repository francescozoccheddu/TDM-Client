package com.francescozoccheddu.tdmclient.utils.data

import androidx.annotation.FloatRange
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round

fun travelDuration(@FloatRange(from = 0.0) distance: Float) = distance / 0.8f

