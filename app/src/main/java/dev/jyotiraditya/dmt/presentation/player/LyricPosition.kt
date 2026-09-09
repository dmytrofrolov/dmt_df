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
            display.moveTo(positionMs)
            return@LaunchedEffect
        }

        while (true) {
            withFrameNanos {
                val sampledAgoMs = SystemClock.elapsedRealtime() - positionAtMs
                display.moveTo(positionMs + sampledAgoMs)
            }
        }
    }

    return display
}

private fun MutableLongState.moveTo(target: Long) {
    val steppingBackSlightly = target < longValue && longValue - target < JUMP_BACK_MS
    if (!steppingBackSlightly) longValue = target
}
