package me.jitish.gymuu.ui.exercise

import android.content.Context
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.ImageLoader
import coil.compose.SubcomposeAsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.ImageRequest
import me.jitish.gymuu.ui.theme.GymMuted

@Composable
internal fun ExerciseMediaPreview(
    url: String?,
    mimeType: String? = null,
    modifier: Modifier = Modifier,
    showVideoControls: Boolean = true,
    resetKey: Any? = null,
    playWhenActive: Boolean = false,
    mediaActive: Boolean = true
) {
    if (url.isNullOrBlank()) {
        Box(modifier = modifier) {
            MediaPlaceholder()
        }
        return
    }

    if (isVideoMedia(url, mimeType)) {
        CustomExerciseVideoPreview(
            url = url,
            modifier = modifier,
            loop = true,
            showControls = showVideoControls,
            resetKey = resetKey,
            playWhenActive = playWhenActive,
            mediaActive = mediaActive
        )
    } else {
        GifPreview(url = url, modifier = modifier)
    }
}

@Composable
internal fun GifPreview(url: String?, modifier: Modifier = Modifier) {
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

@Composable
internal fun ExerciseMediaFullscreenDialog(url: String, mimeType: String?, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            if (isVideoMedia(url, mimeType)) {
                CustomExerciseFullscreenVideoPlayer(url = url, modifier = Modifier.fillMaxSize())
            } else {
                GifPreview(url = url, modifier = Modifier.fillMaxSize())
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(10.dp)
                    .background(Color.Black.copy(alpha = 0.55f))
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close fullscreen", tint = Color.White)
            }
        }
    }
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

private fun isVideoMedia(url: String, mimeType: String?): Boolean {
    if (mimeType?.startsWith("video/", ignoreCase = true) == true) return true

    val path = url.substringBefore('?').substringBefore('#').lowercase()
    return listOf(".mp4", ".m4v", ".mov", ".webm", ".3gp", ".3gpp", ".mkv", ".avi").any(path::endsWith)
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
