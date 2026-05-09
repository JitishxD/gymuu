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

enum class VibrationPattern(
    val storageValue: String,
    val label: String,
    val description: String
) {
    SHORT("short", "Short", "Quick single buzz"),
    LONG("long", "Long", "One continuous buzz"),
    DOUBLE("double", "Double", "Two alert pulses"),
    TRIPLE("triple", "Triple", "Three alert pulses");

    companion object {
        val DEFAULT = LONG

        fun fromStorage(value: String?): VibrationPattern {
            return entries.firstOrNull { it.storageValue == value } ?: DEFAULT
        }
    }
}

object VibrationRepeatOptions {
    const val DEFAULT = 1
    const val MIN = 1
    const val MAX = 5
    val OPTIONS = listOf(1, 2, 3, 5)

    fun sanitize(value: Int): Int {
        return value.coerceIn(MIN, MAX)
    }
}
