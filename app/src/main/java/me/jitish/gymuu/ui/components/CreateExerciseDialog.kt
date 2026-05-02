package me.jitish.gymuu.ui.components

import android.content.Context
import android.content.Intent
import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
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
import me.jitish.gymuu.ui.theme.GymBorder
import me.jitish.gymuu.ui.theme.GymCard
import me.jitish.gymuu.ui.theme.GymCardAlt
import me.jitish.gymuu.ui.theme.GymDanger
import me.jitish.gymuu.ui.theme.GymMuted
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
        ?: preferredCustomMediaMimeType(initialMediaUrl, null)
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
    val mediaPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val detectedMimeType = context.contentResolver.getType(uri)
        val candidateMimeType = preferredCustomMediaMimeType(uri.toString(), detectedMimeType)
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
        Surface(shape = RoundedCornerShape(28.dp), color = GymCard, border = BorderStroke(1.dp, GymBorder), modifier = Modifier.fillMaxWidth()) {
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
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("MEDIA", color = GymMuted, fontSize = 14.sp, letterSpacing = 1.sp)
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
                            border = BorderStroke(1.dp, GymBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(58.dp)
                                .clickable { mediaPicker.launch(arrayOf("image/*", "video/*")) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Default.AttachFile, contentDescription = null, tint = GymMuted)
                                Text(
                                    text = when {
                                        uploadedMediaUrl.isBlank() -> "CHOOSE FILE"
                                        else -> mediaAttachmentLabel(uploadedMediaUrl, uploadedMediaMimeType)
                                    },
                                    color = if (uploadedMediaUrl.isBlank()) GymMuted else Color.White,
                                    fontSize = 16.sp,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    } else {
                        GymInput(
                            label = "WEB LINK",
                            value = linkMediaUrl,
                            onValueChange = { value ->
                                linkMediaUrl = value
                                val mimeType = preferredCustomMediaMimeType(value, null)
                                linkMediaMimeType = mimeType.orEmpty()
                                mediaRemoved = false
                                mediaErrorMessage = webMediaLinkError(value, mimeType)
                            },
                            placeholder = "https://example.com/demo.mp4",
                            trailing = { Icon(Icons.Default.Link, contentDescription = null, tint = GymMuted) }
                        )
                    }
                    if (selectedMediaUrl.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.AttachFile, contentDescription = null, tint = GymMuted)
                            Text(
                                mediaAttachmentLabel(selectedMediaUrl, selectedMediaMimeType),
                                color = GymMuted,
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
                                Icon(Icons.Default.Close, contentDescription = null, tint = GymMuted)
                                Text("REMOVE", color = GymMuted, letterSpacing = 1.sp)
                            }
                        }
                    }
                    if (validatingMedia) {
                        Text("Checking video compatibility...", color = GymMuted, fontSize = 12.sp, lineHeight = 16.sp)
                    }
                    mediaErrorMessage?.let { message ->
                        Text(message, color = GymDanger, fontSize = 12.sp, lineHeight = 16.sp)
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
                    }) { Text("CONFIRM", color = if (mediaCanConfirm) Color.White else GymMuted, letterSpacing = 1.sp) }
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
        border = BorderStroke(1.dp, if (selected) Color.White else GymBorder),
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

private fun preferredCustomMediaMimeType(mediaUrl: String, detectedMimeType: String?): String? {
    detectedMimeType?.takeIf { it.startsWith("image/", ignoreCase = true) || it.startsWith("video/", ignoreCase = true) }?.let {
        return it
    }

    val path = mediaUrl.substringBefore('?').substringBefore('#').lowercase()
    return when {
        path.endsWith(".gif") -> "image/gif"
        path.endsWith(".jpg") || path.endsWith(".jpeg") -> "image/jpeg"
        path.endsWith(".png") -> "image/png"
        path.endsWith(".webp") -> "image/webp"
        path.endsWith(".mp4") || path.endsWith(".m4v") -> "video/mp4"
        path.endsWith(".mov") -> "video/quicktime"
        path.endsWith(".webm") -> "video/webm"
        path.endsWith(".3gp") || path.endsWith(".3gpp") -> "video/3gpp"
        path.endsWith(".mkv") -> "video/x-matroska"
        path.endsWith(".avi") -> "video/x-msvideo"
        else -> null
    }
}

private fun webMediaLinkError(mediaUrl: String, mimeType: String?): String? {
    if (mediaUrl.isBlank()) return null
    if (!mediaUrl.isWebMediaLink()) return "Use a direct http or https media link."
    if (!isMediaMimeType(mimeType)) return "Use a direct GIF, image, or video file link."
    return null
}

