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
import me.jitish.gymuu.R
import me.jitish.gymuu.data.routine.Routine
import me.jitish.gymuu.ui.GymUiState
import me.jitish.gymuu.ui.GymViewModel
import me.jitish.gymuu.ui.components.AppIconBadge
import me.jitish.gymuu.ui.components.ConfirmDeleteDialog
import me.jitish.gymuu.ui.components.DividerLine
import me.jitish.gymuu.ui.components.GymFab
import me.jitish.gymuu.ui.components.NameDialog
import me.jitish.gymuu.ui.components.RoutineRow
import me.jitish.gymuu.ui.components.SectionHeading
import me.jitish.gymuu.ui.navigation.Routes
import me.jitish.gymuu.ui.theme.GymBlack
import me.jitish.gymuu.ui.theme.GymMuted
import java.time.LocalDate

private sealed interface RoutineDialogState {
    data object Create : RoutineDialogState
    data class Edit(val routine: Routine) : RoutineDialogState
}

@Composable
internal fun RoutineLaunchScreen(state: GymUiState, navController: NavHostController) {
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
internal fun RoutineListScreen(state: GymUiState, viewModel: GymViewModel, navController: NavHostController) {
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

private fun defaultRoutineBackupFileName(): String {
    return "gymuu-routines-${LocalDate.now()}.json"
}

