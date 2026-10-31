package me.jitish.gymuu.ui

import android.app.Application
import android.content.Context
import android.os.SystemClock
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.jitish.gymuu.data.exercise.Exercise
import me.jitish.gymuu.data.exercise.ExerciseRepository
import me.jitish.gymuu.data.routine.CreateExerciseDraft
import me.jitish.gymuu.data.routine.CustomExercise
import me.jitish.gymuu.data.routine.Routine
import me.jitish.gymuu.data.routine.RoutineExercise
import me.jitish.gymuu.data.routine.RoutineExercisePastePosition
import me.jitish.gymuu.data.routine.RoutineRepository
import me.jitish.gymuu.data.routine.WorkoutDay
import me.jitish.gymuu.data.settings.AppSettingsRepository
import me.jitish.gymuu.data.settings.VibrationIntensity
import me.jitish.gymuu.data.settings.VibrationPattern
import me.jitish.gymuu.data.settings.VibrationRepeatOptions
import me.jitish.gymuu.ui.exercise.RestTimerAlarmStore
import me.jitish.gymuu.ui.exercise.RestTimerCompletion
import me.jitish.gymuu.ui.exercise.RestTimerNotifier
import me.jitish.gymuu.ui.exercise.formatRestCountdown
import me.jitish.gymuu.ui.exercise.isAppInForeground
import me.jitish.gymuu.ui.exercise.parseRestTimeToSeconds
import me.jitish.gymuu.ui.exercise.triggerRestCompleteVibration

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val app get() = getApplication<Application>()
    private val exerciseRepository = ExerciseRepository(application)
    private val routineRepository = RoutineRepository(application, viewModelScope)
    private val settingsRepository = AppSettingsRepository(application)
    private val navigationPrefs =
        application.getSharedPreferences(NAVIGATION_PREFS_NAME, Context.MODE_PRIVATE)
    private val notifier = RestTimerNotifier(application)

    private val searchQuery = MutableStateFlow("")
    private val selectedCategory = MutableStateFlow(ExerciseCategory.ALL)
    private val clipboard = MutableStateFlow<List<RoutineExercise>>(emptyList())
    private val lastWorkoutRoute = MutableStateFlow(loadLastWorkoutRoute())
    private val restTimers = MutableStateFlow<Map<String, RestTimerState>>(emptyMap())
    private var tickerJob: Job? = null

    /** Lowercased search text + categories computed once per exercise, off the main thread. */
    private val searchIndex = exerciseRepository.exercises
        .map { list -> list.map(::IndexedExercise) }
        .flowOn(Dispatchers.Default)

    private val pickerState = combine(
        searchIndex, routineRepository.customExercises, searchQuery, selectedCategory, ::buildPicker
    ).flowOn(Dispatchers.Default)

    private val settingsState = combine(
        settingsRepository.vibrationIntensity, settingsRepository.vibrationPattern,
        settingsRepository.vibrationRepeatCount, settingsRepository.vibrateUntilConfirmed
    ) { i, p, r, u -> AppSettingsState(i, p, r, u) }

    private val dataState = combine(
        exerciseRepository.exercises, routineRepository.routines, routineRepository.customExercises,
        searchQuery, selectedCategory
    ) { exercises, routines, customs, query, category ->
        AppUiState(
            exercises = exercises,
            routines = routines,
            customExercises = customs,
            searchQuery = query,
            selectedCategory = category
        )
    }

    val uiState: StateFlow<AppUiState> = combine(
        dataState,
        pickerState,
        clipboard,
        restTimers,
        combine(lastWorkoutRoute, settingsState, ::Pair)
    ) { state, picker, copied, timers, (route, settings) ->
        state.copy(
            pickerCustomExercises = picker.customs,
            pickerSections = picker.sections,
            copiedExercises = copied,
            restTimers = timers,
            lastWorkoutRoute = route,
            vibrationIntensity = settings.intensity,
            vibrationPattern = settings.pattern,
            vibrationRepeatCount = settings.repeatCount,
            vibrateUntilConfirmed = settings.untilConfirmed
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppUiState())

    private val processObserver = LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_START -> {
                notifier.cancelRestCompleteVibration()
                restTimers.value.values.forEach {
                    notifier.cancelRunningTimer(it.routineExerciseId)
                    notifier.scheduleTimerComplete(it) // upgrades to exact if permission was just granted
                }
            }

            Lifecycle.Event.ON_STOP -> {
                routineRepository.flush()
                restTimers.value.values.forEach(notifier::showRunningTimer)
            }

            else -> Unit
        }
    }

    init {
        restoreScheduledRestTimers()
        ProcessLifecycleOwner.get().lifecycle.addObserver(processObserver)
        viewModelScope.launch {
            exerciseRepository.loadExercises()
            routineRepository.refreshBuiltInGifUrls(exerciseRepository.exercises.value)
        }
    }

    // ---------- Rest timers ----------

    fun startRestTimer(routineId: String, dayId: String, exercise: RoutineExercise) {
        val total = parseRestTimeToSeconds(exercise.rest)
        if (total <= 0) return
        routineRepository.updateRest(routineId, dayId, exercise.id, formatRestCountdown(total))

        val timer = RestTimerState(
            routineId = routineId,
            dayId = dayId,
            routineExerciseId = exercise.id,
            exerciseName = exercise.name,
            totalSeconds = total,
            remainingSeconds = total,
            endElapsedRealtimeMillis = SystemClock.elapsedRealtime() + total * 1_000L,
            endWallClockMillis = System.currentTimeMillis() + total * 1_000L
        )
        notifier.cancelTimerComplete(exercise.id)
        restTimers.update { it + (exercise.id to timer) }
        notifier.scheduleTimerComplete(timer)
        if (!isAppInForeground()) notifier.showRunningTimer(timer)
        ensureTicker()
    }

    fun cancelRestTimer(routineExerciseId: String) {
        restTimers.update { it - routineExerciseId }
        notifier.cancelScheduledTimer(routineExerciseId)
        notifier.cancelTimerNotifications(routineExerciseId)
    }

    private fun ensureTicker() {
        if (tickerJob?.isActive == true) return
        tickerJob = viewModelScope.launch {
            while (restTimers.value.isNotEmpty()) {
                delay(TICK_MS)
                tick()
            }
        }
    }

    private fun tick() {
        restTimers.value.values.forEach { timer ->
            val id = timer.routineExerciseId
            val remaining = remainingSeconds(timer.endElapsedRealtimeMillis)
            when {
                remaining <= 0 -> {
                    restTimers.update { it - id }
                    // If the alarm receiver already claimed it, it already delivered completion.
                    RestTimerAlarmStore.claim(app, id)?.let {
                        notifier.cancelScheduledTimer(id)
                        RestTimerCompletion.deliver(app, it)
                    }
                }

                RestTimerAlarmStore.load(app, id) == null -> { // cancelled elsewhere
                    restTimers.update { it - id }
                    notifier.cancelRunningTimer(id)
                }

                remaining != timer.remainingSeconds ->
                    restTimers.update { it + (id to timer.copy(remainingSeconds = remaining)) }
            }
        }
    }

    private fun restoreScheduledRestTimers() {
        RestTimerAlarmStore.loadAll(app).forEach { stored ->
            val remainingMs = stored.endWallClockMillis - System.currentTimeMillis()
            if (remainingMs <= 0) {
                notifier.cancelScheduledTimer(stored.routineExerciseId) // stale (e.g. after reboot)
                return@forEach
            }
            val restored = stored.copy(
                remainingSeconds = ((remainingMs + 999) / 1_000).toInt(),
                endElapsedRealtimeMillis = SystemClock.elapsedRealtime() + remainingMs
            )
            restTimers.update { it + (restored.routineExerciseId to restored) }
            notifier.scheduleTimerComplete(restored)
        }
        ensureTicker()
    }

    private fun remainingSeconds(endElapsed: Long): Int =
        ((endElapsed - SystemClock.elapsedRealtime() + 999L) / 1_000L).toInt().coerceAtLeast(0)

    private fun cancelRestTimersMatching(predicate: (RestTimerState) -> Boolean) {
        restTimers.value.values.filter(predicate).forEach { cancelRestTimer(it.routineExerciseId) }
    }

    // ---------- Navigation memory ----------

    fun rememberLastWorkout(routineId: String, dayId: String) {
        if (routineId.isBlank() || dayId.isBlank()) return
        val route = LastWorkoutRoute(routineId, dayId)
        if (lastWorkoutRoute.value == route) return
        navigationPrefs.edit {
            putString(LAST_ROUTINE_ID_KEY, routineId); putString(
            LAST_DAY_ID_KEY,
            dayId
        )
        }
        lastWorkoutRoute.value = route
    }

    private fun loadLastWorkoutRoute(): LastWorkoutRoute? {
        val r = navigationPrefs.getString(LAST_ROUTINE_ID_KEY, null)?.takeIf { it.isNotBlank() }
            ?: return null
        val d = navigationPrefs.getString(LAST_DAY_ID_KEY, null)?.takeIf { it.isNotBlank() }
            ?: return null
        return LastWorkoutRoute(r, d)
    }

    // ---------- Picker ----------

    fun onSearchChange(value: String) {
        searchQuery.value = value
    }

    fun onCategorySelected(category: ExerciseCategory) {
        selectedCategory.value = category
    }

    // ---------- Routine passthroughs ----------

    fun createRoutine(name: String) = routineRepository.createRoutine(name)
    fun updateRoutineName(routineId: String, name: String) =
        routineRepository.updateRoutineName(routineId, name)

    fun deleteRoutine(routineId: String) {
        cancelRestTimersMatching { it.routineId == routineId }
        routineRepository.deleteRoutine(routineId)
    }

    fun addDay(routineId: String): WorkoutDay? = routineRepository.addDay(routineId)
    fun updateDayName(routineId: String, dayId: String, name: String) =
        routineRepository.updateDayName(routineId, dayId, name)

    fun removeDay(routineId: String, dayId: String) {
        cancelRestTimersMatching { it.routineId == routineId && it.dayId == dayId }
        routineRepository.removeDay(routineId, dayId)
    }

    fun addBuiltInExercise(routineId: String, dayId: String, exercise: Exercise) =
        routineRepository.addBuiltInExercise(routineId, dayId, exercise)

    fun addCustomExercise(routineId: String, dayId: String, exercise: CustomExercise) =
        routineRepository.addCustomExercise(routineId, dayId, exercise)

    fun swapWithBuiltInExercise(
        routineId: String,
        dayId: String,
        routineExerciseId: String,
        exercise: Exercise
    ) =
        routineRepository.swapWithBuiltInExercise(routineId, dayId, routineExerciseId, exercise)

    fun swapWithCustomExercise(
        routineId: String,
        dayId: String,
        routineExerciseId: String,
        exercise: CustomExercise
    ) =
        routineRepository.swapWithCustomExercise(routineId, dayId, routineExerciseId, exercise)

    fun removeExercise(routineId: String, dayId: String, routineExerciseId: String) {
        cancelRestTimer(routineExerciseId)
        routineRepository.removeExercise(routineId, dayId, routineExerciseId)
    }

    fun removeExercises(routineId: String, dayId: String, routineExerciseIds: Set<String>) {
        routineExerciseIds.forEach(::cancelRestTimer)
        routineRepository.removeExercises(routineId, dayId, routineExerciseIds)
    }

    fun copyExercises(exercises: List<RoutineExercise>) {
        clipboard.value = exercises
    }

    fun clearCopiedExercises() {
        clipboard.value = emptyList()
    }

    fun pasteCopiedExercises(
        routineId: String, dayId: String, anchorExerciseId: String? = null,
        position: RoutineExercisePastePosition = RoutineExercisePastePosition.END
    ): Int {
        val copied = clipboard.value
        routineRepository.pasteExercisesToDay(routineId, dayId, copied, anchorExerciseId, position)
        clipboard.value = emptyList()
        return copied.size
    }

    fun restoreCutExercises(
        routineId: String,
        dayId: String,
        exercises: List<IndexedValue<RoutineExercise>>
    ): Int =
        routineRepository.restoreExercisesToDay(routineId, dayId, exercises)

    fun moveExercise(routineId: String, dayId: String, routineExerciseId: String, offset: Int) =
        routineRepository.moveExercise(routineId, dayId, routineExerciseId, offset)

    fun addSet(routineId: String, dayId: String, routineExerciseId: String) =
        routineRepository.addSet(routineId, dayId, routineExerciseId)

    fun removeSet(routineId: String, dayId: String, routineExerciseId: String, setId: String) =
        routineRepository.removeSet(routineId, dayId, routineExerciseId, setId)

    fun updateSet(
        routineId: String, dayId: String, routineExerciseId: String, setId: String,
        reps: String? = null, weight: String? = null, completed: Boolean? = null
    ) = routineRepository.updateSet(
        routineId,
        dayId,
        routineExerciseId,
        setId,
        reps,
        weight,
        completed
    )

    fun updateRest(routineId: String, dayId: String, routineExerciseId: String, rest: String) =
        routineRepository.updateRest(routineId, dayId, routineExerciseId, rest)

    fun updateNotes(routineId: String, dayId: String, routineExerciseId: String, notes: String) =
        routineRepository.updateNotes(routineId, dayId, routineExerciseId, notes)

    fun upsertCustomExercise(draft: CreateExerciseDraft) =
        routineRepository.upsertCustomExercise(draft)

    fun deleteCustomExercise(exerciseId: String) =
        routineRepository.deleteCustomExercise(exerciseId)

    fun exportRoutineBackup(): String = routineRepository.exportBackup()
    fun importRoutineBackup(json: String): Result<String> =
        routineRepository.importBackup(json).onSuccess {
            routineRepository.refreshBuiltInGifUrls(exerciseRepository.exercises.value)
        }

    // ---------- Settings ----------

    fun updateVibrationIntensity(intensity: VibrationIntensity) =
        settingsRepository.updateVibrationIntensity(intensity)

    fun updateVibrationPattern(pattern: VibrationPattern) =
        settingsRepository.updateVibrationPattern(pattern)

    fun updateVibrationRepeatCount(count: Int) =
        settingsRepository.updateVibrationRepeatCount(count)

    fun updateVibrateUntilConfirmed(enabled: Boolean) =
        settingsRepository.updateVibrateUntilConfirmed(enabled)

    fun previewRestCompleteVibration(
        intensity: VibrationIntensity = settingsRepository.vibrationIntensity.value,
        pattern: VibrationPattern = settingsRepository.vibrationPattern.value,
        repeatCount: Int = settingsRepository.vibrationRepeatCount.value
    ) = triggerRestCompleteVibration(app, intensity, pattern, repeatCount)

    override fun onCleared() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(processObserver)
        routineRepository.flush()
        // Activity finished (e.g. back pressed) while timers run: alarms stay scheduled, show countdowns.
        restTimers.value.values.forEach(notifier::showRunningTimer)
        super.onCleared()
    }

    private companion object {
        const val TICK_MS = 250L
        const val NAVIGATION_PREFS_NAME = "gymuu_navigation"
        const val LAST_ROUTINE_ID_KEY = "last_routine_id"
        const val LAST_DAY_ID_KEY = "last_day_id"
    }
}

