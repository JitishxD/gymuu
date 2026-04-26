package me.jitish.gymuu.ui

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.DrawerValue
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.ImageLoader
import coil.compose.SubcomposeAsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import me.jitish.gymuu.data.CreateExerciseDraft
import me.jitish.gymuu.data.CustomExercise
import me.jitish.gymuu.data.Exercise
import me.jitish.gymuu.data.ExerciseSource
import me.jitish.gymuu.data.Routine
import me.jitish.gymuu.data.RoutineExercise
import me.jitish.gymuu.data.WorkoutDay
import me.jitish.gymuu.data.WorkoutSet
import me.jitish.gymuu.ui.theme.GymBlack
import me.jitish.gymuu.ui.theme.GymBorder
import me.jitish.gymuu.ui.theme.GymCard
import me.jitish.gymuu.ui.theme.GymCardAlt
import me.jitish.gymuu.ui.theme.GymDanger
import me.jitish.gymuu.ui.theme.GymMuted

private object Routes {
    const val START = "start"
    const val ROUTINES = "routines"
    const val WORKOUT = "workout/{routineId}/{dayId}"
    const val SELECT = "select/{routineId}/{dayId}"

    fun workout(routineId: String, dayId: String) = "workout/${Uri.encode(routineId)}/${Uri.encode(dayId)}"
    fun select(routineId: String, dayId: String) = "select/${Uri.encode(routineId)}/${Uri.encode(dayId)}"
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
                arguments = listOf(navArgument("routineId") { type = NavType.StringType }, navArgument("dayId") { type = NavType.StringType })
            ) { entry ->
                val routineId = Uri.decode(entry.arguments?.getString("routineId").orEmpty())
                val dayId = Uri.decode(entry.arguments?.getString("dayId").orEmpty())
                WorkoutDayScreen(state = state, viewModel = viewModel, navController = navController, routineId = routineId, dayId = dayId)
            }
            composable(
                route = Routes.SELECT,
                arguments = listOf(navArgument("routineId") { type = NavType.StringType }, navArgument("dayId") { type = NavType.StringType })
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
    var showRoutineDialog by rememberSaveable { mutableStateOf(false) }
    var routineToEdit by remember { mutableStateOf<Routine?>(null) }
    var routineToDelete by remember { mutableStateOf<Routine?>(null) }

    Scaffold(
        containerColor = GymBlack,
        floatingActionButton = {
            GymFab(onClick = { showRoutineDialog = true })
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
                    GymLogo()
                    Spacer(Modifier.height(24.dp))
                    DividerLine()
                }
            }
            item { SectionHeading("MY ROUTINES") }
            items(state.routines, key = { it.id }) { routine ->
                RoutineRow(
                    routine = routine,
                    onOpen = {
                        val firstDay = routine.days.orEmpty().firstOrNull()
                        if (firstDay != null) navController.navigate(Routes.workout(routine.id, firstDay.id))
                    },
                    onEdit = {
                        routineToEdit = routine
                        showRoutineDialog = true
                    },
                    onDelete = { routineToDelete = routine }
                )
            }
        }
    }

    if (showRoutineDialog) {
        NameDialog(
            title = if (routineToEdit == null) "CREATE ROUTINE" else "EDIT ROUTINE",
            initialValue = routineToEdit?.name.orEmpty(),
            label = "ROUTINE NAME",
            onDismiss = {
                showRoutineDialog = false
                routineToEdit = null
            },
            onConfirm = { name ->
                routineToEdit?.let { viewModel.updateRoutineName(it.id, name) } ?: viewModel.createRoutine(name)
                showRoutineDialog = false
                routineToEdit = null
            }
        )
    }

    routineToDelete?.let { routine ->
        ConfirmDeleteDialog(
            title = "DELETE ROUTINE",
            text = "Delete ${routine.name}?",
            onDismiss = { routineToDelete = null },
            onConfirm = {
                viewModel.deleteRoutine(routine.id)
                routineToDelete = null
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

    LaunchedEffect(routine?.days?.size, dayId) {
        if (routine != null && days.isNotEmpty() && state.day(routineId, dayId) == null) {
            routine.days.orEmpty().firstOrNull()?.let { navController.navigate(Routes.workout(routine.id, it.id)) { popUpTo(Routes.ROUTINES) } }
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
                    selected.days.orEmpty().firstOrNull()?.let { navController.navigate(Routes.workout(selected.id, it.id)) }
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
                        val exercises = day.exercises.orEmpty()
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
            onDismiss = { dayToRename = null },
            onConfirm = { name ->
                viewModel.updateDayName(routineId, day.id, name)
                dayToRename = null
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
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var customToEdit by remember { mutableStateOf<CustomExercise?>(null) }
    var visibleBuiltInCount by rememberSaveable(routineId, dayId) { mutableIntStateOf(BUILT_IN_PAGE_SIZE) }
    val targetDay = state.day(routineId, dayId)
    val selectedByExerciseId = remember(targetDay?.id, targetDay?.exercises) {
        targetDay?.exercises
            .orEmpty()
            .filter { !it.exerciseId.isNullOrBlank() }
            .associateBy { it.exerciseId.orEmpty() }
    }
    val selectedIds = selectedByExerciseId.keys

    if (targetDay == null) {
        Scaffold(containerColor = GymBlack) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                TopTitleBar(title = "SELECT EXERCISE", onBack = { navController.popBackStack() })
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
        floatingActionButton = { GymFab(onClick = { showCreateDialog = true }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                TopTitleBar(title = "SELECT EXERCISE", onBack = { navController.popBackStack() })
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
                                selectedByExerciseId[custom.id]?.let { viewModel.removeExercise(routineId, dayId, it.id) }
                            } else {
                                viewModel.addCustomExercise(routineId, dayId, custom)
                            }
                        },
                        onEdit = {
                            customToEdit = custom
                            showCreateDialog = true
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
                                selectedByExerciseId[exercise.exerciseId]?.let { viewModel.removeExercise(routineId, dayId, it.id) }
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

    if (showCreateDialog) {
        CreateExerciseDialog(
            initial = customToEdit,
            onDismiss = {
                showCreateDialog = false
                customToEdit = null
            },
            onConfirm = { draft ->
                viewModel.upsertCustomExercise(draft)
                showCreateDialog = false
                customToEdit = null
            }
        )
    }
}

@Composable
private fun RoutineExerciseCard(index: Int, routineId: String, dayId: String, exercise: RoutineExercise, viewModel: GymViewModel) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = GymCard),
        border = BorderStroke(1.dp, GymBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Text(index.toString(), color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = exercise.name.uppercase(),
                    color = Color.White,
                    fontSize = 21.sp,
                    lineHeight = 28.sp,
                    letterSpacing = 2.sp,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                CompactIconButton(onClick = { viewModel.removeExercise(routineId, dayId, exercise.id) }) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete exercise", tint = GymDanger, modifier = Modifier.size(24.dp))
                }
            }

            if (!exercise.gifUrl.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .padding(6.dp)
                ) {
                    GifPreview(
                        url = exercise.gifUrl,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.72f),
                        lightContainer = true
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Timer, contentDescription = null, tint = GymMuted, modifier = Modifier.size(17.dp))
                Text("REST:", color = GymMuted, fontSize = 15.sp)
                InlineEditText(value = exercise.rest, onValueChange = { viewModel.updateRest(routineId, dayId, exercise.id, it) }, width = 70.dp)
            }

            exercise.sets.forEach { set ->
                SetRow(
                    set = set,
                    onCompleted = { viewModel.updateSet(routineId, dayId, exercise.id, set.id, completed = it) },
                    onReps = { viewModel.updateSet(routineId, dayId, exercise.id, set.id, reps = it) },
                    onWeight = { viewModel.updateSet(routineId, dayId, exercise.id, set.id, weight = it) },
                    onRemove = { viewModel.removeSet(routineId, dayId, exercise.id, set.id) }
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                CompactIconButton(onClick = { viewModel.addSet(routineId, dayId, exercise.id) }) {
                    Icon(Icons.Default.AddCircleOutline, contentDescription = "Add set", tint = GymMuted, modifier = Modifier.size(22.dp))
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.NoteAlt, contentDescription = null, tint = GymMuted, modifier = Modifier.size(18.dp))
                Text("NOTES", color = GymMuted, fontSize = 15.sp)
                InlineEditText(
                    value = exercise.notes,
                    onValueChange = { viewModel.updateNotes(routineId, dayId, exercise.id, it) },
                    placeholder = "...",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SetRow(set: WorkoutSet, onCompleted: (Boolean) -> Unit, onReps: (String) -> Unit, onWeight: (String) -> Unit, onRemove: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Checkbox(
            checked = set.completed,
            onCheckedChange = onCompleted,
            modifier = Modifier.size(38.dp),
            colors = CheckboxDefaults.colors(checkedColor = Color.White, uncheckedColor = GymMuted, checkmarkColor = Color.Black)
        )
        SetCell(label = "SET NO.", value = set.setNo.toString(), modifier = Modifier.weight(1f), editable = false)
        VerticalRule()
        SetCell(label = "REPS", value = set.reps, modifier = Modifier.weight(1f), onValueChange = onReps)
        VerticalRule()
        SetCell(label = "WEIGHT", value = set.weight, placeholder = "-", modifier = Modifier.weight(1f), onValueChange = onWeight)
        CompactIconButton(onClick = onRemove) {
            Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Remove set", tint = GymDanger, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun SetCell(label: String, value: String, modifier: Modifier = Modifier, placeholder: String = "", editable: Boolean = true, onValueChange: (String) -> Unit = {}) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Text(label, color = GymMuted, fontSize = 13.sp, letterSpacing = 1.sp, maxLines = 1)
        if (editable) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                        if (value.isBlank()) Text(placeholder, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        inner()
                    }
                }
            )
        } else {
            Text(value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ExerciseListCard(exercise: Exercise, selected: Boolean, onClick: () -> Unit) {
    SelectableCard(selected = selected, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GifPreview(
                url = exercise.gifUrl,
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White),
                lightContainer = true
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(exercise.name.toTitleCase(), color = Color.White, fontSize = 22.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("3 sets x 8-12 reps", color = GymMuted, fontSize = 15.sp)
            }
            if (selected) {
                Box(Modifier.size(34.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Check, contentDescription = "Selected", tint = Color.Black, modifier = Modifier.size(24.dp))
                }
            } else {
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = GymMuted, modifier = Modifier.size(34.dp))
            }
        }
    }
}

@Composable
private fun CustomExerciseCard(exercise: CustomExercise, selected: Boolean, onClick: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    SelectableCard(selected = selected, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(exercise.name, color = Color.White, fontSize = 22.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("${exercise.sets} sets x ${exercise.reps} reps", color = GymMuted, fontSize = 15.sp)
            }
            CompactIconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White, modifier = Modifier.size(22.dp)) }
            CompactIconButton(onClick = onDelete) { Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = GymDanger, modifier = Modifier.size(22.dp)) }
            if (selected) {
                Box(Modifier.size(34.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Check, contentDescription = "Selected", tint = Color.Black, modifier = Modifier.size(24.dp))
                }
            } else {
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = GymMuted, modifier = Modifier.size(34.dp))
            }
        }
    }
}

@Composable
private fun SelectableCard(selected: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = if (selected) Color(0xFF2B2B2B) else GymCard),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) Color.White else GymBorder)
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
private fun GifPreview(url: String?, modifier: Modifier = Modifier, lightContainer: Boolean = false) {
    val context = LocalContext.current
    if (url.isNullOrBlank()) {
        Box(modifier = modifier) {
            MediaPlaceholder(lightContainer = lightContainer)
        }
        return
    }

    val appContext = context.applicationContext
    val imageLoader = remember(appContext) { SharedGifImageLoader.get(appContext) }
    val model = remember(url) {
        ImageRequest.Builder(appContext)
            .data(url)
            .crossfade(false)
            .allowHardware(false)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .build()
    }

    // Remote GIF rendering uses the exercise gifUrl directly; missing or failed media falls back to the dumbbell placeholder.
    SubcomposeAsyncImage(
        model = model,
        imageLoader = imageLoader,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier,
        loading = {
            Box(Modifier.fillMaxSize().background(if (lightContainer) Color.White else GymCardAlt), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GymMuted, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
            }
        },
        error = { MediaPlaceholder(lightContainer = lightContainer) }
    )
}

