package me.jitish.gymuu.ui.exercise

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import me.jitish.gymuu.data.exercise.Exercise
import me.jitish.gymuu.data.routine.CustomExercise
import me.jitish.gymuu.ui.AppUiState
import me.jitish.gymuu.ui.AppViewModel
import me.jitish.gymuu.ui.ExerciseCategory
import me.jitish.gymuu.ui.components.CategoryChip
import me.jitish.gymuu.ui.components.CreateExerciseDialog
import me.jitish.gymuu.ui.components.EmptyState
import me.jitish.gymuu.ui.components.GymuuFab
import me.jitish.gymuu.ui.components.SearchBox
import me.jitish.gymuu.ui.components.SectionHeading
import me.jitish.gymuu.ui.components.TopTitleBar
import me.jitish.gymuu.ui.navigation.Routes
import me.jitish.gymuu.ui.theme.GymuuBlack

private sealed interface ExerciseDialogState {
    data object Create : ExerciseDialogState
    data class Edit(val exercise: CustomExercise) : ExerciseDialogState
}

@Composable
internal fun SelectExerciseScreen(
    state: AppUiState,
    viewModel: AppViewModel,
    navController: NavHostController,
    routineId: String,
    dayId: String,
    swapExerciseId: String?
) {
    var dialogState by remember { mutableStateOf<ExerciseDialogState?>(null) }
    var infoExercise by remember { mutableStateOf<Exercise?>(null) }
    var customInfoExercise by remember { mutableStateOf<CustomExercise?>(null) }
    val listState = rememberLazyListState()

    val goBack: () -> Unit = {
        if (!navController.popBackStack()) {
            navController.navigate(Routes.workout(routineId, dayId)) { launchSingleTop = true }
        }
    }
    BackHandler(onBack = goBack)

    val targetDay = state.day(routineId, dayId)
    if (targetDay == null) {
        Scaffold(containerColor = GymuuBlack) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                TopTitleBar(title = "SELECT EXERCISE", onBack = goBack)
                EmptyState("Workout day not found")
            }
        }
        return
    }

    val routineExerciseByLibraryId = remember(targetDay.exercises) {
        targetDay.exercises.filter { !it.exerciseId.isNullOrBlank() }
            .associateBy { it.exerciseId!! }
    }
    val swapTarget = remember(
        targetDay.exercises,
        swapExerciseId
    ) { targetDay.exercises.firstOrNull { it.id == swapExerciseId } }

    fun isSelected(libraryId: String) =
        if (swapTarget != null) swapTarget.exerciseId == libraryId else libraryId in routineExerciseByLibraryId

    fun toggle(libraryId: String, add: () -> Unit, swap: () -> Unit) {
        when {
            swapTarget != null -> swap()
            isSelected(libraryId) -> routineExerciseByLibraryId[libraryId]?.let {
                viewModel.removeExercise(
                    routineId,
                    dayId,
                    it.id
                )
            }

            else -> add()
        }
    }

    LaunchedEffect(state.searchQuery, state.selectedCategory) { listState.scrollToItem(0) }

    Scaffold(
        containerColor = GymuuBlack,
        floatingActionButton = {
            if (swapTarget == null) GymuuFab(onClick = {
                dialogState = ExerciseDialogState.Create
            })
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 18.dp,
                bottom = 112.dp
            ),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item(key = "title") {
                TopTitleBar(
                    title = if (swapTarget != null) "SWAP EXERCISE" else "SELECT EXERCISE",
                    onBack = goBack
                )
            }
            item(key = "search") {
                SearchBox(
                    query = state.searchQuery,
                    onQueryChange = viewModel::onSearchChange
                )
            }
            item(key = "chips") {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ExerciseCategory.entries.forEach { c ->
                        CategoryChip(
                            label = c.label,
                            selected = state.selectedCategory == c,
                            onClick = { viewModel.onCategorySelected(c) })
                    }
                }
            }

            if (state.pickerCustomExercises.isNotEmpty()) {
                item(key = "h_custom") { SectionHeading("CUSTOM") }
                items(state.pickerCustomExercises, key = { "c_${it.id}" }) { custom ->
                    CustomExerciseCard(
                        exercise = custom,
                        selected = isSelected(custom.id),
                        onClick = {
                            toggle(
                                custom.id,
                                add = { viewModel.addCustomExercise(routineId, dayId, custom) },
                                swap = {
                                    viewModel.swapWithCustomExercise(
                                        routineId,
                                        dayId,
                                        swapTarget!!.id,
                                        custom
                                    )
                                })
                        },
                        onEdit = { dialogState = ExerciseDialogState.Edit(custom) },
                        onDelete = { viewModel.deleteCustomExercise(custom.id) },
                        onLongClick = { customInfoExercise = custom }
                    )
                }
            }

            state.pickerSections.forEach { (category, exercises) ->
                item(key = "h_${category.name}") { SectionHeading(category.label) }
                items(exercises, key = { "b_${it.exerciseId}" }) { exercise ->
                    ExerciseListCard(
                        exercise = exercise,
                        selected = isSelected(exercise.exerciseId),
                        onClick = {
                            toggle(
                                exercise.exerciseId,
                                add = { viewModel.addBuiltInExercise(routineId, dayId, exercise) },
                                swap = {
                                    viewModel.swapWithBuiltInExercise(
                                        routineId,
                                        dayId,
                                        swapTarget!!.id,
                                        exercise
                                    )
                                })
                        },
                        onLongClick = { infoExercise = exercise }
                    )
                }
            }

            if (state.pickerCustomExercises.isEmpty() && state.pickerSections.isEmpty() && state.exercises.isNotEmpty()) {
                item(key = "empty") { EmptyState("No exercises match your search") }
            }
        }
    }

    dialogState?.let { d ->
        CreateExerciseDialog(
            initial = (d as? ExerciseDialogState.Edit)?.exercise,
            onDismiss = { dialogState = null },
            onConfirm = { draft -> viewModel.upsertCustomExercise(draft); dialogState = null }
        )
    }
    infoExercise?.let { ExerciseInfoDialog(exercise = it, onDismiss = { infoExercise = null }) }
    customInfoExercise?.let {
        CustomExerciseInfoDialog(
            exercise = it,
            onDismiss = { customInfoExercise = null })
    }
}
