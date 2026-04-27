package me.jitish.gymuu.data

import android.content.Context
import androidx.annotation.Keep
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant
import java.util.UUID

private const val STORAGE_VERSION = 1

class RoutineRepository(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    private val initialState = loadState()

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

    fun exportBackup(): String {
        return gson.toJson(
            PersistedAppState.fromDomain(
                routines = _routines.value,
                customExercises = _customExercises.value
            )
        )
    }

    fun importBackup(json: String): Result<String> = runCatching {
        val importedState = parseAppState(json)
            ?: error("This file doesn't contain a valid Gymuu routines backup.")
        if (importedState.routines.isEmpty()) {
            error("The selected backup doesn't contain any routines.")
        }

        val mergedState = mergeImportedState(importedState)
        _routines.value = mergedState.routines
        _customExercises.value = mergedState.customExercises
        persistState(mergedState.routines, mergedState.customExercises)

        val routineLabel = if (mergedState.routinesAdded == 1) "1 routine" else "${mergedState.routinesAdded} routines"
        val customExerciseLabel = if (mergedState.customExercisesAdded == 1) {
            "1 new custom exercise"
        } else {
            "${mergedState.customExercisesAdded} new custom exercises"
        }
        "Imported $routineLabel and $customExerciseLabel. Your existing routines were kept."
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
        persistState(routines = sanitized, customExercises = _customExercises.value)
    }

    private fun updateCustomExercises(exercises: List<CustomExercise>) {
        val sanitized = exercises.sanitizeCustomExercises()
        _customExercises.value = sanitized
        persistState(routines = _routines.value, customExercises = sanitized)
    }

    private fun loadState(): PersistedDomainState {
        val stateJson = prefs.getString(KEY_APP_STATE, null)
        if (!stateJson.isNullOrBlank()) {
            parseAppState(stateJson)?.let { return it }
        }

        val legacyState = PersistedDomainState(
            routines = loadLegacyRoutines(),
            customExercises = loadLegacyCustomExercises()
        )
        persistState(legacyState.routines, legacyState.customExercises)
        return legacyState
    }

    private fun loadLegacyRoutines(): List<Routine> {
        val json = prefs.getString(KEY_ROUTINES, null) ?: return defaultRoutines()
        return runCatching {
            val type = object : TypeToken<List<Routine?>>() {}.type
            val routines = gson.fromJson<List<Routine?>>(json, type).sanitize()
            if (routines.isLegacySeed()) defaultRoutines() else routines
        }.getOrElse { defaultRoutines() }
    }

    private fun loadLegacyCustomExercises(): List<CustomExercise> {
        val json = prefs.getString(KEY_CUSTOM_EXERCISES, null) ?: return emptyList()
        return runCatching {
            val type = object : TypeToken<List<CustomExercise?>>() {}.type
            gson.fromJson<List<CustomExercise?>>(json, type).sanitizeCustomExercises()
        }.getOrElse { emptyList() }
    }

    private fun persistState(routines: List<Routine>, customExercises: List<CustomExercise>) {
        val sanitizedRoutines = routines.sanitize()
        val sanitizedCustomExercises = customExercises.sanitizeCustomExercises()
        val appState = PersistedAppState.fromDomain(
            routines = sanitizedRoutines,
            customExercises = sanitizedCustomExercises
        )

        // Use a synchronous commit so edits still land even if the app is closed immediately after changes.
        prefs.edit(commit = true) {
            putString(KEY_APP_STATE, gson.toJson(appState))
            putString(KEY_ROUTINES, gson.toJson(sanitizedRoutines))
            putString(KEY_CUSTOM_EXERCISES, gson.toJson(sanitizedCustomExercises))
        }
    }

    private fun parseAppState(json: String): PersistedDomainState? {
        return runCatching {
            gson.fromJson(json, PersistedAppState::class.java)
                ?.takeIf { it.routines != null || it.customExercises != null }
                ?.toDomainState()
        }.getOrNull()
    }

    private fun mergeImportedState(importedState: PersistedDomainState): ImportedMergeResult {
        val currentRoutines = _routines.value.sanitize()
        val currentCustomExercises = _customExercises.value.sanitizeCustomExercises()
        val mergedCustomExercises = currentCustomExercises.toMutableList()
        val existingBySignature = currentCustomExercises.associateByTo(mutableMapOf()) { it.customSignature() }
        val customExerciseIdMap = mutableMapOf<String, String>()

        fun mergeOrReuseCustomExercise(exercise: CustomExercise): String {
            val sanitizedExercise = exercise.copy(
                name = exercise.name.trim(),
                sets = exercise.sets.coerceIn(1, 10),
                reps = exercise.reps.trim().ifBlank { "10" },
                rest = normalizeRest(exercise.rest),
                source = ExerciseSource.CUSTOM
            )
            val signature = sanitizedExercise.customSignature()
            existingBySignature[signature]?.let { return it.id }

            val importedCopy = sanitizedExercise.copy(id = newId())
            mergedCustomExercises += importedCopy
            existingBySignature[signature] = importedCopy
            return importedCopy.id
        }

        importedState.customExercises.forEach { customExercise ->
            customExerciseIdMap[customExercise.id] = mergeOrReuseCustomExercise(customExercise)
        }

        importedState.routines.forEach { routine ->
            routine.days.forEach { day ->
                day.exercises.forEach { exercise ->
                    val importedCustomId = exercise.exerciseId
                    if (exercise.source == ExerciseSource.CUSTOM && !importedCustomId.isNullOrBlank() && importedCustomId !in customExerciseIdMap) {
                        customExerciseIdMap[importedCustomId] = mergeOrReuseCustomExercise(
                            exercise.toFallbackCustomExercise(importedCustomId)
                        )
                    }
                }
            }
        }

        val importedRoutines = importedState.routines.map { routine ->
            routine.rekeyForImport(customExerciseIdMap)
        }

        return ImportedMergeResult(
            routines = (currentRoutines + importedRoutines).sanitize(),
            customExercises = mergedCustomExercises.sanitizeCustomExercises(),
            routinesAdded = importedRoutines.size,
            customExercisesAdded = (mergedCustomExercises.size - currentCustomExercises.size).coerceAtLeast(0)
        )
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
                                    id = set.id.safeString().ifBlank { newId() },
                                    setNo = setIndex + 1,
                                    reps = set.reps.safeString().ifBlank { "8-12" },
                                    weight = set.weight.safeString(),
                                    completed = set.completed
                                )
                            }
                        exercise.copy(
                            id = exercise.id.safeString().ifBlank { newId() },
                            exerciseId = exercise.exerciseId,
                            name = exercise.name.safeString().ifBlank { "Exercise" },
                            gifUrl = exercise.gifUrl,
                            sets = sets,
                            rest = exercise.rest.safeString().ifBlank { "2:00" },
                            notes = exercise.notes.safeString(),
                            source = exercise.source.safeRoutineSource()
                        )
                    }
                    day.copy(
                        id = day.id.safeString().ifBlank { newId() },
                        name = day.name.safeString().ifBlank { "DAY ${dayIndex + 1}" },
                        exercises = exercises
                    )
                }

            routine.copy(
                id = routine.id.safeString().ifBlank { newId() },
                name = routine.name.safeString().ifBlank { "New Routine" },
                days = if (days.isEmpty()) listOf(WorkoutDay(id = newId(), name = "DAY 1")) else days
            )
        }.ifEmpty { defaultRoutines() }
    }

    private fun List<Routine>.isLegacySeed(): Boolean {
        if (size != 2) return false
        val names = map { it.name.safeString() }.toSet()
        val oldSeedNames = setOf("Push Day", "Legs & Core")
        val hasUserData = any { routine ->
            routine.days.any { day -> day.exercises.isNotEmpty() } ||
                routine.name.safeString() !in oldSeedNames
        }
        return names == oldSeedNames && !hasUserData
    }

    private fun List<CustomExercise?>?.sanitizeCustomExercises(): List<CustomExercise> {
        val usedIds = mutableSetOf<String>()
        return orEmpty().mapNotNull { exercise ->
            if (exercise == null) return@mapNotNull null
            val name = exercise.name.safeString().trim()
            if (name.isBlank()) return@mapNotNull null

            val preferredId = exercise.id.safeString().trim()
            val id = if (preferredId.isNotBlank() && usedIds.add(preferredId)) preferredId else newId().also { usedIds.add(it) }
            exercise.copy(
                id = id,
                name = name,
                sets = exercise.sets.coerceIn(1, 10),
                reps = exercise.reps.safeString().ifBlank { "10" },
                rest = normalizeRest(exercise.rest.safeString()),
                source = ExerciseSource.CUSTOM
            )
        }
    }

    private fun nextDayName(days: List<WorkoutDay>): String {
        val used = days.mapNotNull { day ->
            DAY_NAME_REGEX.matchEntire(day.name.safeString().trim())?.groupValues?.getOrNull(1)?.toIntOrNull()
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
        private const val KEY_APP_STATE = "app_state"
        private const val KEY_ROUTINES = "routines"
        private const val KEY_CUSTOM_EXERCISES = "custom_exercises"
        private val DAY_NAME_REGEX = Regex("DAY\\s+(\\d+)", RegexOption.IGNORE_CASE)
    }

    private fun ExerciseSource?.safeRoutineSource(): ExerciseSource {
        return this ?: ExerciseSource.BUILT_IN
    }

    private fun CustomExercise.customSignature(): String {
        return listOf(
            name.trim().lowercase(),
            sets.coerceIn(1, 10).toString(),
            reps.trim(),
            normalizeRest(rest)
        ).joinToString("|")
    }

    private fun PersistedAppState.toDomainState(): PersistedDomainState {
        val parsedCustomExercises = customExercises.orEmpty().mapNotNull { it?.toCustomExercise() }.sanitizeCustomExercises()
        val parsedRoutines = routines.orEmpty()
            .mapNotNull { storedRoutine -> storedRoutine?.toRoutine() }
            .sanitize()
            .let { if (it.isLegacySeed()) defaultRoutines() else it }

        return PersistedDomainState(
            routines = parsedRoutines,
            customExercises = parsedCustomExercises
        )
    }

    private fun StoredRoutine.toRoutine(): Routine {
        val parsedDays = days.orEmpty()
            .mapNotNull { it?.toWorkoutDay() }
            .ifEmpty { listOf(WorkoutDay(id = newId(), name = "DAY 1")) }

        return Routine(
            id = id.orEmpty().ifBlank { newId() },
            name = name.orEmpty().ifBlank { "New Routine" },
            days = parsedDays
        )
    }

    private fun StoredWorkoutDay.toWorkoutDay(): WorkoutDay {
        return WorkoutDay(
            id = id.orEmpty().ifBlank { newId() },
            name = name.orEmpty().ifBlank { "DAY 1" },
            exercises = exercises.orEmpty().mapNotNull { it?.toRoutineExercise() }
        )
    }

    private fun StoredRoutineExercise.toRoutineExercise(): RoutineExercise {
        val parsedSets = sets.orEmpty()
            .mapNotNull { it?.toWorkoutSet() }
            .ifEmpty { defaultSets(3, "8-12") }
            .mapIndexed { index, set -> set.copy(setNo = index + 1) }

        return RoutineExercise(
            id = id.orEmpty().ifBlank { newId() },
            exerciseId = exerciseId?.takeIf { it.isNotBlank() },
            name = name.orEmpty().ifBlank { "Exercise" },
            gifUrl = gifUrl?.takeIf { it.isNotBlank() },
            sets = parsedSets,
            rest = normalizeRest(rest.orEmpty().ifBlank { "2:00" }),
            notes = notes.orEmpty(),
            source = source.toExerciseSource(default = ExerciseSource.BUILT_IN)
        )
    }

    private fun StoredWorkoutSet.toWorkoutSet(): WorkoutSet {
        return WorkoutSet(
            id = id.orEmpty().ifBlank { newId() },
            setNo = setNo?.takeIf { it > 0 } ?: 1,
            reps = reps.orEmpty().ifBlank { "8-12" },
            weight = weight.orEmpty(),
            completed = completed ?: false
        )
    }

    private fun StoredCustomExercise.toCustomExercise(): CustomExercise {
        return CustomExercise(
            id = id.orEmpty().ifBlank { newId() },
            name = name.orEmpty().trim(),
            sets = (sets ?: 3).coerceIn(1, 10),
            reps = reps.orEmpty().ifBlank { "10" },
            rest = normalizeRest(rest.orEmpty()),
            source = ExerciseSource.CUSTOM
        )
    }

    private fun RoutineExercise.toFallbackCustomExercise(preferredId: String): CustomExercise {
        val repsValue = sets.map { it.reps.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .singleOrNull()
            ?: sets.firstOrNull()?.reps.orEmpty().ifBlank { "10" }

        return CustomExercise(
            id = preferredId,
            name = name.trim().ifBlank { "Custom Exercise" },
            sets = sets.size.coerceIn(1, 10),
            reps = repsValue,
            rest = normalizeRest(rest),
            source = ExerciseSource.CUSTOM
        )
    }

    private fun Routine.rekeyForImport(customExerciseIdMap: Map<String, String>): Routine {
        return copy(
            id = newId(),
            days = days.mapIndexed { dayIndex, day ->
                day.copy(
                    id = newId(),
                    name = day.name.ifBlank { "DAY ${dayIndex + 1}" },
                    exercises = day.exercises.map { exercise ->
                        val remappedExerciseId = if (exercise.source == ExerciseSource.CUSTOM) {
                            exercise.exerciseId?.let { customExerciseIdMap[it] ?: it }
                        } else {
                            exercise.exerciseId
                        }

                        exercise.copy(
                            id = newId(),
                            exerciseId = remappedExerciseId,
                            sets = exercise.sets.mapIndexed { setIndex, set ->
                                set.copy(
                                    id = newId(),
                                    setNo = setIndex + 1
                                )
                            }
                        )
                    }
                )
            }
        )
    }
}

@Keep
private data class PersistedDomainState(
    val routines: List<Routine>,
    val customExercises: List<CustomExercise>
)

@Keep
private data class ImportedMergeResult(
    val routines: List<Routine>,
    val customExercises: List<CustomExercise>,
    val routinesAdded: Int,
    val customExercisesAdded: Int
)

@Keep
private data class PersistedAppState(
    @SerializedName("version") val version: Int,
    @SerializedName("exportedAt") val exportedAt: String,
    @SerializedName("routines") val routines: List<StoredRoutine?>? = null,
    @SerializedName("customExercises") val customExercises: List<StoredCustomExercise?>? = null
) {
    companion object {
        fun fromDomain(routines: List<Routine>, customExercises: List<CustomExercise>): PersistedAppState {
            return PersistedAppState(
                version = STORAGE_VERSION,
                exportedAt = Instant.now().toString(),
                routines = routines.map(StoredRoutine::fromDomain),
                customExercises = customExercises.map(StoredCustomExercise::fromDomain)
            )
        }
    }
}

@Keep
private data class StoredRoutine(
    @SerializedName("id") val id: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("days") val days: List<StoredWorkoutDay?>? = null
) {
    companion object {
        fun fromDomain(routine: Routine): StoredRoutine {
            return StoredRoutine(
                id = routine.id,
                name = routine.name,
                days = routine.days.map(StoredWorkoutDay::fromDomain)
            )
        }
    }
}

@Keep
private data class StoredWorkoutDay(
    @SerializedName("id") val id: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("exercises") val exercises: List<StoredRoutineExercise?>? = null
) {
    companion object {
        fun fromDomain(day: WorkoutDay): StoredWorkoutDay {
            return StoredWorkoutDay(
                id = day.id,
                name = day.name,
                exercises = day.exercises.map(StoredRoutineExercise::fromDomain)
            )
        }
    }
}

@Keep
private data class StoredRoutineExercise(
    @SerializedName("id") val id: String? = null,
    @SerializedName("exerciseId") val exerciseId: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("gifUrl") val gifUrl: String? = null,
    @SerializedName("sets") val sets: List<StoredWorkoutSet?>? = null,
    @SerializedName("rest") val rest: String? = null,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("source") val source: String? = null
) {
    companion object {
        fun fromDomain(exercise: RoutineExercise): StoredRoutineExercise {
            return StoredRoutineExercise(
                id = exercise.id,
                exerciseId = exercise.exerciseId,
                name = exercise.name,
                gifUrl = exercise.gifUrl,
                sets = exercise.sets.map(StoredWorkoutSet::fromDomain),
                rest = exercise.rest,
                notes = exercise.notes,
                source = exercise.source.name
            )
        }
    }
}

@Keep
private data class StoredWorkoutSet(
    @SerializedName("id") val id: String? = null,
    @SerializedName("setNo") val setNo: Int? = null,
    @SerializedName("reps") val reps: String? = null,
    @SerializedName("weight") val weight: String? = null,
    @SerializedName("completed") val completed: Boolean? = null
) {
    companion object {
        fun fromDomain(set: WorkoutSet): StoredWorkoutSet {
            return StoredWorkoutSet(
                id = set.id,
                setNo = set.setNo,
                reps = set.reps,
                weight = set.weight,
                completed = set.completed
            )
        }
    }
}

@Keep
private data class StoredCustomExercise(
    @SerializedName("id") val id: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("sets") val sets: Int? = null,
    @SerializedName("reps") val reps: String? = null,
    @SerializedName("rest") val rest: String? = null
) {
    companion object {
        fun fromDomain(exercise: CustomExercise): StoredCustomExercise {
            return StoredCustomExercise(
                id = exercise.id,
                name = exercise.name,
                sets = exercise.sets,
                reps = exercise.reps,
                rest = exercise.rest
            )
        }
    }
}

private fun String?.toExerciseSource(default: ExerciseSource): ExerciseSource {
    return ExerciseSource.entries
        .firstOrNull { source -> source.name.equals(this, ignoreCase = true) }
        ?: default
}

private fun Any?.safeString(): String = this as? String ?: ""
