package ai.joshmiller.orrery.presentation

import android.location.Location
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.wear.compose.material3.Text
import ai.joshmiller.orrery.astronomy.CelestialCalculator
import ai.joshmiller.orrery.location.LocationProvider
import ai.joshmiller.orrery.presentation.theme.OrreryTheme
import ai.joshmiller.orrery.presentation.theme.SkyBackground
import ai.joshmiller.orrery.settings.DisplaySettings
import ai.joshmiller.orrery.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.atan2

sealed class OrreryState {
    data object Loading : OrreryState()
    data object NeedsPermission : OrreryState()
    data class Ready(
        val snapshot: CelestialCalculator.SkySnapshot
    ) : OrreryState()
    data class Error(val message: String) : OrreryState()
}

@Composable
fun OrreryApp(
    hasLocationPermission: Boolean,
    permissionPermanentlyDenied: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    OrreryTheme {
        var state by remember { mutableStateOf<OrreryState>(OrreryState.Loading) }
        val context = LocalContext.current
        val locationProvider = remember { LocationProvider(context) }
        val calculator = remember { CelestialCalculator() }
        val settingsRepository = remember { SettingsRepository(context) }

        // Display settings
        var displaySettings by remember { mutableStateOf(settingsRepository.load()) }
        var showSettings by remember { mutableStateOf(false) }

        // Rotation via circular drag, in degrees
        val rotationAnimatable = remember { Animatable(0f) }
        val rotationDegrees = rotationAnimatable.value

        // Time scrubbing via crown — offset in milliseconds from "now"
        var timeOffsetMs by remember { mutableLongStateOf(0L) }

        val focusRequester = remember { FocusRequester() }
        val coroutineScope = rememberCoroutineScope()
        val vibrator = remember { context.getSystemService(Vibrator::class.java) }
        val tickEffect = remember { VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK) }

        // Accumulate scroll pixels — each "click" threshold = 1 hour of time
        var scrollAccumulator by remember { mutableFloatStateOf(0f) }
        val pixelsPerHourClick = 15f  // scroll pixels to trigger one hour step
        val msPerHour = 3_600_000L

        // Cached location so we can recalculate when time changes
        var cachedLocation by remember { mutableStateOf<Location?>(null) }

        // Incremented each time the app resumes to trigger recalculation
        var refreshTrigger by remember { mutableIntStateOf(0) }

        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
            refreshTrigger++
            // Reset time offset when app resumes — show current sky
            timeOffsetMs = 0L
        }

        // Initial location fetch
        LaunchedEffect(hasLocationPermission, refreshTrigger) {
            if (!hasLocationPermission) {
                state = OrreryState.NeedsPermission
                return@LaunchedEffect
            }

            // Keep showing the previous sky while refreshing — only show
            // the loading screen when there's nothing to display yet.
            if (state !is OrreryState.Ready) {
                state = OrreryState.Loading
            }

            val location = withContext(Dispatchers.IO) {
                locationProvider.getLocation()
            }

            if (location == null) {
                if (state !is OrreryState.Ready) {
                    state = OrreryState.Error("Could not determine location")
                }
                return@LaunchedEffect
            }

            cachedLocation = location

            val snapshot = withContext(Dispatchers.Default) {
                calculator.calculate(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    elevation = location.altitude
                )
            }

            state = OrreryState.Ready(snapshot = snapshot)
        }

        // Recalculate when time offset changes (debounced for fast scrolling)
        LaunchedEffect(timeOffsetMs) {
            val location = cachedLocation ?: return@LaunchedEffect

            // Short debounce — if the user is spinning the crown fast,
            // wait for them to pause before recalculating
            delay(100)

            val snapshot = withContext(Dispatchers.Default) {
                calculator.calculate(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    elevation = location.altitude,
                    timeMillis = System.currentTimeMillis() + timeOffsetMs
                )
            }

            state = OrreryState.Ready(snapshot = snapshot)
        }

        if (showSettings) {
            SettingsScreen(
                settings = displaySettings,
                onSettingsChanged = { newSettings ->
                    displaySettings = newSettings
                    settingsRepository.save(newSettings)
                },
                onDismiss = {
                    showSettings = false
                }
            )
        } else {
            // Re-request focus every time we return to the sky map
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onRotaryScrollEvent { event ->
                        // Crown scroll: scrub time in 1-hour clicks
                        scrollAccumulator += event.verticalScrollPixels
                        val clicks = (scrollAccumulator / pixelsPerHourClick).toInt()
                        if (clicks != 0) {
                            scrollAccumulator -= clicks * pixelsPerHourClick
                            timeOffsetMs += clicks * msPerHour
                            vibrator?.vibrate(tickEffect)
                        }
                        true
                    }
                    .focusRequester(focusRequester)
                    .focusable()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                coroutineScope.launch {
                                    // Animate rotation back to north
                                    val current = rotationAnimatable.value
                                    val target = if (current > 180f) 360f else 0f
                                    rotationAnimatable.animateTo(
                                        targetValue = target,
                                        animationSpec = tween(durationMillis = 400)
                                    )
                                    rotationAnimatable.snapTo(0f)
                                    // Reset time to now
                                    timeOffsetMs = 0L
                                    scrollAccumulator = 0f
                                }
                            },
                            onDoubleTap = { showSettings = true },
                            onLongPress = {
                                // Toggle all labels on/off
                                val anyLabelsOn = displaySettings.showLabels ||
                                    displaySettings.showConstellationLabels
                                val newSettings = displaySettings.copy(
                                    showLabels = !anyLabelsOn,
                                    showConstellationLabels = !anyLabelsOn
                                )
                                displaySettings = newSettings
                                settingsRepository.save(newSettings)
                                vibrator?.vibrate(tickEffect)
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        // Circular drag for rotation
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        var previousAngle: Float? = null

                        detectDragGestures(
                            onDragStart = { offset ->
                                previousAngle = atan2(
                                    offset.y - centerY,
                                    offset.x - centerX
                                ).let { Math.toDegrees(it.toDouble()).toFloat() }
                            },
                            onDrag = { change, _ ->
                                val currentAngle = atan2(
                                    change.position.y - centerY,
                                    change.position.x - centerX
                                ).let { Math.toDegrees(it.toDouble()).toFloat() }

                                previousAngle?.let { prev ->
                                    var delta = currentAngle - prev
                                    // Handle wrap-around at ±180
                                    if (delta > 180f) delta -= 360f
                                    if (delta < -180f) delta += 360f

                                    coroutineScope.launch {
                                        // Negate: dragging clockwise should rotate sky clockwise
                                        val newValue = rotationAnimatable.value - delta
                                        rotationAnimatable.snapTo(
                                            ((newValue % 360f) + 360f) % 360f
                                        )
                                    }
                                }
                                previousAngle = currentAngle
                            },
                            onDragEnd = { previousAngle = null },
                            onDragCancel = { previousAngle = null }
                        )
                    }
            ) {
                when (val s = state) {
                    is OrreryState.Loading -> {
                        CenteredMessage("Loading...")
                    }
                    is OrreryState.NeedsPermission -> {
                        if (permissionPermanentlyDenied) {
                            CenteredMessage(
                                "Location permission needed\n\nTap to open settings",
                                onClick = onOpenSettings
                            )
                        } else {
                            LaunchedEffect(Unit) {
                                onRequestPermission()
                            }
                            CenteredMessage(
                                "Location access needed\n\nTap to grant",
                                onClick = onRequestPermission
                            )
                        }
                    }
                    is OrreryState.Ready -> {
                        val snap = s.snapshot

                        // Filter bodies based on visibility setting
                        val filteredBodies = snap.bodies.filter {
                            displaySettings.visibleBodies.includes(it.body)
                        }

                        SkyMapCanvas(
                            bodies = filteredBodies,
                            moonPhaseDegrees = snap.moonPhaseDegrees,
                            sunAltitude = snap.sunAltitude,
                            sunAzimuth = snap.sunAzimuth,
                            eclipticPoints = snap.eclipticPoints,
                            stars = snap.stars,
                            rotationDegrees = rotationDegrees,
                            displaySettings = displaySettings,
                            southernHemisphere = (cachedLocation?.latitude ?: 0.0) < 0.0
                        )

                        // Time indicator when scrubbing away from "now"
                        if (timeOffsetMs != 0L) {
                            val projectedTime = System.currentTimeMillis() + timeOffsetMs
                            val formatter = remember { SimpleDateFormat("MMM d  h:mm a", Locale.getDefault()) }
                            val timeString = formatter.format(Date(projectedTime))

                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Text(
                                    text = timeString,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xDDFFD700),
                                    modifier = Modifier
                                        .padding(bottom = 8.dp)
                                        .background(
                                            Color(0x80000000),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    is OrreryState.Error -> {
                        CenteredMessage(
                            "${s.message}\n\nTap to retry",
                            onClick = { refreshTrigger++ }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CenteredMessage(text: String, onClick: (() -> Unit)? = null) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SkyBackground)
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick)
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}
