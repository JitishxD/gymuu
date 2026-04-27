package me.jitish.gymuu.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import me.jitish.gymuu.R
import me.jitish.gymuu.data.CustomExercise
import me.jitish.gymuu.data.Routine
import me.jitish.gymuu.data.WorkoutDay
import me.jitish.gymuu.ui.theme.GymBlack
import me.jitish.gymuu.ui.theme.GymMuted
import java.time.LocalDate

private object Routes {
    const val START = "start"
    const val ROUTINES = "routines"
    const val WORKOUT = "workout/{routineId}/{dayId}"
    const val SELECT = "select/{routineId}/{dayId}"

    fun workout(routineId: String, dayId: String) = "workout/${Uri.encode(routineId)}/${Uri.encode(dayId)}"
    fun select(routineId: String, dayId: String) = "select/${Uri.encode(routineId)}/${Uri.encode(dayId)}"
}

private sealed interface RoutineDialogState {
    data object Create : RoutineDialogState
    data class Edit(val routine: Routine) : RoutineDialogState
}

private sealed interface ExerciseDialogState {
    data object Create : ExerciseDialogState
    data class Edit(val exercise: CustomExercise) : ExerciseDialogState
}

@Composable
fun GymuuApp(viewModel: GymViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()

    Surface(color = GymBlack, modifier = Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = Routes.START) {
            composable(Routes.START) {
                RoutineLaunchScreen(state = state, navController = navController)
            }
            composable(Routes.ROUTINES) {
                RoutineListScreen(state = state, viewModel = viewModel, navController = navController)
            }
            composable(
                route = Routes.WORKOUT,
                arguments = listOf(
                    navArgument("routineId") { type = NavType.StringType },
                    navArgument("dayId") { type = NavType.StringType }
                )
            ) { entry ->
                val routineId = Uri.decode(entry.arguments?.getString("routineId").orEmpty())
                val dayId = Uri.decode(entry.arguments?.getString("dayId").orEmpty())
                WorkoutDayScreen(
                    state = state,
                    viewModel = viewModel,
                    navController = navController,
                    routineId = routineId,
                    dayId = dayId
                )
            }
            composable(
                route = Routes.SELECT,
                arguments = listOf(
                    navArgument("routineId") { type = NavType.StringType },
                    navArgument("dayId") { type = NavType.StringType }
                )
            ) { entry ->
                SelectExerciseScreen(
                    state = state,
                    viewModel = viewModel,
                    navController = navController,
                    routineId = Uri.decode(entry.arguments?.getString("routineId").orEmpty()),
                    dayId = Uri.decode(entry.arguments?.getString("dayId").orEmpty())
                )
            }
        }
    }
}

@Composable
private fun RoutineLaunchScreen(state: GymUiState, navController: NavHostController) {
    val routine = state.routines.firstOrNull()
    val day = routine?.days.orEmpty().firstOrNull()

    LaunchedEffect(routine?.id, day?.id) {
        if (routine != null && day != null) {
            navController.navigate(Routes.workout(routine.id, day.id)) {
                popUpTo(Routes.START) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GymBlack)
            .statusBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("LOADING ROUTINE", color = GymMuted, fontSize = 14.sp, letterSpacing = 3.sp)
    }
}

@Composable
private fun RoutineListScreen(state: GymUiState, viewModel: GymViewModel, navController: NavHostController) {
    val context = LocalContext.current
    var routineDialogState by remember { mutableStateOf<RoutineDialogState?>(null) }
    var routineToDelete by remember { mutableStateOf<Routine?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        runCatching {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                writer.write(viewModel.exportRoutineBackup())
            } ?: error("Couldn't open the selected file.")
        }.onSuccess {
            Toast.makeText(context, "Routines exported.", Toast.LENGTH_SHORT).show()
        }.onFailure { error ->
            Toast.makeText(context, error.message ?: "Export failed.", Toast.LENGTH_LONG).show()
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                reader.readText()
            } ?: error("Couldn't read the selected file.")
        }.mapCatching { json ->
            viewModel.importRoutineBackup(json).getOrThrow()
        }.onSuccess { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }.onFailure { error ->
            Toast.makeText(context, error.message ?: "Import failed.", Toast.LENGTH_LONG).show()
        }
    }

    fun closeRoutineDialog() {
        routineDialogState = null
    }

    fun closeDeleteRoutineDialog() {
        routineToDelete = null
    }

    Scaffold(
        containerColor = GymBlack,
        floatingActionButton = {
            GymFab(onClick = { routineDialogState = RoutineDialogState.Create })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    AppIconBadge()
                    Text(
                        text = stringResource(R.string.app_name),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(24.dp))
                    DividerLine()
                }
            }
            item { SectionHeading("MY ROUTINES") }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { exportLauncher.launch(defaultRoutineBackupFileName()) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("EXPORT", fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                    }
                    Button(
                        onClick = { importLauncher.launch(arrayOf("application/json", "text/plain")) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1D1D1D),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("IMPORT", fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                    }
                }
            }
            item {
                Text(
                    text = "Export saves all routines and custom exercises as a JSON backup file.",
                    color = GymMuted,
                    fontSize = 16.sp
                )
            }
            items(state.routines, key = { it.id }) { routine ->
                RoutineRow(
                    routine = routine,
                    onOpen = {
                        val firstDay = routine.days.firstOrNull()
                        if (firstDay != null) navController.navigate(Routes.workout(routine.id, firstDay.id))
                    },
                    onEdit = {
                        routineDialogState = RoutineDialogState.Edit(routine)
                    },
                    onDelete = { routineToDelete = routine }
                )
            }
        }
    }

    routineDialogState?.let { dialogState ->
        val routineToEdit = (dialogState as? RoutineDialogState.Edit)?.routine
        NameDialog(
            title = if (dialogState is RoutineDialogState.Create) "CREATE ROUTINE" else "EDIT ROUTINE",
            initialValue = routineToEdit?.name.orEmpty(),
            label = "ROUTINE NAME",
            onDismiss = ::closeRoutineDialog,
            onConfirm = { name ->
                routineToEdit?.let { viewModel.updateRoutineName(it.id, name) } ?: viewModel.createRoutine(name)
                closeRoutineDialog()
            }
        )
    }

    routineToDelete?.let { routine ->
        ConfirmDeleteDialog(
            title = "DELETE ROUTINE",
            text = "Delete ${routine.name}?",
            onDismiss = ::closeDeleteRoutineDialog,
            onConfirm = {
                viewModel.deleteRoutine(routine.id)
                closeDeleteRoutineDialog()
            }
        )
    }
}

