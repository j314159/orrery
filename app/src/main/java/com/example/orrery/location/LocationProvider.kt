package com.example.orrery.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await

class LocationProvider(private val context: Context) {

    private val client = LocationServices.getFusedLocationProviderClient(context)

    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    suspend fun getLocation(): Location? {
        if (!hasPermission()) return null

        // Always request a fresh location fix so emulator location changes
        // are picked up, and real-device locations stay current
        return try {
            val request = CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setMaxUpdateAgeMillis(0)
                .build()
            client.getCurrentLocation(request, null).await()
        } catch (_: SecurityException) {
            // Fall back to last known if fresh fix fails
            try {
                client.lastLocation.await()
            } catch (_: SecurityException) {
                null
            }
        }
    }
}
