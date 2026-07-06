package ai.joshmiller.orrery.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

class LocationProvider(private val context: Context) {

    private val client = LocationServices.getFusedLocationProviderClient(context)
    private val prefs = context.getSharedPreferences("orrery_location", Context.MODE_PRIVATE)

    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Save location to SharedPreferences so the complication service
     * can always access it even when GPS is unavailable.
     */
    fun cacheLocation(location: Location) {
        prefs.edit()
            .putFloat("lat", location.latitude.toFloat())
            .putFloat("lon", location.longitude.toFloat())
            .putFloat("alt", location.altitude.toFloat())
            .putLong("time", System.currentTimeMillis())
            .apply()
        Log.d(TAG, "Cached location: ${location.latitude}, ${location.longitude}")
    }

    /**
     * Retrieve cached location from SharedPreferences. Caches older than
     * [MAX_CACHE_AGE_MS] are discarded — better to show nothing than the
     * sky for wherever the user was weeks ago.
     */
    fun getCachedLocation(): Location? {
        if (!prefs.contains("lat")) return null
        val ageMs = System.currentTimeMillis() - prefs.getLong("time", 0L)
        if (ageMs > MAX_CACHE_AGE_MS) {
            Log.w(TAG, "Cached location too old (${ageMs / 86_400_000}d), ignoring")
            return null
        }
        return Location("cached").apply {
            latitude = prefs.getFloat("lat", 0f).toDouble()
            longitude = prefs.getFloat("lon", 0f).toDouble()
            altitude = prefs.getFloat("alt", 0f).toDouble()
        }
    }

    /**
     * Get location with multiple fallbacks:
     * 1. Last known location (instant)
     * 2. Fresh fix
     * 3. Cached location from SharedPreferences (saved by main app)
     */
    suspend fun getLocation(): Location? {
        if (!hasPermission()) {
            Log.w(TAG, "No location permission, trying cached")
            return getCachedLocation()
        }

        // Try last known location first — instant, no fix needed
        try {
            val last = client.lastLocation.await()
            if (last != null) {
                Log.d(TAG, "Using lastLocation: ${last.latitude}, ${last.longitude}")
                cacheLocation(last)
                return last
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // SecurityException, ApiException (location off, GMS unavailable), ...
            Log.w(TAG, "lastLocation failed: ${e.message}")
        }

        // Fall back to a fresh fix
        try {
            val request = CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                .setMaxUpdateAgeMillis(60_000)
                .build()
            val fresh = client.getCurrentLocation(request, null).await()
            if (fresh != null) {
                Log.d(TAG, "Using fresh fix: ${fresh.latitude}, ${fresh.longitude}")
                cacheLocation(fresh)
                return fresh
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "getCurrentLocation failed: ${e.message}")
        }

        // Last resort: cached location from a previous app session
        val cached = getCachedLocation()
        if (cached != null) {
            Log.d(TAG, "Using cached location: ${cached.latitude}, ${cached.longitude}")
        } else {
            Log.w(TAG, "No location available at all")
        }
        return cached
    }

    companion object {
        private const val TAG = "LocationProvider"
        private const val MAX_CACHE_AGE_MS = 14L * 24 * 60 * 60 * 1000  // 14 days
    }
}