private fun pickedMediaTypeError(mimeType: String?): String? {
    return if (isMediaMimeType(mimeType)) null else "Choose a GIF, image, or video file."
}

private fun pickedMediaError(context: Context, uri: Uri, mimeType: String?): String? {
    pickedMediaTypeError(mimeType)?.let { return it }
    if (!mimeType.isVideoMimeType()) return null

    return pickedVideoError(context, uri)
}

private fun pickedVideoError(context: Context, uri: Uri): String? {
    val extractor = MediaExtractor()
    return try {
        extractor.setDataSource(context, uri, null)
        val formats = buildList {
            for (index in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(index)
                if (format.mediaMimeType()?.startsWith("video/", ignoreCase = true) == true) {
                    add(format)
                }
            }
        }

        when {
            formats.isEmpty() -> "Choose a video file with a visible video track."
            formats.none { it.hasReadableFrameSize() } -> "This video has no readable frame size. Choose another video."
            formats.none { it.hasSupportedDecoder() } -> "This video uses a codec this device cannot display. Choose another MP4/WebM video."
            else -> pickedVideoFrameError(context, uri)
        }
    } catch (_: RuntimeException) {
        "This video could not be read. Choose another video file."
    } finally {
        extractor.release()
    }
}

private fun pickedVideoFrameError(context: Context, uri: Uri): String? {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, uri)
        val hasVideo = retriever
            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO)
            ?.equals("yes", ignoreCase = true) == true
        if (!hasVideo) return "Choose a video file with a visible video track."

        val width = retriever.metadataIntValue(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
        val height = retriever.metadataIntValue(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
        if (width <= 0 || height <= 0) {
            return "This video has no readable frame size. Choose another video."
        }

        val frame = retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            ?: retriever.getFrameAtTime(-1L)
        val hasFrame = frame != null && frame.width > 0 && frame.height > 0
        frame?.recycle()

        if (hasFrame) {
            null
        } else {
            "This video's audio can be read, but Android could not decode a visible frame."
        }
    } catch (_: RuntimeException) {
        "This video could not be read. Choose another video file."
    } finally {
        retriever.release()
    }
}

private fun isMediaMimeType(mimeType: String?): Boolean {
    return mimeType?.startsWith("image/", ignoreCase = true) == true ||
        mimeType.isVideoMimeType()
}

private fun String?.isVideoMimeType(): Boolean {
    return this?.startsWith("video/", ignoreCase = true) == true
}

private fun MediaFormat.mediaMimeType(): String? {
    return if (containsKey(MediaFormat.KEY_MIME)) getString(MediaFormat.KEY_MIME) else null
}

private fun MediaFormat.hasReadableFrameSize(): Boolean {
    val width = intValueOrNull(MediaFormat.KEY_WIDTH) ?: 0
    val height = intValueOrNull(MediaFormat.KEY_HEIGHT) ?: 0
    return width > 0 && height > 0
}

private fun MediaFormat.hasSupportedDecoder(): Boolean {
    return runCatching {
        MediaCodecList(MediaCodecList.REGULAR_CODECS).findDecoderForFormat(this)
    }.getOrNull().isNullOrBlank().not()
}

private fun MediaFormat.intValueOrNull(key: String): Int? {
    if (!containsKey(key)) return null
    return runCatching { getInteger(key) }.getOrNull()
}

private fun MediaMetadataRetriever.metadataIntValue(keyCode: Int): Int {
    return extractMetadata(keyCode)?.toIntOrNull() ?: 0
}

private fun mediaAttachmentLabel(mediaUrl: String, mediaMimeType: String): String {
    val inferredMimeType = preferredCustomMediaMimeType(mediaUrl, null)
    return when {
        mediaMimeType.equals("image/gif", ignoreCase = true) || inferredMimeType.equals("image/gif", ignoreCase = true) -> "GIF SELECTED"
        mediaMimeType.startsWith("image/", ignoreCase = true) || inferredMimeType?.startsWith("image/") == true -> "IMAGE SELECTED"
        mediaMimeType.startsWith("video/", ignoreCase = true) || inferredMimeType?.startsWith("video/") == true -> "VIDEO SELECTED"
        mediaUrl.isWebMediaLink() -> "LINK SELECTED"
        else -> "MEDIA SELECTED"
    }
}

private fun String.isWebMediaLink(): Boolean {
    return startsWith("http://", ignoreCase = true) || startsWith("https://", ignoreCase = true)
}

private const val MEDIA_MODE_UPLOAD = "upload"
private const val MEDIA_MODE_LINK = "link"
