package me.jitish.gymuu.ui.exercise

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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
        val foreground = isAppInForeground()
        val requiresAck = !foreground && AppSettingsRepository(context).loadVibrateUntilConfirmed()
        val shown = !foreground && notifier.showTimerComplete(timer, requiresAck)
        triggerRestCompleteVibration(context, allowUntilConfirmed = shown && requiresAck)
    }
}
