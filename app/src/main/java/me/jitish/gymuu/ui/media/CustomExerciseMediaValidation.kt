package me.jitish.gymuu.ui.media

import android.content.Context
import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri

internal fun inferExerciseMediaMimeType(mediaUrl: String, detectedMimeType: String?): String? {
    detectedMimeType
        ?.takeIf { it.startsWith("image/", ignoreCase = true) || it.startsWith("video/", ignoreCase = true) }
        ?.let { return it }

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

internal fun isVideoMedia(mediaUrl: String, mimeType: String?): Boolean {
    if (mimeType.isVideoMimeType()) return true

    val path = mediaUrl.substringBefore('?').substringBefore('#').lowercase()
    return listOf(".mp4", ".m4v", ".mov", ".webm", ".3gp", ".3gpp", ".mkv", ".avi").any(path::endsWith)
}

internal fun webMediaLinkError(mediaUrl: String, mimeType: String?): String? {
    if (mediaUrl.isBlank()) return null
    if (!mediaUrl.isWebMediaLink()) return "Use a direct http or https media link."
    if (!isMediaMimeType(mimeType)) return "Use a direct GIF, image, or video file link."
    return null
}

internal fun pickedMediaTypeError(mimeType: String?): String? {
    return if (isMediaMimeType(mimeType)) null else "Choose a GIF, image, or video file."
}

internal fun pickedMediaError(context: Context, uri: Uri, mimeType: String?): String? {
    pickedMediaTypeError(mimeType)?.let { return it }
    if (!mimeType.isVideoMimeType()) return null

    return pickedVideoError(context, uri)
}

internal fun mediaAttachmentLabel(mediaUrl: String, mediaMimeType: String): String {
    val inferredMimeType = inferExerciseMediaMimeType(mediaUrl, null)
    return when {
        mediaMimeType.equals("image/gif", ignoreCase = true) ||
            inferredMimeType.equals("image/gif", ignoreCase = true) -> "GIF SELECTED"
        mediaMimeType.startsWith("image/", ignoreCase = true) ||
            inferredMimeType?.startsWith("image/") == true -> "IMAGE SELECTED"
        mediaMimeType.startsWith("video/", ignoreCase = true) ||
            inferredMimeType?.startsWith("video/") == true -> "VIDEO SELECTED"
        mediaUrl.isWebMediaLink() -> "LINK SELECTED"
        else -> "MEDIA SELECTED"
    }
}

internal fun String.isWebMediaLink(): Boolean {
    return startsWith("http://", ignoreCase = true) || startsWith("https://", ignoreCase = true)
}

internal fun String?.isVideoMimeType(): Boolean {
    return this?.startsWith("video/", ignoreCase = true) == true
}

private fun isMediaMimeType(mimeType: String?): Boolean {
    return mimeType?.startsWith("image/", ignoreCase = true) == true || mimeType.isVideoMimeType()
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
