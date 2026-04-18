package com.bakertelekom.portugaltowers.domain

import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class UserLocation(
    val latitude: Double,
    val longitude: Double,
)

fun distanceMeters(
    fromLatitude: Double,
    fromLongitude: Double,
    toLatitude: Double,
    toLongitude: Double,
): Double {
    val earthRadiusMeters = 6_371_000.0
    val deltaLat = Math.toRadians(toLatitude - fromLatitude)
    val deltaLon = Math.toRadians(toLongitude - fromLongitude)
    val fromLatRad = Math.toRadians(fromLatitude)
    val toLatRad = Math.toRadians(toLatitude)
    val a = sin(deltaLat / 2) * sin(deltaLat / 2) +
        cos(fromLatRad) * cos(toLatRad) * sin(deltaLon / 2) * sin(deltaLon / 2)
    return earthRadiusMeters * 2 * atan2(sqrt(a), sqrt(1 - a))
}

fun formatDistance(meters: Double): String =
    if (meters < 1000) {
        "${meters.toInt()} m"
    } else {
        "%.1f km".format(Locale.US, meters / 1000.0)
    }