private object SharedGifImageLoader {
    @Volatile
    private var loader: ImageLoader? = null

    fun get(context: Context): ImageLoader {
        return loader ?: synchronized(this) {
            loader ?: ImageLoader.Builder(context.applicationContext)
                .components {
                    if (Build.VERSION.SDK_INT >= 28) add(ImageDecoderDecoder.Factory()) else add(GifDecoder.Factory())
                }
                .memoryCache {
                    MemoryCache.Builder(context.applicationContext)
                        .maxSizePercent(0.25)
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(context.cacheDir.resolve("image_cache"))
                        .maxSizeBytes(256L * 1024L * 1024L)
                        .build()
                }
                .respectCacheHeaders(false)
                .crossfade(false)
                .build()
                .also { loader = it }
        }
    }
}

@Composable
private fun MediaPlaceholder(lightContainer: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (lightContainer) Color.White else GymCardAlt),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = if (lightContainer) Color(0xFF333333) else GymMuted, modifier = Modifier.size(32.dp))
    }
}

@Composable
private fun CreateExerciseDialog(initial: CustomExercise?, onDismiss: () -> Unit, onConfirm: (CreateExerciseDraft) -> Unit) {
    var name by rememberSaveable(initial?.id) { mutableStateOf(initial?.name.orEmpty()) }
    var sets by rememberSaveable(initial?.id) { mutableIntStateOf(initial?.sets ?: 3) }
    var reps by rememberSaveable(initial?.id) { mutableStateOf(initial?.reps.orEmpty()) }
    var rest by rememberSaveable(initial?.id) { mutableStateOf(initial?.rest.orEmpty()) }
    var expanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = GymCard, border = BorderStroke(1.dp, GymBorder), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("CREATE EXERCISE", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                GymInput(label = "NAME", value = name, onValueChange = { name = it }, placeholder = "e.g. Bench Press")
                Column {
                    Text("SETS", color = GymMuted, fontSize = 14.sp, letterSpacing = 1.sp)
                    Box {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(58.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, GymBorder, RoundedCornerShape(10.dp))
                                .clickable { expanded = true }
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(sets.toString(), color = Color.White, fontSize = 20.sp, modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = GymMuted)
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(GymCardAlt)) {
                            (1..10).forEach { count ->
                                DropdownMenuItem(text = { Text(count.toString(), color = Color.White) }, onClick = {
                                    sets = count
                                    expanded = false
                                })
                            }
                        }
                    }
                }
                GymInput(
                    label = "REPS",
                    value = reps,
                    onValueChange = { reps = it },
                    placeholder = "e.g. 10",
                    trailing = { Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = GymMuted) },
                    helper = "Press <-> to enter a range (e.g. 10-12)."
                )
                GymInput(
                    label = "REST (MIN)",
                    value = rest,
                    onValueChange = { rest = it },
                    placeholder = "e.g. 1:30 or 2",
                    trailing = { Icon(Icons.Default.Timer, contentDescription = null, tint = GymMuted) }
                )
                Spacer(Modifier.height(80.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("CANCEL", color = Color.White, letterSpacing = 1.sp) }
                    TextButton(onClick = {
                        onConfirm(CreateExerciseDraft(id = initial?.id, name = name, sets = sets, reps = reps, rest = rest))
                    }) { Text("CONFIRM", color = Color.White, letterSpacing = 1.sp) }
                }
            }
        }
    }
}

