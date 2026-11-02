package me.jitish.gymuu.data.media

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

/**
 * Owns the media files picked for custom exercises.
 *
 * A picker result is a short-lived `content://` grant that means nothing on another device (or
 * even on this one after a reinstall), so media that has to survive is copied here and
 * referenced by a `file://` URI. Remote http(s) links stay links and are never copied.
 */
internal class ExerciseMediaStore(private val context: Context) {

    private val directory: File
        get() = File(context.filesDir, DIRECTORY_NAME).apply { mkdirs() }

    /** Opens local or still-granted media for reading. Null for links and dead URIs. */
    fun open(mediaUrl: String): InputStream? = runCatching {
        when {
            mediaUrl.startsWith("content://") ->
                context.contentResolver.openInputStream(Uri.parse(mediaUrl))

            mediaUrl.startsWith("file://") || mediaUrl.startsWith("/") ->
                FileInputStream(fileFor(mediaUrl))

            else -> null
        }
    }.getOrNull()

    /**
     * Copies [input] into app storage and returns the `file://` URI to store on the exercise.
     * The extension is preserved so media type detection still works after a restore, when the
     * original file name is gone.
     */
    fun store(originalName: String?, mimeType: String?, input: InputStream): String? = runCatching {
        val file = File(directory, "${UUID.randomUUID()}${mediaExtension(originalName, mimeType)}")
        file.outputStream().use { output -> copyLimited(input, output) }
        Uri.fromFile(file).toString()
    }.getOrNull()

    /** Deletes a file this store created. Anything outside it is left alone. */
    fun delete(mediaUrl: String?) {
        val file = mediaUrl?.takeIf { it.startsWith("file://") || it.startsWith("/") }
            ?.let(::fileFor) ?: return
        if (file.parentFile == directory) file.delete()
    }

    private fun fileFor(mediaUrl: String): File =
        if (mediaUrl.startsWith("/")) File(mediaUrl) else File(Uri.parse(mediaUrl).path.orEmpty())

    private fun copyLimited(input: InputStream, output: OutputStream) {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        while (true) {
            val read = input.read(buffer)
            if (read < 0) return
            if (read == 0) continue
            total += read
            require(total <= MAX_MEDIA_BYTES) { "Media files must be smaller than 64 MB." }
            output.write(buffer, 0, read)
        }
    }

    private companion object {
        const val DIRECTORY_NAME = "exercise_media"
        const val MAX_MEDIA_BYTES = 64L * 1024L * 1024L
    }
}

/** File extension for a stored copy or backup entry: file name first, then MIME type. */
internal fun mediaExtension(originalName: String?, mimeType: String?): String {
    originalName?.substringAfterLast('.', "")
        ?.takeIf { it.length in 1..5 && it.all(Char::isLetterOrDigit) }
        ?.let { return ".${it.lowercase()}" }
    return when {
        mimeType.equals("image/gif", ignoreCase = true) -> ".gif"
        mimeType.equals("image/jpeg", ignoreCase = true) -> ".jpg"
        mimeType.equals("image/png", ignoreCase = true) -> ".png"
        mimeType.equals("image/webp", ignoreCase = true) -> ".webp"
        mimeType.equals("video/mp4", ignoreCase = true) -> ".mp4"
        mimeType.equals("video/webm", ignoreCase = true) -> ".webm"
        mimeType.equals("video/quicktime", ignoreCase = true) -> ".mov"
        mimeType?.contains("matroska", ignoreCase = true) == true -> ".mkv"
        else -> ""
    }
}
