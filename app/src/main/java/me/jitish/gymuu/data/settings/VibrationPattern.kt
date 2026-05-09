package me.jitish.gymuu.data.settings

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
