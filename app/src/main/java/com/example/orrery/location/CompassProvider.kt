package com.example.orrery.location

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.atan2
import kotlin.math.sqrt

class CompassProvider(context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    val isAvailable: Boolean get() = rotationSensor != null

    /**
     * Emits the horizontal compass direction the user is facing, in degrees 0-360.
     * 0 = north, 90 = east, 180 = south, 270 = west.
     *
     * Works regardless of how the watch is tilted:
     * - Watch flat: uses the direction 12 o'clock points horizontally
     * - Watch raised/tilted: uses the direction the screen faces as a proxy
     *   for which way the user is facing
     *
     * This means only turning your body rotates the sky map. Wrist tilts
     * are ignored.
     */
    fun azimuthFlow(): Flow<Float> = callbackFlow {
        val rotationMatrix = FloatArray(9)

        var smoothedAzimuth = 0f
        var initialized = false
        val alpha = 0.10f

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

                // Rotation matrix R maps device coords to world coords:
                //   World: X=East, Y=North, Z=Up
                //   Device: X=3 o'clock, Y=12 o'clock, Z=out of screen
                //
                // Device Y-axis (12 o'clock) in world coords = column 1:
                //   east = R[1], north = R[4], up = R[7]
                //
                // Device -Z axis (into screen, toward user) in world coords:
                //   east = -R[2], north = -R[5], up = -R[8]

                val yEast = rotationMatrix[1]
                val yNorth = rotationMatrix[4]
                val yHorizMag = sqrt(yEast * yEast + yNorth * yNorth)

                val zEast = -rotationMatrix[2]
                val zNorth = -rotationMatrix[5]
                val zHorizMag = sqrt(zEast * zEast + zNorth * zNorth)

                // When the watch is flat, 12 o'clock has a strong horizontal component.
                // When tilted (raised to look at), 12 o'clock points up and the screen
                // faces the user — so -Z has the better horizontal signal.
                // Blend between them based on which has a stronger horizontal projection.
                val rawAzimuth = if (yHorizMag > 0.25f && yHorizMag >= zHorizMag) {
                    // Watch is roughly flat — use 12 o'clock direction
                    Math.toDegrees(atan2(yEast.toDouble(), yNorth.toDouble())).toFloat()
                } else if (zHorizMag > 0.15f) {
                    // Watch is tilted — screen faces the user, so -Z ≈ forward direction
                    Math.toDegrees(atan2(zEast.toDouble(), zNorth.toDouble())).toFloat()
                } else {
                    // Ambiguous orientation (watch face pointing straight up or down)
                    // Keep the previous smoothed value
                    smoothedAzimuth
                }

                var azimuthDeg = rawAzimuth
                if (azimuthDeg < 0f) azimuthDeg += 360f

                if (!initialized) {
                    smoothedAzimuth = azimuthDeg
                    initialized = true
                } else {
                    var delta = azimuthDeg - smoothedAzimuth
                    if (delta > 180f) delta -= 360f
                    if (delta < -180f) delta += 360f
                    smoothedAzimuth += alpha * delta
                    if (smoothedAzimuth < 0f) smoothedAzimuth += 360f
                    if (smoothedAzimuth >= 360f) smoothedAzimuth -= 360f
                }

                trySend(smoothedAzimuth)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        rotationSensor?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
        }

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
}
