package me.jitish.gymuu.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.jitish.gymuu.data.exercise.Exercise
import me.jitish.gymuu.data.routine.CustomExercise
import me.jitish.gymuu.data.routine.ExerciseSource
import me.jitish.gymuu.data.routine.Routine
import me.jitish.gymuu.data.routine.RoutineExercise
import me.jitish.gymuu.data.routine.WorkoutDay
import me.jitish.gymuu.data.routine.WorkoutSet
import me.jitish.gymuu.ui.components.AppIconBadge
import me.jitish.gymuu.ui.components.CategoryChip
import me.jitish.gymuu.ui.components.CreateExerciseDialog
import me.jitish.gymuu.ui.components.DividerLine
import me.jitish.gymuu.ui.components.EmptyState
import me.jitish.gymuu.ui.components.GymFab
import me.jitish.gymuu.ui.components.GymLogo
import me.jitish.gymuu.ui.components.RoutineDrawer
import me.jitish.gymuu.ui.components.RoutineRow
import me.jitish.gymuu.ui.components.SearchBox
import me.jitish.gymuu.ui.components.SectionHeading
import me.jitish.gymuu.ui.components.TopTitleBar
import me.jitish.gymuu.ui.exercise.CustomExerciseCard
import me.jitish.gymuu.ui.exercise.ExerciseListCard
import me.jitish.gymuu.ui.theme.GymBlack
import me.jitish.gymuu.ui.theme.GymuuTheme

internal object PreviewData {
    val builtInExercise = Exercise(
        exerciseId = "bench_press",
        name = "barbell bench press",
        bodyParts = listOf("chest"),
        equipments = listOf("barbell"),
        targetMuscles = listOf("pectorals"),
        secondaryMuscles = listOf("triceps", "shoulders")
    )

    val customExercise = CustomExercise(
        id = "custom_cable_fly",
        name = "Cable Fly",
        sets = 4,
        reps = "12-15",
        rest = "1:00"
    )

    val routineExercise = RoutineExercise(
        id = "routine_bench_press",
        exerciseId = builtInExercise.exerciseId,
        name = "BARBELL BENCH PRESS",
        gifUrl = null,
        sets = listOf(
            WorkoutSet(id = "set_1", setNo = 1, reps = "12", weight = "40 kg"),
            WorkoutSet(id = "set_2", setNo = 2, reps = "10", weight = "50 kg", completed = true),
            WorkoutSet(id = "set_3", setNo = 3, reps = "8", weight = "55 kg")
        ),
        rest = "2:00",
        notes = "Slow eccentric",
        source = ExerciseSource.BUILT_IN
    )

    val routine = Routine(
        id = "push_day",
        name = "Push Day",
        days = listOf(
            WorkoutDay(
                id = "day_1",
                name = "DAY 1",
                exercises = listOf(routineExercise)
            )
        )
    )

    val routines = listOf(
        routine,
        Routine(id = "pull_day", name = "Pull Day", days = listOf(WorkoutDay(id = "day_2", name = "DAY 1"))),
        Routine(id = "legs_day", name = "Leg Day", days = listOf(WorkoutDay(id = "day_3", name = "DAY 1")))
    )
}

@Composable
private fun PreviewFrame(content: @Composable () -> Unit) {
    GymuuTheme {
        Box(
            modifier = Modifier
                .background(GymBlack)
                .padding(16.dp)
        ) {
            content()
        }
    }
}

@Preview(name = "Gym Logo", showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun GymLogoPreview() {
    PreviewFrame {
        GymLogo()
    }
}

@Preview(name = "Routine Row", showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun RoutineRowPreview() {
    PreviewFrame {
        RoutineRow(
            routine = PreviewData.routine,
            onOpen = {},
            onEdit = {},
            onDelete = {}
        )
    }
}

@Preview(name = "Routine Drawer", widthDp = 360, heightDp = 640, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun RoutineDrawerPreview() {
    PreviewFrame {
        Box(modifier = Modifier.widthIn(max = 320.dp)) {
            RoutineDrawer(
                routines = PreviewData.routines,
                onManageRoutines = {},
                onRoutineClick = {}
            )
        }
    }
}

@Preview(name = "Top Title Bar", widthDp = 360, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun TopTitleBarPreview() {
    PreviewFrame {
        TopTitleBar(title = "SELECT EXERCISE", onBack = {})
    }
}

@Preview(name = "Search Box", widthDp = 360, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun SearchBoxPreview() {
    PreviewFrame {
        SearchBox(query = "bench", onQueryChange = {})
    }
}

@Preview(name = "Category Chips", widthDp = 360, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun CategoryChipsPreview() {
    PreviewFrame {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CategoryChip(label = "ALL", selected = true, onClick = {})
            CategoryChip(label = "CHEST", selected = false, onClick = {})
        }
    }
}

@Preview(name = "Create Exercise Dialog", widthDp = 360, heightDp = 640, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun CreateExerciseDialogPreview() {
    PreviewFrame {
        CreateExerciseDialog(
            initial = PreviewData.customExercise,
            onDismiss = {},
            onConfirm = {}
        )
    }
}

@Preview(name = "Exercise Card", widthDp = 360, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun ExerciseListCardPreview() {
    PreviewFrame {
        ExerciseListCard(
            exercise = PreviewData.builtInExercise,
            selected = false,
            onClick = {}
        )
    }
}

@Preview(name = "Custom Exercise Card", widthDp = 360, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun CustomExerciseCardPreview() {
    PreviewFrame {
        CustomExerciseCard(
            exercise = PreviewData.customExercise,
            selected = true,
            onClick = {},
            onEdit = {},
            onDelete = {}
        )
    }
}

@Preview(name = "Section States", widthDp = 360, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun SharedStatesPreview() {
    PreviewFrame {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionHeading("MY ROUTINES")
            DividerLine()
            EmptyState("Add exercises to build this workout")
            GymFab(onClick = {})
        }
    }
}
