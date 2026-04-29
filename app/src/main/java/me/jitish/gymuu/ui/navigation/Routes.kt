package me.jitish.gymuu.ui.navigation

import android.net.Uri

internal object Routes {
    const val START = "start"
    const val ROUTINES = "routines"
    const val WORKOUT = "workout/{routineId}/{dayId}"
    const val SELECT = "select/{routineId}/{dayId}?swapExerciseId={swapExerciseId}"

    fun workout(routineId: String, dayId: String) = "workout/${Uri.encode(routineId)}/${Uri.encode(dayId)}"
    fun select(routineId: String, dayId: String, swapExerciseId: String? = null): String {
        val base = "select/${Uri.encode(routineId)}/${Uri.encode(dayId)}"
        return swapExerciseId?.takeIf { it.isNotBlank() }
            ?.let { "$base?swapExerciseId=${Uri.encode(it)}" }
            ?: base
    }
}

