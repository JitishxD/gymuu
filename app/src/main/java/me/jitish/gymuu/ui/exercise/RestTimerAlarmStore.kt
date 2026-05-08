package me.jitish.gymuu.ui.exercise

import android.content.Context
import androidx.core.content.edit
import com.google.gson.Gson
import me.jitish.gymuu.ui.RestTimerState

internal object RestTimerAlarmStore {
    private const val PREFS_NAME = "gymuu_rest_timer_alarms"
    private const val TIMER_PREFIX = "timer_"
    private const val APP_FOREGROUND_KEY = "app_foreground"

    private val gson = Gson()

    fun saveTimer(context: Context, timer: RestTimerState) {
        prefs(context).edit {
            putString(timerKey(timer.routineExerciseId), gson.toJson(timer))
        }
    }

    fun removeTimer(context: Context, routineExerciseId: String) {
        prefs(context).edit {
            remove(timerKey(routineExerciseId))
        }
    }

    fun loadTimer(context: Context, routineExerciseId: String): RestTimerState? {
        val json = prefs(context).getString(timerKey(routineExerciseId), null) ?: return null
        return runCatching { gson.fromJson(json, RestTimerState::class.java) }.getOrNull()
    }

    fun loadTimers(context: Context): List<RestTimerState> {
        return prefs(context).all.mapNotNull { (key, value) ->
            if (!key.startsWith(TIMER_PREFIX) || value !is String) return@mapNotNull null
            runCatching { gson.fromJson(value, RestTimerState::class.java) }.getOrNull()
        }
    }

    fun setAppForeground(context: Context, foreground: Boolean) {
        prefs(context).edit {
            putBoolean(APP_FOREGROUND_KEY, foreground)
        }
    }

    fun isAppForeground(context: Context): Boolean {
        return prefs(context).getBoolean(APP_FOREGROUND_KEY, false)
    }

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun timerKey(routineExerciseId: String) = "$TIMER_PREFIX$routineExerciseId"
}
