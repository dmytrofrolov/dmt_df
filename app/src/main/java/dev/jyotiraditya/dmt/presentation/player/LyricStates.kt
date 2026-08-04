package dev.jyotiraditya.dmt.presentation.player

import dev.jyotiraditya.lyrics.LyricLine

internal enum class LineState { ACTIVE, PASSED, UPCOMING }

internal fun List<LyricLine>.indexAt(positionMs: Long): Int {
    val found = binarySearch { line -> line.startMs.compareTo(positionMs) }

    return if (found >= 0) found else -found - 2
}

internal fun lineState(
    line: LyricLine,
    positionMs: Long,
    synced: Boolean,
): LineState =
    when {
        !synced || line.startMs < 0 -> LineState.UPCOMING
        positionMs in line.startMs until line.endMs -> LineState.ACTIVE
        line.endMs <= positionMs -> LineState.PASSED
        else -> LineState.UPCOMING
    }

internal fun runState(
    run: LyricRun,
    positionMs: Long,
    fallback: LineState,
): LineState {
    if (run.words.isEmpty()) return fallback

    val start = run.words.minOf { it.startMs }
    val end = run.words.maxOf { it.endMs }

    return when {
        positionMs < start -> LineState.UPCOMING
        positionMs >= end -> LineState.PASSED
        else -> LineState.ACTIVE
    }
}

internal fun LineState.positionIn(line: LyricLine, positionMs: Long): Long =
    when (this) {
        LineState.ACTIVE -> positionMs
        LineState.PASSED -> line.endMs
        LineState.UPCOMING -> line.startMs - 1
    }