data class LastWorkoutRoute(val routineId: String, val dayId: String)

data class RestTimerState(
    val routineId: String,
    val dayId: String,
    val routineExerciseId: String,
    val exerciseName: String,
    val totalSeconds: Int,
    val remainingSeconds: Int,
    val endElapsedRealtimeMillis: Long,
    val endWallClockMillis: Long
)

data class AppUiState(
    val exercises: List<Exercise> = emptyList(),
    val routines: List<Routine> = emptyList(),
    val customExercises: List<CustomExercise> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: ExerciseCategory = ExerciseCategory.ALL,
    val pickerCustomExercises: List<CustomExercise> = emptyList(),
    val pickerSections: List<Pair<ExerciseCategory, List<Exercise>>> = emptyList(),
    val copiedExercises: List<RoutineExercise> = emptyList(),
    val restTimers: Map<String, RestTimerState> = emptyMap(),
    val lastWorkoutRoute: LastWorkoutRoute? = null,
    val vibrationIntensity: VibrationIntensity = VibrationIntensity.DEFAULT,
    val vibrationPattern: VibrationPattern = VibrationPattern.DEFAULT,
    val vibrationRepeatCount: Int = VibrationRepeatOptions.DEFAULT,
    val vibrateUntilConfirmed: Boolean = false
) {
    fun routine(routineId: String): Routine? = routines.firstOrNull { it.id == routineId }
    fun day(routineId: String, dayId: String): WorkoutDay? =
        routine(routineId)?.days?.firstOrNull { it.id == dayId }
}

