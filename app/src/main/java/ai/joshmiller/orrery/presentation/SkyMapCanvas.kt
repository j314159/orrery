package ai.joshmiller.orrery.presentation

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
import ai.joshmiller.orrery.astronomy.BodyPosition
import ai.joshmiller.orrery.astronomy.CONSTELLATIONS
import ai.joshmiller.orrery.astronomy.CelestialBody
import ai.joshmiller.orrery.astronomy.CelestialCalculator.StarPosition
import ai.joshmiller.orrery.astronomy.EclipticPoint
import ai.joshmiller.orrery.presentation.theme.CardinalTickColor
import ai.joshmiller.orrery.presentation.theme.HorizonRing
import ai.joshmiller.orrery.settings.DisplaySettings
import ai.joshmiller.orrery.settings.MoonStyle
import ai.joshmiller.orrery.settings.PlanetStyle
import ai.joshmiller.orrery.settings.StarVisibility
import ai.joshmiller.orrery.util.projectToScreen
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
 * @param rotationDegrees Manual rotation from the rotary crown, in degrees.
 *                        0 = north at top (default). Turning the crown rotates
 *                        the sky map so you can align it with the direction
 *                        you're facing.
 */
@Composable
fun SkyMapCanvas(
    bodies: List<BodyPosition>,
    moonPhaseDegrees: Double,
    sunAltitude: Double,
    sunAzimuth: Double = 180.0,
    eclipticPoints: List<EclipticPoint> = emptyList(),
    stars: List<StarPosition> = emptyList(),
    rotationDegrees: Float = 0f,
    displaySettings: DisplaySettings = DisplaySettings(),
    southernHemisphere: Boolean = false
) {
    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val skyRadius = size.minDimension / 2f * 0.82f

        // Time-of-day background gradient (rotates with crown)
        drawSkyBackground(center, size, sunAltitude, sunAzimuth, rotationDegrees.toDouble())

        // Horizon circle
        val horizonColor = if (sunAltitude > SUN_AT_HORIZON) {
            Color(0x80FFFFFF)  // semi-transparent white for daytime visibility
        } else {
            HorizonRing
        }
        drawCircle(
            color = horizonColor,
            radius = skyRadius,
            center = center,
            style = Stroke(width = 4f)
        )

        val rotationOffset = rotationDegrees.toDouble()

        // Cardinal direction ticks and labels (rotated)
        drawCardinalLabels(center, skyRadius, textMeasurer, sunAltitude, rotationOffset)

        // Ecliptic line — the orbital plane of the solar system
        drawEclipticLine(eclipticPoints, center, skyRadius, rotationOffset, sunAltitude)

        // Stars and constellations — visibility controlled by setting
        val showStars = when (displaySettings.starVisibility) {
            StarVisibility.ALWAYS -> true
            StarVisibility.NIGHT_ONLY -> sunAltitude < SUN_CIVIL_TWILIGHT
            StarVisibility.NEVER -> false
        }

        if (showStars) {
            // Alpha: full brightness at night, fade during twilight, dim during daytime (for ALWAYS mode)
            val starAlpha = when {
                sunAltitude <= SUN_BELOW_HORIZON -> 1f
                sunAltitude < SUN_CIVIL_TWILIGHT -> {
                    // Fade stars in during twilight
                    ((SUN_CIVIL_TWILIGHT - sunAltitude) / (SUN_CIVIL_TWILIGHT - SUN_BELOW_HORIZON)).toFloat()
                }
                else -> 0.35f // Daytime — dim but visible when ALWAYS is set
            }

            // Build star position lookup for constellation rendering
            val starScreenPositions: Map<String, Offset> = stars.associate { star ->
                star.name to projectToScreen(
                    star.altitude,
                    star.azimuth - rotationOffset,
                    center.x, center.y, skyRadius
                )
            }

            // Constellation lines (drawn behind stars)
            if (displaySettings.showConstellationLines) {
                drawConstellationLines(starScreenPositions, starAlpha, skyRadius, center)
                if (displaySettings.showConstellationLabels) {
                    drawConstellationLabels(starScreenPositions, starAlpha, textMeasurer, skyRadius, center)
                }
            }

            drawStars(stars, center, skyRadius, rotationOffset, starAlpha)
        }

        // Compute screen positions with rotation applied
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
                when (displaySettings.moonStyle) {
                    MoonStyle.DYNAMIC -> drawMoonPhase(pos, moonPhaseDegrees, 14f, southernHemisphere)
                    MoonStyle.PHASE_ICON -> drawMoonPhaseIcon(pos, moonPhaseDegrees, 14f, southernHemisphere)
                    MoonStyle.ALCHEMICAL -> drawPlanetSymbol(CelestialBody.MOON, pos, textMeasurer)
                }
            } else {
                when (displaySettings.planetStyle) {
                    PlanetStyle.SYMBOLS -> drawPlanetSymbol(body.body, pos, textMeasurer)
                    PlanetStyle.ILLUSTRATED -> drawPlanetIllustration(body.body, pos)
                }
            }

            // Draw label below the body if enabled
            if (displaySettings.showLabels) {
                drawBodyLabel(body.body, pos, textMeasurer, sunAltitude)
            }
        }
    }
}

