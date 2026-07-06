package ai.joshmiller.orrery.presentation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import ai.joshmiller.orrery.astronomy.CelestialBody
import kotlin.math.cos
import kotlin.math.sin

/**
 * Draws a tiny illustrated planet at the given position.
 * Each planet has a recognizable mini-illustration:
 * small sphere with distinctive features.
 */
fun DrawScope.drawPlanetIllustration(
    body: CelestialBody,
    position: Offset,
    baseRadius: Float = 10f
) {
    when (body) {
        CelestialBody.SUN -> drawSunIllustration(position, baseRadius * 1.3f)
        CelestialBody.MERCURY -> drawMercuryIllustration(position, baseRadius * 0.7f)
        CelestialBody.VENUS -> drawVenusIllustration(position, baseRadius * 0.9f)
        CelestialBody.MARS -> drawMarsIllustration(position, baseRadius * 0.8f)
        CelestialBody.JUPITER -> drawJupiterIllustration(position, baseRadius * 1.2f)
        CelestialBody.SATURN -> drawSaturnIllustration(position, baseRadius * 1.0f)
        CelestialBody.URANUS -> drawUranusIllustration(position, baseRadius * 0.9f)
        CelestialBody.NEPTUNE -> drawNeptuneIllustration(position, baseRadius * 0.85f)
        CelestialBody.MOON -> { /* Moon handled separately */ }
    }
}

// Sun: glowing yellow-orange disc with corona rays
private fun DrawScope.drawSunIllustration(center: Offset, radius: Float) {
    // Outer glow
    drawCircle(
        color = Color(0x30FFD700),
        radius = radius * 1.8f,
        center = center,
        style = Fill
    )
    // Corona rays
    val rayColor = Color(0x60FFA500)
    for (i in 0 until 8) {
        val angle = Math.toRadians(i * 45.0)
        val innerR = radius * 1.1f
        val outerR = radius * 1.6f
        drawLine(
            color = rayColor,
            start = Offset(
                center.x + innerR * cos(angle).toFloat(),
                center.y + innerR * sin(angle).toFloat()
            ),
            end = Offset(
                center.x + outerR * cos(angle).toFloat(),
                center.y + outerR * sin(angle).toFloat()
            ),
            strokeWidth = 1.5f
        )
    }
    // Main disc
    drawCircle(color = Color(0xFFFFD700), radius = radius, center = center, style = Fill)
    // Bright center
    drawCircle(color = Color(0xFFFFF0A0), radius = radius * 0.5f, center = center, style = Fill)
}

// Mercury: small gray cratered sphere
private fun DrawScope.drawMercuryIllustration(center: Offset, radius: Float) {
    drawCircle(color = Color(0xFF909090), radius = radius, center = center, style = Fill)
    // Craters — small darker circles
    drawCircle(
        color = Color(0xFF707070),
        radius = radius * 0.25f,
        center = Offset(center.x - radius * 0.3f, center.y - radius * 0.2f),
        style = Fill
    )
    drawCircle(
        color = Color(0xFF686868),
        radius = radius * 0.2f,
        center = Offset(center.x + radius * 0.25f, center.y + radius * 0.3f),
        style = Fill
    )
    drawCircle(
        color = Color(0xFF757575),
        radius = radius * 0.15f,
        center = Offset(center.x + radius * 0.1f, center.y - radius * 0.4f),
        style = Fill
    )
    // Subtle outline
    drawCircle(color = Color(0x40FFFFFF), radius = radius, center = center, style = Stroke(0.5f))
}

// Venus: yellowish-white cloudy sphere with swirl hints
private fun DrawScope.drawVenusIllustration(center: Offset, radius: Float) {
    drawCircle(color = Color(0xFFE8D8A0), radius = radius, center = center, style = Fill)
    // Cloud bands — subtle lighter arcs
    val bandPath = Path().apply {
        val bandRect = Rect(
            center.x - radius * 0.8f, center.y - radius * 0.3f,
            center.x + radius * 0.6f, center.y + radius * 0.3f
        )
        arcTo(bandRect, -30f, 120f, forceMoveTo = true)
    }
    drawPath(bandPath, Color(0x40FFFFFF), style = Stroke(1.5f))
    val bandPath2 = Path().apply {
        val bandRect = Rect(
            center.x - radius * 0.5f, center.y + radius * 0.0f,
            center.x + radius * 0.9f, center.y + radius * 0.6f
        )
        arcTo(bandRect, 160f, 100f, forceMoveTo = true)
    }
    drawPath(bandPath2, Color(0x30FFFFFF), style = Stroke(1.2f))
    // Soft glow
    drawCircle(color = Color(0x15FFFFFF), radius = radius * 1.3f, center = center, style = Fill)
    drawCircle(color = Color(0x40FFFFFF), radius = radius, center = center, style = Stroke(0.5f))
}

// Mars: rusty red with polar cap hint
private fun DrawScope.drawMarsIllustration(center: Offset, radius: Float) {
    drawCircle(color = Color(0xFFC1440E), radius = radius, center = center, style = Fill)
    // Darker region (Syrtis Major-ish)
    drawCircle(
        color = Color(0xFF8B2500),
        radius = radius * 0.35f,
        center = Offset(center.x + radius * 0.1f, center.y + radius * 0.15f),
        style = Fill
    )
    // North polar cap
    val capPath = Path().apply {
        val capRect = Rect(
            center.x - radius * 0.4f, center.y - radius * 1.1f,
            center.x + radius * 0.4f, center.y - radius * 0.4f
        )
        arcTo(capRect, 0f, 180f, forceMoveTo = true)
        close()
    }
    drawPath(capPath, Color(0xCCE8E8E8))
    drawCircle(color = Color(0x40FFFFFF), radius = radius, center = center, style = Stroke(0.5f))
}

