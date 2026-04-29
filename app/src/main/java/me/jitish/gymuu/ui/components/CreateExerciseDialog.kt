package me.jitish.gymuu.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import me.jitish.gymuu.data.routine.CreateExerciseDraft
import me.jitish.gymuu.data.routine.CustomExercise
import me.jitish.gymuu.ui.theme.GymBorder
import me.jitish.gymuu.ui.theme.GymCard
import me.jitish.gymuu.ui.theme.GymCardAlt
import me.jitish.gymuu.ui.theme.GymMuted

@Composable
internal fun CreateExerciseDialog(initial: CustomExercise?, onDismiss: () -> Unit, onConfirm: (CreateExerciseDraft) -> Unit) {
    var name by rememberSaveable(initial?.id, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(textFieldValueAtEnd(initial?.name.orEmpty()))
    }
    var sets by rememberSaveable(initial?.id) { mutableIntStateOf(initial?.sets ?: 3) }
    var reps by rememberSaveable(initial?.id) { mutableStateOf(initial?.reps.orEmpty()) }
    var rest by rememberSaveable(initial?.id) { mutableStateOf(initial?.rest.orEmpty()) }
    var expanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = GymCard, border = BorderStroke(1.dp, GymBorder), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("CREATE EXERCISE", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                GymInput(label = "NAME", value = name, onValueChange = { name = it }, placeholder = "e.g. Bench Press", autoFocus = true)
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
                        onConfirm(CreateExerciseDraft(id = initial?.id, name = name.text, sets = sets, reps = reps, rest = rest))
                    }) { Text("CONFIRM", color = Color.White, letterSpacing = 1.sp) }
                }
            }
        }
    }
}
