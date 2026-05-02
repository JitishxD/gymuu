package me.jitish.gymuu.data.routine

import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

internal class RoutinePersistence(
    private val prefs: SharedPreferences,
    private val gson: Gson,
    private val newId: () -> String
) {
    fun loadState(): PersistedDomainState {
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

    fun exportBackup(routines: List<Routine>, customExercises: List<CustomExercise>): String {
        return gson.toJson(
            PersistedAppState.fromDomain(
                routines = routines,
                customExercises = customExercises
            )
        )
    }

    fun importBackup(
        json: String,
        currentRoutines: List<Routine>,
        currentCustomExercises: List<CustomExercise>
    ): Result<ImportedMergeResult> = runCatching {
        val importedState = parseAppState(json)
            ?: error("This file doesn't contain a valid Gymuu routines backup.")
        if (importedState.routines.isEmpty()) {
            error("The selected backup doesn't contain any routines.")
        }

        mergeImportedState(
            importedState = importedState,
            currentRoutines = currentRoutines,
            currentCustomExercises = currentCustomExercises
        )
    }

    fun persistState(routines: List<Routine>, customExercises: List<CustomExercise>) {
        val sanitizedRoutines = sanitizeRoutines(routines)
        val sanitizedCustomExercises = sanitizeCustomExercises(customExercises)
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

    fun defaultRoutines(): List<Routine> = listOf(
        Routine(id = newId(), name = "DEMO", days = listOf(WorkoutDay(id = newId(), name = "DAY 1")))
    )

    fun sanitizeRoutines(routines: List<Routine?>?): List<Routine> {
        return routines.orEmpty().mapNotNull { routine ->
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
                            .ifEmpty { defaultSets(3, "8-12", newId) }
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
                            mediaMimeType = exercise.mediaMimeType.safeString().takeIf { it.isNotBlank() },
                            sets = sets,
                            rest = exercise.rest.safeString(),
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

    fun sanitizeCustomExercises(exercises: List<CustomExercise?>?): List<CustomExercise> {
        val usedIds = mutableSetOf<String>()
        return exercises.orEmpty().mapNotNull { exercise ->
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
                mediaUrl = exercise.mediaUrl.safeString().trim().takeIf { it.isNotBlank() },
                mediaMimeType = exercise.mediaMimeType.safeString().trim().takeIf { it.isNotBlank() },
                source = ExerciseSource.CUSTOM
            )
        }
    }

    private fun loadLegacyRoutines(): List<Routine> {
        val json = prefs.getString(KEY_ROUTINES, null) ?: return defaultRoutines()
        return runCatching {
            val type = object : TypeToken<List<Routine?>>() {}.type
            val routines = sanitizeRoutines(gson.fromJson<List<Routine?>>(json, type))
            if (routines.isLegacySeed()) defaultRoutines() else routines
        }.getOrElse { defaultRoutines() }
    }

    private fun loadLegacyCustomExercises(): List<CustomExercise> {
        val json = prefs.getString(KEY_CUSTOM_EXERCISES, null) ?: return emptyList()
        return runCatching {
            val type = object : TypeToken<List<CustomExercise?>>() {}.type
            sanitizeCustomExercises(gson.fromJson<List<CustomExercise?>>(json, type))
        }.getOrElse { emptyList() }
    }

    private fun parseAppState(json: String): PersistedDomainState? {
        return runCatching {
            gson.fromJson(json, PersistedAppState::class.java)
                ?.takeIf { it.routines != null || it.customExercises != null }
                ?.toDomainState()
        }.getOrNull()
    }

    private fun mergeImportedState(
        importedState: PersistedDomainState,
        currentRoutines: List<Routine>,
        currentCustomExercises: List<CustomExercise>
    ): ImportedMergeResult {
        val sanitizedCurrentRoutines = sanitizeRoutines(currentRoutines)
        val sanitizedCurrentCustomExercises = sanitizeCustomExercises(currentCustomExercises)
        val mergedCustomExercises = sanitizedCurrentCustomExercises.toMutableList()
        val existingBySignature = sanitizedCurrentCustomExercises.associateByTo(mutableMapOf()) { it.customSignature() }
        val customExerciseIdMap = mutableMapOf<String, String>()

        fun mergeOrReuseCustomExercise(exercise: CustomExercise): String {
            val sanitizedExercise = exercise.copy(
                name = exercise.name.trim(),
                sets = exercise.sets.coerceIn(1, 10),
                reps = exercise.reps.trim().ifBlank { "10" },
                rest = normalizeRest(exercise.rest),
                mediaUrl = exercise.mediaUrl?.trim()?.takeIf { it.isNotBlank() },
                mediaMimeType = exercise.mediaMimeType?.trim()?.takeIf { it.isNotBlank() },
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
            routines = sanitizeRoutines(sanitizedCurrentRoutines + importedRoutines),
            customExercises = sanitizeCustomExercises(mergedCustomExercises),
            routinesAdded = importedRoutines.size,
            customExercisesAdded = (mergedCustomExercises.size - sanitizedCurrentCustomExercises.size).coerceAtLeast(0)
        )
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

    private fun CustomExercise.customSignature(): String {
        return listOf(
            name.trim().lowercase(),
            sets.coerceIn(1, 10).toString(),
            reps.trim(),
            normalizeRest(rest),
            mediaUrl.orEmpty().trim(),
            mediaMimeType.orEmpty().trim().lowercase()
        ).joinToString("|")
    }

    private fun PersistedAppState.toDomainState(): PersistedDomainState {
        val parsedCustomExercises = sanitizeCustomExercises(customExercises.orEmpty().mapNotNull { it?.toCustomExercise() })
        val parsedRoutines = sanitizeRoutines(
            routines.orEmpty().mapNotNull { storedRoutine -> storedRoutine?.toRoutine() }
        ).let { if (it.isLegacySeed()) defaultRoutines() else it }

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
            .ifEmpty { defaultSets(3, "8-12", newId) }
            .mapIndexed { index, set -> set.copy(setNo = index + 1) }

        return RoutineExercise(
            id = id.orEmpty().ifBlank { newId() },
            exerciseId = exerciseId?.takeIf { it.isNotBlank() },
            name = name.orEmpty().ifBlank { "Exercise" },
            gifUrl = gifUrl?.takeIf { it.isNotBlank() },
            mediaMimeType = mediaMimeType?.takeIf { it.isNotBlank() },
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
            mediaUrl = mediaUrl?.trim()?.takeIf { it.isNotBlank() },
            mediaMimeType = mediaMimeType?.trim()?.takeIf { it.isNotBlank() },
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
            mediaUrl = gifUrl?.takeIf { it.isNotBlank() },
            mediaMimeType = mediaMimeType?.takeIf { it.isNotBlank() },
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

    companion object {
        private const val KEY_APP_STATE = "app_state"
        private const val KEY_ROUTINES = "routines"
        private const val KEY_CUSTOM_EXERCISES = "custom_exercises"
    }
}

private fun String?.toExerciseSource(default: ExerciseSource): ExerciseSource {
    return ExerciseSource.entries
        .firstOrNull { source -> source.name.equals(this, ignoreCase = true) }
        ?: default
}

private fun ExerciseSource?.safeRoutineSource(): ExerciseSource {
    return this ?: ExerciseSource.BUILT_IN
}

