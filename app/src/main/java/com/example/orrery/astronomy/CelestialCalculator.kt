package com.example.orrery.astronomy

import io.github.cosinekitty.astronomy.Aberration
import io.github.cosinekitty.astronomy.EquatorEpoch
import io.github.cosinekitty.astronomy.Observer
import io.github.cosinekitty.astronomy.Refraction
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.equator
import io.github.cosinekitty.astronomy.horizon
import io.github.cosinekitty.astronomy.moonPhase

class CelestialCalculator {

    data class SkySnapshot(
        val bodies: List<BodyPosition>,
        val moonPhaseDegrees: Double
    )

    fun calculate(latitude: Double, longitude: Double, elevation: Double = 0.0): SkySnapshot {
        val observer = Observer(latitude, longitude, elevation)
        val time = Time.fromMillisecondsSince1970(System.currentTimeMillis())

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

            val position = BodyPosition(
                body = celestialBody,
                altitude = horizontal.altitude,
                azimuth = horizontal.azimuth
            )

            if (position.isAboveHorizon) position else null
        }

        val phase = moonPhase(time)

        return SkySnapshot(bodies, phase)
    }
}
