package me.jitish.gymuu.ui.exercise

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import me.jitish.gymuu.data.settings.AppSettingsRepository
import me.jitish.gymuu.ui.RestTimerState

class RestTimerCompleteReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(RestTimerNotifier.EXTRA_ROUTINE_EXERCISE_ID) ?: return
        when (intent.action) {
            RestTimerNotifier.ACTION_TIMER_COMPLETE ->
                RestTimerAlarmStore.claim(context, id)
                    ?.let { RestTimerCompletion.deliver(context, it) }

            RestTimerNotifier.ACTION_DISMISS_ALERT ->
                RestTimerNotifier(context).cancelRestCompleteAlert(id)
        }
    }
}

internal fun isAppInForeground(): Boolean =
    ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)

/** Called by whoever successfully claimed the timer (alarm receiver or in-app ticker). */
internal object RestTimerCompletion {
    fun deliver(context: Context, timer: RestTimerState) {
        val notifier = RestTimerNotifier(context)
        notifier.cancelRunningTimer(timer.routineExerciseId)
        // The alert is posted even when the app is in front: the user may be on another
        // screen and a skipped alert used to be lost for good. Foreground only relaxes
        // the "vibrate until confirmed" mode, since the user is already looking at the app.
        val requiresAck =
            !isAppInForeground() && AppSettingsRepository(context).loadVibrateUntilConfirmed()
        val shown = notifier.showTimerComplete(timer, requiresAck)
        triggerRestCompleteVibration(context, allowUntilConfirmed = shown && requiresAck)
    }
}

/**
 * The stored timers are the source of truth. An alarm can be dropped (reboot, app update,
 * force stop, aggressive battery managers) or delivered late (inexact alarms in Doze), so
 * every process start and every reboot re-arms what is still running and delivers what
 * already ended instead of silently forgetting it.
 */
internal object RestTimerRecovery {
    /** Completions older than this are stale, not a missed alert. */
    private const val GRACE_MILLIS = 15 * 60_000L

    /**
     * Re-arms every stored timer, delivers the ones that ended while nothing was
     * listening, and drops stale ones. Returns the timers still running, with their
     * timing re-anchored to [SystemClock.elapsedRealtime].
     */
    fun reconcile(
        context: Context,
        postRunningNotifications: Boolean = false
    ): List<RestTimerState> {
        val notifier = RestTimerNotifier(context)
        val wallNow = System.currentTimeMillis()
        val elapsedNow = SystemClock.elapsedRealtime()
        return RestTimerAlarmStore.loadAll(context).mapNotNull { stored ->
            val remainingMs = stored.endWallClockMillis - wallNow
            when {
                remainingMs > 0L -> {
                    val live = stored.copy(
                        remainingSeconds = ((remainingMs + 999L) / 1_000L).toInt(),
                        endElapsedRealtimeMillis = elapsedNow + remainingMs
                    )
                    notifier.scheduleTimerComplete(live)
                    if (postRunningNotifications) notifier.showRunningTimer(live)
                    live
                }

                wallNow - stored.endWallClockMillis <= GRACE_MILLIS -> {
                    // Claim first: if another path already delivered, this is a no-op.
                    RestTimerAlarmStore.claim(context, stored.routineExerciseId)
                        ?.let { RestTimerCompletion.deliver(context, it) }
                    null
                }

                else -> {
                    notifier.cancelScheduledTimer(stored.routineExerciseId)
                    null
                }
            }
        }
    }
}