@Composable
private fun GymInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    trailing: @Composable (() -> Unit)? = null,
    helper: String? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = GymMuted, fontSize = 14.sp, letterSpacing = 1.sp)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = Color(0xFF6E6E6E)) },
            trailingIcon = trailing,
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color(0xFF151515),
                unfocusedContainerColor = Color(0xFF151515),
                focusedBorderColor = GymBorder,
                unfocusedBorderColor = GymBorder,
                cursorColor = Color.White
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        )
        helper?.let { Text(it, color = GymMuted, fontSize = 13.sp) }
    }
}

@Composable
private fun NameDialog(title: String, initialValue: String, label: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var value by rememberSaveable(initialValue) { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GymCard,
        title = { Text(title, color = Color.White) },
        text = { GymInput(label = label, value = value, onValueChange = { value = it }, placeholder = "e.g. Push Day") },
        confirmButton = { TextButton(onClick = { onConfirm(value) }) { Text("CONFIRM", color = Color.White) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL", color = GymMuted) } }
    )
}

@Composable
private fun ConfirmDeleteDialog(title: String, text: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GymCard,
        title = { Text(title, color = Color.White) },
        text = { Text(text, color = GymMuted) },
        confirmButton = { TextButton(onClick = onConfirm) { Text("DELETE", color = GymDanger) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL", color = Color.White) } }
    )
}

@Composable
private fun WorkoutHeader(
    title: String,
    onMenu: () -> Unit,
    onRename: () -> Unit,
    onAddDay: () -> Unit,
    onRemoveDay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth()) {
        CompactIconButton(onClick = onMenu) { Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White, modifier = Modifier.size(28.dp)) }
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(6.dp))
                .clickable(onClick = onRename)
                .padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = Color.White, fontSize = 23.sp, letterSpacing = 3.sp, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.width(6.dp))
            Icon(Icons.Default.Edit, contentDescription = "Rename day", tint = GymMuted, modifier = Modifier.size(16.dp))
        }
        CompactIconButton(onClick = onAddDay) { Icon(Icons.Default.AddCircleOutline, contentDescription = "Add day", tint = Color.White, modifier = Modifier.size(24.dp)) }
        CompactIconButton(onClick = onRemoveDay) { Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Remove day", tint = GymDanger, modifier = Modifier.size(24.dp)) }
        CompactIconButton(onClick = onPrevious) { Icon(Icons.Default.ChevronLeft, contentDescription = "Previous day", tint = Color.White, modifier = Modifier.size(28.dp)) }
        CompactIconButton(onClick = onNext) { Icon(Icons.Default.ChevronRight, contentDescription = "Next day", tint = Color.White, modifier = Modifier.size(28.dp)) }
    }
}

