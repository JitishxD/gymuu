package me.jitish.gymuu.ui.exercise

import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface
import android.view.TextureView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import me.jitish.gymuu.ui.theme.GymMuted

@Composable
internal fun CustomExerciseVideoPreview(
    url: String,
    modifier: Modifier,
    loop: Boolean,
    showControls: Boolean,
    resetKey: Any?,
    playWhenActive: Boolean,
    mediaActive: Boolean,
) {
    TextureVideoPlayer(
        url = url,
        modifier = modifier,
        loop = loop,
        controls = if (showControls) VideoControls.MINIMAL else VideoControls.NONE,
        resetKey = resetKey,
        playWhenActive = playWhenActive,
        mediaActive = mediaActive
    )
}

@Composable
internal fun CustomExerciseFullscreenVideoPlayer(url: String, modifier: Modifier = Modifier) {
    TextureVideoPlayer(
        url = url,
        modifier = modifier,
        loop = false,
        controls = VideoControls.FULL,
        resetKey = null,
        playWhenActive = true,
        mediaActive = true
    )
}

@Composable
private fun TextureVideoPlayer(
    url: String,
    modifier: Modifier,
    loop: Boolean,
    controls: VideoControls,
    resetKey: Any?,
    playWhenActive: Boolean,
    mediaActive: Boolean
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var mediaPlayer by remember(url) { mutableStateOf<MediaPlayer?>(null) }
    var surface by remember(url) { mutableStateOf<Surface?>(null) }
    var isPrepared by remember(url) { mutableStateOf(false) }
    var isPlaying by remember(url) { mutableStateOf(false) }
    var hasError by remember(url) { mutableStateOf(false) }
    var durationMs by remember(url) { mutableIntStateOf(0) }
    var positionMs by remember(url) { mutableIntStateOf(0) }
    var isScrubbing by remember(url) { mutableStateOf(false) }
    var playOnReady by remember(url) { mutableStateOf(false) }
    var muted by remember(url) { mutableStateOf(true) }
    var prepareRequested by remember(url, controls) { mutableStateOf(controls != VideoControls.MINIMAL) }
    var videoAspectRatio by remember(url) { mutableStateOf<Float?>(null) }
    var transientControlVisible by remember(url) { mutableStateOf(false) }
    var transientControlPulse by remember(url) { mutableIntStateOf(0) }

    fun togglePlayback() {
        if (!prepareRequested) {
            prepareRequested = true
            playOnReady = true
            return
        }

        val player = mediaPlayer
        if (player == null || !isPrepared) {
            playOnReady = !playOnReady
            return
        }

        runCatching {
            if (player.isPlaying) {
                player.pause()
                playOnReady = false
            } else {
                player.start()
                playOnReady = true
            }
            isPlaying = player.isPlaying
        }.onFailure {
            hasError = true
        }
    }

    Box(
        modifier = modifier
            .background(Color.Black)
            .clickable(enabled = controls == VideoControls.MINIMAL) {
                togglePlayback()
                transientControlVisible = true
                transientControlPulse += 1
            }
    ) {
        if (prepareRequested) {
            AndroidView(
                factory = { context ->
                    TextureView(context).apply {
                        surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                            override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
                                surface = Surface(texture)
                            }

                            override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {
                                applyVideoFitTransform(videoAspectRatio)
                            }

                            override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
                                mediaPlayer?.setSurface(null)
                                surface?.release()
                                surface = null
                                return true
                            }

                            override fun onSurfaceTextureUpdated(texture: SurfaceTexture) = Unit
                        }
                    }
                },
                update = { textureView ->
                    textureView.applyVideoFitTransform(videoAspectRatio)
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            VideoIdlePlaceholder()
        }

        if (hasError) {
            VideoErrorPlaceholder()
        } else {
            if (prepareRequested && !isPrepared) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(28.dp)
                )
            }

            if (controls == VideoControls.MINIMAL && transientControlVisible) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(62.dp)
                        .background(Color.Black.copy(alpha = 0.58f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying || playOnReady) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying || playOnReady) "Pause media" else "Play media",
                        tint = Color.White
                    )
                }
            }

            if (controls == VideoControls.MINIMAL) {
                VideoVolumeButton(
                    muted = muted,
                    onClick = { muted = !muted },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                )
            } else if (controls == VideoControls.FULL) {
                FullVideoControls(
                    durationMs = durationMs,
                    positionMs = positionMs,
                    isPlaying = isPlaying,
                    playOnReady = playOnReady,
                    muted = muted,
                    onPlayPause = ::togglePlayback,
                    onMutedChange = { muted = it },
                    onPositionChange = { value ->
                        isScrubbing = true
                        positionMs = value
                    },
                    onSeekFinished = {
                        mediaPlayer?.seekTo(positionMs)
                        isScrubbing = false
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }

    LaunchedEffect(transientControlPulse, controls) {
        if (controls == VideoControls.MINIMAL && transientControlVisible) {
            delay(850)
            transientControlVisible = false
        }
    }

    LaunchedEffect(resetKey) {
        if (resetKey == null) return@LaunchedEffect

        val player = mediaPlayer
        if (controls == VideoControls.MINIMAL) {
            prepareRequested = false
        }
        hasError = false
        videoAspectRatio = null
        playOnReady = false
        transientControlVisible = false
        runCatching {
            if (isPrepared && player != null) {
                if (player.isPlaying) {
                    player.pause()
                }
                player.seekToFirstFrame()
            }
        }
        isPlaying = false
        positionMs = 0
    }

    LaunchedEffect(playWhenActive, mediaActive, mediaPlayer, isPrepared, controls) {
        if (!playWhenActive || controls == VideoControls.NONE) return@LaunchedEffect

        val player = mediaPlayer
        if (mediaActive) {
            prepareRequested = true
            playOnReady = true
            runCatching {
                if (player != null && isPrepared && !player.isPlaying) {
                    player.start()
                }
                isPlaying = player?.isPlaying == true
            }.onFailure {
                hasError = true
            }
        } else {
            playOnReady = false
            transientControlVisible = false
            runCatching {
                if (player != null && isPrepared) {
                    if (player.isPlaying) {
                        player.pause()
                    }
                    player.seekToFirstFrame()
                }
            }
            isPlaying = false
            positionMs = 0
        }
    }

    DisposableEffect(lifecycleOwner, mediaPlayer, isPrepared) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                runCatching {
                    if (isPrepared && mediaPlayer?.isPlaying == true) {
                        mediaPlayer?.pause()
                    }
                }
                isPlaying = false
                playOnReady = false
                transientControlVisible = false
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(mediaPlayer, muted) {
        mediaPlayer?.applyMutedVolume(muted)
    }

    DisposableEffect(url, surface, prepareRequested) {
        val activeSurface = surface
        if (!prepareRequested || activeSurface == null) {
            onDispose {}
        } else {
            val player = MediaPlayer()
            mediaPlayer = player
            hasError = false
            isPrepared = false
            durationMs = 0
            positionMs = 0
            isPlaying = false

            runCatching {
                player.setDataSource(context, Uri.parse(url))
                player.setSurface(activeSurface)
                player.applyMutedVolume(muted)
                player.isLooping = loop
                player.setOnVideoSizeChangedListener { _, width, height ->
                    safeAspectRatio(width, height)?.let { ratio ->
                        videoAspectRatio = ratio
                    }
                }
                player.setOnPreparedListener { preparedPlayer ->
                    durationMs = preparedPlayer.duration.coerceAtLeast(0)
                    preparedPlayer.applyMutedVolume(muted)
                    preparedPlayer.safeVideoAspectRatio()?.let { ratio ->
                        videoAspectRatio = ratio
                    }
                    isPrepared = true
                    if (playOnReady) {
                        preparedPlayer.start()
                    } else {
                        preparedPlayer.seekToFirstFrame()
                    }
                    isPlaying = preparedPlayer.isPlaying
                }
                player.setOnCompletionListener {
                    isPlaying = false
                    playOnReady = false
                    positionMs = durationMs
                }
                player.setOnErrorListener { _, _, _ ->
                    hasError = true
                    isPrepared = false
                    playOnReady = false
                    true
                }
                player.prepareAsync()
            }.onFailure {
                hasError = true
                playOnReady = false
                runCatching { player.release() }
                if (mediaPlayer === player) mediaPlayer = null
            }

            onDispose {
                runCatching {
                    player.setOnPreparedListener(null)
                    player.setOnVideoSizeChangedListener(null)
                    player.setOnCompletionListener(null)
                    player.setOnErrorListener(null)
                    player.setSurface(null)
                    player.release()
                }
                if (mediaPlayer === player) mediaPlayer = null
            }
        }
    }

    LaunchedEffect(mediaPlayer, isPrepared, isScrubbing, controls) {
        if (controls != VideoControls.FULL) return@LaunchedEffect

        while (true) {
            val player = mediaPlayer
            if (player != null && isPrepared && !isScrubbing) {
                runCatching {
                    durationMs = player.duration.coerceAtLeast(0)
                    positionMs = player.currentPosition.coerceAtLeast(0)
                    isPlaying = player.isPlaying
                }.onFailure {
                    hasError = true
                }
            }
            delay(300)
        }
    }

    LaunchedEffect(isPrepared, isPlaying, videoAspectRatio) {
        if (isPrepared && isPlaying && videoAspectRatio == null) {
            delay(700)
            if (mediaPlayer?.isPlaying == true && videoAspectRatio == null) {
                runCatching { mediaPlayer?.pause() }
                isPlaying = false
                playOnReady = false
                hasError = true
            }
        }
    }
}

@Composable
private fun FullVideoControls(
    durationMs: Int,
    positionMs: Int,
    isPlaying: Boolean,
    playOnReady: Boolean,
    muted: Boolean,
    onPlayPause: () -> Unit,
    onMutedChange: (Boolean) -> Unit,
    onPositionChange: (Int) -> Unit,
    onSeekFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.72f))
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val sliderMax = durationMs.coerceAtLeast(1).toFloat()
        Slider(
            value = positionMs.coerceIn(0, sliderMax.toInt()).toFloat(),
            onValueChange = { value -> onPositionChange(value.toInt()) },
            onValueChangeFinished = onSeekFinished,
            valueRange = 0f..sliderMax,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = GymMuted
            )
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconButton(onClick = onPlayPause) {
                Icon(
                    imageVector = if (isPlaying || playOnReady) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying || playOnReady) "Pause media" else "Play media",
                    tint = Color.White
                )
            }
            VideoVolumeButton(muted = muted, onClick = { onMutedChange(!muted) })
            Text(formatPlaybackTime(positionMs), color = Color.White, fontSize = 13.sp)
            Spacer(Modifier.weight(1f))
            Text("-${formatPlaybackTime((durationMs - positionMs).coerceAtLeast(0))}", color = GymMuted, fontSize = 13.sp)
            Text(formatPlaybackTime(durationMs), color = Color.White, fontSize = 13.sp)
        }
    }
}

