package ai.joshmiller.orrery.astronomy

import io.github.cosinekitty.astronomy.Aberration
import io.github.cosinekitty.astronomy.EquatorEpoch
import io.github.cosinekitty.astronomy.Observer
import io.github.cosinekitty.astronomy.Refraction
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.equator
import io.github.cosinekitty.astronomy.horizon
import io.github.cosinekitty.astronomy.moonPhase
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * A point on the ecliptic line in horizontal coordinates.
 */
data class EclipticPoint(
    val altitude: Double,
    val azimuth: Double
)

class CelestialCalculator {

    /**
     * A star's position in horizontal coordinates for rendering.
     */
    data class StarPosition(
        val name: String,
        val altitude: Double,
        val azimuth: Double,
        val magnitude: Double
    )

    data class SkySnapshot(
        val bodies: List<BodyPosition>,
        val moonPhaseDegrees: Double,
        val sunAltitude: Double,
        val sunAzimuth: Double,
        val eclipticPoints: List<EclipticPoint>,
        val stars: List<StarPosition> = emptyList()
    )

    // Obliquity of the ecliptic (approximate, good enough for display)
    private val obliquityRad = Math.toRadians(23.44)

    fun calculate(
        latitude: Double,
        longitude: Double,
        elevation: Double = 0.0,
        timeMillis: Long = System.currentTimeMillis()
    ): SkySnapshot {
        val observer = Observer(latitude, longitude, elevation)
        val time = Time.fromMillisecondsSince1970(timeMillis)

        var sunAlt = -90.0
        var sunAz = 0.0

        val bodies = CelestialBody.entries.mapNotNull { celestialBody ->
            val equatorial = equator(
                celestialBody.body,
                time,
                observer,
                EquatorEpoch.OfDate,
                Aberration.Corrected
            )

            val horizontal = horizon(
                time,
                observer,
                equatorial.ra,
                equatorial.dec,
                Refraction.Normal
            )

            if (celestialBody == CelestialBody.SUN) {
                sunAlt = horizontal.altitude
                sunAz = horizontal.azimuth
            }

            val position = BodyPosition(
                body = celestialBody,
                altitude = horizontal.altitude,
                azimuth = horizontal.azimuth
            )

            if (position.isAboveHorizon) position else null
        }

        val phase = moonPhase(time)
        val ecliptic = computeEclipticPoints(observer, time)
        val stars = computeStarPositions(observer, time)

        return SkySnapshot(bodies, phase, sunAlt, sunAz, ecliptic, stars)
    }

    /**
     * Sample points along the ecliptic (ecliptic latitude = 0) and convert
     * to horizontal coordinates for the given observer and time.
     *
     * The ecliptic is the apparent path of the Sun through the sky over a year.
     * All planets orbit close to this plane.
     */
    private fun computeEclipticPoints(observer: Observer, time: Time): List<EclipticPoint> {
        val points = mutableListOf<EclipticPoint>()
        val cosObl = cos(obliquityRad)
        val sinObl = sin(obliquityRad)

        // Sample every 3 degrees of ecliptic longitude for a smooth curve
        for (i in 0..120) {
            val eclLonDeg = i * 3.0
            val eclLonRad = Math.toRadians(eclLonDeg)

            // Ecliptic to equatorial conversion (ecliptic latitude = 0):
            //   RA  = atan2(sin(λ) * cos(ε), cos(λ))
            //   Dec = asin(sin(λ) * sin(ε))
            val sinLon = sin(eclLonRad)
            val cosLon = cos(eclLonRad)

            val raRad = atan2(sinLon * cosObl, cosLon)
            val decRad = asin(sinLon * sinObl)

            // Convert RA from radians to hours (library expects hours)
            var raHours = Math.toDegrees(raRad) / 15.0
            if (raHours < 0) raHours += 24.0

            val decDeg = Math.toDegrees(decRad)

            val horizontal = horizon(
                time, observer, raHours, decDeg, Refraction.Normal
            )

            points.add(EclipticPoint(horizontal.altitude, horizontal.azimuth))
        }

        return points
    }

    /**
     * Compute horizontal coordinates for all catalog stars.
     * Stars have fixed RA/Dec (J2000), so we just convert to alt/az
     * for the observer's location and time. Only returns above-horizon stars.
     */
    private fun computeStarPositions(observer: Observer, time: Time): List<StarPosition> {
        return BRIGHT_STARS.mapNotNull { star ->
            val horizontal = horizon(
                time, observer, star.ra, star.dec, Refraction.Normal
            )

            if (horizontal.altitude > 0.0) {
                StarPosition(
                    name = star.name,
                    altitude = horizontal.altitude,
                    azimuth = horizontal.azimuth,
                    magnitude = star.magnitude
                )
            } else {
                null
            }
        }
    }
}
