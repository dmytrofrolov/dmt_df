package dev.jyotiraditya.dmt.presentation.player

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.graphics.lerp
import dev.jyotiraditya.dmt.util.clusters
import kotlin.math.ceil
import kotlin.math.floor

private const val SWEEP_FADE_CLUSTERS = 2.5f
private const val SWEEP_FADE_MAX_CLUSTERS = 6f
private const val HELD_MS_PER_CLUSTER = 200f

internal data class SweepColors(
    val sung: Color,
    val unsung: Color,
)

internal data class Sweep(
    val edge: Float,
    val fade: Float,
)

internal class RunClusters(val text: String) {
    val boundaries: List<Int> = text.clusters(0, text.length)

    private val positions = FloatArray(text.length + 1) { boundaries.size - 1f }

    init {
        for (index in 0 until boundaries.size - 1) {
            val from = boundaries[index]
            val span = (boundaries[index + 1] - from).coerceAtLeast(1)

            for (offset in from until boundaries[index + 1]) {
                positions[offset] = index + (offset - from).toFloat() / span
            }
        }
    }

    val count: Int get() = boundaries.size - 1

    fun at(offset: Int): Float = positions[offset.coerceIn(0, text.length)]
}

internal fun sweepEdge(
    run: LyricRun,
    clusters: RunClusters,
    positionMs: Long,
): Sweep {
    var edge = 0f

    run.words.forEach { word ->
        val start = clusters.at(word.start)
        val end = clusters.at(word.end)

        when {
            positionMs >= word.endMs -> edge = end

            positionMs >= word.startMs -> {
                val span = (word.endMs - word.startMs).coerceAtLeast(1)
                val fraction = ((positionMs - word.startMs).toFloat() / span).coerceIn(0f, 1f)

                return Sweep(
                    edge = start + (end - start) * fraction,
                    fade = fadeFor(span, end - start),
                )
            }

            else -> return Sweep(edge, SWEEP_FADE_CLUSTERS)
        }
    }

    return Sweep(edge, SWEEP_FADE_CLUSTERS)
}

internal fun sweptText(
    run: LyricRun,
    clusters: RunClusters,
    positionMs: Long,
    colors: SweepColors,
): AnnotatedString = buildAnnotatedString {
    append(run.text)

    val sweep = sweepEdge(run, clusters, positionMs)
    val half = sweep.fade / 2f
    val first = ceil(sweep.edge - half - 0.5f).toInt().coerceIn(0, clusters.count)
    val last = floor(sweep.edge + half - 0.5f).toInt().coerceIn(-1, clusters.count - 1)

    if (first > 0) {
        addStyle(SpanStyle(color = colors.sung), 0, clusters.boundaries[first])
    }

    for (i in first..last) {
        val sung = ((sweep.edge - (i + 0.5f)) / sweep.fade + 0.5f).coerceIn(0f, 1f)
        addStyle(
            SpanStyle(color = lerp(colors.unsung, colors.sung, sung)),
            clusters.boundaries[i],
            clusters.boundaries[i + 1],
        )
    }

    if (last < clusters.count - 1) {
        addStyle(
            SpanStyle(color = colors.unsung),
            clusters.boundaries[last + 1],
            run.text.length,
        )
    }
}

private fun fadeFor(spanMs: Long, spanClusters: Float): Float {
    if (spanClusters <= 0f) return SWEEP_FADE_CLUSTERS

    val held = spanMs / spanClusters / HELD_MS_PER_CLUSTER

    return (SWEEP_FADE_CLUSTERS * held.coerceAtLeast(1f))
        .coerceAtMost(SWEEP_FADE_MAX_CLUSTERS)
}
