package ai.joshmiller.orrery.settings

import android.content.Context
import android.content.SharedPreferences

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("orrery_settings", Context.MODE_PRIVATE)

    fun load(): DisplaySettings {
        return DisplaySettings(
            planetStyle = prefs.getString(KEY_PLANET_STYLE, null)
                ?.let { runCatching { PlanetStyle.valueOf(it) }.getOrNull() }
                ?: PlanetStyle.ILLUSTRATED,
            moonStyle = prefs.getString(KEY_MOON_STYLE, null)
                ?.let { runCatching { MoonStyle.valueOf(it) }.getOrNull() }
                ?: MoonStyle.DYNAMIC,
            visibleBodies = prefs.getString(KEY_VISIBLE_BODIES, null)
                ?.let { runCatching { VisibleBodies.valueOf(it) }.getOrNull() }
                ?: VisibleBodies.NAKED_EYE,
            showLabels = prefs.getBoolean(KEY_SHOW_LABELS, true),
            showConstellationLines = prefs.getBoolean(KEY_CONSTELLATION_LINES, true),
            showConstellationLabels = prefs.getBoolean(KEY_CONSTELLATION_LABELS, true),
            starVisibility = prefs.getString(KEY_STAR_VISIBILITY, null)
                ?.let { runCatching { StarVisibility.valueOf(it) }.getOrNull() }
                ?: StarVisibility.NIGHT_ONLY
        )
    }

    fun save(settings: DisplaySettings) {
        prefs.edit()
            .putString(KEY_PLANET_STYLE, settings.planetStyle.name)
            .putString(KEY_MOON_STYLE, settings.moonStyle.name)
            .putString(KEY_VISIBLE_BODIES, settings.visibleBodies.name)
            .putBoolean(KEY_SHOW_LABELS, settings.showLabels)
            .putBoolean(KEY_CONSTELLATION_LINES, settings.showConstellationLines)
            .putBoolean(KEY_CONSTELLATION_LABELS, settings.showConstellationLabels)
            .putString(KEY_STAR_VISIBILITY, settings.starVisibility.name)
            .apply()
    }

    companion object {
        private const val KEY_PLANET_STYLE = "planet_style"
        private const val KEY_MOON_STYLE = "moon_style"
        private const val KEY_VISIBLE_BODIES = "visible_bodies"
        private const val KEY_SHOW_LABELS = "show_labels"
        private const val KEY_CONSTELLATION_LINES = "constellation_lines"
        private const val KEY_CONSTELLATION_LABELS = "constellation_labels"
        private const val KEY_STAR_VISIBILITY = "star_visibility"
    }
}
