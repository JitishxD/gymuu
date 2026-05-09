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
import me.jitish.gymuu.data.settings.VibrationRepeatOptions

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
    previewPattern: VibrationPattern? = null,
    previewRepeatCount: Int? = null,
    allowUntilConfirmed: Boolean = false
) {
    if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.VIBRATE) != PackageManager.PERMISSION_GRANTED) {
        return
    }

    val settingsRepository = AppSettingsRepository(context)
    val intensity = previewIntensity ?: settingsRepository.loadVibrationIntensity()
    val pattern = previewPattern ?: settingsRepository.loadVibrationPattern()
    val repeatCount = previewRepeatCount ?: settingsRepository.loadVibrationRepeatCount()
    val repeatUntilCancelled = allowUntilConfirmed && settingsRepository.loadVibrateUntilConfirmed()
    if (!intensity.enabled) return

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(VibratorManager::class.java) ?: return
        val vibrator = vibratorManager.defaultVibrator
        if (!vibrator.hasVibrator()) return

        vibrator.playRestCompleteVibration(intensity, pattern, repeatCount, repeatUntilCancelled)
        return
    }

    @Suppress("DEPRECATION")
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
    if (!vibrator.hasVibrator()) return

    vibrator.playRestCompleteVibration(intensity, pattern, repeatCount, repeatUntilCancelled)
}

internal fun cancelRestCompleteVibration(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(VibratorManager::class.java) ?: return
        vibratorManager.defaultVibrator.cancel()
        return
    }

    @Suppress("DEPRECATION")
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
    vibrator.cancel()
}

private fun Vibrator.playRestCompleteVibration(
    intensity: VibrationIntensity,
    pattern: VibrationPattern,
    repeatCount: Int,
    repeatUntilCancelled: Boolean
) {
    val effect = restCompleteEffect(
        intensity = intensity,
        pattern = pattern,
        repeatCount = repeatCount,
        repeatUntilCancelled = repeatUntilCancelled,
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
    repeatCount: Int,
    repeatUntilCancelled: Boolean,
    amplitudeControlAvailable: Boolean
): VibrationEffect {
    val timings = repeatedPatternTimings(
        pattern = pattern,
        repeatCount = if (repeatUntilCancelled) 1 else repeatCount
    )
    val repeatIndex = if (repeatUntilCancelled) 0 else -1
    return waveformEffect(timings, intensity, amplitudeControlAvailable, repeatIndex)
}

private fun repeatedPatternTimings(pattern: VibrationPattern, repeatCount: Int): LongArray {
    val basePattern = when (pattern) {
        VibrationPattern.SHORT -> longArrayOf(0, 350)
        VibrationPattern.LONG -> longArrayOf(0, 1_000)
        VibrationPattern.DOUBLE -> longArrayOf(0, 250, 130, 250)
        VibrationPattern.TRIPLE -> longArrayOf(0, 260, 140, 260, 140, 360)
    }
    val sanitizedRepeatCount = VibrationRepeatOptions.sanitize(repeatCount)
    if (sanitizedRepeatCount == 1) return basePattern

    val timings = mutableListOf<Long>()
    repeat(sanitizedRepeatCount) { index ->
        if (index == 0) {
            timings.addAll(basePattern.toList())
        } else {
            timings.add(REPEAT_GAP_MILLIS)
            timings.addAll(basePattern.drop(1))
        }
    }
    return timings.toLongArray()
}

private fun waveformEffect(
    pattern: LongArray,
    intensity: VibrationIntensity,
    amplitudeControlAvailable: Boolean,
    repeatIndex: Int
): VibrationEffect {
    return if (amplitudeControlAvailable) {
        VibrationEffect.createWaveform(pattern, amplitudesFor(pattern, intensity), repeatIndex)
    } else {
        VibrationEffect.createWaveform(pattern, repeatIndex)
    }
}

private fun amplitudesFor(pattern: LongArray, intensity: VibrationIntensity): IntArray {
    return IntArray(pattern.size) { index ->
        if (index % 2 == 1) intensity.amplitude else 0
    }
}

private const val REPEAT_GAP_MILLIS = 300L
