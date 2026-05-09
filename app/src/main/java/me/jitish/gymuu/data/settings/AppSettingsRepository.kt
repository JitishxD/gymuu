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

    private val _vibrationRepeatCount = MutableStateFlow(loadVibrationRepeatCount())
    val vibrationRepeatCount: StateFlow<Int> = _vibrationRepeatCount

    private val _vibrateUntilConfirmed = MutableStateFlow(loadVibrateUntilConfirmed())
    val vibrateUntilConfirmed: StateFlow<Boolean> = _vibrateUntilConfirmed

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

    fun updateVibrationRepeatCount(count: Int) {
        val sanitizedCount = VibrationRepeatOptions.sanitize(count)
        if (_vibrationRepeatCount.value == sanitizedCount) return

        prefs.edit {
            putInt(VIBRATION_REPEAT_COUNT_KEY, sanitizedCount)
        }
        _vibrationRepeatCount.value = sanitizedCount
    }

    fun updateVibrateUntilConfirmed(enabled: Boolean) {
        if (_vibrateUntilConfirmed.value == enabled) return

        prefs.edit {
            putBoolean(VIBRATE_UNTIL_CONFIRMED_KEY, enabled)
        }
        _vibrateUntilConfirmed.value = enabled
    }

    fun loadVibrationIntensity(): VibrationIntensity {
        return VibrationIntensity.fromStorage(prefs.getString(VIBRATION_INTENSITY_KEY, null))
    }

    fun loadVibrationPattern(): VibrationPattern {
        return VibrationPattern.fromStorage(prefs.getString(VIBRATION_PATTERN_KEY, null))
    }

    fun loadVibrationRepeatCount(): Int {
        return VibrationRepeatOptions.sanitize(
            prefs.getInt(VIBRATION_REPEAT_COUNT_KEY, VibrationRepeatOptions.DEFAULT)
        )
    }

    fun loadVibrateUntilConfirmed(): Boolean {
        return prefs.getBoolean(VIBRATE_UNTIL_CONFIRMED_KEY, false)
    }

    private companion object {
        const val PREFS_NAME = "gymuu_app_settings"
        const val VIBRATION_INTENSITY_KEY = "vibration_intensity"
        const val VIBRATION_PATTERN_KEY = "vibration_pattern"
        const val VIBRATION_REPEAT_COUNT_KEY = "vibration_repeat_count"
        const val VIBRATE_UNTIL_CONFIRMED_KEY = "vibrate_until_confirmed"
    }
}