private fun DrawScope.drawSkyBackground(
    center: Offset,
    size: Size,
    sunAltitude: Double,
    sunAzimuth: Double,
    rotationOffset: Double
) {
    // Full night — no gradient needed, just dark sky
    if (sunAltitude <= SUN_BELOW_HORIZON) {
        drawRect(Color(0xFF06060F))
        return
    }

    // For twilight and daytime, we create a directional gradient that
    // radiates from where the sun actually is. The gradient goes from
    // the sun's direction (warm/bright) to the opposite side (dark/cool).
    //
    // Sun azimuth is rotated by the crown offset so the gradient moves
    // with the sky when the user turns the crown.
    val rotatedAzRad = Math.toRadians(sunAzimuth - rotationOffset)

    // Direction vector pointing from center toward the sun's azimuth on the horizon
    // Azimuth convention: 0=N (top), 90=E (right), 180=S (bottom), 270=W (left)
    val sunDirX = sin(rotatedAzRad).toFloat()
    val sunDirY = -cos(rotatedAzRad).toFloat()

    // Gradient start = sun side (extend to edge of canvas), end = opposite side
    val extent = size.maxDimension / 2f
    val startX = center.x + sunDirX * extent
    val startY = center.y + sunDirY * extent
    val endX = center.x - sunDirX * extent
    val endY = center.y - sunDirY * extent

    // Compute the two gradient colors based on sun altitude
    val (sunSideColor, oppositeSideColor) = when {
        // Astronomical/nautical twilight — subtle warm glow toward sun
        sunAltitude <= SUN_CIVIL_TWILIGHT -> {
            val t = ((sunAltitude - SUN_BELOW_HORIZON) / (SUN_CIVIL_TWILIGHT - SUN_BELOW_HORIZON)).toFloat()
            val sunSide = lerp(Color(0xFF06060F), Color(0xFF1A1A3A), t)
            val opposite = lerp(Color(0xFF06060F), Color(0xFF0A1628), t)
            sunSide to opposite
        }
        // Civil twilight — warm horizon glow near sun, deep blue opposite
        sunAltitude <= SUN_AT_HORIZON -> {
            val t = ((sunAltitude - SUN_CIVIL_TWILIGHT) / (SUN_AT_HORIZON - SUN_CIVIL_TWILIGHT)).toFloat()
            val sunSide = lerp(Color(0xFF1A1A3A), Color(0xFFCC6633), t)
            val opposite = lerp(Color(0xFF0A1628), Color(0xFF1B2844), t)
            sunSide to opposite
        }
        // Sun just above horizon — sunrise/sunset
        sunAltitude <= SUN_LOW -> {
            val t = ((sunAltitude - SUN_AT_HORIZON) / (SUN_LOW - SUN_AT_HORIZON)).toFloat()
            val sunSide = lerp(Color(0xFFCC6633), Color(0xFF87CEEB), t)
            val opposite = lerp(Color(0xFF1B2844), Color(0xFF3A6BAF), t)
            sunSide to opposite
        }
        // Sun moderately high — mostly blue, slightly brighter toward sun
        sunAltitude <= SUN_HIGH -> {
            val t = ((sunAltitude - SUN_LOW) / (SUN_HIGH - SUN_LOW)).toFloat()
            val sunSide = lerp(Color(0xFF87CEEB), Color(0xFF6CB4EE), t)
            val opposite = lerp(Color(0xFF3A6BAF), Color(0xFF4A90D9), t)
            sunSide to opposite
        }
        // Full daylight — uniform blue (slight directional variation)
        else -> {
            Color(0xFF6CB4EE) to Color(0xFF4A90D9)
        }
    }

    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(sunSideColor, oppositeSideColor),
            start = Offset(startX, startY),
            end = Offset(endX, endY)
        )
    )
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

