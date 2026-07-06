package ai.joshmiller.orrery.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Text
import kotlinx.coroutines.launch
import ai.joshmiller.orrery.presentation.theme.SkyBackground
import ai.joshmiller.orrery.settings.DisplaySettings
import ai.joshmiller.orrery.settings.MoonStyle
import ai.joshmiller.orrery.settings.PlanetStyle
import ai.joshmiller.orrery.settings.StarVisibility
import ai.joshmiller.orrery.settings.VisibleBodies

@Composable
fun SettingsScreen(
    settings: DisplaySettings,
    onSettingsChanged: (DisplaySettings) -> Unit,
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SkyBackground)
            .onRotaryScrollEvent { event ->
                coroutineScope.launch {
                    scrollState.scrollBy(event.verticalScrollPixels)
                }
                true
            }
            .focusRequester(focusRequester)
            .focusable()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Settings",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Planet Style
        SettingSection(title = "Planets") {
            SettingOption(
                label = "Symbols",
                symbol = "\u2643",
                selected = settings.planetStyle == PlanetStyle.SYMBOLS,
                onClick = { onSettingsChanged(settings.copy(planetStyle = PlanetStyle.SYMBOLS)) }
            )
            SettingOption(
                label = "Illustrated",
                symbol = "\u25CF",
                selected = settings.planetStyle == PlanetStyle.ILLUSTRATED,
                onClick = { onSettingsChanged(settings.copy(planetStyle = PlanetStyle.ILLUSTRATED)) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Moon Style
        SettingSection(title = "Moon") {
            SettingOption(
                label = "Dynamic",
                symbol = "\u25D0",
                selected = settings.moonStyle == MoonStyle.DYNAMIC,
                onClick = { onSettingsChanged(settings.copy(moonStyle = MoonStyle.DYNAMIC)) }
            )
            SettingOption(
                label = "Phase icon",
                symbol = "\uD83C\uDF14",
                selected = settings.moonStyle == MoonStyle.PHASE_ICON,
                onClick = { onSettingsChanged(settings.copy(moonStyle = MoonStyle.PHASE_ICON)) }
            )
            SettingOption(
                label = "Symbol",
                symbol = "\u263D",
                selected = settings.moonStyle == MoonStyle.ALCHEMICAL,
                onClick = { onSettingsChanged(settings.copy(moonStyle = MoonStyle.ALCHEMICAL)) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Visible Bodies
        SettingSection(title = "Visible planets") {
            SettingOption(
                label = "All planets",
                symbol = "\u26E2",
                selected = settings.visibleBodies == VisibleBodies.ALL,
                onClick = { onSettingsChanged(settings.copy(visibleBodies = VisibleBodies.ALL)) }
            )
            SettingOption(
                label = "Naked eye",
                symbol = "\u2606",
                selected = settings.visibleBodies == VisibleBodies.NAKED_EYE,
                onClick = { onSettingsChanged(settings.copy(visibleBodies = VisibleBodies.NAKED_EYE)) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Labels toggle
        SettingSection(title = "Planet labels") {
            SettingOption(
                label = "Off",
                symbol = "\u2014",
                selected = !settings.showLabels,
                onClick = { onSettingsChanged(settings.copy(showLabels = false)) }
            )
            SettingOption(
                label = "Show names",
                symbol = "Aa",
                selected = settings.showLabels,
                onClick = { onSettingsChanged(settings.copy(showLabels = true)) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Star visibility
        SettingSection(title = "Stars") {
            SettingOption(
                label = "Night only",
                symbol = "☽",
                selected = settings.starVisibility == StarVisibility.NIGHT_ONLY,
                onClick = { onSettingsChanged(settings.copy(starVisibility = StarVisibility.NIGHT_ONLY)) }
            )
            SettingOption(
                label = "Always",
                symbol = "✦",
                selected = settings.starVisibility == StarVisibility.ALWAYS,
                onClick = { onSettingsChanged(settings.copy(starVisibility = StarVisibility.ALWAYS)) }
            )
            SettingOption(
                label = "Never",
                symbol = "—",
                selected = settings.starVisibility == StarVisibility.NEVER,
                onClick = { onSettingsChanged(settings.copy(
                    starVisibility = StarVisibility.NEVER,
                    showConstellationLines = false,
                    showConstellationLabels = false
                )) }
            )
        }

        // Constellation settings — only shown when stars are visible
        if (settings.starVisibility != StarVisibility.NEVER) {
            Spacer(modifier = Modifier.height(8.dp))

            SettingSection(title = "Constellations") {
                SettingOption(
                    label = "Lines off",
                    symbol = "\u2014",
                    selected = !settings.showConstellationLines,
                    onClick = { onSettingsChanged(settings.copy(
                        showConstellationLines = false,
                        showConstellationLabels = false
                    )) }
                )
                SettingOption(
                    label = "Lines",
                    symbol = "\u2731",
                    selected = settings.showConstellationLines && !settings.showConstellationLabels,
                    onClick = { onSettingsChanged(settings.copy(
                        showConstellationLines = true,
                        showConstellationLabels = false
                    )) }
                )
                SettingOption(
                    label = "Lines + names",
                    symbol = "\u2731A",
                    selected = settings.showConstellationLines && settings.showConstellationLabels,
                    onClick = { onSettingsChanged(settings.copy(
                        showConstellationLines = true,
                        showConstellationLabels = true
                    )) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Done button
        Text(
            text = "Done",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4A90D9),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onDismiss() }
                .padding(vertical = 8.dp)
        )

        // Extra padding at bottom for round screen
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SettingSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF888899),
            modifier = Modifier.padding(bottom = 4.dp)
        )
        content()
    }
}

@Composable
private fun SettingOption(
    label: String,
    symbol: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val textColor = if (selected) Color(0xFFFFD700) else Color(0xFFAAAAAA)
    val bgColor = if (selected) Color(0x20FFD700) else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Text(
            text = symbol,
            fontSize = 16.sp,
            color = textColor,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = label,
            fontSize = 13.sp,
            color = textColor
        )
    }
}