// Jupiter: banded with red spot
private fun DrawScope.drawJupiterIllustration(center: Offset, radius: Float) {
    // Base color — warm tan/orange
    drawCircle(color = Color(0xFFD4A574), radius = radius, center = center, style = Fill)

    // Horizontal bands
    val bands = listOf(
        -0.6f to Color(0xFFC49A6C),
        -0.3f to Color(0xFFB8875A),
        0.0f to Color(0xFFD4A574),
        0.25f to Color(0xFFAA7744),
        0.5f to Color(0xFFC49A6C)
    )
    for ((yFrac, color) in bands) {
        val bandY = center.y + radius * yFrac
        val bandHalfWidth = radius * cos(Math.asin(yFrac.toDouble())).toFloat()
        drawLine(
            color = color,
            start = Offset(center.x - bandHalfWidth, bandY),
            end = Offset(center.x + bandHalfWidth, bandY),
            strokeWidth = radius * 0.2f
        )
    }

    // Great Red Spot
    drawOval(
        color = Color(0xFFCC5533),
        topLeft = Offset(center.x + radius * 0.05f, center.y + radius * 0.1f),
        size = Size(radius * 0.45f, radius * 0.25f)
    )
    // Re-clip to circle with outline
    drawCircle(color = Color(0x40FFFFFF), radius = radius, center = center, style = Stroke(0.5f))
}

// Saturn: sphere with iconic ring
private fun DrawScope.drawSaturnIllustration(center: Offset, radius: Float) {
    // Ring behind planet (top half)
    val ringPath = Path().apply {
        val outerRect = Rect(
            center.x - radius * 2.0f, center.y - radius * 0.5f,
            center.x + radius * 2.0f, center.y + radius * 0.5f
        )
        arcTo(outerRect, 180f, 180f, forceMoveTo = true)
        val innerRect = Rect(
            center.x - radius * 1.5f, center.y - radius * 0.3f,
            center.x + radius * 1.5f, center.y + radius * 0.3f
        )
        arcTo(innerRect, 0f, -180f, forceMoveTo = false)
        close()
    }
    drawPath(ringPath, Color(0xFFA09060))

    // Planet body
    drawCircle(color = Color(0xFFD4BC82), radius = radius, center = center, style = Fill)
    // Subtle band
    drawLine(
        color = Color(0xFFC4A862),
        start = Offset(center.x - radius * 0.8f, center.y - radius * 0.15f),
        end = Offset(center.x + radius * 0.8f, center.y - radius * 0.15f),
        strokeWidth = radius * 0.15f
    )

    // Ring in front of planet (bottom half)
    val frontRingPath = Path().apply {
        val outerRect = Rect(
            center.x - radius * 2.0f, center.y - radius * 0.5f,
            center.x + radius * 2.0f, center.y + radius * 0.5f
        )
        arcTo(outerRect, 0f, 180f, forceMoveTo = true)
        val innerRect = Rect(
            center.x - radius * 1.5f, center.y - radius * 0.3f,
            center.x + radius * 1.5f, center.y + radius * 0.3f
        )
        arcTo(innerRect, 180f, -180f, forceMoveTo = false)
        close()
    }
    drawPath(frontRingPath, Color(0xFFA09060))

    drawCircle(color = Color(0x40FFFFFF), radius = radius, center = center, style = Stroke(0.5f))
}

// Uranus: pale cyan/teal sphere with subtle tilt ring
private fun DrawScope.drawUranusIllustration(center: Offset, radius: Float) {
    drawCircle(color = Color(0xFF7EC8D8), radius = radius, center = center, style = Fill)
    // Subtle lighter band
    drawCircle(color = Color(0x30FFFFFF), radius = radius * 0.6f, center = center, style = Fill)
    // Tilted ring (nearly vertical — Uranus's axial tilt)
    val ringRect = Rect(
        center.x - radius * 0.3f, center.y - radius * 1.6f,
        center.x + radius * 0.3f, center.y + radius * 1.6f
    )
    drawOval(
        color = Color(0x60A0D0E0),
        topLeft = Offset(ringRect.left, ringRect.top),
        size = Size(ringRect.width, ringRect.height),
        style = Stroke(1.0f)
    )
    drawCircle(color = Color(0x40FFFFFF), radius = radius, center = center, style = Stroke(0.5f))
}

// Neptune: deep blue sphere
private fun DrawScope.drawNeptuneIllustration(center: Offset, radius: Float) {
    drawCircle(color = Color(0xFF3050B0), radius = radius, center = center, style = Fill)
    // Lighter atmospheric band
    drawLine(
        color = Color(0x505080D0),
        start = Offset(center.x - radius * 0.7f, center.y + radius * 0.2f),
        end = Offset(center.x + radius * 0.7f, center.y + radius * 0.2f),
        strokeWidth = radius * 0.15f
    )
    // Dark spot hint
    drawCircle(
        color = Color(0xFF253A80),
        radius = radius * 0.2f,
        center = Offset(center.x - radius * 0.2f, center.y - radius * 0.1f),
        style = Fill
    )
    drawCircle(color = Color(0x40FFFFFF), radius = radius, center = center, style = Stroke(0.5f))
}
