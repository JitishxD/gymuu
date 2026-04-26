package me.jitish.gymuu.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import me.jitish.gymuu.data.CreateExerciseDraft
import me.jitish.gymuu.data.CustomExercise
import me.jitish.gymuu.data.Routine
import me.jitish.gymuu.ui.theme.GymBlack
import me.jitish.gymuu.ui.theme.GymBorder
import me.jitish.gymuu.ui.theme.GymCard
import me.jitish.gymuu.ui.theme.GymCardAlt
import me.jitish.gymuu.ui.theme.GymDanger
import me.jitish.gymuu.ui.theme.GymMuted

@Composable
internal fun CreateExerciseDialog(initial: CustomExercise?, onDismiss: () -> Unit, onConfirm: (CreateExerciseDraft) -> Unit) {
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
internal fun GymInput(
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
internal fun NameDialog(title: String, initialValue: String, label: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
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
internal fun ConfirmDeleteDialog(title: String, text: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
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
internal fun WorkoutHeader(
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
internal fun TopTitleBar(title: String, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().height(56.dp), contentAlignment = Alignment.Center) {
        CompactIconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(32.dp))
        }
        Text(title, color = Color.White, fontSize = 24.sp, letterSpacing = 5.sp)
    }
}

@Composable
internal fun SearchBox(query: String, onQueryChange: (String) -> Unit) {
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
internal fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
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
internal fun RoutineRow(routine: Routine, onOpen: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
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
private fun ManageRoutinesAction(icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Icon(icon, contentDescription = null, tint = GymMuted, modifier = Modifier.size(22.dp))
        Text("Manage routines", color = Color(0xFFC8C8C8), fontSize = 20.sp)
    }
}

@Composable
internal fun RoutineDrawer(
    routines: List<Routine>,
    onManageRoutines: () -> Unit,
    onRoutineClick: (Routine) -> Unit
) {
    ModalDrawerSheet(drawerContainerColor = GymBlack, drawerContentColor = Color.White, modifier = Modifier.fillMaxHeight().widthIn(max = 320.dp)) {
        Column(modifier = Modifier.statusBarsPadding().padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            GymLogo()
            DividerLine()
            ManageRoutinesAction(icon = Icons.Default.Edit, onClick = onManageRoutines)
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
internal fun CompactIconButton(modifier: Modifier = Modifier, onClick: () -> Unit, content: @Composable () -> Unit) {
    IconButton(onClick = onClick, modifier = modifier.size(40.dp)) {
        content()
    }
}

@Composable
internal fun GymLogo() {
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
internal fun GymFab(onClick: () -> Unit) {
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
internal fun SectionHeading(text: String) {
    Text(text, color = GymMuted, fontSize = 16.sp, letterSpacing = 3.sp, fontWeight = FontWeight.Light)
}

@Composable
internal fun EmptyState(text: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(36.dp), contentAlignment = Alignment.Center) {
        Text(text, color = GymMuted, fontSize = 16.sp, textAlign = TextAlign.Center)
    }
}

@Composable
internal fun DividerLine() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White))
}

@Composable
internal fun VerticalRule() {
    Box(Modifier.height(48.dp).width(1.dp).background(GymBorder))
}

@Composable
internal fun InlineEditText(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 110.dp,
    placeholder: String = ""
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold),
        modifier = modifier.width(width),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        decorationBox = { inner ->
            Box {
                if (value.isBlank()) Text(placeholder, color = GymMuted, fontSize = 16.sp)
                inner()
            }
        }
    )
}

internal fun String.toTitleCase(): String {
    return split(" ").joinToString(" ") { word ->
        word.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase() else char.toString() }
    }
}
