package com.example.orrery.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.example.orrery.astronomy.BodyPosition
import com.example.orrery.astronomy.CelestialBody
import com.example.orrery.presentation.theme.CardinalTickColor
import com.example.orrery.presentation.theme.HorizonRing
import com.example.orrery.util.projectToScreen
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// Sun altitude thresholds for sky gradient
private const val SUN_BELOW_HORIZON = -18.0  // astronomical twilight ends
private const val SUN_CIVIL_TWILIGHT = -6.0   // civil twilight
private const val SUN_AT_HORIZON = 0.0
private const val SUN_LOW = 10.0
private const val SUN_HIGH = 30.0

/**
 * @param compassAzimuth Device compass bearing in degrees (0=north, 90=east).
 *                       When non-null, the sky map rotates so the direction
 *                       the user faces is at the top of the watch.
 */
@Composable
fun SkyMapCanvas(
    bodies: List<BodyPosition>,
    moonPhaseDegrees: Double,
    sunAltitude: Double,
    compassAzimuth: Float? = null
) {
    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val skyRadius = size.minDimension / 2f * 0.82f

        // Time-of-day background gradient (not rotated)
        drawSkyBackground(center, size, sunAltitude)

        // Horizon circle
        val horizonColor = if (sunAltitude > SUN_AT_HORIZON) {
            HorizonRing.copy(alpha = 0.5f)
        } else {
            HorizonRing
        }
        drawCircle(
            color = horizonColor,
            radius = skyRadius,
            center = center,
            style = Stroke(width = 2f)
        )

        // The rotation offset: when compass says we're facing east (90°),
        // east should be at the top, so we subtract the compass azimuth
        // from each body's azimuth before projecting.
        val rotationOffset = compassAzimuth?.toDouble() ?: 0.0

        // Cardinal direction ticks and labels (rotated)
        drawCardinalLabels(center, skyRadius, textMeasurer, sunAltitude, rotationOffset)

        // Compute screen positions with compass rotation applied
        val positions = bodies.map { body ->
            body to projectToScreen(
                body.altitude,
                body.azimuth - rotationOffset,
                center.x, center.y, skyRadius
            )
        }

        // Resolve overlaps by nudging colliding symbols apart
        val resolvedPositions = resolveOverlaps(positions)

        // Draw celestial bodies
        for ((body, pos) in resolvedPositions) {
            if (body.body == CelestialBody.MOON) {
                drawMoonPhase(pos, moonPhaseDegrees, 14f)
            } else {
                drawPlanetSymbol(body.body, pos, textMeasurer)
            }
        }
    }
}

private fun DrawScope.drawSkyBackground(center: Offset, size: Size, sunAltitude: Double) {
    when {
        // Full night — dark sky
        sunAltitude <= SUN_BELOW_HORIZON -> {
            drawRect(Color(0xFF06060F))
        }
        // Astronomical/nautical twilight — very deep blue
        sunAltitude <= SUN_CIVIL_TWILIGHT -> {
            val t = ((sunAltitude - SUN_BELOW_HORIZON) / (SUN_CIVIL_TWILIGHT - SUN_BELOW_HORIZON)).toFloat()
            val topColor = lerp(Color(0xFF06060F), Color(0xFF0A1628), t)
            val bottomColor = lerp(Color(0xFF06060F), Color(0xFF1A1A3A), t)
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(topColor, bottomColor),
                    startY = 0f,
                    endY = size.height
                )
            )
        }
        // Civil twilight — deep blue to warm horizon
        sunAltitude <= SUN_AT_HORIZON -> {
            val t = ((sunAltitude - SUN_CIVIL_TWILIGHT) / (SUN_AT_HORIZON - SUN_CIVIL_TWILIGHT)).toFloat()
            val topColor = lerp(Color(0xFF0A1628), Color(0xFF1B2844), t)
            val bottomColor = lerp(Color(0xFF1A1A3A), Color(0xFFCC6633), t)
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(topColor, bottomColor),
                    startY = 0f,
                    endY = size.height
                )
            )
        }
        // Sun just above horizon — sunrise/sunset colors
        sunAltitude <= SUN_LOW -> {
            val t = ((sunAltitude - SUN_AT_HORIZON) / (SUN_LOW - SUN_AT_HORIZON)).toFloat()
            val topColor = lerp(Color(0xFF1B2844), Color(0xFF3A6BAF), t)
            val bottomColor = lerp(Color(0xFFCC6633), Color(0xFF87CEEB), t)
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(topColor, bottomColor),
                    startY = 0f,
                    endY = size.height
                )
            )
        }
        // Sun high — full daylight blue
        sunAltitude <= SUN_HIGH -> {
            val t = ((sunAltitude - SUN_LOW) / (SUN_HIGH - SUN_LOW)).toFloat()
            val skyBlue = lerp(Color(0xFF3A6BAF), Color(0xFF4A90D9), t)
            val lowerBlue = lerp(Color(0xFF87CEEB), Color(0xFF6CB4EE), t)
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(skyBlue, lowerBlue),
                    startY = 0f,
                    endY = size.height
                )
            )
        }
        // Full daylight
        else -> {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF4A90D9), Color(0xFF6CB4EE)),
                    startY = 0f,
                    endY = size.height
                )
            )
        }
    }
}

private fun lerp(a: Color, b: Color, t: Float): Color {
    val clamped = t.coerceIn(0f, 1f)
    return Color(
        red = a.red + (b.red - a.red) * clamped,
        green = a.green + (b.green - a.green) * clamped,
        blue = a.blue + (b.blue - a.blue) * clamped,
        alpha = 1f
    )
}

