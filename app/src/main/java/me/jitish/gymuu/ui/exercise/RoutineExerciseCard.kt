package me.jitish.gymuu.ui.exercise

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.jitish.gymuu.data.routine.RoutineExercise
import me.jitish.gymuu.ui.GymViewModel
import me.jitish.gymuu.ui.components.CompactIconButton
import me.jitish.gymuu.ui.components.InlineEditText
import me.jitish.gymuu.ui.theme.GymBorder
import me.jitish.gymuu.ui.theme.GymCard
import me.jitish.gymuu.ui.theme.GymDanger
import me.jitish.gymuu.ui.theme.GymMuted

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun RoutineExerciseCard(
    index: Int,
    routineId: String,
    dayId: String,
    exercise: RoutineExercise,
    viewModel: GymViewModel,
    selectionMode: Boolean,
    selected: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    showActions: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onInfoClick: () -> Unit,
    onSwap: () -> Unit,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val restActiveColor = Color(0xFF4CAF50)
    val isRestTimerRunning = remember(exercise.id) { mutableStateOf(false) }
    val remainingRestSeconds = remember(exercise.id) { mutableStateOf(0) }
    val restTimerJob = remember(exercise.id) { mutableStateOf<Job?>(null) }
    val normalizedRestValue = remember(exercise.id) { mutableStateOf<String?>(null) }

    fun restoreNormalizedRestValue() {
        normalizedRestValue.value?.let { normalized ->
            viewModel.updateRest(routineId, dayId, exercise.id, normalized)
        }
        normalizedRestValue.value = null
    }

    fun cancelRestTimer() {
        isRestTimerRunning.value = false
        restTimerJob.value?.cancel()
        restTimerJob.value = null
        remainingRestSeconds.value = 0
        restoreNormalizedRestValue()
    }

    fun startRestTimer() {
        if (restTimerJob.value?.isActive == true) return

        val totalRestSeconds = parseRestTimeToSeconds(exercise.rest)
        if (totalRestSeconds <= 0) return

        // Normalize the raw input (e.g. "120:00" → "99:59", "7" → "07:00") and persist it
        val correctedRest = formatRestCountdown(totalRestSeconds)
        normalizedRestValue.value = correctedRest
        viewModel.updateRest(routineId, dayId, exercise.id, correctedRest)

        isRestTimerRunning.value = true
        remainingRestSeconds.value = totalRestSeconds

        restTimerJob.value = coroutineScope.launch {
            try {
                while (remainingRestSeconds.value > 0) {
                    delay(1000)

                    remainingRestSeconds.value -= 1

                    viewModel.updateRest(
                        routineId,
                        dayId,
                        exercise.id,
                        formatRestCountdown(remainingRestSeconds.value)
                    )
                }

                if (isRestTimerRunning.value) {
                    triggerRestCompleteVibration(context)
                }
            } finally {
                isRestTimerRunning.value = false
                remainingRestSeconds.value = 0
                restTimerJob.value = null
                restoreNormalizedRestValue()
            }
        }
    }

    DisposableEffect(exercise.id) {
        onDispose {
            cancelRestTimer()
        }
    }

    val selectionActive = selectionMode && selected
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = GymCard),
        border = BorderStroke(
            if (isRestTimerRunning.value || selectionActive) 2.dp else 1.dp,
            when {
                isRestTimerRunning.value -> restActiveColor
                selectionActive -> Color.White
                else -> GymBorder
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (selectionMode) {
                        onSelectedChange(!selected)
                    } else {
                        onClick()
                    }
                },
                onLongClick = onLongClick
            )
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (selectionMode) {
                    Checkbox(
                        checked = selected,
                        onCheckedChange = onSelectedChange,
                        modifier = Modifier.size(38.dp),
                        colors = CheckboxDefaults.colors(checkedColor = Color.White, uncheckedColor = GymMuted, checkmarkColor = Color.Black)
                    )
                }
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
                if (showActions) {
                    CompactIconButton(onClick = onInfoClick) {
                        Icon(Icons.Default.Info, contentDescription = "Exercise info", tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                }
            }

            if (showActions) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    CompactIconButton(enabled = canMoveUp, onClick = onMoveUp) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Move exercise up", tint = if (canMoveUp) Color.White else GymMuted.copy(alpha = 0.4f), modifier = Modifier.size(22.dp))
                    }
                    CompactIconButton(enabled = canMoveDown, onClick = onMoveDown) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = "Move exercise down", tint = if (canMoveDown) Color.White else GymMuted.copy(alpha = 0.4f), modifier = Modifier.size(22.dp))
                    }
                    CompactIconButton(onClick = onSwap) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = "Swap exercise", tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                    CompactIconButton(onClick = { viewModel.removeExercise(routineId, dayId, exercise.id) }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete exercise", tint = GymDanger, modifier = Modifier.size(24.dp))
                    }
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
                            .aspectRatio(1.72f)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    Icons.Default.Timer,
                    contentDescription = null,
                    tint = if (isRestTimerRunning.value) restActiveColor else GymMuted,
                    modifier = Modifier.size(17.dp)
                )
                Text("REST:", color = if (isRestTimerRunning.value) restActiveColor else GymMuted, fontSize = 15.sp)
                InlineEditText(
                    value = exercise.rest,
                    onValueChange = { input ->
                        if (isRestTimerRunning.value) return@InlineEditText
                        if (TIME_INPUT_PATTERN.matches(input)) {
                            viewModel.updateRest(routineId, dayId, exercise.id, input)
                        }
                    },
                    width = 70.dp,
                    placeholder = "00:00",
                    keyboardType = KeyboardType.Ascii,
                    textColor = if (isRestTimerRunning.value) restActiveColor else Color.White
                )
            }

            exercise.sets.forEach { set ->
                SetRow(
                    set = set,
                    onCompleted = { completed ->
                        if (isRestTimerRunning.value && completed && !set.completed) return@SetRow
                        viewModel.updateSet(routineId, dayId, exercise.id, set.id, completed = completed)
                        if (completed) {
                            startRestTimer()
                        } else {
                            cancelRestTimer()
                        }
                    },
                    checkboxEnabled = !isRestTimerRunning.value || set.completed,
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
                    modifier = Modifier.weight(1f),
                    width = null,
                    singleLine = false
                )
            }
        }
    }
}