@Composable
private fun WorkoutDayScreen(
    state: GymUiState,
    viewModel: GymViewModel,
    navController: NavHostController,
    routineId: String,
    dayId: String
) {
    val routine = state.routine(routineId)
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var dayToRename by remember { mutableStateOf<WorkoutDay?>(null) }
    var pendingDayId by remember { mutableStateOf<String?>(null) }
    val days = routine?.days.orEmpty()
    val routeDayIndex = days.indexOfFirst { it.id == dayId }.takeIf { it >= 0 } ?: 0
    val pagerState = rememberPagerState(initialPage = routeDayIndex, pageCount = { days.size })
    val activeDay = days.getOrNull(pagerState.currentPage.coerceIn(0, (days.size - 1).coerceAtLeast(0)))

    fun closeRenameDayDialog() {
        dayToRename = null
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

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
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
                                        viewModel = viewModel
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
}

@Composable
private fun SelectExerciseScreen(
    state: GymUiState,
    viewModel: GymViewModel,
    navController: NavHostController,
    routineId: String,
    dayId: String
) {
    var exerciseDialogState by remember { mutableStateOf<ExerciseDialogState?>(null) }
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
        floatingActionButton = { GymFab(onClick = { exerciseDialogState = ExerciseDialogState.Create }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                TopTitleBar(title = "SELECT EXERCISE", onBack = navigateBackToDay)
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
                    val selected = selectedIds.contains(custom.id)
                    CustomExerciseCard(
                        exercise = custom,
                        selected = selected,
                        onClick = {
                            if (selected) {
                                selectedByExerciseId[custom.id]?.let {
                                    viewModel.removeExercise(routineId, dayId, it.id)
                                }
                            } else {
                                viewModel.addCustomExercise(routineId, dayId, custom)
                            }
                        },
                        onEdit = {
                            exerciseDialogState = ExerciseDialogState.Edit(custom)
                        },
                        onDelete = { viewModel.deleteCustomExercise(custom.id) }
                    )
                }
            }

            builtInSections.forEach { (category, exercises) ->
                item { SectionHeading(category.label) }
                items(exercises, key = { it.exerciseId }) { exercise ->
                    val selected = selectedIds.contains(exercise.exerciseId)
                    ExerciseListCard(
                        exercise = exercise,
                        selected = selected,
                        onClick = {
                            if (selected) {
                                selectedByExerciseId[exercise.exerciseId]?.let {
                                    viewModel.removeExercise(routineId, dayId, it.id)
                                }
                            } else {
                                viewModel.addBuiltInExercise(routineId, dayId, exercise)
                            }
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
}

private const val BUILT_IN_PAGE_SIZE = 24

private fun defaultRoutineBackupFileName(): String {
    return "gymuu-routines-${LocalDate.now()}.json"
}
