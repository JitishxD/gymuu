package me.jitish.gymuu.data.routine

import androidx.annotation.Keep

@Keep
internal data class PersistedDomainState(
    val routines: List<Routine>,
    val customExercises: List<CustomExercise>
)

@Keep
internal data class ImportedMergeResult(
    val routines: List<Routine>,
    val customExercises: List<CustomExercise>,
    val routinesAdded: Int,
    val customExercisesAdded: Int
)
