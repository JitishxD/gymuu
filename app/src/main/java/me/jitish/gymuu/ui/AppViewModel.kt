package me.jitish.gymuu.ui

import android.app.Application
import android.content.Context
import android.os.SystemClock
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.jitish.gymuu.data.exercise.Exercise
import me.jitish.gymuu.data.exercise.ExerciseRepository
import me.jitish.gymuu.data.settings.AppSettingsRepository
import me.jitish.gymuu.data.settings.VibrationIntensity
import me.jitish.gymuu.data.settings.VibrationPattern
import me.jitish.gymuu.data.settings.VibrationRepeatOptions
import me.jitish.gymuu.data.routine.CreateExerciseDraft
import me.jitish.gymuu.data.routine.CustomExercise
import me.jitish.gymuu.data.routine.Routine
import me.jitish.gymuu.data.routine.RoutineExercise
import me.jitish.gymuu.data.routine.RoutineExercisePastePosition
import me.jitish.gymuu.data.routine.RoutineRepository
import me.jitish.gymuu.data.routine.WorkoutDay
import me.jitish.gymuu.ui.exercise.RestTimerAlarmStore
import me.jitish.gymuu.ui.exercise.RestTimerNotifier
import me.jitish.gymuu.ui.exercise.formatRestCountdown
import me.jitish.gymuu.ui.exercise.parseRestTimeToSeconds
import me.jitish.gymuu.ui.exercise.triggerRestCompleteVibration

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val exerciseRepository = ExerciseRepository(application)
    private val routineRepository = RoutineRepository(application)
    private val settingsRepository = AppSettingsRepository(application)
    private val navigationPrefs = application.getSharedPreferences(NAVIGATION_PREFS_NAME, Context.MODE_PRIVATE)

    private val searchQuery = MutableStateFlow("")
    private val selectedCategory = MutableStateFlow(ExerciseCategory.ALL)
    private val routineExerciseClipboard = MutableStateFlow<List<RoutineExercise>>(emptyList())
    private val lastWorkoutRoute = MutableStateFlow(loadLastWorkoutRoute())
    private val restTimerNotifier = RestTimerNotifier(application)
    private val restTimerJobs = mutableMapOf<String, Job>()
    private val _restTimers = MutableStateFlow<Map<String, RestTimerState>>(emptyMap())
    private val appInForeground = MutableStateFlow(false)

    val uiState: StateFlow<AppUiState> = combine(
        combine(
            exerciseRepository.exercises,
            routineRepository.routines,
            routineRepository.customExercises,
            searchQuery,
            selectedCategory
        ) { exercises, routines, customExercises, search, category ->
            AppUiState(
                exercises = exercises,
                routines = routines,
                customExercises = customExercises,
                searchQuery = search,
                selectedCategory = category
            )
        },
        routineExerciseClipboard,
        _restTimers,
        lastWorkoutRoute,
        combine(
            combine(
                settingsRepository.vibrationIntensity,
                settingsRepository.vibrationPattern
            ) { vibrationIntensity, vibrationPattern ->
                vibrationIntensity to vibrationPattern
            },
            combine(
                settingsRepository.vibrationRepeatCount,
                settingsRepository.vibrateUntilConfirmed
            ) { vibrationRepeatCount, vibrateUntilConfirmed ->
                vibrationRepeatCount to vibrateUntilConfirmed
            }
        ) { vibration, repeat ->
            AppSettingsState(
                vibrationIntensity = vibration.first,
                vibrationPattern = vibration.second,
                vibrationRepeatCount = repeat.first,
                vibrateUntilConfirmed = repeat.second
            )
        }
    ) { state, copiedExercises, restTimers, savedRoute, settings ->
        state.copy(
            copiedExercises = copiedExercises,
            restTimers = restTimers,
            lastWorkoutRoute = savedRoute,
            vibrationIntensity = settings.vibrationIntensity,
            vibrationPattern = settings.vibrationPattern,
            vibrationRepeatCount = settings.vibrationRepeatCount,
            vibrateUntilConfirmed = settings.vibrateUntilConfirmed
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppUiState()
    )

    init {
        restoreScheduledRestTimers()
        viewModelScope.launch {
            exerciseRepository.loadExercises()
        }
    }

    fun onAppForegroundChanged(inForeground: Boolean) {
        RestTimerAlarmStore.setAppForeground(getApplication(), inForeground)
        if (appInForeground.value == inForeground) return

        appInForeground.value = inForeground
        val timers = _restTimers.value.values
        if (inForeground) {
            restTimerNotifier.cancelRestCompleteVibration()
            timers.forEach(restTimerNotifier::cancelRunningTimer)
        } else {
            timers.forEach(restTimerNotifier::showRunningTimer)
        }
    }

    fun rememberLastWorkout(routineId: String, dayId: String) {
        if (routineId.isBlank() || dayId.isBlank()) return

        val route = LastWorkoutRoute(routineId = routineId, dayId = dayId)
        if (lastWorkoutRoute.value == route) return

        navigationPrefs.edit {
            putString(LAST_ROUTINE_ID_KEY, route.routineId)
            putString(LAST_DAY_ID_KEY, route.dayId)
        }
        lastWorkoutRoute.value = route
    }

    fun startRestTimer(routineId: String, dayId: String, exercise: RoutineExercise) {
        val totalRestSeconds = parseRestTimeToSeconds(exercise.rest)
        if (totalRestSeconds <= 0) return

        val correctedRest = formatRestCountdown(totalRestSeconds)
        routineRepository.updateRest(routineId, dayId, exercise.id, correctedRest)

        val timer = RestTimerState(
            routineId = routineId,
            dayId = dayId,
            routineExerciseId = exercise.id,
            exerciseName = exercise.name,
            totalSeconds = totalRestSeconds,
            remainingSeconds = totalRestSeconds,
            endElapsedRealtimeMillis = SystemClock.elapsedRealtime() + totalRestSeconds * 1_000L,
            endWallClockMillis = System.currentTimeMillis() + totalRestSeconds * 1_000L
        )

        restTimerJobs.remove(exercise.id)?.cancel()
        _restTimers.update { timers -> timers + (exercise.id to timer) }
        restTimerNotifier.cancelTimerComplete(timer)
        restTimerNotifier.scheduleTimerComplete(timer)
        if (!appInForeground.value) {
            restTimerNotifier.showRunningTimer(timer)
        }

        launchRestTimerJob(timer)
    }

    private fun launchRestTimerJob(timer: RestTimerState) {
        restTimerJobs[timer.routineExerciseId] = viewModelScope.launch {
            while (true) {
                delay(1_000L)

                val currentTimer = _restTimers.value[timer.routineExerciseId] ?: return@launch
                if (RestTimerAlarmStore.loadTimer(getApplication(), timer.routineExerciseId) == null) {
                    restTimerJobs.remove(timer.routineExerciseId)
                    _restTimers.update { timers -> timers - timer.routineExerciseId }
                    restTimerNotifier.cancelRunningTimer(timer.routineExerciseId)
                    return@launch
                }

                val remaining = remainingRestSeconds(currentTimer.endElapsedRealtimeMillis)
                if (remaining <= 0) {
                    finishRestTimer(timer.routineExerciseId)
                    return@launch
                }

                val updatedTimer = currentTimer.copy(remainingSeconds = remaining)
                _restTimers.update { timers ->
                    if (timers[timer.routineExerciseId]?.endElapsedRealtimeMillis == currentTimer.endElapsedRealtimeMillis) {
                        timers + (timer.routineExerciseId to updatedTimer)
                    } else {
                        timers
                    }
                }
                if (!appInForeground.value) {
                    restTimerNotifier.showRunningTimer(updatedTimer)
                }
            }
        }
    }

    fun cancelRestTimer(routineExerciseId: String) {
        restTimerJobs.remove(routineExerciseId)?.cancel()
        val timer = _restTimers.value[routineExerciseId]
        _restTimers.update { timers -> timers - routineExerciseId }
        if (timer != null) {
            restTimerNotifier.cancelScheduledTimer(timer)
            restTimerNotifier.cancelRunningTimer(timer)
            restTimerNotifier.cancelTimerComplete(timer)
        } else {
            restTimerNotifier.cancelScheduledTimer(routineExerciseId)
            restTimerNotifier.cancelTimerNotifications(routineExerciseId)
        }
    }

    private fun finishRestTimer(routineExerciseId: String) {
        val timer = _restTimers.value[routineExerciseId] ?: return
        val alarmStillPending = RestTimerAlarmStore.loadTimer(getApplication(), routineExerciseId) != null
        restTimerJobs.remove(routineExerciseId)
        _restTimers.update { timers -> timers - routineExerciseId }
        restTimerNotifier.cancelScheduledTimer(timer)
        restTimerNotifier.cancelRunningTimer(timer)

        val completionNotificationShown = !appInForeground.value &&
            alarmStillPending &&
            restTimerNotifier.showTimerComplete(
                timer = timer,
                requiresAcknowledgement = settingsRepository.loadVibrateUntilConfirmed()
            )
        if (appInForeground.value || alarmStillPending) {
            triggerRestCompleteVibration(
                context = getApplication(),
                allowUntilConfirmed = completionNotificationShown
            )
        }
    }

    private fun cancelRestTimersMatching(predicate: (RestTimerState) -> Boolean) {
        _restTimers.value.values
            .filter(predicate)
            .map { it.routineExerciseId }
            .forEach(::cancelRestTimer)
    }

    private fun restoreScheduledRestTimers() {
        RestTimerAlarmStore.loadTimers(getApplication()).forEach { storedTimer ->
            val remaining = remainingRestSecondsFromWallClock(storedTimer.endWallClockMillis)
            if (remaining <= 0) {
                restTimerNotifier.cancelScheduledTimer(storedTimer)
                return@forEach
            }

            val restoredTimer = storedTimer.copy(
                remainingSeconds = remaining,
                endElapsedRealtimeMillis = SystemClock.elapsedRealtime() + remaining * 1_000L
            )
            _restTimers.update { timers -> timers + (restoredTimer.routineExerciseId to restoredTimer) }
            restTimerNotifier.scheduleTimerComplete(restoredTimer)
            launchRestTimerJob(restoredTimer)
        }
    }

    private fun remainingRestSeconds(endElapsedRealtimeMillis: Long): Int {
        val millisRemaining = endElapsedRealtimeMillis - SystemClock.elapsedRealtime()
        return ((millisRemaining + 999L) / 1_000L).toInt().coerceAtLeast(0)
    }

    private fun remainingRestSecondsFromWallClock(endWallClockMillis: Long): Int {
        val millisRemaining = endWallClockMillis - System.currentTimeMillis()
        return ((millisRemaining + 999L) / 1_000L).toInt().coerceAtLeast(0)
    }

    private fun loadLastWorkoutRoute(): LastWorkoutRoute? {
        val routineId = navigationPrefs.getString(LAST_ROUTINE_ID_KEY, null)?.takeIf { it.isNotBlank() }
        val dayId = navigationPrefs.getString(LAST_DAY_ID_KEY, null)?.takeIf { it.isNotBlank() }
        return if (routineId != null && dayId != null) {
            LastWorkoutRoute(routineId = routineId, dayId = dayId)
        } else {
            null
        }
    }

    fun onSearchChange(value: String) {
        searchQuery.value = value
    }

    fun onCategorySelected(category: ExerciseCategory) {
        selectedCategory.value = category
    }

    fun createRoutine(name: String) = routineRepository.createRoutine(name)
    fun updateRoutineName(routineId: String, name: String) = routineRepository.updateRoutineName(routineId, name)
    fun deleteRoutine(routineId: String) {
        cancelRestTimersMatching { it.routineId == routineId }
        routineRepository.deleteRoutine(routineId)
    }
    fun addDay(routineId: String) = routineRepository.addDay(routineId)
    fun updateDayName(routineId: String, dayId: String, name: String) = routineRepository.updateDayName(routineId, dayId, name)
    fun removeDay(routineId: String, dayId: String) {
        cancelRestTimersMatching { it.routineId == routineId && it.dayId == dayId }
        routineRepository.removeDay(routineId, dayId)
    }
    fun addBuiltInExercise(routineId: String, dayId: String, exercise: Exercise) = routineRepository.addBuiltInExercise(routineId, dayId, exercise)
    fun addCustomExercise(routineId: String, dayId: String, exercise: CustomExercise) = routineRepository.addCustomExercise(routineId, dayId, exercise)
    fun swapWithBuiltInExercise(routineId: String, dayId: String, routineExerciseId: String, exercise: Exercise) = routineRepository.swapWithBuiltInExercise(routineId, dayId, routineExerciseId, exercise)
    fun swapWithCustomExercise(routineId: String, dayId: String, routineExerciseId: String, exercise: CustomExercise) = routineRepository.swapWithCustomExercise(routineId, dayId, routineExerciseId, exercise)
    fun removeExercise(routineId: String, dayId: String, routineExerciseId: String) {
        cancelRestTimer(routineExerciseId)
        routineRepository.removeExercise(routineId, dayId, routineExerciseId)
    }
    fun removeExercises(routineId: String, dayId: String, routineExerciseIds: Set<String>) {
        routineExerciseIds.forEach(::cancelRestTimer)
        routineRepository.removeExercises(routineId, dayId, routineExerciseIds)
    }
    fun copyExercises(exercises: List<RoutineExercise>) {
        routineExerciseClipboard.value = exercises
    }
    fun clearCopiedExercises() {
        routineExerciseClipboard.value = emptyList()
    }
    fun pasteCopiedExercises(
        routineId: String,
        dayId: String,
        anchorExerciseId: String? = null,
        position: RoutineExercisePastePosition = RoutineExercisePastePosition.END
    ): Int {
        val copiedExercises = routineExerciseClipboard.value
        routineRepository.pasteExercisesToDay(
            routineId = routineId,
            dayId = dayId,
            exercises = copiedExercises,
            anchorExerciseId = anchorExerciseId,
            position = position
        )
        routineExerciseClipboard.value = emptyList()
        return copiedExercises.size
    }
    fun restoreCutExercises(routineId: String, dayId: String, exercises: List<IndexedValue<RoutineExercise>>): Int {
        return routineRepository.restoreExercisesToDay(routineId, dayId, exercises)
    }
    fun moveExercise(routineId: String, dayId: String, routineExerciseId: String, offset: Int) = routineRepository.moveExercise(routineId, dayId, routineExerciseId, offset)
    fun addSet(routineId: String, dayId: String, routineExerciseId: String) = routineRepository.addSet(routineId, dayId, routineExerciseId)
    fun removeSet(routineId: String, dayId: String, routineExerciseId: String, setId: String) = routineRepository.removeSet(routineId, dayId, routineExerciseId, setId)
    fun updateSet(routineId: String, dayId: String, routineExerciseId: String, setId: String, reps: String? = null, weight: String? = null, completed: Boolean? = null) = routineRepository.updateSet(routineId, dayId, routineExerciseId, setId, reps, weight, completed)
    fun updateRest(routineId: String, dayId: String, routineExerciseId: String, rest: String) = routineRepository.updateRest(routineId, dayId, routineExerciseId, rest)
    fun updateNotes(routineId: String, dayId: String, routineExerciseId: String, notes: String) = routineRepository.updateNotes(routineId, dayId, routineExerciseId, notes)
    fun upsertCustomExercise(draft: CreateExerciseDraft) = routineRepository.upsertCustomExercise(draft)
    fun deleteCustomExercise(exerciseId: String) = routineRepository.deleteCustomExercise(exerciseId)
    fun exportRoutineBackup(): String = routineRepository.exportBackup()
    fun importRoutineBackup(json: String): Result<String> = routineRepository.importBackup(json)
    fun updateVibrationIntensity(intensity: VibrationIntensity) = settingsRepository.updateVibrationIntensity(intensity)
    fun updateVibrationPattern(pattern: VibrationPattern) = settingsRepository.updateVibrationPattern(pattern)
    fun updateVibrationRepeatCount(count: Int) = settingsRepository.updateVibrationRepeatCount(count)
    fun updateVibrateUntilConfirmed(enabled: Boolean) = settingsRepository.updateVibrateUntilConfirmed(enabled)
    fun previewRestCompleteVibration(
        intensity: VibrationIntensity = settingsRepository.vibrationIntensity.value,
        pattern: VibrationPattern = settingsRepository.vibrationPattern.value,
        repeatCount: Int = settingsRepository.vibrationRepeatCount.value
    ) {
        triggerRestCompleteVibration(getApplication(), intensity, pattern, repeatCount)
    }

    override fun onCleared() {
        val timers = _restTimers.value.values
        restTimerJobs.values.forEach { it.cancel() }
        if (appInForeground.value) {
            timers.forEach { timer ->
                restTimerNotifier.cancelScheduledTimer(timer)
                restTimerNotifier.cancelRunningTimer(timer)
            }
        } else {
            timers.forEach(restTimerNotifier::showRunningTimer)
        }
        super.onCleared()
    }

    private companion object {
        const val NAVIGATION_PREFS_NAME = "gymuu_navigation"
        const val LAST_ROUTINE_ID_KEY = "last_routine_id"
        const val LAST_DAY_ID_KEY = "last_day_id"
    }
}

