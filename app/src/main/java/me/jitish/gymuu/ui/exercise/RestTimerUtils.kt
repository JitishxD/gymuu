package me.jitish.gymuu.ui.exercise

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import me.jitish.gymuu.data.settings.AppSettingsRepository
import me.jitish.gymuu.data.settings.VibrationIntensity
import me.jitish.gymuu.data.settings.VibrationPattern
import me.jitish.gymuu.data.settings.VibrationRepeatOptions

private const val MAX_REST_SECONDS = 5999 // 99:59
private const val REPEAT_GAP_MILLIS = 300L
private val REST_REGEX = Regex("""^(\d*):(\d{0,2})$""")

internal val TIME_INPUT_PATTERN = Regex("""^\d{0,4}(:\d{0,2})?$""")

/** Returns 0 for "no rest" (blank, 0, 0:00); never invents a default. */
internal fun parseRestTimeToSeconds(restValue: String): Int {
    val value = restValue.trim()
    if (value.isEmpty()) return 0
    REST_REGEX.matchEntire(value)?.let { m ->
        val minutes = m.groupValues[1].ifEmpty { "0" }.toInt()
        val seconds = m.groupValues[2].ifEmpty { "0" }.toInt()
        return (minutes * 60 + seconds).coerceIn(0, MAX_REST_SECONDS)
    }
    return value.toIntOrNull()?.let { (it * 60).coerceIn(0, MAX_REST_SECONDS) } ?: 0
}

internal fun formatRestCountdown(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    return "${(safe / 60).toString().padStart(2, '0')}:${(safe % 60).toString().padStart(2, '0')}"
}

internal fun triggerRestCompleteVibration(
    context: Context,
    previewIntensity: VibrationIntensity? = null,
    previewPattern: VibrationPattern? = null,
    previewRepeatCount: Int? = null,
    allowUntilConfirmed: Boolean = false
) {
    val settings = AppSettingsRepository(context)
    val intensity = previewIntensity ?: settings.loadVibrationIntensity()
    if (!intensity.enabled) return
    val pattern = previewPattern ?: settings.loadVibrationPattern()
    val repeatCount = previewRepeatCount ?: settings.loadVibrationRepeatCount()
    val untilCancelled = allowUntilConfirmed && settings.loadVibrateUntilConfirmed()

    val vibrator = vibrator(context) ?: return
    if (!vibrator.hasVibrator()) return

    val timings = patternTimings(pattern, if (untilCancelled) 1 else repeatCount)
    val repeatIndex = if (untilCancelled) 0 else -1
    val effect = if (vibrator.hasAmplitudeControl()) {
        val amplitudes = IntArray(timings.size) { if (it % 2 == 1) intensity.amplitude else 0 }
        VibrationEffect.createWaveform(timings, amplitudes, repeatIndex)
    } else {
        VibrationEffect.createWaveform(timings, repeatIndex)
    }

    // ALARM usage is allowed from the background and is the right semantic for "rest is over".
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        vibrator.vibrate(
            effect,
            VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM)
        )
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(
            effect,
            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build()
        )
    }
}

internal fun cancelRestCompleteVibration(context: Context) {
    vibrator(context)?.cancel()
}

private fun vibrator(context: Context): Vibrator? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

private fun patternTimings(pattern: VibrationPattern, repeatCount: Int): LongArray {
    val base = when (pattern) {
        VibrationPattern.SHORT -> longArrayOf(0, 350)
        VibrationPattern.LONG -> longArrayOf(0, 1_000)
        VibrationPattern.DOUBLE -> longArrayOf(0, 250, 130, 250)
        VibrationPattern.TRIPLE -> longArrayOf(0, 260, 140, 260, 140, 360)
    }
    val count = VibrationRepeatOptions.sanitize(repeatCount)
    if (count == 1) return base
    return buildList {
        repeat(count) { i ->
            if (i == 0) addAll(base.toList()) else {
                add(REPEAT_GAP_MILLIS); addAll(base.drop(1))
            }
        }
    }.toLongArray()
}