private fun DrawScope.drawEclipticLine(
    points: List<EclipticPoint>,
    center: Offset,
    skyRadius: Float,
    rotationOffset: Double,
    sunAltitude: Double
) {
    if (points.isEmpty()) return

    // Muted warm color — subtle enough to not distract, visible enough to read
    val eclipticColor = if (sunAltitude > SUN_AT_HORIZON) {
        Color(0xAAFFD700)  // visible gold during daytime (~67% opacity)
    } else {
        Color(0x80C8A050)  // warm gold at ~50% opacity at night
    }

    // Project all points and track which are above the horizon
    data class ScreenPoint(val offset: Offset, val aboveHorizon: Boolean)

    val screenPoints = points.map { pt ->
        val pos = projectToScreen(
            pt.altitude,
            pt.azimuth - rotationOffset,
            center.x, center.y, skyRadius
        )
        ScreenPoint(pos, pt.altitude > 0.0)
    }

    // Draw connected segments for runs of above-horizon points
    var inSegment = false
    val path = Path()

    for (i in screenPoints.indices) {
        val pt = screenPoints[i]
        if (pt.aboveHorizon) {
            if (!inSegment) {
                path.moveTo(pt.offset.x, pt.offset.y)
                inSegment = true
            } else {
                path.lineTo(pt.offset.x, pt.offset.y)
            }
        } else {
            inSegment = false
        }
    }

    drawPath(
        path = path,
        color = eclipticColor,
        style = Stroke(width = 3.5f)
    )
}

/**
 * Draws stars as tiny dots sized by magnitude.
 * Brighter stars (lower magnitude) get larger, brighter dots.
 */
private fun DrawScope.drawStars(
    stars: List<StarPosition>,
    center: Offset,
    skyRadius: Float,
    rotationOffset: Double,
    alpha: Float
) {
    for (star in stars) {
        val pos = projectToScreen(
            star.altitude,
            star.azimuth - rotationOffset,
            center.x, center.y, skyRadius
        )

        // Size by magnitude: brightest (mag -1.5) → ~3px radius, dimmest (mag 4) → ~0.5px
        val magRange = 5.5f  // from -1.5 to 4.0
        val normalizedBrightness = ((4.0 - star.magnitude) / magRange).toFloat().coerceIn(0f, 1f)
        val starRadius = 0.5f + normalizedBrightness * 2.5f

        // Brightness also affects opacity — dim stars are more transparent
        val starAlpha = (0.3f + normalizedBrightness * 0.7f) * alpha

        // Slight warm tint for the very brightest stars, white for the rest
        val starColor = if (star.magnitude < 1.0) {
            Color(0xFFE8E0D0).copy(alpha = starAlpha)
        } else {
            Color.White.copy(alpha = starAlpha)
        }

        drawCircle(
            color = starColor,
            radius = starRadius,
            center = pos,
            style = Fill
        )

        // Subtle glow for the brightest stars (mag < 0.5)
        if (star.magnitude < 0.5 && alpha > 0.5f) {
            drawCircle(
                color = Color.White.copy(alpha = starAlpha * 0.15f),
                radius = starRadius * 3f,
                center = pos,
                style = Fill
            )
        }
    }
}

