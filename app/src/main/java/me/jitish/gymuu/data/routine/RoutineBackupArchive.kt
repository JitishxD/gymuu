package me.jitish.gymuu.data.routine

import me.jitish.gymuu.data.media.ExerciseMediaStore
import me.jitish.gymuu.data.media.mediaExtension
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Container for a full backup. A `.gymuu` file is a ZIP archive:
 *
 *  - `state.json`      the versioned routines + custom-exercise payload (unchanged format)
 *  - `media.json`      maps every media reference in the payload to its archive entry
 *  - `media/<n>.<ext>` the bytes of each media file that was readable at export time
 *
 * Media entries are extracted through [ExerciseMediaStore], so an import always ends up with
 * files this device can actually open instead of the exporter's dead `content://` URIs.
 */
internal class RoutineBackupArchive(private val mediaStore: ExerciseMediaStore) {

    class Contents(
        val stateJson: String,
        /** Media reference stored in the payload -> copy now living on this device. */
        val mediaUrls: Map<String, String>
    )

    /** A media file that should travel with the backup. [open] returning null skips it. */
    class MediaFile(
        val reference: String,
        val mimeType: String?,
        val open: () -> InputStream?
    )

    fun write(output: OutputStream, stateJson: String, media: List<MediaFile>) {
        val manifest = JSONObject()
        ZipOutputStream(BufferedOutputStream(output)).use { zip ->
            zip.writeEntry(STATE_ENTRY, stateJson)
            media.forEachIndexed { index, file ->
                val input = runCatching { file.open() }.getOrNull() ?: return@forEachIndexed
                val entryName = "$MEDIA_ENTRY_PREFIX$index${mediaExtension(file.reference, file.mimeType)}"
                zip.putNextEntry(ZipEntry(entryName))
                input.use { stream -> stream.copyTo(zip) }
                zip.closeEntry()
                manifest.put(
                    file.reference,
                    JSONObject().put("entry", entryName).put("mimeType", file.mimeType.orEmpty())
                )
            }
            // Written last: an archive that lost its tail has no manifest, so no media restore.
            zip.writeEntry(MANIFEST_ENTRY, manifest.toString())
        }
    }

    fun read(input: InputStream): Contents? {
        var stateJson: String? = null
        var manifest = JSONObject()
        val extracted = HashMap<String, String>()

        val valid = runCatching {
            ZipInputStream(BufferedInputStream(input)).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    when {
                        entry.isDirectory -> Unit
                        entry.name == STATE_ENTRY ->
                            stateJson = zip.readBytes().toString(Charsets.UTF_8)

                        entry.name == MANIFEST_ENTRY ->
                            manifest = JSONObject(zip.readBytes().toString(Charsets.UTF_8))

                        entry.name.startsWith(MEDIA_ENTRY_PREFIX) ->
                            mediaStore.store(entry.name, null, zip)
                                ?.let { extracted[entry.name] = it }
                    }
                    zip.closeEntry()
                }
            }
        }.isSuccess

        val payload = stateJson.takeIf { valid }
        if (payload == null) {
            // Not a .gymuu archive (or truncated): reject it, leaving no extracted files behind.
            extracted.values.forEach(mediaStore::delete)
            return null
        }

        val mediaUrls = HashMap<String, String>()
        manifest.keys().forEach { reference ->
            val entry = manifest.optJSONObject(reference)?.optString("entry").orEmpty()
            extracted.remove(entry)?.let { mediaUrls[reference] = it }
        }
        // Entries no exercise refers to were never part of the backup's state.
        extracted.values.forEach(mediaStore::delete)
        return Contents(payload, mediaUrls)
    }

    private fun ZipOutputStream.writeEntry(name: String, content: String) {
        putNextEntry(ZipEntry(name))
        write(content.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private companion object {
        const val STATE_ENTRY = "state.json"
        const val MANIFEST_ENTRY = "media.json"
        const val MEDIA_ENTRY_PREFIX = "media/"
    }
}
