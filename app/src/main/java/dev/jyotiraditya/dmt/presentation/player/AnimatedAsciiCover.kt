package dev.jyotiraditya.dmt.presentation.player

import android.graphics.Bitmap
import android.view.TextureView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dev.jyotiraditya.dmt.core.common.AsciiCover
import dev.jyotiraditya.dmt.core.common.toAsciiBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private const val FRAME_INTERVAL_MS = 90L
private const val GRAB_SIZE = 128
private val PROBE_SIZE = 1.dp

@Composable
fun AnimatedAsciiCover(
    url: String,
    playing: Boolean,
    cols: Int,
    wave: Boolean,
    onFirstFrame: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var view by remember(url) { mutableStateOf<TextureView?>(null) }
    var art by remember(url) { mutableStateOf<Bitmap?>(null) }
    var rendered by remember(url) { mutableStateOf(false) }

    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            trackSelectionParameters = trackSelectionParameters
                .buildUpon()
                .setMaxVideoSize(GRAB_SIZE, GRAB_SIZE)
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

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val resumed by lifecycle.currentStateFlow.collectAsState()
    val running = playing && resumed.isAtLeast(Lifecycle.State.RESUMED)

    LaunchedEffect(player, running) {
        player.playWhenReady = running
    }

    LaunchedEffect(view, cols, running, rendered) {
        val target = view ?: return@LaunchedEffect
        if (!rendered) return@LaunchedEffect
        val grab = Bitmap.createBitmap(GRAB_SIZE, GRAB_SIZE, Bitmap.Config.ARGB_8888)

        while (running) {
            val frame = runCatching { target.getBitmap(grab) }.getOrNull()

            if (frame != null && target.isAvailable) {
                val next = withContext(Dispatchers.Default) {
                    runCatching { frame.toAsciiBitmap(context, cols) }.getOrNull()
                }

                if (next != null) {
                    if (art == null) onFirstFrame()
                    art = next
                }
            }

            delay(FRAME_INTERVAL_MS)
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { owner ->
                TextureView(owner).also { texture ->
                    player.setVideoTextureView(texture)
                    view = texture
                }
            },
            modifier = Modifier
                .size(PROBE_SIZE)
                .alpha(0f),
        )

        art?.let { frame ->
            AsciiCover(cover = frame, playing = playing, wave = wave)
        }
    }
}
