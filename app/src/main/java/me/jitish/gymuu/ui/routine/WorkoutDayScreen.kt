package me.jitish.gymuu.ui.routine

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import me.jitish.gymuu.data.exercise.Exercise
import me.jitish.gymuu.data.routine.RoutineExercise
import me.jitish.gymuu.data.routine.RoutineExercisePastePosition
import me.jitish.gymuu.data.routine.WorkoutDay
import me.jitish.gymuu.ui.GymUiState
import me.jitish.gymuu.ui.GymViewModel
import me.jitish.gymuu.ui.components.EmptyState
import me.jitish.gymuu.ui.components.GymFab
import me.jitish.gymuu.ui.components.NameDialog
import me.jitish.gymuu.ui.components.RoutineDrawer
import me.jitish.gymuu.ui.components.WorkoutHeader
import me.jitish.gymuu.ui.exercise.ExerciseInfoDialog
import me.jitish.gymuu.ui.exercise.RoutineExerciseCard
import me.jitish.gymuu.ui.exercise.RoutineExerciseInfoDialog
import me.jitish.gymuu.ui.navigation.Routes
import me.jitish.gymuu.ui.theme.GymBlack

@Composable
internal fun WorkoutDayScreen(
    state: GymUiState,
    viewModel: GymViewModel,
    navController: NavHostController,
    routineId: String,
    dayId: String
) {
    val context = LocalContext.current
    val routine = state.routine(routineId)
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var dayToRename by remember { mutableStateOf<WorkoutDay?>(null) }
    var selectedInfoExercise by remember { mutableStateOf<Exercise?>(null) }
    var selectedRoutineExerciseInfo by remember { mutableStateOf<RoutineExercise?>(null) }
    var pendingDayId by remember { mutableStateOf<String?>(null) }
    var selectedExerciseIds by rememberSaveable(routineId, dayId) { mutableStateOf(emptyList<String>()) }
    var bulkSelectionDayId by rememberSaveable(routineId, dayId) { mutableStateOf<String?>(null) }
    var pasteAnchor by remember { mutableStateOf<PasteAnchor?>(null) }
    val days = routine?.days.orEmpty()
    val exercisesById = remember(state.exercises) { state.exercises.associateBy { it.exerciseId } }
    val routeDayIndex = days.indexOfFirst { it.id == dayId }.takeIf { it >= 0 } ?: 0
    val pagerState = rememberPagerState(initialPage = routeDayIndex, pageCount = { days.size })
    val activeDay = days.getOrNull(pagerState.currentPage.coerceIn(0, (days.size - 1).coerceAtLeast(0)))
    val bulkSelectionActive = bulkSelectionDayId == activeDay?.id
    val copiedExercisesCount = state.copiedExercises.size
    val bulkToolbarVisible = bulkSelectionActive || copiedExercisesCount > 0
    val activeExerciseIds = activeDay?.exercises.orEmpty().map { it.id }
    val selectedExerciseIdSet = selectedExerciseIds.toSet()
    val selectedExercises = selectedRoutineExercises(activeDay, selectedExerciseIds)

    fun closeRenameDayDialog() {
        dayToRename = null
    }

    fun closeBulkSelection() {
        selectedExerciseIds = emptyList()
        bulkSelectionDayId = null
    }

    fun cancelBulkActions() {
        closeBulkSelection()
        pasteAnchor = null
        viewModel.clearCopiedExercises()
    }

    fun enterBulkSelection(day: WorkoutDay, exerciseId: String) {
        bulkSelectionDayId = day.id
        selectedExerciseIds = listOf(exerciseId)
    }

    fun updateExerciseSelection(exerciseId: String, selected: Boolean) {
        selectedExerciseIds = updatedExerciseSelection(selectedExerciseIds, exerciseId, selected)
    }

    fun copySelectedExercises() {
        if (selectedExercises.isEmpty()) return
        viewModel.copyExercises(selectedExercises)
        Toast.makeText(context, "Copied ${selectedExercises.size} exercises.", Toast.LENGTH_SHORT).show()
        closeBulkSelection()
    }

    fun pasteCopiedExercises(anchor: PasteAnchor? = null, position: RoutineExercisePastePosition = RoutineExercisePastePosition.END) {
        val targetRoutineId = anchor?.routineId ?: routine?.id ?: return
        val targetDayId = anchor?.dayId ?: activeDay?.id ?: return
        val pastedCount = viewModel.pasteCopiedExercises(
            routineId = targetRoutineId,
            dayId = targetDayId,
            anchorExerciseId = anchor?.exerciseId,
            position = position
        )
        if (pastedCount > 0) {
            Toast.makeText(context, "Pasted $pastedCount exercises.", Toast.LENGTH_SHORT).show()
            closeBulkSelection()
            pasteAnchor = null
        }
    }

    fun deleteSelectedExercises() {
        val targetRoutine = routine ?: return
        val targetDay = activeDay ?: return
        val selectedIds = selectedExercises.map { it.id }.toSet()
        if (selectedIds.isEmpty()) return

        viewModel.removeExercises(targetRoutine.id, targetDay.id, selectedIds)
        Toast.makeText(context, "Deleted ${selectedIds.size} exercises.", Toast.LENGTH_SHORT).show()
        closeBulkSelection()
    }

    fun requestAnchoredPaste(day: WorkoutDay, exercise: RoutineExercise) {
        if (copiedExercisesCount == 0) return
        pasteAnchor = PasteAnchor(
            routineId = routine?.id ?: return,
            dayId = day.id,
            exerciseId = exercise.id,
            exerciseName = exercise.name
        )
    }

    LaunchedEffect(routine?.days?.size, dayId) {
        if (routine != null && days.isNotEmpty() && state.day(routineId, dayId) == null) {
            routine.days.firstOrNull()?.let {
                navController.navigate(Routes.workout(routine.id, it.id)) { popUpTo(Routes.ROUTINES) }
            }
        }
    }

    LaunchedEffect(routine?.id, dayId) {
        if (days.isNotEmpty() && pagerState.currentPage != routeDayIndex) {
            pagerState.scrollToPage(routeDayIndex.coerceIn(0, days.lastIndex))
        }
    }

    LaunchedEffect(days.map { it.id }, pendingDayId) {
        val targetId = pendingDayId ?: return@LaunchedEffect
        val targetIndex = days.indexOfFirst { it.id == targetId }
        if (targetIndex >= 0) {
            pagerState.animateScrollToPage(targetIndex)
            pendingDayId = null
        }
    }

    LaunchedEffect(days.size) {
        if (days.isNotEmpty() && pagerState.currentPage > days.lastIndex) {
            pagerState.scrollToPage(days.lastIndex)
        }
    }

    LaunchedEffect(activeDay?.id) {
        closeBulkSelection()
    }

    LaunchedEffect(activeExerciseIds) {
        selectedExerciseIds = selectedExerciseIds.filter { it in activeExerciseIds }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            RoutineDrawer(
                routines = state.routines,
                onManageRoutines = {
                    navController.navigate(Routes.ROUTINES) {
                        launchSingleTop = true
                    }
                    scope.launch { drawerState.close() }
                },
                onRoutineClick = { selected ->
                    selected.days.firstOrNull()?.let {
                        navController.navigate(Routes.workout(selected.id, it.id))
                    }
                    scope.launch { drawerState.close() }
                }
            )
        }
    ) {
        Scaffold(
            containerColor = GymBlack,
            floatingActionButton = {
                if (routine != null && activeDay != null) {
                    GymFab(onClick = { navController.navigate(Routes.select(routine.id, activeDay.id)) })
                }
            }
        ) { padding ->
            if (routine == null || days.isEmpty() || activeDay == null) {
                EmptyState("Routine not found")
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    WorkoutHeader(
                        title = activeDay.name,
                        onMenu = { scope.launch { drawerState.open() } },
                        onRename = { dayToRename = activeDay },
                        onAddDay = {
                            viewModel.addDay(routine.id)?.let { newDay ->
                                pendingDayId = newDay.id
                            }
                        },
                        onRemoveDay = { viewModel.removeDay(routine.id, activeDay.id) },
                        onPrevious = {
                            scope.launch {
                                val previous = (pagerState.currentPage - 1).coerceAtLeast(0)
                                pagerState.animateScrollToPage(previous)
                            }
                        },
                        onNext = {
                            scope.launch {
                                val next = (pagerState.currentPage + 1).coerceAtMost(days.lastIndex)
                                pagerState.animateScrollToPage(next)
                            }
                        },
                        modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 6.dp)
                    )

                    if (bulkToolbarVisible) {
                        WorkoutBulkActionBar(
                            selectionMode = bulkSelectionActive,
                            selectedCount = selectedExercises.size,
                            exerciseCount = activeDay.exercises.size,
                            copiedCount = copiedExercisesCount,
                            onSelectAll = {
                                selectedExerciseIds = selectAllOrClearExerciseIds(activeDay, selectedExercises.size)
                            },
                            onCopy = ::copySelectedExercises,
                            onPaste = { pasteCopiedExercises() },
                            onDelete = ::deleteSelectedExercises,
                            onClear = ::cancelBulkActions,
                            modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 14.dp)
                        )
                    }

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        beyondViewportPageCount = 1
                    ) { page ->
                        val day = days[page]
                        val exercises = day.exercises
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 18.dp, bottom = 96.dp),
                            verticalArrangement = Arrangement.spacedBy(22.dp)
                        ) {
                            if (exercises.isEmpty()) {
                                item { EmptyState("Add exercises to build this workout") }
                            } else {
                                itemsIndexed(exercises, key = { _, item -> item.id }) { index, exercise ->
                                    RoutineExerciseCard(
                                        index = index + 1,
                                        routineId = routine.id,
                                        dayId = day.id,
                                        exercise = exercise,
                                        viewModel = viewModel,
                                        selectionMode = bulkSelectionDayId == day.id,
                                        selected = exercise.id in selectedExerciseIdSet,
                                        onSelectedChange = { selected ->
                                            updateExerciseSelection(exercise.id, selected)
                                        },
                                        showActions = !bulkToolbarVisible,
                                        onClick = {
                                            requestAnchoredPaste(day, exercise)
                                        },
                                        onLongClick = {
                                            if (copiedExercisesCount > 0) {
                                                requestAnchoredPaste(day, exercise)
                                            } else {
                                                enterBulkSelection(day, exercise.id)
                                            }
                                        },
                                        onInfoClick = {
                                            val info = exercise.exerciseId?.let(exercisesById::get)
                                            if (info != null) {
                                                selectedInfoExercise = info
                                            } else {
                                                selectedRoutineExerciseInfo = exercise
                                            }
                                        },
                                        onSwap = {
                                            navController.navigate(Routes.select(routine.id, day.id, exercise.id))
                                        },
                                        canMoveUp = index > 0,
                                        canMoveDown = index < exercises.lastIndex,
                                        onMoveUp = { viewModel.moveExercise(routine.id, day.id, exercise.id, -1) },
                                        onMoveDown = { viewModel.moveExercise(routine.id, day.id, exercise.id, 1) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    dayToRename?.let { day ->
        NameDialog(
            title = "RENAME DAY",
            initialValue = day.name,
            label = "DAY NAME",
            onDismiss = ::closeRenameDayDialog,
            onConfirm = { name ->
                viewModel.updateDayName(routineId, day.id, name)
                closeRenameDayDialog()
            }
        )
    }

    selectedInfoExercise?.let { exercise ->
        ExerciseInfoDialog(
            exercise = exercise,
            onDismiss = { selectedInfoExercise = null }
        )
    }

    selectedRoutineExerciseInfo?.let { exercise ->
        RoutineExerciseInfoDialog(
            exercise = exercise,
            onDismiss = { selectedRoutineExerciseInfo = null }
        )
    }

    pasteAnchor?.let { anchor ->
        PastePlacementDialog(
            anchor = anchor,
            copiedCount = copiedExercisesCount,
            onDismiss = { pasteAnchor = null },
            onPasteBefore = { pasteCopiedExercises(anchor, RoutineExercisePastePosition.BEFORE) },
            onPasteAfter = { pasteCopiedExercises(anchor, RoutineExercisePastePosition.AFTER) }
        )
    }
}

