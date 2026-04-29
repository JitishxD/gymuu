package me.jitish.gymuu.ui.exercise

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.jitish.gymuu.data.exercise.Exercise
import me.jitish.gymuu.data.routine.CustomExercise
import me.jitish.gymuu.data.routine.RoutineExercise
import me.jitish.gymuu.ui.components.toTitleCase
import me.jitish.gymuu.ui.theme.GymCard
import me.jitish.gymuu.ui.theme.GymMuted

@Composable
internal fun ExerciseInfoDialog(exercise: Exercise, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GymCard,
        title = {
            SelectionContainer {
                Text(exercise.name.toTitleCase(), color = Color.White, fontSize = 22.sp)
            }
        },
        text = {
            SelectionContainer {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ExerciseInfoLine(label = "Body Parts", value = exercise.bodyParts.joinToString(", "))
                    ExerciseInfoLine(label = "Equipment", value = exercise.equipments.joinToString(", "))
                    ExerciseInfoLine(label = "Target Muscles", value = exercise.targetMuscles.joinToString(", "))
                    ExerciseInfoLine(label = "Secondary", value = exercise.secondaryMuscles.joinToString(", "))
                    Text("INSTRUCTIONS", color = GymMuted, fontSize = 13.sp, letterSpacing = 1.sp)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (exercise.instructions.isEmpty()) {
                            Text("No instructions available.", color = GymMuted, fontSize = 14.sp)
                        } else {
                            exercise.instructions.forEachIndexed { index, step ->
                                Text("${index + 1}. $step", color = Color.White, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE", color = Color.White)
            }
        }
    )
}

@Composable
internal fun CustomExerciseInfoDialog(exercise: CustomExercise, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GymCard,
        title = {
            SelectionContainer {
                Text(exercise.name, color = Color.White, fontSize = 22.sp)
            }
        },
        text = {
            SelectionContainer {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ExerciseInfoLine(label = "Sets", value = exercise.sets.toString())
                    ExerciseInfoLine(label = "Reps", value = exercise.reps)
                    ExerciseInfoLine(label = "Rest", value = exercise.rest)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE", color = Color.White)
            }
        }
    )
}

@Composable
internal fun RoutineExerciseInfoDialog(exercise: RoutineExercise, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GymCard,
        title = {
            SelectionContainer {
                Text(exercise.name.toTitleCase(), color = Color.White, fontSize = 22.sp)
            }
        },
        text = {
            SelectionContainer {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ExerciseInfoLine(label = "Sets", value = exercise.sets.size.toString())
                    ExerciseInfoLine(label = "Reps", value = exercise.repsSummary())
                    ExerciseInfoLine(label = "Rest", value = exercise.rest)
                    ExerciseInfoLine(label = "Notes", value = exercise.notes)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE", color = Color.White)
            }
        }
    )
}

@Composable
private fun ExerciseInfoLine(label: String, value: String) {
    val text = value.ifBlank { "-" }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label.uppercase(), color = GymMuted, fontSize = 13.sp, letterSpacing = 1.sp)
        Text(text, color = Color.White, fontSize = 14.sp)
    }
}

private fun RoutineExercise.repsSummary(): String {
    val reps = sets.map { it.reps.trim() }.filter { it.isNotBlank() }.distinct()
    return when (reps.size) {
        0 -> "-"
        1 -> reps.first()
        else -> reps.joinToString(", ")
    }
}

