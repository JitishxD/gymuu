package me.jitish.gymuu.ui.exercise

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import me.jitish.gymuu.data.settings.AppSettingsRepository

class RestTimerCompleteReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val routineExerciseId = intent.getStringExtra(RestTimerNotifier.EXTRA_ROUTINE_EXERCISE_ID) ?: return
        val notifier = RestTimerNotifier(context)

        if (intent.action != RestTimerNotifier.ACTION_TIMER_COMPLETE) {
            notifier.cancelScheduledTimer(routineExerciseId)
            notifier.cancelTimerNotifications(routineExerciseId)
            return
        }

        val timer = RestTimerAlarmStore.loadTimer(context, routineExerciseId)
            ?.copy(remainingSeconds = 0)
            ?: return

        RestTimerAlarmStore.removeTimer(context, routineExerciseId)

        notifier.cancelRunningTimer(timer)

        if (!RestTimerAlarmStore.isAppForeground(context)) {
            val completionNotificationShown = notifier.showTimerComplete(
                timer = timer,
                requiresAcknowledgement = AppSettingsRepository(context).loadVibrateUntilConfirmed()
            )
            triggerRestCompleteVibration(context, allowUntilConfirmed = completionNotificationShown)
        }
    }
}
