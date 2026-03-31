package com.example.orrery.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.Fill
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
import com.example.orrery.presentation.theme.SkyBackground
import com.example.orrery.util.projectToScreen
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun SkyMapCanvas(
    bodies: List<BodyPosition>,
    moonPhaseDegrees: Double
) {
    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val skyRadius = size.minDimension / 2f * 0.85f

        // Background
        drawRect(SkyBackground)

        // Horizon circle
        drawCircle(
            color = HorizonRing,
            radius = skyRadius,
            center = center,
            style = Stroke(width = 1.5f)
        )

        // Cardinal direction ticks and labels
        drawCardinalLabels(center, skyRadius, textMeasurer)

        // Compute screen positions for all bodies
        val positions = bodies.map { body ->
            body to projectToScreen(
                body.altitude, body.azimuth,
                center.x, center.y, skyRadius
            )
        }

        // Resolve overlaps by nudging colliding symbols apart
        val resolvedPositions = resolveOverlaps(positions)

        // Draw celestial bodies
        for ((body, pos) in resolvedPositions) {
            if (body.body == CelestialBody.MOON) {
                drawMoonPhase(pos, moonPhaseDegrees, 10f)
            } else {
                drawPlanetSymbol(body.body, pos, textMeasurer)
            }
        }
    }
}

private fun resolveOverlaps(
    positions: List<Pair<BodyPosition, Offset>>
): List<Pair<BodyPosition, Offset>> {
    val minDistance = 28f
    val resolved = positions.map { it.first to it.second.copy() }.toMutableList()

    // Run a few iterations of overlap resolution
    repeat(5) {
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
                    // Nearly identical positions — nudge apart vertically
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
    textMeasurer: TextMeasurer
) {
    val tickLength = 8f
    val cardinals = listOf(
        0.0 to "N",
        90.0 to "E",
        180.0 to "S",
        270.0 to "W"
    )

    val labelStyle = TextStyle(
        color = CardinalTickColor,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        fontFamily = FontFamily.Default
    )

    for ((azimuth, label) in cardinals) {
        val azRad = Math.toRadians(azimuth)
        val sinAz = sin(azRad).toFloat()
        val cosAz = cos(azRad).toFloat()

        // Tick mark
        val outerX = center.x + radius * sinAz
        val outerY = center.y - radius * cosAz
        val innerX = center.x + (radius - tickLength) * sinAz
        val innerY = center.y - (radius - tickLength) * cosAz

        drawLine(
            color = CardinalTickColor,
            start = Offset(innerX, innerY),
            end = Offset(outerX, outerY),
            strokeWidth = 2f
        )

        // Label just outside the horizon ring
        val measured = textMeasurer.measure(AnnotatedString(label), labelStyle)
        val labelDist = radius + 10f
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
        fontSize = 20.sp,
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
    val moonColor = Color(0xFFC0C0C0)
    val shadowColor = Color(0xFF0A0A14)

    // Illumination: 0 at new moon, 1 at full moon
    val illumination = (1.0 - cos(Math.toRadians(phaseDegrees))) / 2.0
    val isWaxing = phaseDegrees <= 180.0

    // Draw shadow base circle
    drawCircle(color = shadowColor, radius = radius, center = center, style = Fill)

    // Draw lit portion on top using two arcs:
    // 1. A semicircle on the lit limb side
    // 2. A semi-ellipse for the terminator (the curved shadow edge)
    //    Width of terminator ellipse = |2 * illumination - 1| * radius
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
            // Lit side is RIGHT
            // Right semicircle (limb) from top to bottom
            arcTo(boundingRect, -90f, 180f, forceMoveTo = true)
            // Terminator from bottom back to top
            if (illumination >= 0.5) {
                // Gibbous: terminator curves left (lit area is large)
                arcTo(terminatorRect, 90f, 180f, forceMoveTo = false)
            } else {
                // Crescent: terminator curves right (lit area is thin)
                arcTo(terminatorRect, 90f, -180f, forceMoveTo = false)
            }
        } else {
            // Lit side is LEFT
            // Left semicircle (limb) from bottom to top
            arcTo(boundingRect, 90f, 180f, forceMoveTo = true)
            // Terminator from top back to bottom
            if (illumination >= 0.5) {
                // Gibbous: terminator curves right
                arcTo(terminatorRect, -90f, 180f, forceMoveTo = false)
            } else {
                // Crescent: terminator curves left
                arcTo(terminatorRect, -90f, -180f, forceMoveTo = false)
            }
        }
        close()
    }

    drawPath(litPath, moonColor)
}
