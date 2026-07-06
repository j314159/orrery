package ai.joshmiller.orrery.settings

import ai.joshmiller.orrery.astronomy.CelestialBody

enum class PlanetStyle {
    SYMBOLS,       // Astronomical/planetary symbols (default)
    ILLUSTRATED    // Tiny planet illustrations drawn on canvas
}

enum class MoonStyle {
    DYNAMIC,       // Continuous arc-based phase rendering (default)
    PHASE_ICON,    // 8 discrete phase icons
    ALCHEMICAL     // Static crescent symbol (☽)
}

enum class VisibleBodies {
    ALL,           // All planets including Uranus and Neptune (default)
    NAKED_EYE;     // Only planets visible to the naked eye

    fun includes(body: CelestialBody): Boolean = when (this) {
        ALL -> true
        NAKED_EYE -> body != CelestialBody.URANUS && body != CelestialBody.NEPTUNE
    }
}

enum class StarVisibility {
    ALWAYS,        // Show stars and constellations day and night
    NIGHT_ONLY,    // Show only at night / twilight (default)
    NEVER          // Never show stars or constellations
}

data class DisplaySettings(
    val planetStyle: PlanetStyle = PlanetStyle.ILLUSTRATED,
    val moonStyle: MoonStyle = MoonStyle.DYNAMIC,
    val visibleBodies: VisibleBodies = VisibleBodies.NAKED_EYE,
    val showLabels: Boolean = true,
    val showConstellationLines: Boolean = true,
    val showConstellationLabels: Boolean = true,
    val starVisibility: StarVisibility = StarVisibility.NIGHT_ONLY
)