data class LastWorkoutRoute(
    val routineId: String,
    val dayId: String
)

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
    val copiedExercises: List<RoutineExercise> = emptyList(),
    val restTimers: Map<String, RestTimerState> = emptyMap(),
    val lastWorkoutRoute: LastWorkoutRoute? = null,
    val vibrationIntensity: VibrationIntensity = VibrationIntensity.DEFAULT,
    val vibrationPattern: VibrationPattern = VibrationPattern.DEFAULT,
    val vibrationRepeatCount: Int = VibrationRepeatOptions.DEFAULT,
    val vibrateUntilConfirmed: Boolean = false
) {
    fun routine(routineId: String): Routine? = routines.firstOrNull { it.id == routineId }
    fun day(routineId: String, dayId: String): WorkoutDay? = routine(routineId)?.days?.firstOrNull { it.id == dayId }

    fun filteredCustomExercises(): List<CustomExercise> {
        if (selectedCategory != ExerciseCategory.ALL && selectedCategory != ExerciseCategory.CUSTOM) return emptyList()
        val query = searchQuery.trim().lowercase()
        return customExercises.filter { query.isBlank() || it.name.lowercase().contains(query) }
    }

    fun filteredBuiltInExercises(): List<Exercise> {
        if (selectedCategory == ExerciseCategory.CUSTOM) return emptyList()
        val query = searchQuery.trim().lowercase()
        return exercises.filter { exercise ->
            val categoryMatch = selectedCategory == ExerciseCategory.ALL || exercise.matchesCategory(selectedCategory)
            val searchMatch = query.isBlank() || exercise.searchableText().contains(query)
            categoryMatch && searchMatch
        }
    }

    fun builtInSectionGroups(filteredExercises: List<Exercise> = filteredBuiltInExercises()): Map<ExerciseCategory, List<Exercise>> {
        if (selectedCategory != ExerciseCategory.ALL && selectedCategory != ExerciseCategory.CUSTOM) {
            return if (filteredExercises.isEmpty()) {
                emptyMap()
            } else {
                linkedMapOf(selectedCategory to filteredExercises)
            }
        }

        val grouped = filteredExercises
            .groupBy { exercise -> ExerciseCategory.sectionOrder.firstOrNull { exercise.matchesCategory(it) } ?: ExerciseCategory.CORE }

        return linkedMapOf<ExerciseCategory, List<Exercise>>().apply {
            ExerciseCategory.sectionOrder.forEach { category ->
                grouped[category]?.takeIf { it.isNotEmpty() }?.let { put(category, it) }
            }
        }
    }

    fun visibleBuiltInSections(sectionGroups: Map<ExerciseCategory, List<Exercise>>, limit: Int = Int.MAX_VALUE): Map<ExerciseCategory, List<Exercise>> {
        var remaining = limit.coerceAtLeast(0)
        if (remaining == 0) return emptyMap()

        val visibleSections = linkedMapOf<ExerciseCategory, List<Exercise>>()
        sectionGroups.forEach { (category, exercises) ->
            if (remaining == 0) return@forEach

            val visibleExercises = exercises.take(remaining)
            if (visibleExercises.isNotEmpty()) {
                visibleSections[category] = visibleExercises
                remaining -= visibleExercises.size
            }
        }

        return visibleSections
    }

    fun builtInSections(limit: Int = Int.MAX_VALUE): Map<ExerciseCategory, List<Exercise>> {
        return visibleBuiltInSections(builtInSectionGroups(), limit)
    }
}

