package me.jitish.gymuu.data

import android.content.Context
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

class RoutineRepository(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _routines = MutableStateFlow(loadRoutines())
    val routines: StateFlow<List<Routine>> = _routines

    private val _customExercises = MutableStateFlow(loadCustomExercises())
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
        updateRoutines(_routines.value.filterNot { it.id == routineId }.ifEmpty { defaultRoutines() })
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
            sets = defaultSets(count = 3, reps = "8-12"),
            rest = "2:00",
            source = ExerciseSource.BUILT_IN
        )
        addExerciseToDay(routineId, dayId, routineExercise)
    }

    fun addCustomExercise(routineId: String, dayId: String, exercise: CustomExercise) {
        val reps = exercise.reps.ifBlank { "10" }
        val rest = normalizeRest(exercise.rest)
        val routineExercise = RoutineExercise(
            id = newId(),
            exerciseId = exercise.id,
            name = exercise.name,
            gifUrl = null,
            sets = defaultSets(count = exercise.sets, reps = reps),
            rest = rest,
            source = ExerciseSource.CUSTOM
        )
        addExerciseToDay(routineId, dayId, routineExercise)
    }

    fun removeExercise(routineId: String, dayId: String, routineExerciseId: String) {
        updateDay(routineId, dayId) { day ->
            day.copy(exercises = day.exercises.filterNot { it.id == routineExerciseId })
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

    fun updateSet(routineId: String, dayId: String, routineExerciseId: String, setId: String, reps: String? = null, weight: String? = null, completed: Boolean? = null) {
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

    private fun addExerciseToDay(routineId: String, dayId: String, exercise: RoutineExercise) {
        updateDay(routineId, dayId) { day ->
            day.copy(exercises = day.exercises + exercise)
        }
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
        val sanitized = routines.sanitize()
        _routines.value = sanitized
        // Local user data persistence is centralized here so routines survive app restarts.
        prefs.edit { putString(KEY_ROUTINES, gson.toJson(sanitized)) }
    }

    private fun updateCustomExercises(exercises: List<CustomExercise>) {
        val sanitized = exercises.sanitizeCustomExercises()
        _customExercises.value = sanitized
        prefs.edit { putString(KEY_CUSTOM_EXERCISES, gson.toJson(sanitized)) }
    }

    private fun loadRoutines(): List<Routine> {
        val json = prefs.getString(KEY_ROUTINES, null) ?: return defaultRoutines()
        return runCatching {
            val type = object : TypeToken<List<Routine?>>() {}.type
            val routines = gson.fromJson<List<Routine?>>(json, type).sanitize()
            if (routines.isLegacySeed()) defaultRoutines() else routines
        }.getOrElse { defaultRoutines() }
    }

    private fun loadCustomExercises(): List<CustomExercise> {
        val json = prefs.getString(KEY_CUSTOM_EXERCISES, null) ?: return emptyList()
        return runCatching {
            val type = object : TypeToken<List<CustomExercise?>>() {}.type
            gson.fromJson<List<CustomExercise?>>(json, type).sanitizeCustomExercises()
        }.getOrElse { emptyList() }
    }

    private fun defaultRoutines(): List<Routine> = listOf(
        Routine(id = newId(), name = "DEMO", days = listOf(WorkoutDay(id = newId(), name = "DAY 1")))
    )

    private fun defaultSets(count: Int, reps: String): List<WorkoutSet> {
        return List(count.coerceIn(1, 10)) { index ->
            WorkoutSet(id = newId(), setNo = index + 1, reps = reps.ifBlank { "10" })
        }
    }

    private fun List<Routine?>?.sanitize(): List<Routine> {
        return orEmpty().mapNotNull { routine ->
            if (routine == null) return@mapNotNull null

            @Suppress("UNCHECKED_CAST")
            val rawDays = (routine.days as? List<WorkoutDay?>).orEmpty()
            val days = rawDays
                .ifEmpty { listOf(WorkoutDay(id = newId(), name = "DAY 1")) }
                .mapNotNull { it }
                .mapIndexed { dayIndex, day ->
                    @Suppress("UNCHECKED_CAST")
                    val rawExercises = (day.exercises as? List<RoutineExercise?>).orEmpty()
                    val exercises = rawExercises.mapNotNull { exercise ->
                        if (exercise == null) return@mapNotNull null

                        @Suppress("UNCHECKED_CAST")
                        val rawSets = (exercise.sets as? List<WorkoutSet?>).orEmpty()
                        val sets = rawSets
                            .ifEmpty { defaultSets(3, "8-12") }
                            .mapNotNull { it }
                            .mapIndexed { setIndex, set ->
                                set.copy(
                                    id = set.id.ifBlank { newId() },
                                    setNo = setIndex + 1,
                                    reps = set.reps.ifBlank { "8-12" },
                                    weight = set.weight
                                )
                            }
                        exercise.copy(
                            id = exercise.id.ifBlank { newId() },
                            exerciseId = exercise.exerciseId,
                            name = exercise.name.ifBlank { "Exercise" },
                            gifUrl = exercise.gifUrl,
                            sets = sets,
                            rest = exercise.rest.ifBlank { "2:00" },
                            notes = exercise.notes,
                            source = exercise.source
                        )
                    }
                    day.copy(
                        id = day.id.ifBlank { newId() },
                        name = day.name.ifBlank { "DAY ${dayIndex + 1}" },
                        exercises = exercises
                    )
                }

            routine.copy(
                id = routine.id.ifBlank { newId() },
                name = routine.name.ifBlank { "New Routine" },
                days = if (days.isEmpty()) listOf(WorkoutDay(id = newId(), name = "DAY 1")) else days
            )
        }.ifEmpty { defaultRoutines() }
    }

    private fun List<Routine>.isLegacySeed(): Boolean {
        if (size != 2) return false
        val names = map { it.name }.toSet()
        val oldSeedNames = setOf("Push Day", "Legs & Core")
        val hasUserData = any { routine ->
            routine.days.any { day -> day.exercises.isNotEmpty() } ||
                routine.name !in oldSeedNames
        }
        return names == oldSeedNames && !hasUserData
    }

    private fun List<CustomExercise?>?.sanitizeCustomExercises(): List<CustomExercise> {
        val usedIds = mutableSetOf<String>()
        return orEmpty().mapNotNull { exercise ->
            if (exercise == null) return@mapNotNull null
            val name = exercise.name.trim()
            if (name.isBlank()) return@mapNotNull null

            val preferredId = exercise.id.trim()
            val id = if (preferredId.isNotBlank() && usedIds.add(preferredId)) preferredId else newId().also { usedIds.add(it) }
            exercise.copy(
                id = id,
                name = name,
                sets = exercise.sets.coerceIn(1, 10),
                reps = exercise.reps.ifBlank { "10" },
                rest = normalizeRest(exercise.rest),
                source = ExerciseSource.CUSTOM
            )
        }
    }

    private fun nextDayName(days: List<WorkoutDay>): String {
        val used = days.mapNotNull { day ->
            DAY_NAME_REGEX.matchEntire(day.name.trim())?.groupValues?.getOrNull(1)?.toIntOrNull()
        }.toSet()
        val next = generateSequence(1) { it + 1 }.first { it !in used }
        return "DAY $next"
    }

    private fun normalizeRest(rest: String): String {
        val clean = rest.trim()
        if (clean.isBlank()) return "1:30"
        return if (clean.contains(":")) clean else "$clean:00"
    }

    private fun newId(): String = UUID.randomUUID().toString()

    companion object {
        private const val PREFS_NAME = "gymuu_user_data"
        private const val KEY_ROUTINES = "routines"
        private const val KEY_CUSTOM_EXERCISES = "custom_exercises"
        private val DAY_NAME_REGEX = Regex("DAY\\s+(\\d+)", RegexOption.IGNORE_CASE)
    }
}