private fun resolveOverlaps(
    positions: List<Pair<BodyPosition, Offset>>
): List<Pair<BodyPosition, Offset>> {
    val minDistance = 36f
    val resolved = positions.map { it.first to it.second.copy() }.toMutableList()

    repeat(6) {
        for (i in resolved.indices) {
            for (j in i + 1 until resolved.size) {
                val (_, posA) = resolved[i]
                val (_, posB) = resolved[j]

                val dx = posB.x - posA.x
                val dy = posB.y - posA.y
                val dist = sqrt(dx * dx + dy * dy)

                if (dist < minDistance && dist > 0.01f) {
                    val overlap = (minDistance - dist) / 2f
                    val nx = dx / dist
                    val ny = dy / dist

                    resolved[i] = resolved[i].first to Offset(
                        posA.x - nx * overlap,
                        posA.y - ny * overlap
                    )
                    resolved[j] = resolved[j].first to Offset(
                        posB.x + nx * overlap,
                        posB.y + ny * overlap
                    )
                } else if (dist <= 0.01f) {
                    resolved[i] = resolved[i].first to Offset(posA.x, posA.y - minDistance / 2f)
                    resolved[j] = resolved[j].first to Offset(posB.x, posB.y + minDistance / 2f)
                }
            }
        }
    }

    return resolved
}

private fun DrawScope.drawCardinalLabels(
    center: Offset,
    radius: Float,
    textMeasurer: TextMeasurer,
    sunAltitude: Double,
    rotationOffset: Double = 0.0
) {
    val tickLength = 10f
    val cardinals = listOf(
        0.0 to "N",
        90.0 to "E",
        180.0 to "S",
        270.0 to "W"
    )

    // Brighter labels during daytime for contrast against blue sky
    val labelColor = if (sunAltitude > SUN_AT_HORIZON) {
        Color(0xCCFFFFFF)
    } else {
        CardinalTickColor
    }

    val labelStyle = TextStyle(
        color = labelColor,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Default
    )

    for ((azimuth, label) in cardinals) {
        val rotatedAz = azimuth - rotationOffset
        val azRad = Math.toRadians(rotatedAz)
        val sinAz = sin(azRad).toFloat()
        val cosAz = cos(azRad).toFloat()

        // Tick mark
        val outerX = center.x + radius * sinAz
        val outerY = center.y - radius * cosAz
        val innerX = center.x + (radius - tickLength) * sinAz
        val innerY = center.y - (radius - tickLength) * cosAz

        drawLine(
            color = labelColor,
            start = Offset(innerX, innerY),
            end = Offset(outerX, outerY),
            strokeWidth = 2.5f
        )

        // Label just outside the horizon ring
        val measured = textMeasurer.measure(AnnotatedString(label), labelStyle)
        val labelDist = radius + 14f
        val labelX = center.x + labelDist * sinAz - measured.size.width / 2f
        val labelY = center.y - labelDist * cosAz - measured.size.height / 2f

        drawText(
            textLayoutResult = measured,
            topLeft = Offset(labelX, labelY)
        )
    }
}

private fun DrawScope.drawPlanetSymbol(
    body: CelestialBody,
    position: Offset,
    textMeasurer: TextMeasurer
) {
    val style = TextStyle(
        color = body.displayColor,
        fontSize = 26.sp,
        fontFamily = FontFamily.Default
    )
    val measured = textMeasurer.measure(AnnotatedString(body.symbol), style)
    drawText(
        textLayoutResult = measured,
        topLeft = Offset(
            position.x - measured.size.width / 2f,
            position.y - measured.size.height / 2f
        )
    )
}

private fun DrawScope.drawMoonPhase(
    center: Offset,
    phaseDegrees: Double,
    radius: Float
) {
    val moonColor = Color(0xFFE8E8D0)
    val shadowColor = Color(0xFF2A2A35)

    // Illumination: 0 at new moon, 1 at full moon
    val illumination = (1.0 - cos(Math.toRadians(phaseDegrees))) / 2.0
    val isWaxing = phaseDegrees <= 180.0

    // Subtle glow around the moon
    drawCircle(
        color = Color(0x20E8E8D0),
        radius = radius + 4f,
        center = center,
        style = Fill
    )

    // Draw shadow base circle
    drawCircle(color = shadowColor, radius = radius, center = center, style = Fill)

    // Draw lit portion using two arcs:
    // 1. A semicircle on the lit limb side
    // 2. A semi-ellipse for the terminator
    val boundingRect = Rect(
        center.x - radius, center.y - radius,
        center.x + radius, center.y + radius
    )

    val terminatorWidth = radius * abs(2f * illumination.toFloat() - 1f)
    val terminatorRect = Rect(
        center.x - terminatorWidth, center.y - radius,
        center.x + terminatorWidth, center.y + radius
    )

    val litPath = Path().apply {
        if (isWaxing) {
            arcTo(boundingRect, -90f, 180f, forceMoveTo = true)
            if (illumination >= 0.5) {
                arcTo(terminatorRect, 90f, 180f, forceMoveTo = false)
            } else {
                arcTo(terminatorRect, 90f, -180f, forceMoveTo = false)
            }
        } else {
            arcTo(boundingRect, 90f, 180f, forceMoveTo = true)
            if (illumination >= 0.5) {
                arcTo(terminatorRect, -90f, 180f, forceMoveTo = false)
            } else {
                arcTo(terminatorRect, -90f, -180f, forceMoveTo = false)
            }
        }
        close()
    }

    drawPath(litPath, moonColor)

    // Thin outline for definition
    drawCircle(
        color = Color(0x40FFFFFF),
        radius = radius,
        center = center,
        style = Stroke(width = 1f)
    )
}
