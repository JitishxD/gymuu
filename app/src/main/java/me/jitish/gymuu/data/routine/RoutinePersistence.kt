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
        prefs.getString(KEY_APP_STATE, null)?.takeIf { it.isNotBlank() }?.let(::parseAppState)
            ?.let { return it }
        val legacy = PersistedDomainState(loadLegacyRoutines(), loadLegacyCustomExercises())
        persistState(legacy.routines, legacy.customExercises)
        return legacy
    }

    fun exportBackup(routines: List<Routine>, customExercises: List<CustomExercise>): String =
        gson.toJson(PersistedAppState.fromDomain(routines, customExercises))

    /** Parses a backup payload without touching the current state. */
    fun parseBackup(json: String): PersistedDomainState? = parseAppState(json)

    /** Writes only the versioned state; legacy keys are removed after migration. Safe to call off the main thread. */
    fun persistState(routines: List<Routine>, customExercises: List<CustomExercise>) {
        val json = gson.toJson(PersistedAppState.fromDomain(routines, customExercises))
        prefs.edit(commit = true) {
            putString(KEY_APP_STATE, json)
            remove(KEY_ROUTINES)
            remove(KEY_CUSTOM_EXERCISES)
        }
    }

    fun defaultRoutines(): List<Routine> =
        listOf(
            Routine(
                id = newId(),
                name = "DEMO",
                days = listOf(WorkoutDay(id = newId(), name = "DAY 1"))
            )
        )

    /** Repairs untrusted data (legacy Gson / imports). Empty set lists are preserved. */
    fun sanitizeRoutines(routines: List<Routine?>?): List<Routine> =
        routines.orEmpty().mapNotNull { routine ->
            routine ?: return@mapNotNull null
            @Suppress("UNCHECKED_CAST")
            val days = (routine.days as? List<WorkoutDay?>).orEmpty().filterNotNull()
                .mapIndexed { dayIndex, day ->
                    @Suppress("UNCHECKED_CAST")
                    val exercises =
                        (day.exercises as? List<RoutineExercise?>).orEmpty().filterNotNull()
                            .map { ex ->
                                @Suppress("UNCHECKED_CAST")
                                val sets = (ex.sets as? List<WorkoutSet?>).orEmpty().filterNotNull()
                                    .mapIndexed { i, s ->
                                        s.copy(
                                            id = s.id.safeString().ifBlank { newId() },
                                            setNo = i + 1,
                                            reps = s.reps.safeString(),
                                            weight = s.weight.safeString()
                                        )
                                    }
                                ex.copy(
                                    id = ex.id.safeString().ifBlank { newId() },
                                    name = ex.name.safeString().ifBlank { "Exercise" },
                                    mediaMimeType = ex.mediaMimeType.safeString()
                                        .takeIf { it.isNotBlank() },
                                    sets = sets,
                                    rest = ex.rest.safeString(),
                                    notes = ex.notes.safeString(),
                                    source = (ex.source as ExerciseSource?)
                                        ?: ExerciseSource.BUILT_IN
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
                days = days.ifEmpty { listOf(WorkoutDay(id = newId(), name = "DAY 1")) }
            )
        }.ifEmpty { defaultRoutines() }

    fun sanitizeCustomExercises(exercises: List<CustomExercise?>?): List<CustomExercise> {
        val usedIds = HashSet<String>()
        return exercises.orEmpty().mapNotNull { ex ->
            ex ?: return@mapNotNull null
            val name = ex.name.safeString().trim()
            if (name.isBlank()) return@mapNotNull null
            val preferred = ex.id.safeString().trim()
            val id =
                if (preferred.isNotBlank() && usedIds.add(preferred)) preferred else newId().also {
                    usedIds.add(it)
                }
            ex.copy(
                id = id,
                name = name,
                sets = ex.sets.coerceIn(1, 10),
                reps = ex.reps.safeString().ifBlank { "10" },
                rest = normalizeRest(ex.rest.safeString()),
                mediaUrl = ex.mediaUrl.safeString().trim().takeIf { it.isNotBlank() },
                mediaMimeType = ex.mediaMimeType.safeString().trim().takeIf { it.isNotBlank() },
                source = ExerciseSource.CUSTOM
            )
        }
    }

    private fun loadLegacyRoutines(): List<Routine> {
        val json = prefs.getString(KEY_ROUTINES, null) ?: return defaultRoutines()
        return runCatching {
            val type = object : TypeToken<List<Routine?>>() {}.type
            sanitizeRoutines(
                gson.fromJson<List<Routine?>>(
                    json,
                    type
                )
            ).let { if (it.isLegacySeed()) defaultRoutines() else it }
        }.getOrElse { defaultRoutines() }
    }

    private fun loadLegacyCustomExercises(): List<CustomExercise> {
        val json = prefs.getString(KEY_CUSTOM_EXERCISES, null) ?: return emptyList()
        return runCatching {
            val type = object : TypeToken<List<CustomExercise?>>() {}.type
            sanitizeCustomExercises(gson.fromJson<List<CustomExercise?>>(json, type))
        }.getOrElse { emptyList() }
    }

    private fun parseAppState(json: String): PersistedDomainState? = runCatching {
        gson.fromJson(json, PersistedAppState::class.java)
            ?.takeIf { it.routines != null || it.customExercises != null }
            ?.toDomainState()
    }.getOrNull()

    fun mergeImportedState(
        imported: PersistedDomainState,
        currentRoutines: List<Routine>,
        currentCustoms: List<CustomExercise>
    ): ImportedMergeResult {
        val customs = sanitizeCustomExercises(currentCustoms)
        val merged = customs.toMutableList()
        val bySignature = customs.associateByTo(HashMap()) { it.customSignature() }
        val idMap = HashMap<String, String>()

        fun mergeOrReuse(exercise: CustomExercise): String {
            val clean = exercise.copy(
                name = exercise.name.trim(), sets = exercise.sets.coerceIn(1, 10),
                reps = exercise.reps.trim().ifBlank { "10" }, rest = normalizeRest(exercise.rest),
                mediaUrl = exercise.mediaUrl?.trim()?.takeIf { it.isNotBlank() },
                mediaMimeType = exercise.mediaMimeType?.trim()?.takeIf { it.isNotBlank() },
                source = ExerciseSource.CUSTOM
            )
            val sig = clean.customSignature()
            bySignature[sig]?.let { return it.id }
            val copy = clean.copy(id = newId())
            merged += copy
            bySignature[sig] = copy
            return copy.id
        }

        imported.customExercises.forEach { idMap[it.id] = mergeOrReuse(it) }
        imported.routines.forEach { r ->
            r.days.forEach { d ->
                d.exercises.forEach { ex ->
                    val cid = ex.exerciseId
                    if (ex.source == ExerciseSource.CUSTOM && !cid.isNullOrBlank() && cid !in idMap) {
                        idMap[cid] = mergeOrReuse(ex.toFallbackCustomExercise(cid))
                    }
                }
            }
        }

        val importedRoutines = imported.routines.map { it.rekeyForImport(idMap) }
        return ImportedMergeResult(
            routines = sanitizeRoutines(currentRoutines + importedRoutines),
            customExercises = sanitizeCustomExercises(merged),
            routinesAdded = importedRoutines.size,
            customExercisesAdded = (merged.size - customs.size).coerceAtLeast(0)
        )
    }

    private fun List<Routine>.isLegacySeed(): Boolean {
        val seed = setOf("Push Day", "Legs & Core")
        return size == 2 && map { it.name }.toSet() == seed && none { r -> r.days.any { it.exercises.isNotEmpty() } }
    }

    private fun CustomExercise.customSignature() = listOf(
        name.trim().lowercase(), sets.coerceIn(1, 10).toString(), reps.trim(), normalizeRest(rest),
        mediaUrl.orEmpty().trim(), mediaMimeType.orEmpty().trim().lowercase()
    ).joinToString("|")

    private fun PersistedAppState.toDomainState(): PersistedDomainState {
        val customs =
            sanitizeCustomExercises(customExercises.orEmpty().mapNotNull { it?.toCustomExercise() })
        val parsed = sanitizeRoutines(routines.orEmpty().mapNotNull { it?.toRoutine() })
            .let { if (it.isLegacySeed()) defaultRoutines() else it }
        return PersistedDomainState(parsed, customs)
    }

    private fun StoredRoutine.toRoutine() = Routine(
        id = id.orEmpty().ifBlank { newId() },
        name = name.orEmpty().ifBlank { "New Routine" },
        days = days.orEmpty().mapNotNull { it?.toWorkoutDay() }
            .ifEmpty { listOf(WorkoutDay(id = newId(), name = "DAY 1")) }
    )

    private fun StoredWorkoutDay.toWorkoutDay() = WorkoutDay(
        id = id.orEmpty().ifBlank { newId() },
        name = name.orEmpty().ifBlank { "DAY 1" },
        exercises = exercises.orEmpty().mapNotNull { it?.toRoutineExercise() }
    )

    private fun StoredRoutineExercise.toRoutineExercise() = RoutineExercise(
        id = id.orEmpty().ifBlank { newId() },
        exerciseId = exerciseId?.takeIf { it.isNotBlank() },
        name = name.orEmpty().ifBlank { "Exercise" },
        gifUrl = gifUrl?.takeIf { it.isNotBlank() },
        mediaMimeType = mediaMimeType?.takeIf { it.isNotBlank() },
        sets = sets.orEmpty().mapNotNull { it?.toWorkoutSet() }
            .mapIndexed { i, s -> s.copy(setNo = i + 1) },
        rest = rest.orEmpty(),
        notes = notes.orEmpty(),
        source = ExerciseSource.entries.firstOrNull { it.name.equals(source, ignoreCase = true) }
            ?: ExerciseSource.BUILT_IN
    )

    private fun StoredWorkoutSet.toWorkoutSet() = WorkoutSet(
        id = id.orEmpty().ifBlank { newId() }, setNo = setNo?.takeIf { it > 0 } ?: 1,
        reps = reps.orEmpty(), weight = weight.orEmpty(), completed = completed ?: false
    )

    private fun StoredCustomExercise.toCustomExercise() = CustomExercise(
        id = id.orEmpty().ifBlank { newId() },
        name = name.orEmpty().trim(),
        sets = (sets ?: 3).coerceIn(1, 10),
        reps = reps.orEmpty().ifBlank { "10" },
        rest = normalizeRest(rest.orEmpty()),
        mediaUrl = mediaUrl?.trim()?.takeIf { it.isNotBlank() },
        mediaMimeType = mediaMimeType?.trim()?.takeIf { it.isNotBlank() },
        source = ExerciseSource.CUSTOM
    )

    private fun RoutineExercise.toFallbackCustomExercise(preferredId: String): CustomExercise {
        val reps = sets.map { it.reps.trim() }.filter { it.isNotBlank() }.distinct().singleOrNull()
            ?: sets.firstOrNull()?.reps.orEmpty().ifBlank { "10" }
        return CustomExercise(
            id = preferredId,
            name = name.trim().ifBlank { "Custom Exercise" },
            sets = sets.size.coerceIn(1, 10),
            reps = reps,
            rest = normalizeRest(rest),
            mediaUrl = gifUrl?.takeIf { it.isNotBlank() },
            mediaMimeType = mediaMimeType?.takeIf { it.isNotBlank() },
            source = ExerciseSource.CUSTOM
        )
    }

    private fun Routine.rekeyForImport(idMap: Map<String, String>) = copy(
        id = newId(),
        days = days.mapIndexed { di, day ->
            day.copy(
                id = newId(),
                name = day.name.ifBlank { "DAY ${di + 1}" },
                exercises = day.exercises.map { ex ->
                    ex.copy(
                        id = newId(),
                        exerciseId = if (ex.source == ExerciseSource.CUSTOM) ex.exerciseId?.let {
                            idMap[it] ?: it
                        } else ex.exerciseId,
                        sets = ex.sets.mapIndexed { si, s -> s.copy(id = newId(), setNo = si + 1) }
                    )
                })
        }
    )

    companion object {
        private const val KEY_APP_STATE = "app_state"
        private const val KEY_ROUTINES = "routines"
        private const val KEY_CUSTOM_EXERCISES = "custom_exercises"
    }
}
