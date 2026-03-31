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

class CompassProvider(context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    val isAvailable: Boolean get() = rotationSensor != null

    /**
     * Emits the compass direction that the 12 o'clock position of the watch
     * is pointing, projected onto the horizontal (ground) plane.
     *
     * This only tracks rotation around the vertical axis (like a compass on
     * a table). Tilting the watch does NOT affect the reading — only turning
     * your body changes it.
     *
     * Returns degrees 0-360: 0 = north, 90 = east, 180 = south, 270 = west.
     */
    fun azimuthFlow(): Flow<Float> = callbackFlow {
        val rotationMatrix = FloatArray(9)

        var smoothedAzimuth = 0f
        var initialized = false
        val alpha = 0.12f

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

                // The rotation matrix R maps device coordinates to world coordinates:
                //   World: X=East, Y=North, Z=Up
                //   Device: X=3 o'clock, Y=12 o'clock, Z=out of screen
                //
                // The device Y-axis (12 o'clock) in world coordinates is column 1:
                //   east component  = R[1]  (index 0*3+1)
                //   north component = R[4]  (index 1*3+1)
                //
                // Project onto horizontal plane and compute bearing from north:
                val eastComponent = rotationMatrix[1]
                val northComponent = rotationMatrix[4]
                var azimuthDeg = Math.toDegrees(
                    atan2(eastComponent.toDouble(), northComponent.toDouble())
                ).toFloat()
                if (azimuthDeg < 0f) azimuthDeg += 360f

                if (!initialized) {
                    smoothedAzimuth = azimuthDeg
                    initialized = true
                } else {
                    // Handle wrap-around at 0/360 boundary
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
