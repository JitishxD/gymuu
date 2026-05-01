package me.jitish.gymuu.ui.routine

import me.jitish.gymuu.data.routine.RoutineExercise
import me.jitish.gymuu.data.routine.WorkoutDay

internal data class PasteAnchor(
    val routineId: String,
    val dayId: String,
    val exerciseId: String,
    val exerciseName: String
)

internal data class PendingExerciseCut(
    val routineId: String,
    val dayId: String,
    val exercises: List<IndexedValue<RoutineExercise>>
)

internal fun selectedRoutineExercises(day: WorkoutDay?, selectedExerciseIds: Collection<String>): List<RoutineExercise> {
    val selectedIds = selectedExerciseIds.toSet()
    return day?.exercises.orEmpty().filter { it.id in selectedIds }
}

internal fun updatedExerciseSelection(current: List<String>, exerciseId: String, selected: Boolean): List<String> {
    return if (selected) {
        (current + exerciseId).distinct()
    } else {
        current - exerciseId
    }
}

internal fun selectAllOrClearExerciseIds(day: WorkoutDay, selectedCount: Int): List<String> {
    return if (selectedCount == day.exercises.size) {
        emptyList()
    } else {
        day.exercises.map { it.id }
    }
}

