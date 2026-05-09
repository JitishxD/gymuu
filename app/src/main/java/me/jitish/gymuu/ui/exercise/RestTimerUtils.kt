package me.jitish.gymuu.ui.exercise

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.VibrationAttributes
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.ContextCompat
import me.jitish.gymuu.data.settings.AppSettingsRepository
import me.jitish.gymuu.data.settings.VibrationIntensity
import me.jitish.gymuu.data.settings.VibrationPattern

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

internal fun triggerRestCompleteVibration(
    context: Context,
    previewIntensity: VibrationIntensity? = null,
    previewPattern: VibrationPattern? = null
) {
    if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.VIBRATE) != PackageManager.PERMISSION_GRANTED) {
        return
    }

    val settingsRepository = AppSettingsRepository(context)
    val intensity = previewIntensity ?: settingsRepository.loadVibrationIntensity()
    val pattern = previewPattern ?: settingsRepository.loadVibrationPattern()
    if (!intensity.enabled) return

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(VibratorManager::class.java) ?: return
        val vibrator = vibratorManager.defaultVibrator
        if (!vibrator.hasVibrator()) return

        vibrator.playRestCompleteVibration(intensity, pattern)
        return
    }

    @Suppress("DEPRECATION")
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
    if (!vibrator.hasVibrator()) return

    vibrator.playRestCompleteVibration(intensity, pattern)
}

private fun Vibrator.playRestCompleteVibration(intensity: VibrationIntensity, pattern: VibrationPattern) {
    val effect = restCompleteEffect(
        intensity = intensity,
        pattern = pattern,
        amplitudeControlAvailable = hasAmplitudeControl()
    )

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_NOTIFICATION))
    } else {
        vibrate(effect)
    }
}

private fun restCompleteEffect(
    intensity: VibrationIntensity,
    pattern: VibrationPattern,
    amplitudeControlAvailable: Boolean
): VibrationEffect {
    val amplitude = if (amplitudeControlAvailable) intensity.amplitude else VibrationEffect.DEFAULT_AMPLITUDE
    return when (pattern) {
        VibrationPattern.SHORT -> VibrationEffect.createOneShot(350, amplitude)
        VibrationPattern.LONG -> VibrationEffect.createOneShot(1_000, amplitude)
        VibrationPattern.DOUBLE -> waveformEffect(longArrayOf(0, 250, 130, 250), intensity, amplitudeControlAvailable)
        VibrationPattern.TRIPLE -> waveformEffect(longArrayOf(0, 260, 140, 260, 140, 360), intensity, amplitudeControlAvailable)
    }
}

private fun waveformEffect(
    pattern: LongArray,
    intensity: VibrationIntensity,
    amplitudeControlAvailable: Boolean
): VibrationEffect {
    return if (amplitudeControlAvailable) {
        VibrationEffect.createWaveform(pattern, amplitudesFor(pattern, intensity), -1)
    } else {
        VibrationEffect.createWaveform(pattern, -1)
    }
}

private fun amplitudesFor(pattern: LongArray, intensity: VibrationIntensity): IntArray {
    return IntArray(pattern.size) { index ->
        if (index % 2 == 1) intensity.amplitude else 0
    }
}
