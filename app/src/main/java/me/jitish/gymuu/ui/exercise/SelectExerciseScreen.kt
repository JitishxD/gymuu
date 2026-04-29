package me.jitish.gymuu.ui.exercise

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import me.jitish.gymuu.data.exercise.Exercise
import me.jitish.gymuu.data.routine.CustomExercise
import me.jitish.gymuu.ui.ExerciseCategory
import me.jitish.gymuu.ui.GymUiState
import me.jitish.gymuu.ui.GymViewModel
import me.jitish.gymuu.ui.components.CategoryChip
import me.jitish.gymuu.ui.components.CreateExerciseDialog
import me.jitish.gymuu.ui.components.EmptyState
import me.jitish.gymuu.ui.components.GymFab
import me.jitish.gymuu.ui.components.SearchBox
import me.jitish.gymuu.ui.components.SectionHeading
import me.jitish.gymuu.ui.components.TopTitleBar
import me.jitish.gymuu.ui.navigation.Routes
import me.jitish.gymuu.ui.theme.GymBlack

private sealed interface ExerciseDialogState {
    data object Create : ExerciseDialogState
    data class Edit(val exercise: CustomExercise) : ExerciseDialogState
}

@Composable
internal fun SelectExerciseScreen(
    state: GymUiState,
    viewModel: GymViewModel,
    navController: NavHostController,
    routineId: String,
    dayId: String,
    swapExerciseId: String?
) {
    var exerciseDialogState by remember { mutableStateOf<ExerciseDialogState?>(null) }
    var selectedInfoExercise by remember { mutableStateOf<Exercise?>(null) }
    var selectedCustomInfoExercise by remember { mutableStateOf<CustomExercise?>(null) }
    var visibleBuiltInCount by rememberSaveable(routineId, dayId) { mutableIntStateOf(BUILT_IN_PAGE_SIZE) }
    val navigateBackToDay = remember(navController, routineId, dayId) {
        {
            navController.navigate(Routes.workout(routineId, dayId)) {
                popUpTo(Routes.WORKOUT) { inclusive = true }
                launchSingleTop = true
            }
        }
    }
    val targetDay = state.day(routineId, dayId)
    val selectedByExerciseId = remember(targetDay?.id, targetDay?.exercises) {
        targetDay?.exercises
            .orEmpty()
            .filter { !it.exerciseId.isNullOrBlank() }
            .associateBy { it.exerciseId.orEmpty() }
    }
    val selectedIds = selectedByExerciseId.keys
    val swapTargetExercise = remember(targetDay?.id, targetDay?.exercises, swapExerciseId) {
        targetDay?.exercises?.firstOrNull { it.id == swapExerciseId }
    }
    val isSwapMode = swapTargetExercise != null

    fun closeExerciseDialog() {
        exerciseDialogState = null
    }

    BackHandler(onBack = navigateBackToDay)

    if (targetDay == null) {
        Scaffold(containerColor = GymBlack) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                TopTitleBar(title = "SELECT EXERCISE", onBack = navigateBackToDay)
                EmptyState("Workout day not found")
            }
        }
        return
    }

    val customExercises = remember(state.customExercises, state.searchQuery, state.selectedCategory) {
        state.filteredCustomExercises()
    }
    val totalBuiltInCount = remember(state.exercises, state.searchQuery, state.selectedCategory) {
        state.filteredBuiltInExercises().size
    }
    val builtInSections = remember(state.exercises, state.searchQuery, state.selectedCategory, visibleBuiltInCount) {
        state.builtInSections(limit = visibleBuiltInCount)
    }

    LaunchedEffect(state.searchQuery, state.selectedCategory) {
        visibleBuiltInCount = BUILT_IN_PAGE_SIZE
    }

    Scaffold(
        containerColor = GymBlack,
        floatingActionButton = {
            if (!isSwapMode) {
                GymFab(onClick = { exerciseDialogState = ExerciseDialogState.Create })
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                TopTitleBar(title = if (isSwapMode) "SWAP EXERCISE" else "SELECT EXERCISE", onBack = navigateBackToDay)
            }
            item {
                SearchBox(query = state.searchQuery, onQueryChange = viewModel::onSearchChange)
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ExerciseCategory.entries.toList().chunked(4).forEach { rowCategories ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            rowCategories.forEach { category ->
                                CategoryChip(
                                    label = category.label,
                                    selected = state.selectedCategory == category,
                                    onClick = { viewModel.onCategorySelected(category) }
                                )
                            }
                        }
                    }
                }
            }

            if (customExercises.isNotEmpty()) {
                item { SectionHeading("CUSTOM") }
                items(customExercises, key = { it.id }) { custom ->
                    val selected = if (isSwapMode) swapTargetExercise.exerciseId == custom.id else selectedIds.contains(custom.id)
                    CustomExerciseCard(
                        exercise = custom,
                        selected = selected,
                        onClick = {
                            if (isSwapMode) {
                                viewModel.swapWithCustomExercise(routineId, dayId, swapTargetExercise.id, custom)
                                // TODO: idk for now keep swap picker open after selection.
                                // navigateBackToDay()
                            } else {
                                if (selected) {
                                    selectedByExerciseId[custom.id]?.let {
                                        viewModel.removeExercise(routineId, dayId, it.id)
                                    }
                                } else {
                                    viewModel.addCustomExercise(routineId, dayId, custom)
                                }
                            }
                        },
                        onEdit = {
                            exerciseDialogState = ExerciseDialogState.Edit(custom)
                        },
                        onDelete = { viewModel.deleteCustomExercise(custom.id) },
                        onLongClick = { selectedCustomInfoExercise = custom }
                    )
                }
            }

            builtInSections.forEach { (category, exercises) ->
                item { SectionHeading(category.label) }
                items(exercises, key = { it.exerciseId }) { exercise ->
                    val selected = if (isSwapMode) swapTargetExercise.exerciseId == exercise.exerciseId else selectedIds.contains(exercise.exerciseId)
                    ExerciseListCard(
                        exercise = exercise,
                        selected = selected,
                        onClick = {
                            if (isSwapMode) {
                                viewModel.swapWithBuiltInExercise(routineId, dayId, swapTargetExercise.id, exercise)
                                // TODO: idk for now keep swap picker open after selection.
                                // navigateBackToDay()
                            } else {
                                if (selected) {
                                    selectedByExerciseId[exercise.exerciseId]?.let {
                                        viewModel.removeExercise(routineId, dayId, it.id)
                                    }
                                } else {
                                    viewModel.addBuiltInExercise(routineId, dayId, exercise)
                                }
                        }
                    },
                    onLongClick = {
                        selectedInfoExercise = exercise
                    }
                    )
                }
            }

            if (visibleBuiltInCount < totalBuiltInCount) {
                item {
                    Button(
                        onClick = {
                            visibleBuiltInCount = (visibleBuiltInCount + BUILT_IN_PAGE_SIZE).coerceAtMost(totalBuiltInCount)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("LOAD MORE", fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                    }
                }
            }

            if (customExercises.isEmpty() && builtInSections.isEmpty()) {
                item { EmptyState("No exercises match your search") }
            }
        }
    }

    exerciseDialogState?.let { dialogState ->
        CreateExerciseDialog(
            initial = (dialogState as? ExerciseDialogState.Edit)?.exercise,
            onDismiss = ::closeExerciseDialog,
            onConfirm = { draft ->
                viewModel.upsertCustomExercise(draft)
                closeExerciseDialog()
            }
        )
    }

    selectedInfoExercise?.let { exercise ->
        ExerciseInfoDialog(
            exercise = exercise,
            onDismiss = { selectedInfoExercise = null }
        )
    }

    selectedCustomInfoExercise?.let { exercise ->
        CustomExerciseInfoDialog(
            exercise = exercise,
            onDismiss = { selectedCustomInfoExercise = null }
        )
    }
}

private const val BUILT_IN_PAGE_SIZE = 24

