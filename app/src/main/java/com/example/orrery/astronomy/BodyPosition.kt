package com.example.orrery.astronomy

data class BodyPosition(
    val body: CelestialBody,
    val altitude: Double,
    val azimuth: Double
) {
    val isAboveHorizon: Boolean get() = altitude > 0.0
}
