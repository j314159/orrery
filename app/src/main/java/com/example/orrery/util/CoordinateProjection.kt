package com.example.orrery.util

import androidx.compose.ui.geometry.Offset
import kotlin.math.cos
import kotlin.math.sin

/**
 * Projects horizontal coordinates (altitude/azimuth) onto a circular sky map.
 *
 * The projection is linear zenithal:
 * - Zenith (altitude 90) maps to the center
 * - Horizon (altitude 0) maps to the edge
 * - North (azimuth 0) is at 12 o'clock
 * - East (azimuth 90) is at 3 o'clock
 */
fun projectToScreen(
    altitude: Double,
    azimuth: Double,
    centerX: Float,
    centerY: Float,
    radius: Float
): Offset {
    val r = radius * (90.0 - altitude) / 90.0
    val azRad = Math.toRadians(azimuth)

    val x = centerX + (r * sin(azRad)).toFloat()
    val y = centerY - (r * cos(azRad)).toFloat()

    return Offset(x, y)
}
