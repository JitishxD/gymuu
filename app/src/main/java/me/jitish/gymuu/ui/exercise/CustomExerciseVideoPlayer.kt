package me.jitish.gymuu.ui.exercise

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

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
    Media3VideoPlayer(
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
    Media3VideoPlayer(
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
private fun Media3VideoPlayer(
    url: String,
    modifier: Modifier,
    loop: Boolean,
    controls: VideoControls,
    resetKey: Any?,
    playWhenActive: Boolean,
    mediaActive: Boolean
) {
    val context = LocalContext.current
    val appContext = remember(context) { context.applicationContext }
    val lifecycleOwner = LocalLifecycleOwner.current
    var muted by remember(url) { mutableStateOf(true) }
    var prepareRequested by remember(url, controls) { mutableStateOf(controls != VideoControls.MINIMAL) }
    var playOnReady by remember(url) { mutableStateOf(playWhenActive && mediaActive && controls != VideoControls.NONE) }
    var isReady by remember(url) { mutableStateOf(false) }
    var isPlaying by remember(url) { mutableStateOf(false) }
    var hasError by remember(url) { mutableStateOf(false) }
    var transientControlVisible by remember(url) { mutableStateOf(false) }
    var transientControlPulse by remember(url) { mutableIntStateOf(0) }

    val player = remember(appContext, url, prepareRequested) {
        if (!prepareRequested) {
            null
        } else {
            ExoPlayer.Builder(appContext).build().apply {
                setMediaItem(MediaItem.fromUri(Uri.parse(url)))
                repeatMode = if (loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
                volume = if (muted) 0f else 1f
                playWhenReady = playOnReady
                prepare()
            }
        }
    }

    fun showTransientControls() {
        if (controls == VideoControls.MINIMAL) {
            transientControlVisible = true
            transientControlPulse += 1
        }
    }

    fun togglePlayback() {
        if (!prepareRequested) {
            hasError = false
            prepareRequested = true
            playOnReady = true
            showTransientControls()
            return
        }

        val activePlayer = player
        if (activePlayer == null) {
            playOnReady = !playOnReady
            showTransientControls()
            return
        }

        runCatching {
            if (activePlayer.isPlaying || playOnReady) {
                activePlayer.pause()
                playOnReady = false
            } else {
                activePlayer.play()
                playOnReady = true
            }
            isPlaying = activePlayer.isPlaying
        }.onFailure {
            hasError = true
            playOnReady = false
            isPlaying = false
        }
        showTransientControls()
    }

    Box(
        modifier = modifier
            .background(Color.Black)
            .clickable(enabled = controls == VideoControls.MINIMAL) { togglePlayback() }
    ) {
        if (player == null) {
            VideoIdlePlaceholder()
        } else {
            PlayerViewSurface(
                player = player,
                showControls = controls == VideoControls.FULL,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (hasError) {
            VideoErrorPlaceholder()
        } else {
            if (prepareRequested && !isReady) {
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
                    val playing = isPlaying || playOnReady
                    Icon(
                        imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (playing) "Pause media" else "Play media",
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
                VideoVolumeButton(
                    muted = muted,
                    onClick = { muted = !muted },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                )
            }
        }
    }

    DisposableEffect(player) {
        if (player == null) {
            onDispose {}
        } else {
            val listener = object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    isReady = playbackState == Player.STATE_READY || playbackState == Player.STATE_ENDED
                    if (playbackState == Player.STATE_ENDED) {
                        isPlaying = false
                        playOnReady = false
                    }
                }

                override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                    isPlaying = isPlayingNow
                }

                override fun onPlayerError(error: PlaybackException) {
                    hasError = true
                    isReady = false
                    isPlaying = false
                    playOnReady = false
                }
            }
            player.addListener(listener)
            isReady = player.playbackState == Player.STATE_READY || player.playbackState == Player.STATE_ENDED
            isPlaying = player.isPlaying

            onDispose {
                player.removeListener(listener)
                player.release()
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

        hasError = false
        playOnReady = false
        isPlaying = false
        isReady = false
        transientControlVisible = false
        if (controls == VideoControls.MINIMAL) {
            prepareRequested = false
        } else {
            runCatching {
                player?.pause()
                player?.seekTo(0L)
            }
        }
    }

    LaunchedEffect(playWhenActive, mediaActive, controls, player) {
        if (!playWhenActive || controls == VideoControls.NONE) return@LaunchedEffect

        if (mediaActive) {
            hasError = false
            prepareRequested = true
            playOnReady = true
        } else {
            playOnReady = false
            transientControlVisible = false
            runCatching {
                player?.pause()
                player?.seekTo(0L)
            }
            isPlaying = false
        }
    }

    DisposableEffect(lifecycleOwner, player) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                runCatching { player?.pause() }
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

    LaunchedEffect(player, muted) {
        player?.volume = if (muted) 0f else 1f
    }

    LaunchedEffect(player, loop) {
        player?.repeatMode = if (loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }

    LaunchedEffect(player, playOnReady, hasError) {
        if (hasError) return@LaunchedEffect
        runCatching {
            if (playOnReady) {
                player?.play()
            } else {
                player?.pause()
            }
        }.onFailure {
            hasError = true
            isPlaying = false
            playOnReady = false
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun PlayerViewSurface(player: Player, showControls: Boolean, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { context ->
            PlayerView(context).apply {
                setBackgroundColor(android.graphics.Color.BLACK)
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                useController = showControls
                controllerAutoShow = showControls
                controllerHideOnTouch = showControls
                controllerShowTimeoutMs = if (showControls) 3000 else 0
                setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                this.player = player
            }
        },
        update = { view ->
            view.useController = showControls
            view.controllerAutoShow = showControls
            view.controllerHideOnTouch = showControls
            view.controllerShowTimeoutMs = if (showControls) 3000 else 0
            view.player = player
        },
        onReset = { view ->
            view.player = null
        },
        onRelease = { view ->
            view.player = null
        },
        modifier = modifier
    )
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
