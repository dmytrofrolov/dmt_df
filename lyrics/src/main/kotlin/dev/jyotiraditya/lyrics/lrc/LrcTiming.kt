package dev.jyotiraditya.lyrics.lrc

import dev.jyotiraditya.lyrics.LyricWord

/*
 * The times a file writes, which say when a line starts and, when it was written with them, when
 * every word of that line does.
 */

/** The `[mm:ss.SS]` a line opens with. */
internal val LINE_TIME = Regex("""\[(\d+):(\d{1,2})(?:[.:](\d{1,3}))?]""")

/** The `<mm:ss.SS>` a word opens with, which only an enhanced file writes. */
internal val WORD_TIME = Regex("""<(\d+):(\d{1,2})(?:[.:](\d{1,3}))?>""")

/** The `[mm:ss.SS]` a word opens with in the files that write word times the way lines are. */
internal val BRACKET_WORD_TIME = Regex("""\[(\d+):(\d{1,2})(?:[.:](\d{1,3}))?]""")

/** How long the last word of a line is sung for when the file closes the line without a time. */
private const val LAST_WORD_MS = 500L

/** The [LINE_TIME] a line opens with, anchored so that only a leading one is found. */
internal val LEADING_TIME = Regex("""^\[\d+:\d{1,2}(?:[.:]\d{1,3})?]""")

/**
 * Returns the time [this] holds in milliseconds.
 *
 * A file may write the fraction of a second in tenths, hundredths or thousandths, so what it
 * means depends on how many digits were written.
 *
 * @return The time, or -1 if the match holds no minutes and seconds.
 */
internal fun MatchResult.toMs(): Long {
    val minutes = groupValues[1].toLongOrNull() ?: return -1L
    val seconds = groupValues[2].toLongOrNull() ?: return -1L

    val fraction = groupValues[3]
    val fractionMs = when (fraction.length) {
        0 -> 0L
        1 -> fraction.toLong() * 100
        2 -> fraction.toLong() * 10
        else -> fraction.take(3).toLong()
    }

    return minutes * 60_000 + seconds * 1_000 + fractionMs
}

/**
 * Returns the times [line] opens with, which are the times it is sung at.
 *
 * Only the times a line opens with are its own: a file that times its words the way it times its
 * lines writes the rest of them among the words, where they belong to a word rather than to the
 * line, so the line is left with the one time it opens with.
 *
 * @param line The line to read.
 * @return The times the line is sung at, in the order they are written.
 */
internal fun lineStamps(line: String): List<MatchResult> {
    val leading = mutableListOf<MatchResult>()
    var cursor = 0

    for (match in LINE_TIME.findAll(line)) {
        if (line.substring(cursor, match.range.first).isNotBlank()) break

        leading += match
        cursor = match.range.last + 1
    }

    if (leading.isEmpty()) return leading

    val rest = line.substring(leading.last().range.last + 1)

    return if (BRACKET_WORD_TIME.containsMatchIn(rest)) leading.take(1) else leading
}

/**
 * Returns the words of [text] and the text left once their times are taken out of it.
 *
 * A word runs until the next word starts. A file that closes a line with a time of its own says
 * when the last word ends; one that does not leaves the last word to be sung for as long as the
 * words before it were, rather than not being sung at all.
 *
 * Word times are written either as `<mm:ss.SS>` or, in some files, as `[mm:ss.SS]`, the way lines
 * are. Only one of the two is read, since a file writes its words one way or the other.
 *
 * @param text The line to read, times and all.
 * @return The line as it reads, and the words it holds times for.
 */
internal fun parseWordTags(text: String): Pair<String, List<LyricWord>> {
    val tags = WORD_TIME.findAll(text).toList()
        .ifEmpty { BRACKET_WORD_TIME.findAll(text).toList() }
    if (tags.isEmpty()) return text to emptyList()

    val plain = StringBuilder()
    val words = mutableListOf<LyricWord>()

    plain.append(text, 0, tags.first().range.first)

    tags.forEachIndexed { index, tag ->
        val gapEnd = tags.getOrNull(index + 1)?.range?.first ?: text.length
        val gap = text.substring(tag.range.last + 1, gapEnd)

        val wordStart = plain.length
        plain.append(gap)

        val startMs = tag.toMs()
        val trimmedLen = gap.trimEnd().length

        if (startMs >= 0 && trimmedLen > 0) {
            words += LyricWord(
                startMs = startMs,
                endMs = tags.getOrNull(index + 1)?.toMs() ?: (startMs + sungFor(words)),
                start = wordStart,
                end = wordStart + trimmedLen,
                background = false,
            )
        }
    }

    return plain.toString().trimEnd() to words
}

/**
 * Returns how long the word after [words] is taken to be sung for, which is as long as the words
 * of the same line were, or [LAST_WORD_MS] when it is the only word there is.
 *
 * @param words The words of the line read so far.
 * @return The length in milliseconds.
 */
private fun sungFor(words: List<LyricWord>): Long {
    val timed = words.filter { it.endMs > it.startMs }

    return if (timed.isEmpty()) {
        LAST_WORD_MS
    } else {
        timed.sumOf { it.endMs - it.startMs } / timed.size
    }
}

/**
 * Returns [this] moved to the line that starts at [startMs].
 *
 * A file writes word times either as times of their own or as how long after the line they fall,
 * which the words all falling before the line starts tells apart. A line written once but sung at
 * several times is moved to each of them.
 *
 * @param startMs When the line these words belong to starts.
 * @param firstStartMs When the first line written with these words starts.
 * @return The words, timed from [startMs].
 */
internal fun List<LyricWord>.shiftedTo(startMs: Long, firstStartMs: Long): List<LyricWord> {
    if (isEmpty()) return this

    val relative = last().endMs <= firstStartMs
    val offset = if (relative) startMs else startMs - firstStartMs
    if (offset == 0L) return this

    return map { it.copy(startMs = it.startMs + offset, endMs = it.endMs + offset) }
}
