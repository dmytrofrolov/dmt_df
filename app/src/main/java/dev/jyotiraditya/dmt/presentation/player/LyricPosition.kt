package dev.jyotiraditya.dmt.presentation.player

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos

private const val JUMP_BACK_MS = 300L

@Composable
internal fun smoothPositionMs(
    positionMs: Long,
    positionAtMs: Long,
    isPlaying: Boolean,
): MutableLongState {
    val display = remember { mutableLongStateOf(positionMs) }

    LaunchedEffect(positionMs, positionAtMs, isPlaying) {
        if (!isPlaying) {
            display.longValue = positionMs
            return@LaunchedEffect
        }

        while (true) {
            withFrameNanos {
                val sampledAgoMs = SystemClock.elapsedRealtime() - positionAtMs
                val playedMs = positionMs + sampledAgoMs
                val wouldJumpBackSlightly =
                    playedMs < display.longValue && display.longValue - playedMs < JUMP_BACK_MS

                if (!wouldJumpBackSlightly) display.longValue = playedMs
            }
        }
    }

    return display
}