/**
 * Draws constellation stick-figure lines between star pairs.
 * Only draws a line segment if both stars are above the horizon
 * (i.e., both have screen positions in our lookup).
 */
private fun DrawScope.drawConstellationLines(
    starPositions: Map<String, Offset>,
    alpha: Float,
    skyRadius: Float,
    center: Offset
) {
    val lineColor = Color(0xFF7799DD).copy(alpha = 0.7f * alpha)

    for (constellation in CONSTELLATIONS) {
        for ((starA, starB) in constellation.lines) {
            val posA = starPositions[starA] ?: continue
            val posB = starPositions[starB] ?: continue

            // Only draw if both points are within the horizon circle
            val distA = sqrt((posA.x - center.x).let { it * it } + (posA.y - center.y).let { it * it })
            val distB = sqrt((posB.x - center.x).let { it * it } + (posB.y - center.y).let { it * it })
            if (distA > skyRadius || distB > skyRadius) continue

            drawLine(
                color = lineColor,
                start = posA,
                end = posB,
                strokeWidth = 2.8f
            )
        }
    }
}

/**
 * Draws constellation name labels at the centroid of each constellation's
 * visible stars. Only labels constellations where at least 2 stars are visible.
 */
private fun DrawScope.drawConstellationLabels(
    starPositions: Map<String, Offset>,
    alpha: Float,
    textMeasurer: TextMeasurer,
    skyRadius: Float,
    center: Offset
) {
    val labelColor = Color(0xFFAABBEE).copy(alpha = 0.95f * alpha)
    val labelStyle = TextStyle(
        color = labelColor,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = FontFamily.Default
    )

    for (constellation in CONSTELLATIONS) {
        // Collect all visible star positions for this constellation
        val allStarNames = constellation.lines.flatMap { (a, b) -> listOf(a, b) }.distinct()
        val visiblePositions = allStarNames.mapNotNull { name ->
            val pos = starPositions[name] ?: return@mapNotNull null
            val dist = sqrt((pos.x - center.x).let { it * it } + (pos.y - center.y).let { it * it })
            if (dist <= skyRadius) pos else null
        }

        if (visiblePositions.size < 2) continue

        // Place label at centroid of visible stars
        val centroidX = visiblePositions.map { it.x }.average().toFloat()
        val centroidY = visiblePositions.map { it.y }.average().toFloat()

        val measured = textMeasurer.measure(AnnotatedString(constellation.name), labelStyle)
        drawText(
            textLayoutResult = measured,
            topLeft = Offset(
                centroidX - measured.size.width / 2f,
                centroidY - measured.size.height / 2f - 12f  // slightly above centroid
            )
        )
    }
}

