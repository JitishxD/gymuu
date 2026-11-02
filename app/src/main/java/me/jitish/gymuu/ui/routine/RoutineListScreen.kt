package me.jitish.gymuu.ui.routine

import android.widget.Toast
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.jitish.gymuu.R
import me.jitish.gymuu.data.routine.Routine
import me.jitish.gymuu.ui.AppUiState
import me.jitish.gymuu.ui.AppViewModel
import me.jitish.gymuu.ui.components.AppIconBadge
import me.jitish.gymuu.ui.components.ConfirmDeleteDialog
import me.jitish.gymuu.ui.components.DividerLine
import me.jitish.gymuu.ui.components.GymuuFab
import me.jitish.gymuu.ui.components.NameDialog
import me.jitish.gymuu.ui.components.RoutineRow
import me.jitish.gymuu.ui.components.SectionHeading
import me.jitish.gymuu.ui.navigation.Routes
import me.jitish.gymuu.ui.theme.GymuuBlack
import me.jitish.gymuu.ui.theme.GymuuMuted
import java.time.LocalDate

private sealed interface RoutineDialogState {
    data object Create : RoutineDialogState
    data class Edit(val routine: Routine) : RoutineDialogState
}

@Composable
internal fun RoutineLaunchScreen(state: AppUiState, navController: NavHostController) {
    val savedRoute = state.lastWorkoutRoute
    val savedRoutine = savedRoute?.let { route -> state.routines.firstOrNull { it.id == route.routineId } }
    val savedDay = savedRoutine?.days?.firstOrNull { it.id == savedRoute.dayId }
    val fallbackRoutine = state.routines.firstOrNull()
    val fallbackDay = fallbackRoutine?.days.orEmpty().firstOrNull()
    val routine = savedRoutine?.takeIf { savedDay != null } ?: fallbackRoutine
    val day = savedDay ?: fallbackDay

    LaunchedEffect(routine?.id, day?.id, savedRoute) {
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
            .background(GymuuBlack)
            .statusBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("LOADING ROUTINE", color = GymuuMuted, fontSize = 14.sp, letterSpacing = 3.sp)
    }
}

@Composable
internal fun RoutineListScreen(state: AppUiState, viewModel: AppViewModel, navController: NavHostController) {
    val context = LocalContext.current
    var routineDialogState by remember { mutableStateOf<RoutineDialogState?>(null) }
    var routineToDelete by remember { mutableStateOf<Routine?>(null) }
    val backupScope = rememberCoroutineScope()

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(BACKUP_MIME_TYPE)
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        // The archive carries media, so the write runs off the main thread.
        backupScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val output = context.contentResolver.openOutputStream(uri, "wt")
                        ?: error("Couldn't open the selected file.")
                    output.use { stream -> viewModel.exportRoutineBackup(stream).getOrThrow() }
                }
            }
            Toast.makeText(
                context,
                result.fold({ "Routines exported." }, { it.message ?: "Export failed." }),
                Toast.LENGTH_LONG
            ).show()
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        backupScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val input = context.contentResolver.openInputStream(uri)
                        ?: error("Couldn't read the selected file.")
                    input.use { stream -> viewModel.importRoutineBackup(stream).getOrThrow() }
                }
            }
            Toast.makeText(
                context,
                result.getOrElse { it.message ?: "Import failed." },
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun closeRoutineDialog() {
        routineDialogState = null
    }

    fun closeDeleteRoutineDialog() {
        routineToDelete = null
    }

    Scaffold(
        containerColor = GymuuBlack,
        floatingActionButton = {
            GymuuFab(onClick = { routineDialogState = RoutineDialogState.Create })
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
                        onClick = { importLauncher.launch(BACKUP_IMPORT_MIME_TYPES) },
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
                    text = "Export saves all routines, custom exercises and their media as a .gymuu backup file.",
                    color = GymuuMuted,
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

private const val BACKUP_MIME_TYPE = "application/zip"

private val BACKUP_IMPORT_MIME_TYPES =
    arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream")

private fun defaultRoutineBackupFileName(): String {
    return "gymuu-backup-${LocalDate.now()}.gymuu"
}

