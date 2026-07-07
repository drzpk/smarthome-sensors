package dev.drzepka.smarthome.sensors.server.domain.util

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.roundToInt

fun Number.asInt(): Int = when (this) {
    is Int -> this
    is Long -> toInt()
    is Float -> roundToInt()
    is Double -> roundToInt()
    is BigDecimal -> setScale(0, RoundingMode.HALF_UP).toInt()
    else -> toInt()
}

fun Number.asFloat(): Float = when (this) {
    is Float -> this
    else -> toFloat()
}

fun Number.asDouble(): Double = when (this) {
    is Double -> this
    else -> toDouble()
}

fun Number.asBigDecimal(): BigDecimal = when (this) {
    is BigDecimal -> this
    is Float -> toBigDecimal()
    is Double -> toBigDecimal()
    else -> BigDecimal.valueOf(toLong())
}