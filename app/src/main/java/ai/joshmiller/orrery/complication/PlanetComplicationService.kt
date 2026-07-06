package ai.joshmiller.orrery.complication

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.util.Log
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import ai.joshmiller.orrery.MainActivity
import ai.joshmiller.orrery.R
import ai.joshmiller.orrery.astronomy.CelestialBody
import ai.joshmiller.orrery.astronomy.CelestialCalculator
import ai.joshmiller.orrery.location.LocationProvider
import ai.joshmiller.orrery.settings.SettingsRepository
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Provides a watch face complication showing how many planets are
 * currently above the horizon. Tapping launches the full Orrery app.
 */
class PlanetComplicationService : SuspendingComplicationDataSourceService() {

    private val calculator = CelestialCalculator()

    override fun getPreviewData(type: ComplicationType): ComplicationData {
        return ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder("5").build(),
            contentDescription = PlainComplicationText.Builder("5 planets overhead").build()
        )
            .setMonochromaticImage(
                MonochromaticImage.Builder(
                    Icon.createWithResource(this, R.drawable.ic_complication)
                ).build()
            )
            .build()
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        Log.d(TAG, "onComplicationRequest called, type=${request.complicationType}")

        val locationProvider = LocationProvider(applicationContext)

        // Use a timeout — GPS can hang in a background service
        val location = withTimeoutOrNull(5_000L) {
            locationProvider.getLocation()
        }

        if (location == null) {
            Log.w(TAG, "Location unavailable, showing fallback")
            return ShortTextComplicationData.Builder(
                text = PlainComplicationText.Builder("--").build(),
                contentDescription = PlainComplicationText.Builder("Location unavailable").build()
            )
                .setMonochromaticImage(
                    MonochromaticImage.Builder(
                        Icon.createWithResource(this, R.drawable.ic_complication)
                    ).build()
                )
                .setTapAction(createLaunchIntent())
                .build()
        }

        Log.d(TAG, "Location: ${location.latitude}, ${location.longitude}")

        val snapshot = calculator.calculate(
            latitude = location.latitude,
            longitude = location.longitude,
            elevation = location.altitude
        )

        // Respect the user's visible bodies setting; the Sun and Moon
        // are in the snapshot but aren't planets, so never count them.
        val settings = SettingsRepository(applicationContext).load()
        val visiblePlanets = snapshot.bodies.count {
            it.body != CelestialBody.SUN &&
                it.body != CelestialBody.MOON &&
                settings.visibleBodies.includes(it.body)
        }

        val description = when (visiblePlanets) {
            0 -> "No planets overhead"
            1 -> "1 planet overhead"
            else -> "$visiblePlanets planets overhead"
        }

        Log.d(TAG, "Returning complication: $visiblePlanets planets overhead")

        return ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder("$visiblePlanets").build(),
            contentDescription = PlainComplicationText.Builder(description).build()
        )
            .setMonochromaticImage(
                MonochromaticImage.Builder(
                    Icon.createWithResource(this, R.drawable.ic_complication)
                ).build()
            )
            .setTapAction(createLaunchIntent())
            .build()
    }

    companion object {
        private const val TAG = "PlanetComplication"
    }

    private fun createLaunchIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
