package me.jitish.gymuu.data.settings

import kotlin.math.roundToInt

enum class VibrationIntensity(
    val storageValue: String,
    val label: String,
    val percent: Int,
    val amplitude: Int
) {
    OFF("off", "Off", 0, 0),
    LIGHT("light", "Light", 25, 64),
    MEDIUM("medium", "Medium", 50, 128),
    STRONG("strong", "Strong", 75, 192),
    MAX("max", "Max", 100, 255);

    val enabled: Boolean
        get() = this != OFF

    companion object {
        val DEFAULT = STRONG

        fun fromStorage(value: String?): VibrationIntensity {
            return entries.firstOrNull { it.storageValue == value } ?: DEFAULT
        }

        fun fromSliderValue(value: Float): VibrationIntensity {
            val index = value.roundToInt().coerceIn(0, entries.lastIndex)
            return entries[index]
        }
    }
}