@Composable
private fun VideoVolumeButton(muted: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(42.dp)
            .background(Color.Black.copy(alpha = 0.58f), CircleShape)
    ) {
        Icon(
            imageVector = if (muted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
            contentDescription = if (muted) "Unmute media" else "Mute media",
            tint = Color.White,
            modifier = Modifier.size(22.dp)
        )
    }
}

private enum class VideoControls {
    NONE,
    MINIMAL,
    FULL
}

private fun MediaPlayer.safeVideoAspectRatio(): Float? {
    return safeAspectRatio(videoWidth, videoHeight)
}

private fun MediaPlayer.applyMutedVolume(muted: Boolean) {
    val volume = if (muted) 0f else 1f
    runCatching {
        setVolume(volume, volume)
    }
}

private fun safeAspectRatio(width: Int, height: Int): Float? {
    if (width <= 0 || height <= 0) return null
    return width.toFloat() / height.toFloat()
}

private fun TextureView.applyVideoFitTransform(videoAspectRatio: Float?) {
    val ratio = videoAspectRatio ?: return
    val viewWidth = width.toFloat()
    val viewHeight = height.toFloat()
    if (viewWidth <= 0f || viewHeight <= 0f) return

    val viewAspectRatio = viewWidth / viewHeight
    val (scaleX, scaleY) = if (ratio > viewAspectRatio) {
        1f to (viewAspectRatio / ratio)
    } else {
        (ratio / viewAspectRatio) to 1f
    }

    setTransform(
        Matrix().apply {
            postScale(scaleX, scaleY, viewWidth / 2f, viewHeight / 2f)
        }
    )
}

private fun MediaPlayer.seekToFirstFrame() {
    runCatching {
        seekTo(1L, MediaPlayer.SEEK_CLOSEST_SYNC)
    }
}

private fun formatPlaybackTime(milliseconds: Int): String {
    val totalSeconds = (milliseconds / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    val paddedSeconds = seconds.toString().padStart(2, '0')

    return if (hours > 0) {
        "$hours:${minutes.toString().padStart(2, '0')}:$paddedSeconds"
    } else {
        "$minutes:$paddedSeconds"
    }
}

@Composable
private fun VideoIdlePlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White.copy(alpha = 0.86f), modifier = Modifier.size(48.dp))
    }
}

@Composable
private fun VideoErrorPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = Color.White.copy(alpha = 0.72f), modifier = Modifier.size(34.dp))
    }
}