enum class ExerciseCategory(val label: String) {
    ALL("ALL"), CUSTOM("CUSTOM"), CHEST("CHEST"), BACK("BACK"), LEGS_GLUTES("LEGS/GLUTES"),
    BICEPS("BICEPS"), TRICEPS("TRICEPS"), SHOULDERS("SHOULDERS"), CORE("CORE");

    companion object {
        val sectionOrder = listOf(CHEST, BACK, SHOULDERS, LEGS_GLUTES, BICEPS, TRICEPS, CORE)
    }
}

private data class AppSettingsState(
    val intensity: VibrationIntensity,
    val pattern: VibrationPattern,
    val repeatCount: Int,
    val untilConfirmed: Boolean
)

private data class PickerState(
    val customs: List<CustomExercise>,
    val sections: List<Pair<ExerciseCategory, List<Exercise>>>
)

private class IndexedExercise(val exercise: Exercise) {
    val searchText: String = listOf(
        exercise.name, exercise.bodyParts.joinToString(" "), exercise.equipments.joinToString(" "),
        exercise.targetMuscles.joinToString(" "), exercise.secondaryMuscles.joinToString(" ")
    ).joinToString(" ").lowercase()
    val categories: List<ExerciseCategory> =
        ExerciseCategory.sectionOrder.filter(exercise::matchesCategory)
    val primary: ExerciseCategory = categories.firstOrNull() ?: ExerciseCategory.CORE
}

