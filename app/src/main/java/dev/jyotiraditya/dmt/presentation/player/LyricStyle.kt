package dev.jyotiraditya.dmt.presentation.player

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import dev.jyotiraditya.dmt.ui.theme.TuiDim
import dev.jyotiraditya.dmt.ui.theme.TuiFaint
import dev.jyotiraditya.dmt.ui.theme.TuiFg
import dev.jyotiraditya.lyrics.LyricLine
import dev.jyotiraditya.lyrics.Voice

internal const val ARABIC_SCALE = 1.18f

internal fun isArabicScript(text: String): Boolean =
    text.any { Character.UnicodeScript.of(it.code) == Character.UnicodeScript.ARABIC }

internal fun TextUnit.scaledBy(factor: Float): TextUnit = (value * factor).sp

internal fun singerColorFor(line: LyricLine): Color =
    when {
        line.interlude -> singerPalette.first()
        line.singer < 0 -> GroupVoice
        else -> singerPalette[line.singer % singerPalette.size]
    }

internal fun alignFor(voice: Voice): TextAlign =
    when (voice) {
        Voice.SECONDARY -> TextAlign.End
        Voice.GROUP -> TextAlign.Center
        else -> TextAlign.Start
    }

internal fun sweepColorsFor(run: LyricRun, singerColor: Color): SweepColors =
    if (run.background) {
        SweepColors(sung = TuiFg, unsung = TuiFaint)
    } else {
        SweepColors(sung = singerColor, unsung = TuiDim)
    }

internal fun runColorFor(
    run: LyricRun,
    state: LineState,
    karaoke: Boolean,
    singerColor: Color,
    hasSinger: Boolean,
): Color =
    when {
        run.background && state == LineState.ACTIVE -> TuiFg
        run.background && state == LineState.PASSED -> TuiFaint
        run.background -> TuiDim
        state == LineState.ACTIVE && karaoke -> TuiDim
        state == LineState.ACTIVE -> singerColor
        state == LineState.PASSED && hasSinger -> lerp(singerColor, TuiFaint, 0.8f)
        state == LineState.PASSED -> TuiFaint
        else -> TuiDim
    }