private fun resolveOverlaps(
    positions: List<Pair<BodyPosition, Offset>>
): List<Pair<BodyPosition, Offset>> {
    val minDistance = 42f
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

    // Strong contrast in all conditions
    val labelColor = if (sunAltitude > SUN_AT_HORIZON) {
        Color(0xEEFFFFFF)
    } else {
        Color(0xCC999999)
    }

    val labelStyle = TextStyle(
        color = labelColor,
        fontSize = 22.sp,
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
            strokeWidth = 3.5f
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
        fontSize = 30.sp,
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

private fun DrawScope.drawBodyLabel(
    body: CelestialBody,
    position: Offset,
    textMeasurer: TextMeasurer,
    sunAltitude: Double
) {
    val labelColor = if (sunAltitude > SUN_AT_HORIZON) {
        Color(0xEEFFFFFF)  // strong white for daytime contrast
    } else {
        Color(0xDDDDDDDD)  // bright gray at night
    }

    val style = TextStyle(
        color = labelColor,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Default
    )
    val measured = textMeasurer.measure(AnnotatedString(body.displayName), style)

    // Position the label just below the symbol/illustration
    val labelOffset = 18f
    drawText(
        textLayoutResult = measured,
        topLeft = Offset(
            position.x - measured.size.width / 2f,
            position.y + labelOffset
        )
    )
}

private fun DrawScope.drawMoonPhase(
    center: Offset,
    phaseDegrees: Double,
    radius: Float,
    southernHemisphere: Boolean = false
) {
    val moonColor = Color(0xFFE8E8D0)
    val shadowColor = Color(0xFF2A2A35)

    // Illumination: 0 at new moon, 1 at full moon
    val illumination = (1.0 - cos(Math.toRadians(phaseDegrees))) / 2.0
    val isWaxing = phaseDegrees <= 180.0
    // A waxing moon is lit on the right in the northern hemisphere,
    // on the left in the southern
    val litOnRight = isWaxing != southernHemisphere

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
        if (litOnRight) {
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

/**
 * Draws one of 8 discrete moon phase icons based on the phase angle.
 * Snaps the continuous phase to the nearest of 8 phases (every 45 degrees)
 * and renders a clean, recognizable icon for that phase.
 */
private fun DrawScope.drawMoonPhaseIcon(
    center: Offset,
    phaseDegrees: Double,
    radius: Float,
    southernHemisphere: Boolean = false
) {
    val moonColor = Color(0xFFE8E8D0)
    val shadowColor = Color(0xFF2A2A35)

    // Snap to nearest of 8 phases (0, 45, 90, 135, 180, 225, 270, 315)
    val phaseIndex = ((phaseDegrees + 22.5) / 45.0).toInt() % 8

    // Subtle glow
    drawCircle(
        color = Color(0x20E8E8D0),
        radius = radius + 4f,
        center = center,
        style = Fill
    )

    when (phaseIndex) {
        0 -> {
            // New moon — dark circle with faint outline
            drawCircle(color = shadowColor, radius = radius, center = center, style = Fill)
            drawCircle(color = Color(0x30FFFFFF), radius = radius, center = center, style = Stroke(1f))
        }
        4 -> {
            // Full moon — fully lit
            drawCircle(color = moonColor, radius = radius, center = center, style = Fill)
            drawCircle(color = Color(0x40FFFFFF), radius = radius, center = center, style = Stroke(1f))
        }
        else -> {
            // For other phases, use a crisp terminator line
            // isWaxing: phases 1-3, isWaning: phases 5-7
            val isWaxing = phaseIndex in 1..3
            // Lit side flips between hemispheres
            val litOnRight = isWaxing != southernHemisphere

            // illumination fraction for each discrete phase
            val illumination = when (phaseIndex) {
                1, 7 -> 0.15f  // thin crescent
                2, 6 -> 0.50f  // quarter
                3, 5 -> 0.85f  // gibbous
                else -> 0.5f
            }

            // Shadow base
            drawCircle(color = shadowColor, radius = radius, center = center, style = Fill)

            val boundingRect = Rect(
                center.x - radius, center.y - radius,
                center.x + radius, center.y + radius
            )
            val terminatorWidth = radius * abs(2f * illumination - 1f)
            val terminatorRect = Rect(
                center.x - terminatorWidth, center.y - radius,
                center.x + terminatorWidth, center.y + radius
            )

            val litPath = Path().apply {
                if (litOnRight) {
                    // Lit on the right side
                    arcTo(boundingRect, -90f, 180f, forceMoveTo = true)
                    if (illumination >= 0.5f) {
                        arcTo(terminatorRect, 90f, 180f, forceMoveTo = false)
                    } else {
                        arcTo(terminatorRect, 90f, -180f, forceMoveTo = false)
                    }
                } else {
                    // Lit on the left side
                    arcTo(boundingRect, 90f, 180f, forceMoveTo = true)
                    if (illumination >= 0.5f) {
                        arcTo(terminatorRect, -90f, 180f, forceMoveTo = false)
                    } else {
                        arcTo(terminatorRect, -90f, -180f, forceMoveTo = false)
                    }
                }
                close()
            }

            drawPath(litPath, moonColor)
            drawCircle(color = Color(0x40FFFFFF), radius = radius, center = center, style = Stroke(1f))
        }
    }
}
