package com.example.orrery.astronomy

import androidx.compose.ui.graphics.Color
import io.github.cosinekitty.astronomy.Body

enum class CelestialBody(
    val body: Body,
    val symbol: String,
    val displayColor: Color,
    val displayName: String
) {
    SUN(Body.Sun, "\u2609", Color(0xFFFFD700), "Sun"),
    MOON(Body.Moon, "\u263D", Color(0xFFC0C0C0), "Moon"),
    MERCURY(Body.Mercury, "\u263F", Color(0xFFB0B0B0), "Mercury"),
    VENUS(Body.Venus, "\u2640", Color(0xFF90EE90), "Venus"),
    MARS(Body.Mars, "\u2642", Color(0xFFFF4444), "Mars"),
    JUPITER(Body.Jupiter, "\u2643", Color(0xFFFFA500), "Jupiter"),
    SATURN(Body.Saturn, "\u2644", Color(0xFFBDB76B), "Saturn"),
    URANUS(Body.Uranus, "\u26E2", Color(0xFF00CED1), "Uranus"),
    NEPTUNE(Body.Neptune, "\u2646", Color(0xFF4169E1), "Neptune");
}