private fun buildPicker(
    index: List<IndexedExercise>,
    customs: List<CustomExercise>,
    query: String,
    category: ExerciseCategory
): PickerState {
    val q = query.trim().lowercase()
    val customMatches =
        if (category == ExerciseCategory.ALL || category == ExerciseCategory.CUSTOM) {
            customs.filter { q.isEmpty() || it.name.lowercase().contains(q) }
        } else emptyList()
    if (category == ExerciseCategory.CUSTOM) return PickerState(customMatches, emptyList())

    val matches = index.filter {
        (category == ExerciseCategory.ALL || category in it.categories) && (q.isEmpty() || it.searchText.contains(
            q
        ))
    }
    val sections = if (category != ExerciseCategory.ALL) {
        if (matches.isEmpty()) emptyList() else listOf(category to matches.map { it.exercise })
    } else {
        val grouped = matches.groupBy({ it.primary }, { it.exercise })
        ExerciseCategory.sectionOrder.mapNotNull { c -> grouped[c]?.let { c to it } }
    }
    return PickerState(customMatches, sections)
}

private fun Exercise.matchesCategory(category: ExerciseCategory): Boolean {
    val body = bodyParts.map { it.lowercase() }
    val target = targetMuscles.map { it.lowercase() }
    val secondary = secondaryMuscles.map { it.lowercase() }
    return when (category) {
        ExerciseCategory.ALL -> true
        ExerciseCategory.CUSTOM -> false
        ExerciseCategory.CHEST -> "chest" in body || "pectorals" in target || "chest" in secondary
        ExerciseCategory.BACK -> "back" in body || target.any {
            it in setOf(
                "lats",
                "upper back",
                "spine"
            )
        }

        ExerciseCategory.LEGS_GLUTES -> body.any { it in setOf("upper legs", "lower legs") } ||
                target.any { it in setOf("glutes", "quads", "hamstrings", "calves", "abductors") }

        ExerciseCategory.BICEPS -> "biceps" in target || "biceps" in secondary
        ExerciseCategory.TRICEPS -> "triceps" in target || "triceps" in secondary
        ExerciseCategory.SHOULDERS -> "shoulders" in body || "delts" in target || "shoulders" in secondary
        ExerciseCategory.CORE -> "waist" in body || "abs" in target || secondary.any {
            it in setOf(
                "core",
                "obliques"
            )
        }
    }
}
