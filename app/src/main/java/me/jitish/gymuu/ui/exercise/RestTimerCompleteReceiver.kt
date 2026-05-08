package me.jitish.gymuu.ui.exercise

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class RestTimerCompleteReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val routineExerciseId = intent.getStringExtra(RestTimerNotifier.EXTRA_ROUTINE_EXERCISE_ID) ?: return
        if (intent.action != RestTimerNotifier.ACTION_TIMER_COMPLETE) {
            val notifier = RestTimerNotifier(context)
            notifier.cancelScheduledTimer(routineExerciseId)
            notifier.cancelTimerNotifications(routineExerciseId)
            return
        }

        val timer = RestTimerAlarmStore.loadTimer(context, routineExerciseId)
            ?.copy(remainingSeconds = 0)
            ?: return

        RestTimerAlarmStore.removeTimer(context, routineExerciseId)

        val notifier = RestTimerNotifier(context)
        notifier.cancelRunningTimer(timer)

        if (!RestTimerAlarmStore.isAppForeground(context)) {
            notifier.showTimerComplete(timer)
            triggerRestCompleteVibration(context)
        }
    }
}
