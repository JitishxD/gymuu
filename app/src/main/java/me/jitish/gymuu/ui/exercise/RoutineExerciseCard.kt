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
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.jitish.gymuu.data.routine.ExerciseSource
import me.jitish.gymuu.data.routine.RoutineExercise
import me.jitish.gymuu.ui.AppViewModel
import me.jitish.gymuu.ui.RestTimerState
import me.jitish.gymuu.ui.components.CompactIconButton
import me.jitish.gymuu.ui.components.InlineEditText
import me.jitish.gymuu.ui.theme.GymuuBorder
import me.jitish.gymuu.ui.theme.GymuuBlack
import me.jitish.gymuu.ui.theme.GymuuCard
import me.jitish.gymuu.ui.theme.GymuuDanger
import me.jitish.gymuu.ui.theme.GymuuMuted

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun RoutineExerciseCard(
    index: Int,
    routineId: String,
    dayId: String,
    exercise: RoutineExercise,
    viewModel: AppViewModel,
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
    onMoveDown: () -> Unit,
    restTimer: RestTimerState? = null,
    restCompletionLocked: Boolean = false,
    onRestCompletionBlocked: () -> Unit = {},
    mediaResetKey: Any? = null,
    mediaActive: Boolean = false
) {
    val restActiveColor = Color(0xFF4CAF50)
    val isRestTimerRunning = restTimer != null
    val displayedRest = restTimer?.let { formatRestCountdown(it.remainingSeconds) } ?: exercise.rest
    val mediaUrl = exercise.gifUrl?.takeIf { it.isNotBlank() }
    var fullscreenMediaUrl by remember(exercise.id, mediaUrl) { mutableStateOf<String?>(null) }
    val cardMediaResetKey = mediaResetKey to (fullscreenMediaUrl != null)

    val selectionActive = selectionMode && selected
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = GymuuCard),
        border = BorderStroke(
            if (isRestTimerRunning || selectionActive) 2.dp else 1.dp,
            when {
                isRestTimerRunning -> restActiveColor
                selectionActive -> Color.White
                else -> GymuuBorder
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
                        colors = CheckboxDefaults.colors(checkedColor = Color.White, uncheckedColor = GymuuMuted, checkmarkColor = Color.Black)
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
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Move exercise up", tint = if (canMoveUp) Color.White else GymuuMuted.copy(alpha = 0.4f), modifier = Modifier.size(22.dp))
                    }
                    CompactIconButton(enabled = canMoveDown, onClick = onMoveDown) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = "Move exercise down", tint = if (canMoveDown) Color.White else GymuuMuted.copy(alpha = 0.4f), modifier = Modifier.size(22.dp))
                    }
                    CompactIconButton(onClick = onSwap) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = "Swap exercise", tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                    CompactIconButton(onClick = { viewModel.removeExercise(routineId, dayId, exercise.id) }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete exercise", tint = GymuuDanger, modifier = Modifier.size(24.dp))
                    }
                }
            }

            if (mediaUrl != null) {
                val customMedia = exercise.source == ExerciseSource.CUSTOM
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (customMedia) GymuuBlack else Color.White)
                        .padding(if (customMedia) 10.dp else 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ExerciseMediaPreview(
                        url = mediaUrl,
                        mimeType = exercise.mediaMimeType,
                        modifier = if (customMedia) {
                            Modifier
                                .fillMaxWidth(0.78f)
                                .aspectRatio(1f)
                        } else {
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(1.72f)
                        },
                        resetKey = cardMediaResetKey,
                        playWhenActive = customMedia,
                        mediaActive = mediaActive && fullscreenMediaUrl == null
                    )
                    if (showActions) {
                        CompactIconButton(
                            onClick = { fullscreenMediaUrl = mediaUrl },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
                        ) {
                            Icon(Icons.Default.OpenInFull, contentDescription = "Open media fullscreen", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isRestTimerRunning) restActiveColor.copy(alpha = 0.14f) else Color.Transparent)
                    .padding(
                        horizontal = if (isRestTimerRunning) 12.dp else 0.dp,
                        vertical = if (isRestTimerRunning) 10.dp else 0.dp
                    )
            ) {
                Icon(
                    Icons.Default.Timer,
                    contentDescription = null,
                    tint = if (isRestTimerRunning) restActiveColor else GymuuMuted,
                    modifier = Modifier.size(if (isRestTimerRunning) 24.dp else 18.dp)
                )
                Text("REST:", color = if (isRestTimerRunning) restActiveColor else GymuuMuted, fontSize = if (isRestTimerRunning) 16.sp else 15.sp)
                if (isRestTimerRunning) {
                    Text(
                        text = displayedRest,
                        color = restActiveColor,
                        fontSize = 24.sp,
                        lineHeight = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    InlineEditText(
                        value = displayedRest,
                        onValueChange = { input ->
                            if (TIME_INPUT_PATTERN.matches(input)) {
                                viewModel.updateRest(routineId, dayId, exercise.id, input)
                            }
                        },
                        width = 82.dp,
                        placeholder = "00:00",
                        keyboardType = KeyboardType.Ascii,
                        textColor = Color.White
                    )
                }
            }

            exercise.sets.forEach { set ->
                SetRow(
                    set = set,
                    onCompleted = { completed ->
                        if (restCompletionLocked && completed && !set.completed) {
                            onRestCompletionBlocked()
                            return@SetRow
                        }
                        viewModel.updateSet(routineId, dayId, exercise.id, set.id, completed = completed)
                        if (completed && !set.completed) {
                            viewModel.startRestTimer(routineId, dayId, exercise)
                        } else {
                            viewModel.cancelRestTimer(exercise.id)
                        }
                    },
                    checkboxEnabled = !restCompletionLocked || set.completed,
                    onBlockedCompletedClick = onRestCompletionBlocked,
                    onReps = { viewModel.updateSet(routineId, dayId, exercise.id, set.id, reps = it) },
                    onWeight = { viewModel.updateSet(routineId, dayId, exercise.id, set.id, weight = it) },
                    onRemove = { viewModel.removeSet(routineId, dayId, exercise.id, set.id) }
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                CompactIconButton(onClick = { viewModel.addSet(routineId, dayId, exercise.id) }) {
                    Icon(Icons.Default.AddCircleOutline, contentDescription = "Add set", tint = GymuuMuted, modifier = Modifier.size(22.dp))
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.NoteAlt, contentDescription = null, tint = GymuuMuted, modifier = Modifier.size(18.dp))
                Text("NOTES", color = GymuuMuted, fontSize = 15.sp)
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

    fullscreenMediaUrl?.let { url ->
        ExerciseMediaFullscreenDialog(
            url = url,
            mimeType = exercise.mediaMimeType,
            onDismiss = { fullscreenMediaUrl = null }
        )
    }
}


