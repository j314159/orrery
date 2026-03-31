package com.example.orrery.location

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class CompassProvider(context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    val isAvailable: Boolean get() = rotationSensor != null

    /**
     * Emits the device's azimuth (bearing from north) in degrees, 0-360.
     * 0 = north, 90 = east, 180 = south, 270 = west.
     * Values are smoothed to reduce jitter.
     */
    fun azimuthFlow(): Flow<Float> = callbackFlow {
        val rotationMatrix = FloatArray(9)
        val orientation = FloatArray(3)

        // Simple low-pass filter for smoothing
        var smoothedAzimuth = 0f
        var initialized = false
        val alpha = 0.15f

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientation)

                // orientation[0] is azimuth in radians, -PI to PI
                var azimuthDeg = Math.toDegrees(orientation[0].toDouble()).toFloat()
                if (azimuthDeg < 0) azimuthDeg += 360f

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
