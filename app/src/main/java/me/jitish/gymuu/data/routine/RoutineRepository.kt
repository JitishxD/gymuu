package me.jitish.gymuu.data.routine

import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import me.jitish.gymuu.data.exercise.Exercise
import java.util.UUID

class RoutineRepository(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    private val persistence = RoutinePersistence(prefs = prefs, gson = gson, newId = ::newId)
    private val initialState = persistence.loadState()

    private val _routines = MutableStateFlow(initialState.routines)
    val routines: StateFlow<List<Routine>> = _routines

    private val _customExercises = MutableStateFlow(initialState.customExercises)
    val customExercises: StateFlow<List<CustomExercise>> = _customExercises

    fun createRoutine(name: String) {
        val cleanName = name.trim().ifBlank { "New Routine" }
        val routine = Routine(
            id = newId(),
            name = cleanName,
            days = listOf(WorkoutDay(id = newId(), name = "DAY 1"))
        )
        updateRoutines(_routines.value + routine)
    }

    fun updateRoutineName(routineId: String, name: String) {
        updateRoutines(
            _routines.value.map { routine ->
                if (routine.id == routineId) routine.copy(name = name.trim().ifBlank { routine.name }) else routine
            }
        )
    }

    fun deleteRoutine(routineId: String) {
        updateRoutines(_routines.value.filterNot { it.id == routineId }.ifEmpty { persistence.defaultRoutines() })
    }

    fun addDay(routineId: String): WorkoutDay? {
        var createdDay: WorkoutDay? = null
        updateRoutine(routineId) { routine ->
            val currentDays = routine.days
            val newDay = WorkoutDay(id = newId(), name = nextDayName(currentDays))
            createdDay = newDay
            routine.copy(days = currentDays + newDay)
        }
        return createdDay
    }

    fun updateDayName(routineId: String, dayId: String, name: String) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return
        updateDay(routineId, dayId) { day -> day.copy(name = cleanName) }
    }

    fun removeDay(routineId: String, dayId: String) {
        updateRoutine(routineId) { routine ->
            val remaining = routine.days.filterNot { it.id == dayId }
            val safeDays = if (remaining.isEmpty()) listOf(WorkoutDay(id = newId(), name = "DAY 1")) else remaining
            routine.copy(days = safeDays)
        }
    }

    fun addBuiltInExercise(routineId: String, dayId: String, exercise: Exercise) {
        val routineExercise = RoutineExercise(
            id = newId(),
            exerciseId = exercise.exerciseId,
            name = exercise.name,
            gifUrl = exercise.gifUrl,
            sets = defaultSets(count = 3, reps = "8-12", newId = ::newId),
            rest = "2:00",
            source = ExerciseSource.BUILT_IN
        )
        addExerciseToDay(routineId, dayId, routineExercise)
    }

    fun addCustomExercise(routineId: String, dayId: String, exercise: CustomExercise) {
        val reps = exercise.reps.ifBlank { "10" }
        val routineExercise = RoutineExercise(
            id = newId(),
            exerciseId = exercise.id,
            name = exercise.name,
            gifUrl = null,
            sets = defaultSets(count = exercise.sets, reps = reps, newId = ::newId),
            rest = normalizeRest(exercise.rest),
            source = ExerciseSource.CUSTOM
        )
        addExerciseToDay(routineId, dayId, routineExercise)
    }

    fun swapWithBuiltInExercise(routineId: String, dayId: String, routineExerciseId: String, exercise: Exercise) {
        updateExercise(routineId, dayId, routineExerciseId) { current ->
            current.copy(
                exerciseId = exercise.exerciseId,
                name = exercise.name,
                gifUrl = exercise.gifUrl,
                source = ExerciseSource.BUILT_IN
            )
        }
    }

    fun swapWithCustomExercise(routineId: String, dayId: String, routineExerciseId: String, exercise: CustomExercise) {
        updateExercise(routineId, dayId, routineExerciseId) { current ->
            current.copy(
                exerciseId = exercise.id,
                name = exercise.name,
                gifUrl = null,
                source = ExerciseSource.CUSTOM
            )
        }
    }

    fun removeExercise(routineId: String, dayId: String, routineExerciseId: String) {
        updateDay(routineId, dayId) { day ->
            day.copy(exercises = day.exercises.filterNot { it.id == routineExerciseId })
        }
    }

    fun removeExercises(routineId: String, dayId: String, routineExerciseIds: Set<String>) {
        if (routineExerciseIds.isEmpty()) return

        updateDay(routineId, dayId) { day ->
            day.copy(exercises = day.exercises.filterNot { it.id in routineExerciseIds })
        }
    }

    fun pasteExercisesToDay(
        routineId: String,
        dayId: String,
        exercises: List<RoutineExercise>,
        anchorExerciseId: String? = null,
        position: RoutineExercisePastePosition = RoutineExercisePastePosition.END
    ) {
        val copiedExercises = exercises.map { it.copyForPaste() }
        if (copiedExercises.isEmpty()) return

        updateDay(routineId, dayId) { day ->
            val insertIndex = day.insertIndexFor(anchorExerciseId, position)
            val updatedExercises = day.exercises.toMutableList().apply {
                addAll(insertIndex, copiedExercises)
            }
            day.copy(exercises = updatedExercises)
        }
    }

    fun restoreExercisesToDay(
        routineId: String,
        dayId: String,
        exercises: List<IndexedValue<RoutineExercise>>
    ): Int {
        if (exercises.isEmpty()) return 0

        var restoredCount = 0
        updateDay(routineId, dayId) { day ->
            val existingExerciseIds = day.exercises.map { it.id }.toSet()
            val exercisesToRestore = exercises
                .filter { it.value.id !in existingExerciseIds }
                .sortedBy { it.index }

            if (exercisesToRestore.isEmpty()) return@updateDay day

            val updatedExercises = day.exercises.toMutableList()
            exercisesToRestore.forEach { exercise ->
                updatedExercises.add(exercise.index.coerceIn(0, updatedExercises.size), exercise.value)
            }
            restoredCount = exercisesToRestore.size
            day.copy(exercises = updatedExercises)
        }
        return restoredCount
    }

    fun moveExercise(routineId: String, dayId: String, routineExerciseId: String, offset: Int) {
        if (offset == 0) return

        updateDay(routineId, dayId) { day ->
            val currentIndex = day.exercises.indexOfFirst { it.id == routineExerciseId }
            if (currentIndex == -1) return@updateDay day

            val targetIndex = (currentIndex + offset).coerceIn(0, day.exercises.lastIndex)
            if (targetIndex == currentIndex) return@updateDay day

            val reorderedExercises = day.exercises.toMutableList()
            val movedExercise = reorderedExercises.removeAt(currentIndex)
            reorderedExercises.add(targetIndex, movedExercise)
            day.copy(exercises = reorderedExercises)
        }
    }

    fun addSet(routineId: String, dayId: String, routineExerciseId: String) {
        updateExercise(routineId, dayId, routineExerciseId) { exercise ->
            val currentSets = exercise.sets
            val nextSet = WorkoutSet(
                id = newId(),
                setNo = currentSets.size + 1,
                reps = currentSets.lastOrNull()?.reps ?: "8-12",
                weight = currentSets.lastOrNull()?.weight.orEmpty()
            )
            exercise.copy(sets = currentSets + nextSet)
        }
    }

    fun removeSet(routineId: String, dayId: String, routineExerciseId: String, setId: String) {
        updateExercise(routineId, dayId, routineExerciseId) { exercise ->
            val remaining = exercise.sets.filterNot { it.id == setId }
            exercise.copy(sets = remaining.mapIndexed { index, set -> set.copy(setNo = index + 1) })
        }
    }

    fun updateSet(
        routineId: String,
        dayId: String,
        routineExerciseId: String,
        setId: String,
        reps: String? = null,
        weight: String? = null,
        completed: Boolean? = null
    ) {
        updateExercise(routineId, dayId, routineExerciseId) { exercise ->
            exercise.copy(
                sets = exercise.sets.map { set ->
                    if (set.id == setId) {
                        set.copy(
                            reps = reps ?: set.reps,
                            weight = weight ?: set.weight,
                            completed = completed ?: set.completed
                        )
                    } else {
                        set
                    }
                }
            )
        }
    }

    fun updateRest(routineId: String, dayId: String, routineExerciseId: String, rest: String) {
        updateExercise(routineId, dayId, routineExerciseId) { it.copy(rest = rest) }
    }

    fun updateNotes(routineId: String, dayId: String, routineExerciseId: String, notes: String) {
        updateExercise(routineId, dayId, routineExerciseId) { it.copy(notes = notes) }
    }

    fun upsertCustomExercise(draft: CreateExerciseDraft) {
        val exercise = CustomExercise(
            id = draft.id ?: newId(),
            name = draft.name.trim(),
            sets = draft.sets.coerceIn(1, 10),
            reps = draft.reps.trim().ifBlank { "10" },
            rest = normalizeRest(draft.rest)
        )
        if (exercise.name.isBlank()) return

        val existing = _customExercises.value
        val updated = if (draft.id == null) {
            existing + exercise
        } else {
            existing.map { if (it.id == draft.id) exercise else it }
        }
        updateCustomExercises(updated)
    }

    fun deleteCustomExercise(exerciseId: String) {
        updateCustomExercises(_customExercises.value.filterNot { it.id == exerciseId })
    }

    fun exportBackup(): String {
        return persistence.exportBackup(
            routines = _routines.value,
            customExercises = _customExercises.value
        )
    }

    fun importBackup(json: String): Result<String> {
        return persistence.importBackup(
            json = json,
            currentRoutines = _routines.value,
            currentCustomExercises = _customExercises.value
        ).map { mergedState ->
            _routines.value = mergedState.routines
            _customExercises.value = mergedState.customExercises
            persistence.persistState(mergedState.routines, mergedState.customExercises)

            val routineLabel = if (mergedState.routinesAdded == 1) "1 routine" else "${mergedState.routinesAdded} routines"
            val customExerciseLabel = if (mergedState.customExercisesAdded == 1) {
                "1 new custom exercise"
            } else {
                "${mergedState.customExercisesAdded} new custom exercises"
            }
            "Imported $routineLabel and $customExerciseLabel. Your existing routines were kept."
        }
    }

    private fun addExerciseToDay(routineId: String, dayId: String, exercise: RoutineExercise) {
        updateDay(routineId, dayId) { day ->
            day.copy(exercises = day.exercises + exercise)
        }
    }

    private fun RoutineExercise.copyForPaste(): RoutineExercise {
        val copiedSets = sets
            .ifEmpty { defaultSets(3, "8-12", ::newId) }
            .mapIndexed { index, set ->
                set.copy(
                    id = newId(),
                    setNo = index + 1,
                    completed = false
                )
            }

        return copy(
            id = newId(),
            sets = copiedSets
        )
    }

    private fun WorkoutDay.insertIndexFor(anchorExerciseId: String?, position: RoutineExercisePastePosition): Int {
        if (position == RoutineExercisePastePosition.END || anchorExerciseId.isNullOrBlank()) {
            return exercises.size
        }

        val anchorIndex = exercises.indexOfFirst { it.id == anchorExerciseId }
        if (anchorIndex == -1) return exercises.size

        return when (position) {
            RoutineExercisePastePosition.BEFORE -> anchorIndex
            RoutineExercisePastePosition.AFTER -> anchorIndex + 1
        }.coerceIn(0, exercises.size)
    }

    private fun updateExercise(routineId: String, dayId: String, routineExerciseId: String, transform: (RoutineExercise) -> RoutineExercise) {
        updateDay(routineId, dayId) { day ->
            day.copy(
                exercises = day.exercises.map { exercise ->
                    if (exercise.id == routineExerciseId) transform(exercise) else exercise
                }
            )
        }
    }

    private fun updateDay(routineId: String, dayId: String, transform: (WorkoutDay) -> WorkoutDay) {
        updateRoutine(routineId) { routine ->
            routine.copy(days = routine.days.map { day -> if (day.id == dayId) transform(day) else day })
        }
    }

    private fun updateRoutine(routineId: String, transform: (Routine) -> Routine) {
        updateRoutines(_routines.value.map { routine -> if (routine.id == routineId) transform(routine) else routine })
    }

    private fun updateRoutines(routines: List<Routine>) {
        val sanitized = persistence.sanitizeRoutines(routines)
        _routines.value = sanitized
        persistence.persistState(routines = sanitized, customExercises = _customExercises.value)
    }

    private fun updateCustomExercises(exercises: List<CustomExercise>) {
        val sanitized = persistence.sanitizeCustomExercises(exercises)
        _customExercises.value = sanitized
        persistence.persistState(routines = _routines.value, customExercises = sanitized)
    }

    private fun nextDayName(days: List<WorkoutDay>): String {
        val used = days.mapNotNull { day ->
            DAY_NAME_REGEX.matchEntire(day.name.safeString().trim())?.groupValues?.getOrNull(1)?.toIntOrNull()
        }.toSet()
        val next = generateSequence(1) { it + 1 }.first { it !in used }
        return "DAY $next"
    }

    private fun newId(): String = UUID.randomUUID().toString()

    companion object {
        private const val PREFS_NAME = "gymuu_user_data"
        private val DAY_NAME_REGEX = Regex("DAY\\s+(\\d+)", RegexOption.IGNORE_CASE)
    }
}

