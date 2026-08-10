package dev.jyotiraditya.dmt.presentation.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.jyotiraditya.dmt.core.common.TuiPanel
import dev.jyotiraditya.dmt.core.common.tuiClickable
import dev.jyotiraditya.dmt.ui.theme.TuiAccent
import dev.jyotiraditya.dmt.ui.theme.TuiDim
import dev.jyotiraditya.dmt.ui.theme.TuiFaint
import dev.jyotiraditya.dmt.util.clusterEnd
import dev.jyotiraditya.lyrics.LyricLine
import dev.jyotiraditya.lyrics.Lyrics
import dev.jyotiraditya.lyrics.TimedText
import kotlin.math.ceil

private val SECTION_GAP = 18.dp
private val LINE_GAP = 6.dp
private const val LINES_ABOVE_ACTIVE = 2

@Composable
fun LyricsPanel(
    lyrics: Lyrics,
    trackId: String?,
    positionMs: Long,
    positionAtMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    romanized: Boolean,
    onSeekFraction: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val position = smoothPositionMs(positionMs, positionAtMs, isPlaying && lyrics.synced)
    val listState = rememberLazyListState()
    val scrollTarget = remember(lyrics) {
        derivedStateOf {
            if (lyrics.synced) lyrics.lines.indexAt(position.longValue) else -1
        }
    }

    LaunchedEffect(trackId) {
        listState.scrollToItem(0)
    }

    LaunchedEffect(lyrics, listState) {
        snapshotFlow { scrollTarget.value }.collect { target ->
            if (lyrics.synced && target >= 0) {
                listState.animateScrollToItem((target - LINES_ABOVE_ACTIVE).coerceAtLeast(0))
            }
        }
    }

    TuiPanel(modifier = modifier) {
        Box {
            LazyColumn(state = listState) {
                itemsIndexed(lyrics.lines) { _, line ->
                    LyricLineRows(
                        line = line,
                        romanized = romanized,
                        position = position,
                        synced = lyrics.synced,
                        seekable = lyrics.synced && durationMs > 0 && line.startMs >= 0,
                        onClick = {
                            onSeekFraction(
                                (line.startMs.toFloat() / durationMs).coerceIn(0f, 1f),
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun LyricLineRows(
    line: LyricLine,
    romanized: Boolean,
    position: MutableLongState,
    synced: Boolean,
    seekable: Boolean,
    onClick: () -> Unit,
) {
    val lineStates = remember(line, synced) {
        derivedStateOf { lineState(line, position.longValue, synced) }
    }
    val state = lineStates.value
    val positionMs = state.positionIn(line, position.longValue)

    val translit = line.transliteration
    val shown = if (romanized && translit != null) {
        line.copy(
            text = translit.text,
            words = translit.words,
            transliteration = TimedText(
                text = line.text,
                words = line.words,
            ),
        )
    } else {
        line
    }

    val rowModifier = Modifier
        .fillMaxWidth()
        .let { if (seekable) it.tuiClickable(onClick) else it }
        .padding(top = if (shown.sectionStart) SECTION_GAP else LINE_GAP, bottom = LINE_GAP)

    if (shown.interlude) {
        InterludeRow(
            line = shown,
            state = state,
            positionMs = positionMs,
            modifier = rowModifier,
        )
        return
    }

    val runs = remember(shown) { buildRuns(shown) }
    val secondaryRuns = remember(shown, runs) { secondaryRunsFor(shown, runs) }

    val singerColor = singerColorFor(shown, rememberSingerPalette())
    val hasSinger = !shown.interlude && shown.singer >= 0
    val align = alignFor(shown.voice)

    Column(modifier = rowModifier) {
        (runs + secondaryRuns).forEach { run ->
            LyricRunText(
                run = run,
                state = state,
                positionMs = positionMs,
                singerColor = singerColor,
                hasSinger = hasSinger,
                align = align,
            )
        }
    }
}

@Composable
private fun LyricRunText(
    run: LyricRun,
    state: LineState,
    positionMs: Long,
    singerColor: Color,
    hasSinger: Boolean,
    align: TextAlign,
) {
    val sweepState = runState(run, positionMs, state)
    val karaoke = sweepState == LineState.ACTIVE && run.words.isNotEmpty()
    val clusters = remember(run.text) { RunClusters(run.text) }

    val annotated = if (karaoke) {
        sweptText(run, clusters, positionMs, sweepColorsFor(run, singerColor))
    } else {
        AnnotatedString(run.text)
    }

    val baseStyle = if (run.background) {
        MaterialTheme.typography.bodySmall
    } else {
        MaterialTheme.typography.headlineSmall
    }
    val arabic = remember(run.text) { isArabicScript(run.text) }

    Text(
        text = annotated,
        style = baseStyle.copy(
            fontSize = if (arabic) baseStyle.fontSize.scaledBy(ARABIC_SCALE) else baseStyle.fontSize,
            fontWeight = if (sweepState == LineState.ACTIVE && !run.background) {
                FontWeight.Bold
            } else {
                FontWeight.Normal
            },
            fontStyle = if (run.background) FontStyle.Italic else FontStyle.Normal,
            letterSpacing = 0.sp,
            textDirection = TextDirection.Content,
        ),
        color = runColorFor(run, sweepState, karaoke, singerColor, hasSinger),
        textAlign = align,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun InterludeRow(
    line: LyricLine,
    state: LineState,
    positionMs: Long,
    modifier: Modifier,
) {
    val annotated = if (state == LineState.ACTIVE) {
        val span = (line.endMs - line.startMs).coerceAtLeast(1)
        val fraction = ((positionMs - line.startMs).toFloat() / span).coerceIn(0f, 1f)
        val filled = line.text.clusterEnd(
            ceil(fraction * line.text.length).toInt().coerceIn(0, line.text.length),
        )
        buildAnnotatedString {
            append(line.text)
            addStyle(SpanStyle(color = TuiAccent), 0, filled)
            addStyle(SpanStyle(color = TuiFaint), filled, line.text.length)
        }
    } else {
        AnnotatedString(line.text)
    }

    Text(
        text = annotated,
        style = MaterialTheme.typography.headlineSmall,
        color = if (state == LineState.PASSED) TuiFaint else TuiDim,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth(),
    )
}
