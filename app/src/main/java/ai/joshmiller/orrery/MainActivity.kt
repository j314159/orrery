package ai.joshmiller.orrery

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import ai.joshmiller.orrery.presentation.OrreryApp

class MainActivity : ComponentActivity() {

    private var hasLocationPermission by mutableStateOf(false)
    private var permissionPermanentlyDenied by mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasLocationPermission = granted
        if (!granted) {
            // After a denial, a false rationale means the system will no longer
            // show the dialog — the only path left is the app settings page.
            permissionPermanentlyDenied =
                !shouldShowRequestPermissionRationale(LOCATION_PERMISSION)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            OrreryApp(
                hasLocationPermission = hasLocationPermission,
                permissionPermanentlyDenied = permissionPermanentlyDenied,
                onRequestPermission = {
                    permissionLauncher.launch(LOCATION_PERMISSION)
                },
                onOpenSettings = { openAppSettings() }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-check on every resume — the user may have changed the
        // permission from the system settings page.
        hasLocationPermission = ContextCompat.checkSelfPermission(
            this, LOCATION_PERMISSION
        ) == PackageManager.PERMISSION_GRANTED
        if (hasLocationPermission) {
            permissionPermanentlyDenied = false
        }
    }

    private fun openAppSettings() {
        startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", packageName, null)
            )
        )
    }

    companion object {
        // Coarse is plenty: kilometer-scale location error is invisible
        // in computed sky positions.
        const val LOCATION_PERMISSION = Manifest.permission.ACCESS_COARSE_LOCATION
    }
}
