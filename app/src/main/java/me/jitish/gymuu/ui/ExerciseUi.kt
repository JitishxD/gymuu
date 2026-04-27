package me.jitish.gymuu.ui

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.core.content.ContextCompat
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import coil.ImageLoader
import coil.compose.SubcomposeAsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.ImageRequest
import me.jitish.gymuu.data.CustomExercise
import me.jitish.gymuu.data.Exercise
import me.jitish.gymuu.data.RoutineExercise
import me.jitish.gymuu.data.WorkoutSet
import me.jitish.gymuu.ui.theme.GymBorder
import me.jitish.gymuu.ui.theme.GymCard
import me.jitish.gymuu.ui.theme.GymDanger
import me.jitish.gymuu.ui.theme.GymMuted
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun RoutineExerciseCard(
    index: Int,
    routineId: String,
    dayId: String,
    exercise: RoutineExercise,
    viewModel: GymViewModel,
    onInfoClick: () -> Unit,
    onSwap: () -> Unit
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

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = GymCard),
        border = BorderStroke(
            if (isRestTimerRunning.value) 2.dp else 1.dp,
            if (isRestTimerRunning.value) restActiveColor else GymBorder
        ),
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
                CompactIconButton(onClick = onInfoClick) {
                    Icon(Icons.Default.Info, contentDescription = "Exercise info", tint = Color.White, modifier = Modifier.size(22.dp))
                }
                CompactIconButton(onClick = onSwap) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = "Swap exercise", tint = Color.White, modifier = Modifier.size(22.dp))
                }
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
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
internal fun ExerciseInfoDialog(exercise: Exercise, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GymCard,
        title = {
            SelectionContainer {
                Text(exercise.name.toTitleCase(), color = Color.White, fontSize = 22.sp)
            }
        },
        text = {
            SelectionContainer {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ExerciseInfoLine(label = "Body Parts", value = exercise.bodyParts.joinToString(", "))
                    ExerciseInfoLine(label = "Equipment", value = exercise.equipments.joinToString(", "))
                    ExerciseInfoLine(label = "Target Muscles", value = exercise.targetMuscles.joinToString(", "))
                    ExerciseInfoLine(label = "Secondary", value = exercise.secondaryMuscles.joinToString(", "))
                    Text("INSTRUCTIONS", color = GymMuted, fontSize = 13.sp, letterSpacing = 1.sp)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (exercise.instructions.isEmpty()) {
                            Text("No instructions available.", color = GymMuted, fontSize = 14.sp)
                        } else {
                            exercise.instructions.forEachIndexed { index, step ->
                                Text("${index + 1}. $step", color = Color.White, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE", color = Color.White)
            }
        }
    )
}

@Composable
private fun ExerciseInfoLine(label: String, value: String) {
    val text = value.ifBlank { "-" }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label.uppercase(), color = GymMuted, fontSize = 13.sp, letterSpacing = 1.sp)
        Text(text, color = Color.White, fontSize = 14.sp)
    }
}

@Composable
private fun SetRow(
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
internal fun ExerciseListCard(exercise: Exercise, selected: Boolean, onClick: () -> Unit) {
    SelectableCard(selected = selected, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GifPreview(
                url = exercise.gifUrl,
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White)
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
internal fun CustomExerciseCard(exercise: CustomExercise, selected: Boolean, onClick: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
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
private fun GifPreview(url: String?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    if (url.isNullOrBlank()) {
        Box(modifier = modifier) {
            MediaPlaceholder()
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
            Box(Modifier.fillMaxSize().background(Color.White), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GymMuted, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
            }
        },
        error = { MediaPlaceholder() }
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

private const val MAX_REST_SECONDS = 5999 // 99:59
private const val DEFAULT_REST_SECONDS = 120  // 2:00

private fun parseRestTimeToSeconds(restValue: String): Int {
    val normalized = restValue.trim()
    if (normalized.isBlank()) return 0

    val match = Regex("""^(\d+):(\d{1,2})$""").matchEntire(normalized)
    if (match != null) {
        val minutes = match.groupValues[1].toIntOrNull() ?: return DEFAULT_REST_SECONDS
        val seconds = match.groupValues[2].toIntOrNull() ?: return DEFAULT_REST_SECONDS
        if (seconds > 59) return DEFAULT_REST_SECONDS
        return (minutes * 60 + seconds).coerceIn(1, MAX_REST_SECONDS)
    }

    // Bare number – treat as minutes (e.g. "2" → 2:00)
    val bareMinutes = normalized.toIntOrNull()
    if (bareMinutes != null && bareMinutes > 0) {
        return (bareMinutes * 60).coerceIn(1, MAX_REST_SECONDS)
    }

    return DEFAULT_REST_SECONDS
}

private val TIME_INPUT_PATTERN = Regex("""^\d{0,4}(:\d{0,2})?$""")

private fun formatRestCountdown(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

private fun triggerRestCompleteVibration(context: Context) {
    if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.VIBRATE) != PackageManager.PERMISSION_GRANTED) {
        return
    }

    val vibrationPattern = longArrayOf(
        0,    // start immediately
        300,  // vibrate
        150,  // pause
        300,  // vibrate
        150,  // pause
        500   // final stronger/longer vibrate
    )

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(VibratorManager::class.java) ?: return
        val vibrator = vibratorManager.defaultVibrator
        if (!vibrator.hasVibrator()) return

        vibrator.vibrate(
            VibrationEffect.createWaveform(vibrationPattern, -1)
        )
        return
    }

    @Suppress("DEPRECATION")
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
    if (!vibrator.hasVibrator()) return

    vibrator.vibrate(
        VibrationEffect.createWaveform(vibrationPattern, -1)
    )
}

@Composable
private fun MediaPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = Color(0xFF333333), modifier = Modifier.size(32.dp))
    }
}