@Composable
private fun TopTitleBar(title: String, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().height(56.dp), contentAlignment = Alignment.Center) {
        CompactIconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(32.dp))
        }
        Text(title, color = Color.White, fontSize = 24.sp, letterSpacing = 5.sp)
    }
}

@Composable
private fun SearchBox(query: String, onQueryChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(GymCard)
            .border(1.dp, GymBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(Icons.Default.Search, contentDescription = null, tint = GymMuted, modifier = Modifier.size(32.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = TextStyle(color = Color.White, fontSize = 21.sp),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                if (query.isBlank()) Text("Search exercise...", color = Color(0xFF5F5F5F), fontSize = 21.sp)
                inner()
            }
        )
    }
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .height(44.dp)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = if (selected) Color.White else GymCard,
        border = BorderStroke(1.dp, if (selected) Color.White else GymBorder)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 22.dp)) {
            Text(label, color = if (selected) Color.Black else Color.White, fontSize = 12.sp)
        }
    }
}

@Composable
private fun RoutineRow(routine: Routine, onOpen: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(GymCard)
            .border(1.dp, GymBorder, RoundedCornerShape(8.dp))
            .clickable(onClick = onOpen)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        Text(routine.name, color = Color.White, fontSize = 19.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        CompactIconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White, modifier = Modifier.size(22.dp)) }
        CompactIconButton(onClick = onDelete) { Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = GymDanger, modifier = Modifier.size(22.dp)) }
    }
}

