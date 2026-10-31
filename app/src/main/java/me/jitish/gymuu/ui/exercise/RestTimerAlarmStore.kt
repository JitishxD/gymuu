package me.jitish.gymuu.ui.exercise

import android.content.Context
import androidx.core.content.edit
import me.jitish.gymuu.ui.RestTimerState
import org.json.JSONObject

/** Persists scheduled timers. Explicit JSON so R8 obfuscation can't break stored data. */
internal object RestTimerAlarmStore {
    private const val PREFS_NAME = "gymuu_rest_timer_alarms"
    private const val TIMER_PREFIX = "timer_"

    fun save(context: Context, timer: RestTimerState) {
        prefs(context).edit(commit = true) {
            putString(
                key(timer.routineExerciseId),
                timer.toJson()
            )
        }
    }

    fun remove(context: Context, id: String) {
        prefs(context).edit(commit = true) { remove(key(id)) }
    }

    fun load(context: Context, id: String): RestTimerState? =
        prefs(context).getString(key(id), null)?.let(::parse)

    fun loadAll(context: Context): List<RestTimerState> =
        prefs(context).all.mapNotNull { (k, v) ->
            if (k.startsWith(TIMER_PREFIX) && v is String) parse(
                v
            ) else null
        }

    /** Atomically takes ownership of a finished timer. Only one caller ever gets non-null. */
    @Synchronized
    fun claim(context: Context, id: String): RestTimerState? {
        val timer = load(context, id) ?: return null
        remove(context, id)
        return timer
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun key(id: String) = "$TIMER_PREFIX$id"

    private fun RestTimerState.toJson(): String = JSONObject()
        .put("routineId", routineId).put("dayId", dayId)
        .put("routineExerciseId", routineExerciseId).put("exerciseName", exerciseName)
        .put("totalSeconds", totalSeconds).put("remainingSeconds", remainingSeconds)
        .put("endElapsedRealtimeMillis", endElapsedRealtimeMillis)
        .put("endWallClockMillis", endWallClockMillis)
        .toString()

    private fun parse(json: String): RestTimerState? = runCatching {
        val o = JSONObject(json)
        RestTimerState(
            routineId = o.getString("routineId"),
            dayId = o.getString("dayId"),
            routineExerciseId = o.getString("routineExerciseId"),
            exerciseName = o.optString("exerciseName"),
            totalSeconds = o.getInt("totalSeconds"),
            remainingSeconds = o.optInt("remainingSeconds"),
            endElapsedRealtimeMillis = o.getLong("endElapsedRealtimeMillis"),
            endWallClockMillis = o.getLong("endWallClockMillis")
        )
    }.getOrNull()
}
