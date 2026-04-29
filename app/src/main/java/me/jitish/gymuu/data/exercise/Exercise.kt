package me.jitish.gymuu.data.exercise

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
