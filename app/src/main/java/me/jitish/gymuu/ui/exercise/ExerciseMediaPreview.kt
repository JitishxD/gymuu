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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.disk.DiskCache
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import coil3.memory.MemoryCache
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import coil3.video.VideoFrameDecoder
import coil3.video.videoFrameMillis
import me.jitish.gymuu.ui.media.isVideoMedia
import me.jitish.gymuu.ui.theme.GymuuMuted
import okio.Path.Companion.toOkioPath

@Composable
internal fun ExerciseMediaPreview(
    url: String?,
    modifier: Modifier = Modifier,
    mimeType: String? = null,
    showVideoControls: Boolean = true,
    resetKey: Any? = null,
    playWhenActive: Boolean = false,
    mediaActive: Boolean = true
) {
    when {
        url.isNullOrBlank() -> Box(modifier) { MediaPlaceholder() }
        isVideoMedia(url, mimeType) && !showVideoControls -> Box(modifier) {
            CoilImage(url = url, videoFrame = true, modifier = Modifier.matchParentSize())
            Icon(
                Icons.Default.PlayArrow,
                null,
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(28.dp)
            )
        }

        isVideoMedia(url, mimeType) -> CustomExerciseVideoPreview(
            url = url, modifier = modifier, loop = true, showControls = true,
            resetKey = resetKey, playWhenActive = playWhenActive, mediaActive = mediaActive
        )

        else -> CoilImage(url = url, videoFrame = false, modifier = modifier)
    }
}

@Composable
internal fun GifPreview(url: String?, modifier: Modifier = Modifier) {
    if (url.isNullOrBlank()) Box(modifier) { MediaPlaceholder() } else CoilImage(
        url,
        videoFrame = false,
        modifier = modifier
    )
}

@Composable
private fun CoilImage(url: String, videoFrame: Boolean, modifier: Modifier) {
    val context = LocalContext.current.applicationContext
    val loader = remember { SharedImageLoader.get(context) }
    var loading by remember(url) { mutableStateOf(true) }
    var failed by remember(url) { mutableStateOf(false) }
    val request = remember(url, videoFrame) {
        ImageRequest.Builder(context)
            .data(url)
            .crossfade(false)
            .allowHardware(!videoFrame && !url.endsWith(".gif", ignoreCase = true))
            .apply { if (videoFrame) videoFrameMillis(0) }
            .build()
    }
    Box(modifier) {
        AsyncImage(
            model = request,
            imageLoader = loader,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.matchParentSize(),
            onLoading = { loading = true; failed = false },
            onSuccess = { loading = false; failed = false },
            onError = { loading = false; failed = true }
        )
        if (loading) {
            Box(
                Modifier
                    .matchParentSize()
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = GymuuMuted,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        if (failed) MediaPlaceholder()
    }
}

@Composable
internal fun ExerciseMediaFullscreenDialog(url: String, mimeType: String?, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(Modifier
            .fillMaxSize()
            .background(Color.Black)) {
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
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close fullscreen",
                    tint = Color.White
                )
            }
        }
    }
}

private object SharedImageLoader {
    @Volatile
    private var loader: ImageLoader? = null

    fun get(context: Context): ImageLoader = loader ?: synchronized(this) {
        loader ?: ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) add(AnimatedImageDecoder.Factory()) else add(
                    GifDecoder.Factory()
                )
                add(VideoFrameDecoder.Factory())
            }
            .memoryCache { MemoryCache.Builder().maxSizePercent(context, 0.25).build() }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache").toOkioPath())
                    .maxSizeBytes(256L * 1024L * 1024L)
                    .build()
            }
            .build()
            .also { loader = it }
    }
}

@Composable
private fun MediaPlaceholder() {
    Box(Modifier
        .fillMaxSize()
        .background(Color.White), contentAlignment = Alignment.Center) {
        Icon(
            Icons.Default.FitnessCenter,
            contentDescription = null,
            tint = Color(0xFF333333),
            modifier = Modifier.size(32.dp)
        )
    }
}
