package com.example.orrery.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.wear.compose.material3.Text
import com.example.orrery.astronomy.BodyPosition
import com.example.orrery.astronomy.CelestialCalculator
import com.example.orrery.location.LocationProvider
import com.example.orrery.presentation.theme.OrreryTheme
import com.example.orrery.presentation.theme.SkyBackground
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class OrreryState {
    data object Loading : OrreryState()
    data object NeedsPermission : OrreryState()
    data class Ready(
        val bodies: List<BodyPosition>,
        val moonPhaseDegrees: Double,
        val sunAltitude: Double
    ) : OrreryState()
    data class Error(val message: String) : OrreryState()
}

@Composable
fun OrreryApp(
    hasLocationPermission: Boolean,
    onRequestPermission: () -> Unit
) {
    OrreryTheme {
        var state by remember { mutableStateOf<OrreryState>(OrreryState.Loading) }
        val context = LocalContext.current
        val locationProvider = remember { LocationProvider(context) }
        val calculator = remember { CelestialCalculator() }

        // Manual rotation via rotary crown, in degrees
        var rotationDegrees by remember { mutableFloatStateOf(0f) }
        val focusRequester = remember { FocusRequester() }

        // Incremented each time the app resumes to trigger recalculation
        var refreshTrigger by remember { mutableIntStateOf(0) }

        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
            refreshTrigger++
        }

        LaunchedEffect(hasLocationPermission, refreshTrigger) {
            if (!hasLocationPermission) {
                state = OrreryState.NeedsPermission
                return@LaunchedEffect
            }

            state = OrreryState.Loading

            val location = withContext(Dispatchers.IO) {
                locationProvider.getLocation()
            }

            if (location == null) {
                state = OrreryState.Error("Could not determine location")
                return@LaunchedEffect
            }

            val snapshot = withContext(Dispatchers.Default) {
                calculator.calculate(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    elevation = location.altitude
                )
            }

            state = OrreryState.Ready(
                bodies = snapshot.bodies,
                moonPhaseDegrees = snapshot.moonPhaseDegrees,
                sunAltitude = snapshot.sunAltitude
            )
        }

        // Request focus for rotary input
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .onRotaryScrollEvent { event ->
                    // Crown scroll: positive = clockwise = rotate map clockwise
                    rotationDegrees += event.verticalScrollPixels * 0.5f
                    // Normalize to 0-360
                    rotationDegrees = ((rotationDegrees % 360f) + 360f) % 360f
                    true
                }
                .focusRequester(focusRequester)
                .focusable()
        ) {
            when (val s = state) {
                is OrreryState.Loading -> {
                    CenteredMessage("Loading...")
                }
                is OrreryState.NeedsPermission -> {
                    LaunchedEffect(Unit) {
                        onRequestPermission()
                    }
                    CenteredMessage("Location access needed")
                }
                is OrreryState.Ready -> {
                    SkyMapCanvas(
                        bodies = s.bodies,
                        moonPhaseDegrees = s.moonPhaseDegrees,
                        sunAltitude = s.sunAltitude,
                        rotationDegrees = rotationDegrees
                    )
                }
                is OrreryState.Error -> {
                    CenteredMessage(s.message)
                }
            }
        }
    }
}

@Composable
private fun CenteredMessage(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SkyBackground),
        contentAlignment = Alignment.Center
    ) {
        Text(text)
    }
}