private data class AppSettingsState(
    val vibrationIntensity: VibrationIntensity,
    val vibrationPattern: VibrationPattern,
    val vibrationRepeatCount: Int,
    val vibrateUntilConfirmed: Boolean
)

enum class ExerciseCategory(val label: String) {
    ALL("ALL"),
    CUSTOM("CUSTOM"),
    CHEST("CHEST"),
    BACK("BACK"),
    LEGS_GLUTES("LEGS/GLUTES"),
    BICEPS("BICEPS"),
    TRICEPS("TRICEPS"),
    SHOULDERS("SHOULDERS"),
    CORE("CORE");

    companion object {
        val sectionOrder = listOf(CHEST, BACK, SHOULDERS, LEGS_GLUTES, BICEPS, TRICEPS, CORE)
    }
}

private fun Exercise.searchableText(): String {
    return listOf(name, bodyParts.joinToString(" "), equipments.joinToString(" "), targetMuscles.joinToString(" "), secondaryMuscles.joinToString(" "))
        .joinToString(" ")
        .lowercase()
}

private fun Exercise.matchesCategory(category: ExerciseCategory): Boolean {
    val body = bodyParts.map { it.lowercase() }
    val target = targetMuscles.map { it.lowercase() }
    val secondary = secondaryMuscles.map { it.lowercase() }

    // Category filtering follows the requested body-part and muscle mapping from the exercise database.
    return when (category) {
        ExerciseCategory.ALL -> true
        ExerciseCategory.CUSTOM -> false
        ExerciseCategory.CHEST -> body.contains("chest") || target.contains("pectorals") || secondary.contains("chest")
        ExerciseCategory.BACK -> body.contains("back") || target.any { it in listOf("lats", "upper back", "spine") }
        ExerciseCategory.LEGS_GLUTES -> body.any { it in listOf("upper legs", "lower legs") } || target.any { it in listOf("glutes", "quads", "hamstrings", "calves", "abductors") }
        ExerciseCategory.BICEPS -> target.contains("biceps") || secondary.contains("biceps")
        ExerciseCategory.TRICEPS -> target.contains("triceps") || secondary.contains("triceps")
        ExerciseCategory.SHOULDERS -> body.contains("shoulders") || target.contains("delts") || secondary.contains("shoulders")
        ExerciseCategory.CORE -> body.contains("waist") || target.contains("abs") || secondary.any { it in listOf("core", "obliques") }
    }
}
