package me.jitish.gymuu.ui.exercise

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.jitish.gymuu.data.routine.WorkoutSet
import me.jitish.gymuu.ui.components.CompactIconButton
import me.jitish.gymuu.ui.components.VerticalRule
import me.jitish.gymuu.ui.components.clearFocusOnKeyboardDismiss
import me.jitish.gymuu.ui.theme.GymDanger
import me.jitish.gymuu.ui.theme.GymMuted

@Composable
internal fun SetRow(
    set: WorkoutSet,
    onCompleted: (Boolean) -> Unit,
    onReps: (String) -> Unit,
    onWeight: (String) -> Unit,
    onRemove: () -> Unit,
    checkboxEnabled: Boolean = true
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Checkbox(
            checked = set.completed,
            onCheckedChange = onCompleted,
            enabled = checkboxEnabled,
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
private fun SetCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    editable: Boolean = true,
    onValueChange: (String) -> Unit = {}
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Text(label, color = GymMuted, fontSize = 13.sp, letterSpacing = 1.sp, maxLines = 1)
        if (editable) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                modifier = Modifier
                    .fillMaxWidth()
                    .clearFocusOnKeyboardDismiss(),
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
