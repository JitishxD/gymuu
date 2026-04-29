package me.jitish.gymuu.ui.exercise

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.ContextCompat

private const val MAX_REST_SECONDS = 5999 // 99:59
private const val DEFAULT_REST_SECONDS = 120 // 2:00

internal val TIME_INPUT_PATTERN = Regex("""^\d{0,4}(:\d{0,2})?$""")

internal fun parseRestTimeToSeconds(restValue: String): Int {
    val normalized = restValue.trim()
    if (normalized.isBlank()) return 0

    val match = Regex("""^(\d+):(\d{1,2})$""").matchEntire(normalized)
    if (match != null) {
        val minutes = match.groupValues[1].toIntOrNull() ?: return DEFAULT_REST_SECONDS
        val seconds = match.groupValues[2].toIntOrNull() ?: return DEFAULT_REST_SECONDS
        if (seconds > 59) return DEFAULT_REST_SECONDS
        return (minutes * 60 + seconds).coerceIn(1, MAX_REST_SECONDS)
    }

    val bareMinutes = normalized.toIntOrNull()
    if (bareMinutes != null && bareMinutes > 0) {
        return (bareMinutes * 60).coerceIn(1, MAX_REST_SECONDS)
    }

    return DEFAULT_REST_SECONDS
}

internal fun formatRestCountdown(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

internal fun triggerRestCompleteVibration(context: Context) {
    if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.VIBRATE) != PackageManager.PERMISSION_GRANTED) {
        return
    }

    val vibrationPattern = longArrayOf(0, 300, 150, 300, 150, 500)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(VibratorManager::class.java) ?: return
        val vibrator = vibratorManager.defaultVibrator
        if (!vibrator.hasVibrator()) return

        vibrator.vibrate(VibrationEffect.createWaveform(vibrationPattern, -1))
        return
    }

    @Suppress("DEPRECATION")
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
    if (!vibrator.hasVibrator()) return

    vibrator.vibrate(VibrationEffect.createWaveform(vibrationPattern, -1))
}
