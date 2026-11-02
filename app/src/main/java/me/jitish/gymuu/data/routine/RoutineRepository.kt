package me.jitish.gymuu.data.routine

import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import me.jitish.gymuu.data.exercise.Exercise
import me.jitish.gymuu.data.media.ExerciseMediaStore
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

/**
 * In-memory source of truth. Edits update the flows immediately; disk writes are
 * debounced onto IO so typing in reps/weight/notes never blocks the main thread.
 */
class RoutineRepository(context: Context, private val scope: CoroutineScope) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val persistence = RoutinePersistence(prefs, Gson(), ::newId)
    private val mediaStore = ExerciseMediaStore(context)
    private val backupArchive = RoutineBackupArchive(mediaStore)
    private val initialState = persistence.loadState()

    private val _routines = MutableStateFlow(initialState.routines)
    val routines: StateFlow<List<Routine>> = _routines

    private val _customExercises = MutableStateFlow(initialState.customExercises)
    val customExercises: StateFlow<List<CustomExercise>> = _customExercises

    private var pendingSave: Job? = null

    fun flush() {
        pendingSave?.cancel()
        pendingSave = null
        persistNow()
    }

    @Synchronized
    private fun persistNow() {
        persistence.persistState(_routines.value, _customExercises.value)
    }

    private fun scheduleSave() {
        pendingSave?.cancel()
        pendingSave = scope.launch(Dispatchers.IO) {
            delay(SAVE_DEBOUNCE_MS)
            persistNow()
        }
    }

    fun createRoutine(name: String) {
        val routine = Routine(
            newId(),
            name.trim().ifBlank { "New Routine" },
            listOf(WorkoutDay(newId(), "DAY 1"))
        )
        setRoutines(_routines.value + routine)
    }

    fun updateRoutineName(routineId: String, name: String) {
        updateRoutine(routineId) { it.copy(name = name.trim().ifBlank { it.name }) }
    }

    fun deleteRoutine(routineId: String) {
        setRoutines(_routines.value.filterNot { it.id == routineId }
            .ifEmpty { persistence.defaultRoutines() })
    }

    fun addDay(routineId: String): WorkoutDay? {
        var created: WorkoutDay? = null
        updateRoutine(routineId) { routine ->
            WorkoutDay(newId(), nextDayName(routine.days)).also { created = it }
                .let { routine.copy(days = routine.days + it) }
        }
        return created
    }

    fun updateDayName(routineId: String, dayId: String, name: String) {
        val clean = name.trim()
        if (clean.isNotBlank()) updateDay(routineId, dayId) { it.copy(name = clean) }
    }

    fun removeDay(routineId: String, dayId: String) {
        updateRoutine(routineId) { routine ->
            routine.copy(days = routine.days.filterNot { it.id == dayId }
                .ifEmpty { listOf(WorkoutDay(newId(), "DAY 1")) })
        }
    }

    fun addBuiltInExercise(routineId: String, dayId: String, exercise: Exercise) = addExerciseToDay(
        routineId, dayId,
        RoutineExercise(
            id = newId(),
            exerciseId = exercise.exerciseId,
            name = exercise.name,
            gifUrl = exercise.gifUrl,
            mediaMimeType = "image/gif",
            sets = defaultSets(3, "8-12", ::newId),
            rest = "2:00",
            source = ExerciseSource.BUILT_IN
        )
    )

    fun addCustomExercise(routineId: String, dayId: String, exercise: CustomExercise) =
        addExerciseToDay(
            routineId, dayId,
            RoutineExercise(
                id = newId(),
                exerciseId = exercise.id,
                name = exercise.name,
                gifUrl = exercise.mediaUrl,
                mediaMimeType = exercise.mediaMimeType,
                sets = defaultSets(exercise.sets, exercise.reps.ifBlank { "10" }, ::newId),
                rest = normalizeRest(exercise.rest),
                source = ExerciseSource.CUSTOM
            )
        )

    fun swapWithBuiltInExercise(
        routineId: String,
        dayId: String,
        routineExerciseId: String,
        exercise: Exercise
    ) =
        updateExercise(routineId, dayId, routineExerciseId) {
            it.copy(
                exerciseId = exercise.exerciseId, name = exercise.name, gifUrl = exercise.gifUrl,
                mediaMimeType = "image/gif", source = ExerciseSource.BUILT_IN
            )
        }

    fun swapWithCustomExercise(
        routineId: String,
        dayId: String,
        routineExerciseId: String,
        exercise: CustomExercise
    ) =
        updateExercise(routineId, dayId, routineExerciseId) {
            it.copy(
                exerciseId = exercise.id, name = exercise.name, gifUrl = exercise.mediaUrl,
                mediaMimeType = exercise.mediaMimeType, source = ExerciseSource.CUSTOM
            )
        }

    fun removeExercise(routineId: String, dayId: String, routineExerciseId: String) =
        removeExercises(routineId, dayId, setOf(routineExerciseId))

    fun removeExercises(routineId: String, dayId: String, ids: Set<String>) {
        if (ids.isEmpty()) return
        updateDay(
            routineId,
            dayId
        ) { day -> day.copy(exercises = day.exercises.filterNot { it.id in ids }) }
    }

    fun pasteExercisesToDay(
        routineId: String,
        dayId: String,
        exercises: List<RoutineExercise>,
        anchorExerciseId: String? = null,
        position: RoutineExercisePastePosition = RoutineExercisePastePosition.END
    ) {
        val copies = exercises.map { it.copyForPaste() }
        if (copies.isEmpty()) return
        updateDay(routineId, dayId) { day ->
            day.copy(
                exercises = day.exercises.toMutableList()
                    .apply { addAll(day.insertIndexFor(anchorExerciseId, position), copies) })
        }
    }

    fun restoreExercisesToDay(
        routineId: String,
        dayId: String,
        exercises: List<IndexedValue<RoutineExercise>>
    ): Int {
        if (exercises.isEmpty()) return 0
        var restored = 0
        updateDay(routineId, dayId) { day ->
            val existing = day.exercises.mapTo(HashSet()) { it.id }
            val toRestore = exercises.filter { it.value.id !in existing }.sortedBy { it.index }
            if (toRestore.isEmpty()) return@updateDay day
            val list = day.exercises.toMutableList()
            toRestore.forEach { list.add(it.index.coerceIn(0, list.size), it.value) }
            restored = toRestore.size
            day.copy(exercises = list)
        }
        return restored
    }

    fun moveExercise(routineId: String, dayId: String, routineExerciseId: String, offset: Int) {
        if (offset == 0) return
        updateDay(routineId, dayId) { day ->
            val from = day.exercises.indexOfFirst { it.id == routineExerciseId }
            if (from == -1) return@updateDay day
            val to = (from + offset).coerceIn(0, day.exercises.lastIndex)
            if (to == from) return@updateDay day
            day.copy(exercises = day.exercises.toMutableList().apply { add(to, removeAt(from)) })
        }
    }

    fun addSet(routineId: String, dayId: String, routineExerciseId: String) =
        updateExercise(routineId, dayId, routineExerciseId) { ex ->
            val last = ex.sets.lastOrNull()
            ex.copy(
                sets = ex.sets + WorkoutSet(
                    newId(),
                    ex.sets.size + 1,
                    last?.reps ?: "8-12",
                    last?.weight.orEmpty()
                )
            )
        }

    /** Removing the last set leaves zero sets (it no longer silently re-adds three). */
    fun removeSet(routineId: String, dayId: String, routineExerciseId: String, setId: String) =
        updateExercise(routineId, dayId, routineExerciseId) { ex ->
            ex.copy(sets = ex.sets.filterNot { it.id == setId }
                .mapIndexed { i, s -> s.copy(setNo = i + 1) })
        }

    fun updateSet(
        routineId: String, dayId: String, routineExerciseId: String, setId: String,
        reps: String? = null, weight: String? = null, completed: Boolean? = null
    ) = updateExercise(routineId, dayId, routineExerciseId) { ex ->
        ex.copy(sets = ex.sets.map { s ->
            if (s.id == setId) s.copy(
                reps = reps ?: s.reps,
                weight = weight ?: s.weight,
                completed = completed ?: s.completed
            ) else s
        })
    }

    fun updateRest(routineId: String, dayId: String, routineExerciseId: String, rest: String) =
        updateExercise(routineId, dayId, routineExerciseId) {
            if (it.rest == rest) it else it.copy(
                rest = rest
            )
        }

    fun updateNotes(routineId: String, dayId: String, routineExerciseId: String, notes: String) =
        updateExercise(routineId, dayId, routineExerciseId) { it.copy(notes = notes) }

    fun upsertCustomExercise(draft: CreateExerciseDraft) {
        val exercise = CustomExercise(
            id = draft.id ?: newId(), name = draft.name.trim(), sets = draft.sets.coerceIn(1, 10),
            reps = draft.reps.trim().ifBlank { "10" }, rest = normalizeRest(draft.rest),
            mediaUrl = draft.mediaUrl?.trim()?.takeIf { it.isNotBlank() },
            mediaMimeType = draft.mediaMimeType?.trim()?.takeIf { it.isNotBlank() }
        )
        if (exercise.name.isBlank()) return

        val existing = _customExercises.value
        val previous = draft.id?.let { id -> existing.firstOrNull { it.id == id } }
        val updated =
            if (previous == null) existing + exercise else existing.map { if (it.id == exercise.id) exercise else it }
        val customs = persistence.sanitizeCustomExercises(updated)
        val saved = customs.firstOrNull { it.id == exercise.id } ?: return

        _customExercises.value = customs
        if (previous != null) _routines.value = _routines.value.syncCustomExercise(previous, saved)
        scheduleSave()
    }

    fun deleteCustomExercise(exerciseId: String) {
        _customExercises.value = _customExercises.value.filterNot { it.id == exerciseId }
        scheduleSave()
    }

    /** Keeps saved routines in sync when the bundled GIF URLs change between app updates. */
    fun refreshBuiltInGifUrls(exercises: List<Exercise>) {
        if (exercises.isEmpty()) return
        val urlById = exercises.associate { it.exerciseId to it.gifUrl }
        var changed = false
        val updated = _routines.value.map { r ->
            r.copy(days = r.days.map { d ->
                d.copy(exercises = d.exercises.map { ex ->
                    val url = ex.exerciseId?.let(urlById::get)
                    if (ex.source == ExerciseSource.BUILT_IN && url != null && url != ex.gifUrl) {
                        changed = true; ex.copy(gifUrl = url)
                    } else ex
                })
            })
        }
        if (changed) {
            _routines.value = updated; scheduleSave()
        }
    }

    /** Writes a .gymuu archive: the state payload plus every custom-exercise media file. */
    fun exportBackupTo(output: OutputStream): Result<Unit> = runCatching {
        val customs = _customExercises.value
        val media = customs.mapNotNull { exercise ->
            exercise.mediaUrl?.takeIf { it.isNotBlank() }?.let { url ->
                RoutineBackupArchive.MediaFile(url, exercise.mediaMimeType) { mediaStore.open(url) }
            }
        }.distinctBy { it.reference }
        backupArchive.write(output, persistence.exportBackup(_routines.value, customs), media)
    }

    /**
     * Reads a .gymuu archive, merges it, and points every imported
     * custom exercise at the copy of its media that now lives on this device.
     */
    fun importBackupFrom(input: InputStream): Result<String> = runCatching {
        val contents = backupArchive.read(input)
            ?: error("This file doesn't contain a valid Gymuu backup.")
        val imported = persistence.parseBackup(contents.stateJson)
            ?: error("This file doesn't contain a valid Gymuu backup.")
        if (imported.routines.isEmpty()) error("The selected backup doesn't contain any routines.")

        val merged = persistence.mergeImportedState(imported, _routines.value, _customExercises.value)
        val customs = merged.customExercises.map { exercise ->
            contents.mediaUrls[exercise.mediaUrl]?.let { exercise.copy(mediaUrl = it) } ?: exercise
        }
        _routines.value = merged.routines.relinkCustomMedia(customs)
        _customExercises.value = customs
        flush()

        val routines =
            if (merged.routinesAdded == 1) "1 routine" else "${merged.routinesAdded} routines"
        val importedCustoms =
            if (merged.customExercisesAdded == 1) "1 new custom exercise" else "${merged.customExercisesAdded} new custom exercises"
        "Imported $routines and $importedCustoms. Your existing routines were kept."
    }

    /** Imported media moved into app storage, so routine rows must point at the copies too. */
    private fun List<Routine>.relinkCustomMedia(customs: List<CustomExercise>): List<Routine> {
        val byId = customs.associateBy { it.id }
        return map { routine ->
            routine.copy(days = routine.days.map { day ->
                day.copy(exercises = day.exercises.map { exercise ->
                    val custom = exercise.exerciseId?.let(byId::get)
                    if (exercise.source == ExerciseSource.CUSTOM && custom != null &&
                        (custom.mediaUrl != exercise.gifUrl ||
                                custom.mediaMimeType != exercise.mediaMimeType)
                    ) {
                        exercise.copy(gifUrl = custom.mediaUrl, mediaMimeType = custom.mediaMimeType)
                    } else {
                        exercise
                    }
                })
            })
        }
    }

    private fun addExerciseToDay(routineId: String, dayId: String, exercise: RoutineExercise) =
        updateDay(routineId, dayId) { it.copy(exercises = it.exercises + exercise) }

    private fun RoutineExercise.copyForPaste() = copy(
        id = newId(),
        sets = sets.mapIndexed { i, s -> s.copy(id = newId(), setNo = i + 1, completed = false) }
    )

    private fun WorkoutDay.insertIndexFor(
        anchorId: String?,
        position: RoutineExercisePastePosition
    ): Int {
        if (position == RoutineExercisePastePosition.END || anchorId.isNullOrBlank()) return exercises.size
        val anchor = exercises.indexOfFirst { it.id == anchorId }
        if (anchor == -1) return exercises.size
        return (if (position == RoutineExercisePastePosition.BEFORE) anchor else anchor + 1).coerceIn(
            0,
            exercises.size
        )
    }

    private fun List<Routine>.syncCustomExercise(
        previous: CustomExercise,
        updated: CustomExercise
    ) = map { r ->
        r.copy(days = r.days.map { d ->
            d.copy(exercises = d.exercises.map {
                it.syncCustomExercise(
                    previous,
                    updated
                )
            })
        })
    }

    private fun RoutineExercise.syncCustomExercise(
        previous: CustomExercise,
        updated: CustomExercise
    ): RoutineExercise {
        if (source != ExerciseSource.CUSTOM || exerciseId != updated.id) return this
        val prevReps = previous.reps.ifBlank { "10" }
        val newReps = updated.reps.ifBlank { "10" }
        val untouched = sets.size == previous.sets.coerceIn(1, 10) &&
                sets.all { it.reps == prevReps && it.weight.isBlank() && !it.completed }
        val syncedSets = if (untouched) {
            List(updated.sets.coerceIn(1, 10)) { i ->
                sets.getOrNull(i)?.copy(setNo = i + 1, reps = newReps) ?: WorkoutSet(
                    newId(),
                    i + 1,
                    newReps
                )
            }
        } else sets
        val syncedRest =
            if (rest == normalizeRest(previous.rest)) normalizeRest(updated.rest) else rest
        return copy(
            name = updated.name,
            gifUrl = updated.mediaUrl,
            mediaMimeType = updated.mediaMimeType,
            sets = syncedSets,
            rest = syncedRest
        )
    }

    private fun updateExercise(
        routineId: String,
        dayId: String,
        exerciseId: String,
        transform: (RoutineExercise) -> RoutineExercise
    ) =
        updateDay(
            routineId,
            dayId
        ) { day -> day.copy(exercises = day.exercises.map { if (it.id == exerciseId) transform(it) else it }) }

    private fun updateDay(routineId: String, dayId: String, transform: (WorkoutDay) -> WorkoutDay) =
        updateRoutine(routineId) { r -> r.copy(days = r.days.map { if (it.id == dayId) transform(it) else it }) }

    private fun updateRoutine(routineId: String, transform: (Routine) -> Routine) =
        setRoutines(_routines.value.map { if (it.id == routineId) transform(it) else it })

    private fun setRoutines(routines: List<Routine>) {
        if (routines == _routines.value) return
        _routines.value = routines
        scheduleSave()
    }

    private fun nextDayName(days: List<WorkoutDay>): String {
        val used = days.mapNotNullTo(HashSet()) {
            DAY_NAME_REGEX.matchEntire(it.name.trim())?.groupValues?.get(1)?.toIntOrNull()
        }
        return "DAY ${generateSequence(1) { it + 1 }.first { it !in used }}"
    }

    private fun newId(): String = UUID.randomUUID().toString()

    companion object {
        private const val PREFS_NAME = "gymuu_user_data"
        private const val SAVE_DEBOUNCE_MS = 400L
        private val DAY_NAME_REGEX = Regex("DAY\\s+(\\d+)", RegexOption.IGNORE_CASE)
    }
}
