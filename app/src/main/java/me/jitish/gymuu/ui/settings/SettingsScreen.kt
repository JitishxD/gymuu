package me.jitish.gymuu.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import me.jitish.gymuu.data.settings.VibrationIntensity
import me.jitish.gymuu.data.settings.VibrationPattern
import me.jitish.gymuu.data.settings.VibrationRepeatOptions
import me.jitish.gymuu.ui.GymUiState
import me.jitish.gymuu.ui.GymViewModel
import me.jitish.gymuu.ui.components.SectionHeading
import me.jitish.gymuu.ui.components.TopTitleBar
import me.jitish.gymuu.ui.navigation.Routes
import me.jitish.gymuu.ui.theme.GymBlack
import me.jitish.gymuu.ui.theme.GymBorder
import me.jitish.gymuu.ui.theme.GymCard
import me.jitish.gymuu.ui.theme.GymMuted

@Composable
internal fun SettingsScreen(
    state: GymUiState,
    viewModel: GymViewModel,
    navController: NavHostController
) {
    Scaffold(containerColor = GymBlack) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                TopTitleBar(
                    title = "SETTINGS",
                    onBack = {
                        if (!navController.navigateUp()) {
                            navController.navigate(Routes.ROUTINES) {
                                launchSingleTop = true
                            }
                        }
                    }
                )
            }
            item { SectionHeading("REST TIMER") }
            item {
                VibrationIntensityPanel(
                    intensity = state.vibrationIntensity,
                    pattern = state.vibrationPattern,
                    repeatCount = state.vibrationRepeatCount,
                    vibrateUntilConfirmed = state.vibrateUntilConfirmed,
                    onIntensityChange = viewModel::updateVibrationIntensity,
                    onPatternChange = viewModel::updateVibrationPattern,
                    onRepeatCountChange = viewModel::updateVibrationRepeatCount,
                    onVibrateUntilConfirmedChange = viewModel::updateVibrateUntilConfirmed,
                    onPreview = viewModel::previewRestCompleteVibration
                )
            }
        }
    }
}

@Composable
private fun VibrationIntensityPanel(
    intensity: VibrationIntensity,
    pattern: VibrationPattern,
    repeatCount: Int,
    vibrateUntilConfirmed: Boolean,
    onIntensityChange: (VibrationIntensity) -> Unit,
    onPatternChange: (VibrationPattern) -> Unit,
    onRepeatCountChange: (Int) -> Unit,
    onVibrateUntilConfirmedChange: (Boolean) -> Unit,
    onPreview: (VibrationIntensity, VibrationPattern, Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(GymCard)
            .border(1.dp, GymBorder, RoundedCornerShape(8.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                Text("Vibration intensity", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Text("Rest complete", color = GymMuted, fontSize = 14.sp, letterSpacing = 1.sp)
            }
            Text(
                text = "${intensity.label.uppercase()} / ${pattern.label.uppercase()} / ${repeatCount}X",
                color = Color.Black,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White)
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            )
        }

        Slider(
            value = VibrationIntensity.entries.indexOf(intensity).toFloat(),
            onValueChange = { onIntensityChange(VibrationIntensity.fromSliderValue(it)) },
            valueRange = 0f..VibrationIntensity.entries.lastIndex.toFloat(),
            steps = VibrationIntensity.entries.size - 2,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                activeTickColor = Color.Black,
                inactiveTrackColor = GymBorder,
                inactiveTickColor = GymMuted
            )
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            VibrationIntensity.entries.forEach { option ->
                Text(
                    text = "${option.percent}%",
                    color = if (option == intensity) Color.White else GymMuted,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(2.dp))

        Button(
            onClick = { onPreview(intensity, pattern, repeatCount) },
            enabled = intensity.enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black,
                disabledContainerColor = GymBorder,
                disabledContentColor = GymMuted
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Vibration, contentDescription = null)
            Text(
                "TEST ${pattern.label.uppercase()} ${repeatCount}X",
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("REPEATS", color = GymMuted, fontSize = 12.sp, letterSpacing = 2.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                VibrationRepeatOptions.OPTIONS.forEach { option ->
                    Button(
                        onClick = {
                            onRepeatCountChange(option)
                            onPreview(intensity, pattern, option)
                        },
                        enabled = intensity.enabled,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (option == repeatCount) Color.White else Color(0xFF2A2A2A),
                            contentColor = if (option == repeatCount) Color.Black else Color.White,
                            disabledContainerColor = GymBorder,
                            disabledContentColor = GymMuted
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("${option}X", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                Text("Until confirmed", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text("Background alert", color = GymMuted, fontSize = 13.sp, letterSpacing = 1.sp)
            }
            Switch(
                checked = vibrateUntilConfirmed,
                onCheckedChange = onVibrateUntilConfirmedChange,
                enabled = intensity.enabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = Color.White,
                    uncheckedThumbColor = GymMuted,
                    uncheckedTrackColor = GymBorder,
                    disabledCheckedThumbColor = GymMuted,
                    disabledCheckedTrackColor = GymBorder,
                    disabledUncheckedThumbColor = GymMuted,
                    disabledUncheckedTrackColor = GymBorder
                )
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("PATTERN", color = GymMuted, fontSize = 12.sp, letterSpacing = 2.sp)
            VibrationPattern.entries.chunked(2).forEach { rowOptions ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    rowOptions.forEach { option ->
                        Button(
                            onClick = {
                                onPatternChange(option)
                                onPreview(intensity, option, repeatCount)
                            },
                            enabled = intensity.enabled,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (option == pattern) Color.White else Color(0xFF2A2A2A),
                                contentColor = if (option == pattern) Color.Black else Color.White,
                                disabledContainerColor = GymBorder,
                                disabledContentColor = GymMuted
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(option.label.uppercase(), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
