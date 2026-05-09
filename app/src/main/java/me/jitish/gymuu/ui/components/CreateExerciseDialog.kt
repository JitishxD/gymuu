package me.jitish.gymuu.ui.components

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.UploadFile
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import me.jitish.gymuu.data.routine.CreateExerciseDraft
import me.jitish.gymuu.data.routine.CustomExercise
import me.jitish.gymuu.ui.media.inferExerciseMediaMimeType
import me.jitish.gymuu.ui.media.isVideoMimeType
import me.jitish.gymuu.ui.media.isWebMediaLink
import me.jitish.gymuu.ui.media.mediaAttachmentLabel
import me.jitish.gymuu.ui.media.pickedMediaError
import me.jitish.gymuu.ui.media.pickedMediaTypeError
import me.jitish.gymuu.ui.media.webMediaLinkError
import me.jitish.gymuu.ui.theme.GymuuBorder
import me.jitish.gymuu.ui.theme.GymuuCard
import me.jitish.gymuu.ui.theme.GymuuCardAlt
import me.jitish.gymuu.ui.theme.GymuuDanger
import me.jitish.gymuu.ui.theme.GymuuMuted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun CreateExerciseDialog(initial: CustomExercise?, onDismiss: () -> Unit, onConfirm: (CreateExerciseDraft) -> Unit) {
    val context = LocalContext.current
    var name by rememberSaveable(initial?.id, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(textFieldValueAtEnd(initial?.name.orEmpty()))
    }
    var sets by rememberSaveable(initial?.id) { mutableIntStateOf(initial?.sets ?: 3) }
    var reps by rememberSaveable(initial?.id) { mutableStateOf(initial?.reps.orEmpty()) }
    var rest by rememberSaveable(initial?.id) { mutableStateOf(initial?.rest.orEmpty()) }
    val initialMediaUrl = initial?.mediaUrl.orEmpty()
    val initialMediaMimeType = initial?.mediaMimeType
        ?: inferExerciseMediaMimeType(initialMediaUrl, null)
        ?: ""
    val initialMediaIsLink = initialMediaUrl.isWebMediaLink()
    var mediaMode by rememberSaveable(initial?.id) {
        mutableStateOf(if (initialMediaIsLink) MEDIA_MODE_LINK else MEDIA_MODE_UPLOAD)
    }
    var uploadedMediaUrl by rememberSaveable(initial?.id) { mutableStateOf(if (initialMediaIsLink) "" else initialMediaUrl) }
    var uploadedMediaMimeType by rememberSaveable(initial?.id) { mutableStateOf(if (initialMediaIsLink) "" else initialMediaMimeType) }
    var linkMediaUrl by rememberSaveable(initial?.id) { mutableStateOf(if (initialMediaIsLink) initialMediaUrl else "") }
    var linkMediaMimeType by rememberSaveable(initial?.id) { mutableStateOf(if (initialMediaIsLink) initialMediaMimeType else "") }
    var mediaRemoved by rememberSaveable(initial?.id) { mutableStateOf(false) }
    var mediaErrorMessage by rememberSaveable(initial?.id) { mutableStateOf<String?>(null) }
    var validatingMedia by rememberSaveable(initial?.id) { mutableStateOf(false) }
    var mediaValidationToken by remember { mutableIntStateOf(0) }
    var expanded by remember { mutableStateOf(false) }
    val mediaValidationScope = rememberCoroutineScope()
    val mediaPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val detectedMimeType = context.contentResolver.getType(uri)
        val candidateMimeType = inferExerciseMediaMimeType(uri.toString(), detectedMimeType)
        pickedMediaTypeError(candidateMimeType)?.let { message ->
            mediaErrorMessage = message
            validatingMedia = false
            return@rememberLauncherForActivityResult
        }

        mediaValidationToken += 1
        val validationToken = mediaValidationToken
        mediaErrorMessage = null
        validatingMedia = candidateMimeType.isVideoMimeType()
        mediaValidationScope.launch {
            val validationError = withContext(Dispatchers.IO) {
                pickedMediaError(context, uri, candidateMimeType)
            }
            if (validationToken != mediaValidationToken) {
                return@launch
            }
            validatingMedia = false
            if (validationError != null) {
                mediaErrorMessage = validationError
                return@launch
            }

            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            mediaMode = MEDIA_MODE_UPLOAD
            mediaRemoved = false
            mediaErrorMessage = null
            uploadedMediaUrl = uri.toString()
            uploadedMediaMimeType = candidateMimeType.orEmpty()
        }
    }
    val activeMediaUrl = if (mediaMode == MEDIA_MODE_LINK) linkMediaUrl else uploadedMediaUrl
    val activeMediaMimeType = if (mediaMode == MEDIA_MODE_LINK) linkMediaMimeType else uploadedMediaMimeType
    val selectedMediaUrl = activeMediaUrl.takeIf { it.isNotBlank() }
        ?: initialMediaUrl.takeIf { !mediaRemoved && it.isNotBlank() }
        ?: ""
    val selectedMediaMimeType = activeMediaMimeType.takeIf { it.isNotBlank() }
        ?: initialMediaMimeType.takeIf { selectedMediaUrl == initialMediaUrl }
        ?: ""
    val mediaCanConfirm = !validatingMedia &&
        mediaErrorMessage == null &&
        (selectedMediaUrl.isBlank() || selectedMediaMimeType.isNotBlank())

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = GymuuCard, border = BorderStroke(1.dp, GymuuBorder), modifier = Modifier.fillMaxWidth()) {
            Column(
                Modifier
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    if (initial == null) "CREATE EXERCISE" else "EDIT EXERCISE",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold
                )
                GymuuInput(label = "NAME", value = name, onValueChange = { name = it }, placeholder = "e.g. Bench Press", autoFocus = true)
                Column {
                    Text("SETS", color = GymuuMuted, fontSize = 14.sp, letterSpacing = 1.sp)
                    Box {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(58.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, GymuuBorder, RoundedCornerShape(10.dp))
                                .clickable { expanded = true }
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(sets.toString(), color = Color.White, fontSize = 20.sp, modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = GymuuMuted)
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(GymuuCardAlt)) {
                            (1..10).forEach { count ->
                                DropdownMenuItem(text = { Text(count.toString(), color = Color.White) }, onClick = {
                                    sets = count
                                    expanded = false
                                })
                            }
                        }
                    }
                }
                GymuuInput(
                    label = "REPS",
                    value = reps,
                    onValueChange = { reps = it },
                    placeholder = "e.g. 10",
                    trailing = { Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = GymuuMuted) },
                    helper = "Press <-> to enter a range (e.g. 10-12)."
                )
                GymuuInput(
                    label = "REST (MIN)",
                    value = rest,
                    onValueChange = { rest = it },
                    placeholder = "e.g. 1:30 or 2",
                    trailing = { Icon(Icons.Default.Timer, contentDescription = null, tint = GymuuMuted) }
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("MEDIA", color = GymuuMuted, fontSize = 14.sp, letterSpacing = 1.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        MediaModeButton(
                            label = "UPLOAD",
                            icon = Icons.Default.UploadFile,
                            selected = mediaMode == MEDIA_MODE_UPLOAD,
                            onClick = {
                                mediaMode = MEDIA_MODE_UPLOAD
                                mediaErrorMessage = null
                            },
                            modifier = Modifier.weight(1f)
                        )
                        MediaModeButton(
                            label = "LINK",
                            icon = Icons.Default.Link,
                            selected = mediaMode == MEDIA_MODE_LINK,
                            onClick = {
                                mediaMode = MEDIA_MODE_LINK
                                mediaErrorMessage = webMediaLinkError(linkMediaUrl, linkMediaMimeType.takeIf { it.isNotBlank() })
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (mediaMode == MEDIA_MODE_UPLOAD) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF151515),
                            border = BorderStroke(1.dp, GymuuBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(58.dp)
                                .clickable {
                                    mediaPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                    )
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Default.AttachFile, contentDescription = null, tint = GymuuMuted)
                                Text(
                                    text = when {
                                        uploadedMediaUrl.isBlank() -> "CHOOSE FILE"
                                        else -> mediaAttachmentLabel(uploadedMediaUrl, uploadedMediaMimeType)
                                    },
                                    color = if (uploadedMediaUrl.isBlank()) GymuuMuted else Color.White,
                                    fontSize = 16.sp,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    } else {
                        GymuuInput(
                            label = "WEB LINK",
                            value = linkMediaUrl,
                            onValueChange = { value ->
                                linkMediaUrl = value
                                val mimeType = inferExerciseMediaMimeType(value, null)
                                linkMediaMimeType = mimeType.orEmpty()
                                mediaRemoved = false
                                mediaErrorMessage = webMediaLinkError(value, mimeType)
                            },
                            placeholder = "https://example.com/demo.mp4",
                            trailing = { Icon(Icons.Default.Link, contentDescription = null, tint = GymuuMuted) }
                        )
                    }
                    if (selectedMediaUrl.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.AttachFile, contentDescription = null, tint = GymuuMuted)
                            Text(
                                mediaAttachmentLabel(selectedMediaUrl, selectedMediaMimeType),
                                color = GymuuMuted,
                                fontSize = 13.sp,
                                letterSpacing = 1.sp,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = {
                                if (mediaMode == MEDIA_MODE_LINK) {
                                    linkMediaUrl = ""
                                    linkMediaMimeType = ""
                                } else {
                                    uploadedMediaUrl = ""
                                    uploadedMediaMimeType = ""
                                }
                                mediaRemoved = true
                                mediaErrorMessage = null
                            }) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = GymuuMuted)
                                Text("REMOVE", color = GymuuMuted, letterSpacing = 1.sp)
                            }
                        }
                    }
                    if (validatingMedia) {
                        Text("Checking video compatibility...", color = GymuuMuted, fontSize = 12.sp, lineHeight = 16.sp)
                    }
                    mediaErrorMessage?.let { message ->
                        Text(message, color = GymuuDanger, fontSize = 12.sp, lineHeight = 16.sp)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("CANCEL", color = Color.White, letterSpacing = 1.sp) }
                    TextButton(enabled = mediaCanConfirm, onClick = {
                        onConfirm(
                            CreateExerciseDraft(
                                id = initial?.id,
                                name = name.text,
                                sets = sets,
                                reps = reps,
                                rest = rest,
                                mediaUrl = selectedMediaUrl.trim().takeIf { it.isNotBlank() },
                                mediaMimeType = selectedMediaMimeType.trim().takeIf { it.isNotBlank() }
                            )
                        )
                    }) { Text("CONFIRM", color = if (mediaCanConfirm) Color.White else GymuuMuted, letterSpacing = 1.sp) }
                }
            }
        }
    }
}

@Composable
private fun MediaModeButton(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (selected) Color.White else Color(0xFF151515),
        border = BorderStroke(1.dp, if (selected) Color.White else GymuuBorder),
        modifier = modifier
            .height(46.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = if (selected) Color.Black else Color.White)
            Text(label, color = if (selected) Color.Black else Color.White, fontSize = 13.sp, letterSpacing = 1.sp)
        }
    }
}

private const val MEDIA_MODE_UPLOAD = "upload"
private const val MEDIA_MODE_LINK = "link"
