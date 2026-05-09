package me.jitish.gymuu.data.settings

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AppSettingsRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _vibrationIntensity = MutableStateFlow(loadVibrationIntensity())
    val vibrationIntensity: StateFlow<VibrationIntensity> = _vibrationIntensity

    private val _vibrationPattern = MutableStateFlow(loadVibrationPattern())
    val vibrationPattern: StateFlow<VibrationPattern> = _vibrationPattern

    fun updateVibrationIntensity(intensity: VibrationIntensity) {
        if (_vibrationIntensity.value == intensity) return

        prefs.edit {
            putString(VIBRATION_INTENSITY_KEY, intensity.storageValue)
        }
        _vibrationIntensity.value = intensity
    }

    fun updateVibrationPattern(pattern: VibrationPattern) {
        if (_vibrationPattern.value == pattern) return

        prefs.edit {
            putString(VIBRATION_PATTERN_KEY, pattern.storageValue)
        }
        _vibrationPattern.value = pattern
    }

    fun loadVibrationIntensity(): VibrationIntensity {
        return VibrationIntensity.fromStorage(prefs.getString(VIBRATION_INTENSITY_KEY, null))
    }

    fun loadVibrationPattern(): VibrationPattern {
        return VibrationPattern.fromStorage(prefs.getString(VIBRATION_PATTERN_KEY, null))
    }

    private companion object {
        const val PREFS_NAME = "gymuu_app_settings"
        const val VIBRATION_INTENSITY_KEY = "vibration_intensity"
        const val VIBRATION_PATTERN_KEY = "vibration_pattern"
    }
}
