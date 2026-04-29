package me.jitish.gymuu.ui.routine

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.jitish.gymuu.ui.components.CompactIconButton
import me.jitish.gymuu.ui.theme.GymBorder
import me.jitish.gymuu.ui.theme.GymCard
import me.jitish.gymuu.ui.theme.GymDanger
import me.jitish.gymuu.ui.theme.GymMuted

@Composable
internal fun WorkoutBulkActionBar(
    selectionMode: Boolean,
    selectedCount: Int,
    exerciseCount: Int,
    copiedCount: Int,
    onSelectAll: () -> Unit,
    onCopy: () -> Unit,
    onPaste: () -> Unit,
    onDelete: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasExercises = exerciseCount > 0
    val hasSelection = selectedCount > 0
    val canSelectAll = selectionMode && hasExercises
    val canPaste = copiedCount > 0
    val statusText = when {
        hasSelection -> "$selectedCount SELECTED"
        canPaste -> "$copiedCount COPIED"
        else -> "$exerciseCount EXERCISES"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(GymCard)
            .border(BorderStroke(1.dp, GymBorder), RoundedCornerShape(8.dp))
            .padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = statusText,
            color = if (hasSelection || canPaste) Color.White else GymMuted,
            fontSize = 14.sp,
            letterSpacing = 1.sp,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        CompactIconButton(enabled = canSelectAll, onClick = onSelectAll) {
            Icon(Icons.Default.SelectAll, contentDescription = "Select all exercises", tint = actionTint(canSelectAll), modifier = Modifier.size(22.dp))
        }
        CompactIconButton(enabled = hasSelection, onClick = onCopy) {
            Icon(Icons.Default.ContentCopy, contentDescription = "Copy selected exercises", tint = actionTint(hasSelection), modifier = Modifier.size(22.dp))
        }
        CompactIconButton(enabled = canPaste, onClick = onPaste) {
            Icon(Icons.Default.ContentPaste, contentDescription = "Paste copied exercises", tint = actionTint(canPaste), modifier = Modifier.size(22.dp))
        }
        CompactIconButton(enabled = hasSelection, onClick = onDelete) {
            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete selected exercises", tint = if (hasSelection) GymDanger else GymMuted.copy(alpha = 0.4f), modifier = Modifier.size(22.dp))
        }
        CompactIconButton(onClick = onClear) {
            Icon(Icons.Default.Close, contentDescription = "Close bulk actions", tint = Color.White, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
internal fun PastePlacementDialog(
    anchor: PasteAnchor,
    copiedCount: Int,
    onDismiss: () -> Unit,
    onPasteBefore: () -> Unit,
    onPasteAfter: () -> Unit
) {
    val exerciseLabel = anchor.exerciseName.ifBlank { "this exercise" }
    val copiedLabel = if (copiedCount == 1) "1 copied exercise" else "$copiedCount copied exercises"

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GymCard,
        title = {
            Text("PASTE EXERCISES", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        },
        text = {
            Text(
                text = "Paste $copiedLabel before or after $exerciseLabel?",
                color = GymMuted,
                fontSize = 15.sp
            )
        },
        confirmButton = {
            TextButton(onClick = onPasteAfter) {
                Text("AFTER", color = Color.White)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onDismiss) {
                    Text("CANCEL", color = GymMuted)
                }
                TextButton(onClick = onPasteBefore) {
                    Text("BEFORE", color = Color.White)
                }
            }
        }
    )
}

private fun actionTint(enabled: Boolean): Color {
    return if (enabled) Color.White else GymMuted.copy(alpha = 0.4f)
}