@Composable
private fun MenuAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Icon(icon, contentDescription = null, tint = GymMuted, modifier = Modifier.size(22.dp))
        Text(label, color = Color(0xFFC8C8C8), fontSize = 20.sp)
    }
}

@Composable
private fun RoutineDrawer(
    routines: List<Routine>,
    onManageRoutines: () -> Unit,
    onRoutineClick: (Routine) -> Unit
) {
    ModalDrawerSheet(drawerContainerColor = GymBlack, drawerContentColor = Color.White, modifier = Modifier.fillMaxHeight().widthIn(max = 320.dp)) {
        Column(modifier = Modifier.statusBarsPadding().padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            GymLogo()
            DividerLine()
            MenuAction(icon = Icons.Default.Edit, label = "Manage routines", onClick = onManageRoutines)
            SectionHeading("MY ROUTINES")
            routines.forEach { routine ->
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onRoutineClick(routine) }.padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                    Text(routine.name, color = Color.White, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun CompactIconButton(modifier: Modifier = Modifier, onClick: () -> Unit, content: @Composable () -> Unit) {
    IconButton(onClick = onClick, modifier = modifier.size(40.dp)) {
        content()
    }
}

@Composable
private fun GymLogo() {
    Box(
        modifier = Modifier
            .size(92.dp)
            .clip(CircleShape)
            .border(2.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = Color.White, modifier = Modifier.size(42.dp))
    }
}

@Composable
private fun GymFab(onClick: () -> Unit) {
    FloatingActionButton(
        onClick = onClick,
        shape = CircleShape,
        containerColor = Color.White,
        contentColor = Color.Black,
        modifier = Modifier.navigationBarsPadding().size(62.dp)
    ) {
        Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(30.dp))
    }
}

@Composable
private fun SectionHeading(text: String) {
    Text(text, color = GymMuted, fontSize = 16.sp, letterSpacing = 3.sp, fontWeight = FontWeight.Light)
}

@Composable
private fun EmptyState(text: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(36.dp), contentAlignment = Alignment.Center) {
        Text(text, color = GymMuted, fontSize = 16.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun DividerLine() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White))
}

@Composable
private fun VerticalRule() {
    Box(Modifier.height(48.dp).width(1.dp).background(GymBorder))
}

@Composable
private fun InlineEditText(value: String, onValueChange: (String) -> Unit, width: androidx.compose.ui.unit.Dp = 110.dp, placeholder: String = "", modifier: Modifier = Modifier.width(width)) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold),
        modifier = modifier,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        decorationBox = { inner ->
            Box {
                if (value.isBlank()) Text(placeholder, color = GymMuted, fontSize = 16.sp)
                inner()
            }
        }
    )
}

private fun String.toTitleCase(): String {
    return split(" ").joinToString(" ") { word ->
        word.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase() else char.toString() }
    }
}

private const val BUILT_IN_PAGE_SIZE = 24
