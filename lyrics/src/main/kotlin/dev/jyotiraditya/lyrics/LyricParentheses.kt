package dev.jyotiraditya.lyrics

/*
 * The brackets a file puts around the words sung behind a line, which say that those words are
 * backing rather than being sung themselves.
 */

private const val OPEN = "(（"
private const val CLOSE = ")）"

/**
 * Returns [this] without the brackets a backing part is written inside.
 *
 * A file writes backing vocals as `(ooh ooh)`, where the brackets only say that the words are
 * backing, which is already held by the words being marked as such and shown the way backing
 * words are shown.
 *
 * @return The lines, with the backing words reading as they are sung.
 */
fun List<LyricLine>.stripBackgroundParentheses(): List<LyricLine> = map { it.stripped() }

/** Returns [this] with the brackets around each of its backing parts taken out. */
private fun LyricLine.stripped(): LyricLine {
    val cuts = backgroundSpans()
        .flatMap { (start, end) -> bracketsAt(text, start, end) }
        .sorted()
    if (cuts.isEmpty()) return this

    fun moved(offset: Int) = offset - cuts.count { it < offset }

    return copy(
        text = text.filterIndexed { index, _ -> index !in cuts },
        words = words
            .map { it.copy(start = moved(it.start), end = moved(it.end)) }
            .filter { it.end > it.start },
    )
}

/** Returns where each run of backing words of [this] begins and ends. */
private fun LyricLine.backgroundSpans(): List<Pair<Int, Int>> {
    val spans = mutableListOf<Pair<Int, Int>>()
    var start = -1
    var end = -1

    words.sortedBy { it.start }.forEach { word ->
        when {
            !word.background && start >= 0 -> {
                spans += start to end
                start = -1
            }

            word.background && start < 0 -> {
                start = word.start
                end = word.end
            }

            word.background -> end = word.end
        }
    }

    if (start >= 0) spans += start to end

    return spans
}

/**
 * Returns where the brackets around what [text] holds between [start] and [end] are, or nothing
 * if what is there is not written inside a pair of them.
 */
private fun bracketsAt(text: String, start: Int, end: Int): List<Int> {
    val open = (start until end).firstOrNull { !text[it].isWhitespace() } ?: return emptyList()
    val close = (start until end).lastOrNull { !text[it].isWhitespace() } ?: return emptyList()
    if (open >= close || !isWrapped(text.substring(open, close + 1))) return emptyList()

    return listOf(open, close)
}

/** Whether [text] is written inside a single pair of brackets, rather than merely holding some. */
private fun isWrapped(text: String): Boolean {
    if (text.first() !in OPEN || text.last() !in CLOSE) return false

    var depth = 0
    text.forEachIndexed { index, c ->
        when {
            c in OPEN -> depth++

            c in CLOSE -> {
                depth--
                if (depth <= 0) return index == text.lastIndex
            }
        }
    }

    return false
}
