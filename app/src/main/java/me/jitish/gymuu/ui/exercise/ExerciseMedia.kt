package me.jitish.gymuu.ui.exercise

import android.content.Context
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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

