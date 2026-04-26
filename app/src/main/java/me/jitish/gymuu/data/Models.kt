package me.jitish.gymuu.data

data class Exercise(
    val exerciseId: String = "",
    val name: String = "",
    val gifUrl: String = "",
    val bodyParts: List<String> = emptyList(),
    val equipments: List<String> = emptyList(),
    val targetMuscles: List<String> = emptyList(),
    val secondaryMuscles: List<String> = emptyList(),
    val instructions: List<String> = emptyList()
)

data class Routine(
    val id: String = "",
    val name: String = "",
    val days: List<WorkoutDay> = emptyList()
)

data class WorkoutDay(
    val id: String = "",
    val name: String = "DAY 1",
    val exercises: List<RoutineExercise> = emptyList()
)

data class RoutineExercise(
    val id: String = "",
    val exerciseId: String? = null,
    val name: String = "",
    val gifUrl: String? = null,
    val sets: List<WorkoutSet> = emptyList(),
    val rest: String = "2:00",
    val notes: String = "",
    val source: ExerciseSource = ExerciseSource.BUILT_IN
)

data class WorkoutSet(
    val id: String = "",
    val setNo: Int = 1,
    val reps: String = "8-12",
    val weight: String = "",
    val completed: Boolean = false
)

enum class ExerciseSource {
    BUILT_IN,
    CUSTOM
}

data class CustomExercise(
    val id: String = "",
    val name: String = "",
    val sets: Int = 3,
    val reps: String = "10",
    val rest: String = "1:30",
    val source: ExerciseSource = ExerciseSource.CUSTOM
)

data class CreateExerciseDraft(
    val id: String? = null,
    val name: String = "",
    val sets: Int = 3,
    val reps: String = "",
    val rest: String = ""
)
