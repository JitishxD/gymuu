package me.jitish.gymuu.ui.exercise

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.jitish.gymuu.data.exercise.Exercise
import me.jitish.gymuu.data.routine.CustomExercise
import me.jitish.gymuu.ui.components.CompactIconButton
import me.jitish.gymuu.ui.components.toTitleCase
import me.jitish.gymuu.ui.theme.GymBorder
import me.jitish.gymuu.ui.theme.GymCard
import me.jitish.gymuu.ui.theme.GymDanger
import me.jitish.gymuu.ui.theme.GymMuted

@Composable
internal fun ExerciseListCard(exercise: Exercise, selected: Boolean, onClick: () -> Unit, onLongClick: (() -> Unit)? = null) {
    SelectableCard(selected = selected, onClick = onClick, onLongClick = onLongClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ExerciseMediaPreview(
                url = exercise.gifUrl,
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White),
                showVideoControls = false
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
internal fun CustomExerciseCard(exercise: CustomExercise, selected: Boolean, onClick: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit, onLongClick: (() -> Unit)? = null) {
    SelectableCard(selected = selected, onClick = onClick, onLongClick = onLongClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            exercise.mediaUrl?.takeIf { it.isNotBlank() }?.let { mediaUrl ->
                ExerciseMediaPreview(
                    url = mediaUrl,
                    mimeType = exercise.mediaMimeType,
                    modifier = Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White),
                    showVideoControls = false
                )
            }
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SelectableCard(selected: Boolean, onClick: () -> Unit, onLongClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = if (selected) Color(0xFF2B2B2B) else GymCard),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) Color.White else GymBorder)
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

