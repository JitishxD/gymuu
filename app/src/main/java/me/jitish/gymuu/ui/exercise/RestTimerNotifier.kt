package me.jitish.gymuu.ui.exercise

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import me.jitish.gymuu.MainActivity
import me.jitish.gymuu.R
import me.jitish.gymuu.ui.RestTimerState

internal class RestTimerNotifier(context: Context) {
    private val appContext = context.applicationContext
    private val alarmManager =
        appContext.getSystemService(AlarmManager::class.java)
    private val notificationManager =
        appContext.getSystemService(NotificationManager::class.java)

    init {
        createChannels()
    }

    fun showRunningTimer(timer: RestTimerState) {
        if (!canPostNotifications()) return

        val remainingSeconds = timer.remainingSeconds.coerceAtLeast(0)
        if (remainingSeconds == 0) {
            cancelRunningTimer(timer)
            return
        }

        cancelTimerComplete(timer)

        val remainingText = formatRestCountdown(remainingSeconds)
        val notificationLayout = restTimerLayout(
            countdownText = remainingText,
            label = appContext.getString(R.string.notification_rest_time_label),
        )
        val notification = NotificationCompat.Builder(appContext, RUNNING_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_rest_timer)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(notificationLayout)
            .setCustomBigContentView(notificationLayout)
            .setCustomHeadsUpContentView(notificationLayout)
            .setContentIntent(openAppIntent())
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setColor(REST_NOTIFICATION_COLOR)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .build()

        notificationManager.notify(timer.runningNotificationId(), notification)
    }

    private fun restTimerLayout(countdownText: String, label: String): RemoteViews {
        return RemoteViews(appContext.packageName, R.layout.notification_rest_timer).apply {
            setTextViewText(R.id.notification_rest_countdown, countdownText)
            setTextViewText(R.id.notification_rest_label, label)
        }
    }

    fun showTimerComplete(timer: RestTimerState) {
        if (!canPostNotifications()) return

        cancelRunningTimer(timer)

        val notificationLayout = restTimerLayout(
            countdownText = "00:00",
            label = appContext.getString(R.string.notification_rest_complete_label),
        )
        val notification = NotificationCompat.Builder(appContext, COMPLETE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_rest_timer)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(notificationLayout)
            .setCustomBigContentView(notificationLayout)
            .setCustomHeadsUpContentView(notificationLayout)
            .setContentIntent(openAppIntent())
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setColor(REST_NOTIFICATION_COLOR)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setVibrate(REST_COMPLETE_VIBRATION_PATTERN)
            .setDefaults(NotificationCompat.DEFAULT_VIBRATE)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            .build()

        notificationManager.notify(timer.completeNotificationId(), notification)
    }

    fun cancelRunningTimer(timer: RestTimerState) {
        cancelRunningTimer(timer.routineExerciseId)
    }

    fun cancelRunningTimer(routineExerciseId: String) {
        notificationManager.cancel(routineExerciseId.runningNotificationId())
    }

    fun cancelTimerComplete(timer: RestTimerState) {
        cancelTimerComplete(timer.routineExerciseId)
    }

    fun cancelTimerComplete(routineExerciseId: String) {
        notificationManager.cancel(routineExerciseId.completeNotificationId())
    }

    fun cancelTimerNotifications(routineExerciseId: String) {
        cancelRunningTimer(routineExerciseId)
        cancelTimerComplete(routineExerciseId)
    }

    fun scheduleTimerComplete(timer: RestTimerState) {
        RestTimerAlarmStore.saveTimer(appContext, timer)

        val pendingIntent = timerActionIntent(
            routineExerciseId = timer.routineExerciseId,
            action = ACTION_TIMER_COMPLETE,
            flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        ) ?: return

        if (canScheduleExactAlarms()) {
            scheduleExactTimer(timer, pendingIntent)
        } else {
            scheduleFallbackTimer(timer, pendingIntent)
        }
    }

    fun cancelScheduledTimer(timer: RestTimerState) {
        cancelScheduledTimer(timer.routineExerciseId)
    }

    fun cancelScheduledTimer(routineExerciseId: String) {
        val pendingIntent =
            timerActionIntent(
                routineExerciseId = routineExerciseId,
                action = ACTION_TIMER_COMPLETE,
                flags = PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
        RestTimerAlarmStore.removeTimer(appContext, routineExerciseId)
    }

    private fun createChannels() {
        val runningChannel = NotificationChannel(
            RUNNING_CHANNEL_ID,
            "Rest timer",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows active rest countdowns while Gymuu is in the background."
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        val completeChannel = NotificationChannel(
            COMPLETE_CHANNEL_ID,
            "Rest complete",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts when a background rest timer finishes."
            setSound(null, null)
            enableVibration(true)
            vibrationPattern = REST_COMPLETE_VIBRATION_PATTERN
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        notificationManager.createNotificationChannels(listOf(runningChannel, completeChannel))
    }

    private fun openAppIntent(): PendingIntent {
        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            appContext,
            OPEN_APP_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun timerActionIntent(routineExerciseId: String, action: String, flags: Int): PendingIntent? {
        val intent = Intent(appContext, RestTimerCompleteReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_ROUTINE_EXERCISE_ID, routineExerciseId)
        }
        return PendingIntent.getBroadcast(
            appContext,
            "$action:$routineExerciseId".notificationRequestCode(),
            intent,
            flags
        )
    }

    private fun canPostNotifications(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun canScheduleExactAlarms(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
    }

    private fun scheduleExactTimer(timer: RestTimerState, pendingIntent: PendingIntent) {
        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                timer.endElapsedRealtimeMillis,
                pendingIntent
            )
        } catch (_: SecurityException) {
            scheduleFallbackTimer(timer, pendingIntent)
        }
    }

    private fun scheduleFallbackTimer(timer: RestTimerState, pendingIntent: PendingIntent) {
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            timer.endElapsedRealtimeMillis,
            pendingIntent
        )
    }

    private fun RestTimerState.runningNotificationId(): Int {
        return routineExerciseId.runningNotificationId()
    }

    private fun RestTimerState.completeNotificationId(): Int {
        return routineExerciseId.completeNotificationId()
    }

    private fun String.runningNotificationId(): Int {
        return RUNNING_NOTIFICATION_ID_BASE + notificationOffset(this)
    }

    private fun String.completeNotificationId(): Int {
        return COMPLETE_NOTIFICATION_ID_BASE + notificationOffset(this)
    }

    private fun String.notificationRequestCode(): Int {
        return ALARM_REQUEST_CODE_BASE + notificationOffset(this)
    }

    private fun notificationOffset(value: String): Int {
        return (value.hashCode() and Int.MAX_VALUE) % NOTIFICATION_ID_RANGE
    }

    companion object {
        const val EXTRA_ROUTINE_EXERCISE_ID = "me.jitish.gymuu.extra.ROUTINE_EXERCISE_ID"
        const val ACTION_TIMER_COMPLETE = "me.jitish.gymuu.action.REST_TIMER_COMPLETE"

        private const val RUNNING_CHANNEL_ID = "rest_timer_running"
        private const val COMPLETE_CHANNEL_ID = "rest_timer_complete_v3"
        private const val OPEN_APP_REQUEST_CODE = 2101
        private const val REST_NOTIFICATION_COLOR = 0xFF4CAF50.toInt()
        private const val RUNNING_NOTIFICATION_ID_BASE = 7_000
        private const val COMPLETE_NOTIFICATION_ID_BASE = 107_000
        private const val ALARM_REQUEST_CODE_BASE = 207_000
        private const val NOTIFICATION_ID_RANGE = 100_000
    }
}
