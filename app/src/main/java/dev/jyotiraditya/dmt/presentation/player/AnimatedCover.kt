package dev.jyotiraditya.dmt.presentation.player

import androidx.annotation.OptIn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.PlayerSurface

private const val COVER_FADE_MS = 320
private const val COVER_VIDEO_MAX = 640

@OptIn(UnstableApi::class)
@Composable
fun AnimatedCover(url: String, playing: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var rendered by remember(url) { mutableStateOf(false) }

    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            trackSelectionParameters = trackSelectionParameters
                .buildUpon()
                .setMaxVideoSize(COVER_VIDEO_MAX, COVER_VIDEO_MAX)
                .build()
            setMediaItem(MediaItem.fromUri(url))
            repeatMode = Player.REPEAT_MODE_ALL
            volume = 0f
            prepare()
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() {
                rendered = true
            }
        }

        player.addListener(listener)

        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(player, playing) {
        player.playWhenReady = playing
    }

    val alpha by animateFloatAsState(
        targetValue = if (rendered) 1f else 0f,
        animationSpec = tween(COVER_FADE_MS),
        label = "animatedCover",
    )

    PlayerSurface(
        player = player,
        modifier = modifier.graphicsLayer { this.alpha = alpha },
    )
}
