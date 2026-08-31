package ai.joshmiller.orrery.complication

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import ai.joshmiller.orrery.MainActivity
import ai.joshmiller.orrery.R
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.moonPhase
import kotlin.math.cos
import kotlin.math.roundToInt

/**
 * Shows the Moon's current illumination percentage. Unlike the planet
 * complication, this needs no location — the phase depends only on time —
 * so it never blocks on GPS and never shows a fallback.
 */
class MoonPhaseComplicationService : SuspendingComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData =
        buildData(phaseDegrees = 130.0)  // waxing gibbous, ~82% — looks good in pickers

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData =
        buildData(moonPhase(Time.fromMillisecondsSince1970(System.currentTimeMillis())))

    private fun buildData(phaseDegrees: Double): ComplicationData {
        // Illumination: 0 at new moon, 1 at full moon (same formula as the sky map)
        val illumination = (1.0 - cos(Math.toRadians(phaseDegrees))) / 2.0
        val percent = (illumination * 100).roundToInt()

        return ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder("$percent%").build(),
            contentDescription = PlainComplicationText.Builder(
                "Moon: ${phaseName(phaseDegrees)}, $percent% illuminated"
            ).build()
        )
            .setMonochromaticImage(
                MonochromaticImage.Builder(
                    Icon.createWithResource(this, R.drawable.ic_moon_phase)
                ).build()
            )
            .setTapAction(createLaunchIntent())
            .build()
    }

    private fun phaseName(degrees: Double): String = when {
        degrees < 22.5 || degrees >= 337.5 -> "new moon"
        degrees < 67.5 -> "waxing crescent"
        degrees < 112.5 -> "first quarter"
        degrees < 157.5 -> "waxing gibbous"
        degrees < 202.5 -> "full moon"
        degrees < 247.5 -> "waning gibbous"
        degrees < 292.5 -> "last quarter"
        else -> "waning crescent"
    }

    private fun createLaunchIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        // Request code 1 — the planet complication uses 0; distinct codes keep
        // the two PendingIntents from colliding.
        return PendingIntent.getActivity(
            this, 1, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
