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
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import me.jitish.gymuu.MainActivity
import me.jitish.gymuu.R
import me.jitish.gymuu.ui.RestTimerState

internal class RestTimerNotifier(context: Context) {
    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(AlarmManager::class.java)
    private val notificationManager = appContext.getSystemService(NotificationManager::class.java)

    init {
        ensureChannels()
    }

    /** Posted once; the Chronometer counts down on its own, no per-second updates needed. */
    fun showRunningTimer(timer: RestTimerState) {
        if (!canPostNotifications()) return
        val remainingMs = timer.endElapsedRealtimeMillis - SystemClock.elapsedRealtime()
        if (remainingMs <= 0) return cancelRunningTimer(timer.routineExerciseId)

        val label = appContext.getString(R.string.notification_rest_time_label)
        val views = RemoteViews(appContext.packageName, R.layout.notification_rest_timer).apply {
            setViewVisibility(R.id.notification_rest_chronometer, View.VISIBLE)
            setViewVisibility(R.id.notification_rest_countdown, View.GONE)
            setChronometer(
                R.id.notification_rest_chronometer,
                timer.endElapsedRealtimeMillis,
                null,
                true
            )
            setChronometerCountDown(R.id.notification_rest_chronometer, true)
            setTextViewText(R.id.notification_rest_label, label)
            setViewVisibility(R.id.notification_rest_hint, View.GONE)
        }
        val notification = NotificationCompat.Builder(appContext, RUNNING_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_rest_timer)
            .setContentTitle(label)
            .setContentText(timer.exerciseName)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(views)
            .setContentIntent(openAppIntent(null))
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)
            .setTimeoutAfter(remainingMs)
            .build()
        notificationManager.notify(
            runningTag(timer.routineExerciseId),
            REST_NOTIFICATION_ID,
            notification
        )
    }

    fun showTimerComplete(timer: RestTimerState, requiresAcknowledgement: Boolean): Boolean {
        if (!canPostNotifications()) return false
        val id = timer.routineExerciseId
        cancelRunningTimer(id)

        val label = appContext.getString(R.string.notification_rest_complete_label)
        val hint = appContext.getString(
            if (requiresAcknowledgement) R.string.notification_tap_to_stop_vibration_label
            else R.string.notification_tap_to_return_label
        )
        val views = RemoteViews(appContext.packageName, R.layout.notification_rest_timer).apply {
            setViewVisibility(R.id.notification_rest_chronometer, View.GONE)
            setViewVisibility(R.id.notification_rest_countdown, View.VISIBLE)
            setTextViewText(R.id.notification_rest_countdown, "00:00")
            setTextViewText(R.id.notification_rest_label, label)
            setTextViewText(R.id.notification_rest_hint, hint)
            setViewVisibility(R.id.notification_rest_hint, View.VISIBLE)
        }
        val dismiss = receiverIntent(ACTION_DISMISS_ALERT, id, DISMISS_BASE)
        val builder = NotificationCompat.Builder(appContext, COMPLETE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_rest_timer)
            .setContentTitle(label)
            .setContentText(hint)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(views)
            .setContentIntent(openAppIntent(id))
            .setDeleteIntent(dismiss) // swiping away also stops "until confirmed" vibration
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setShowWhen(true)
        if (requiresAcknowledgement) {
            builder.addAction(
                0,
                appContext.getString(R.string.notification_stop_vibration),
                dismiss
            )
        }
        notificationManager.notify(completeTag(id), REST_NOTIFICATION_ID, builder.build())
        return true
    }

    fun cancelRunningTimer(id: String) =
        notificationManager.cancel(runningTag(id), REST_NOTIFICATION_ID)

    fun cancelTimerComplete(id: String) =
        notificationManager.cancel(completeTag(id), REST_NOTIFICATION_ID)
    fun cancelTimerNotifications(id: String) {
        cancelRunningTimer(id); cancelTimerComplete(id)
    }

    fun cancelRestCompleteAlert(id: String) {
        cancelTimerComplete(id); cancelRestCompleteVibration(appContext)
    }

    fun cancelRestCompleteVibration() = cancelRestCompleteVibration(appContext)

    fun scheduleTimerComplete(timer: RestTimerState) {
        RestTimerAlarmStore.save(appContext, timer)
        val pi = receiverIntent(ACTION_TIMER_COMPLETE, timer.routineExerciseId, ALARM_BASE)
        val exact =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        if (exact) {
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    timer.endElapsedRealtimeMillis,
                    pi
                )
                return
            } catch (_: SecurityException) {
                // Permission revoked between the check and the call; fall back to inexact.
            }
        }
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            timer.endElapsedRealtimeMillis,
            pi
        )
    }

    /** Cancels the alarm and forgets the timer. */
    fun cancelScheduledTimer(id: String) {
        val intent = Intent(appContext, RestTimerCompleteReceiver::class.java).setAction(
            ACTION_TIMER_COMPLETE
        )
        PendingIntent.getBroadcast(
            appContext, requestCode(ALARM_BASE, id), intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )?.let(alarmManager::cancel)
        RestTimerAlarmStore.remove(appContext, id)
    }

    private fun ensureChannels() {
        if (channelsCreated) return
        LEGACY_CHANNEL_IDS.forEach(notificationManager::deleteNotificationChannel)
        val running = NotificationChannel(
            RUNNING_CHANNEL_ID,
            appContext.getString(R.string.channel_running_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = appContext.getString(R.string.channel_running_desc)
            setSound(null, null); enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        val complete = NotificationChannel(
            COMPLETE_CHANNEL_ID,
            appContext.getString(R.string.channel_complete_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = appContext.getString(R.string.channel_complete_desc)
            setSound(null, null); enableVibration(false) // the app drives the vibration itself
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        notificationManager.createNotificationChannels(listOf(running, complete))
        channelsCreated = true
    }

    private fun openAppIntent(id: String?): PendingIntent {
        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (id != null) {
                action = ACTION_OPEN_REST_COMPLETE; putExtra(EXTRA_ROUTINE_EXERCISE_ID, id)
            }
        }
        val code = if (id == null) OPEN_APP_REQUEST_CODE else requestCode(OPEN_BASE, id)
        return PendingIntent.getActivity(
            appContext,
            code,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun receiverIntent(action: String, id: String, base: Int): PendingIntent {
        val intent = Intent(appContext, RestTimerCompleteReceiver::class.java)
            .setAction(action).putExtra(EXTRA_ROUTINE_EXERCISE_ID, id)
        return PendingIntent.getBroadcast(
            appContext, requestCode(base, id), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun canPostNotifications(): Boolean {
        if (!NotificationManagerCompat.from(appContext).areNotificationsEnabled()) return false
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    appContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
    }

    // The exercise id is the notification tag, so two timers can never overwrite each
    // other's notification through a hash collision.
    private fun runningTag(id: String) = "rest_running:$id"
    private fun completeTag(id: String) = "rest_complete:$id"

    private fun offset(id: String) = (id.hashCode() and Int.MAX_VALUE) % RANGE
    private fun requestCode(base: Int, id: String) = base + offset(id)

    companion object {
        const val EXTRA_ROUTINE_EXERCISE_ID = "me.jitish.gymuu.extra.ROUTINE_EXERCISE_ID"
        const val ACTION_TIMER_COMPLETE = "me.jitish.gymuu.action.REST_TIMER_COMPLETE"
        const val ACTION_DISMISS_ALERT = "me.jitish.gymuu.action.DISMISS_REST_ALERT"
        const val ACTION_OPEN_REST_COMPLETE = "me.jitish.gymuu.action.OPEN_REST_COMPLETE"

        private const val RUNNING_CHANNEL_ID = "rest_timer_running_v2"
        private const val COMPLETE_CHANNEL_ID = "rest_timer_complete_v2"
        private val LEGACY_CHANNEL_IDS =
            listOf("rest_timer_running_lockscreen", "rest_timer_complete_app_controlled")
        private const val OPEN_APP_REQUEST_CODE = 2101
        private const val REST_NOTIFICATION_ID = 1
        private const val RANGE = 100_000
        private const val ALARM_BASE = 207_000
        private const val DISMISS_BASE = 307_000
        private const val OPEN_BASE = 407_000

        @Volatile
        private var channelsCreated = false
    }
}
