package dev.jyotiraditya.dmt.domain.model

private val COLLAB_SEPARATORS = Regex("""\s*[;/]\s*""")

val Track.primaryArtist: String
    get() = albumArtist.ifBlank { artist }

fun String.asCredit(): String = replace(COLLAB_SEPARATORS, " + ")

fun List<Track>.toFolders(): List<Folder> {
    val tracksByDir =
        asSequence()
            .filter { it.path.isNotEmpty() }
            .groupBy { it.path.substringBeforeLast('/') }

    if (tracksByDir.isEmpty()) return emptyList()

    val dirs = tracksByDir.keys.toMutableSet()
    for (dir in tracksByDir.keys) {
        var parent = dir.parentPath()
        while (parent.isNotEmpty() && !parent.isVolumeRoot()) {
            dirs.add(parent)
            parent = parent.parentPath()
        }
    }

    fun build(path: String): Folder =
        Folder(
            name = path.substringAfterLast('/').ifEmpty { path },
            path = path,
            tracks =
                tracksByDir[path]
                    .orEmpty()
                    .sortedWith(folderTrackOrder),
            children =
                dirs.asSequence()
                    .filter { it.parentPath() == path }
                    .sortedBy { it.lowercase() }
                    .map(::build)
                    .toList(),
        )

    return dirs.asSequence()
        .filter { dir ->
            val parent = dir.parentPath()
            parent.isEmpty() || parent.isVolumeRoot() || parent !in dirs
        }
        .sortedBy { it.lowercase() }
        .map(::build)
        .toList()
}

/** Filename first; cue/disc tags break ties so a flac+cue album keeps sheet order. */
private val folderTrackOrder: Comparator<Track> =
    compareBy<Track, String>(String.CASE_INSENSITIVE_ORDER) { it.path.substringAfterLast('/') }
        .thenBy { it.discNumber }
        .thenBy { it.trackNumber.takeIf { number -> number > 0 } ?: Int.MAX_VALUE }
        .thenBy { it.clipStartMs ?: 0L }
        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.title }

fun List<Folder>.findFolder(path: String): Folder? =
    firstOrNull { it.path == path }
        ?: asSequence().mapNotNull { it.children.findFolder(path) }.firstOrNull()

fun List<Folder>.flattenFolders(): List<Folder> = flatMap { it.walk().toList() }

fun Folder.allTracks(): List<Track> = tracks + children.flatMap { it.allTracks() }

fun Folder.walk(): Sequence<Folder> {
    val node = this
    return sequence {
        yield(node)
        for (child in node.children) yieldAll(child.walk())
    }
}

fun folderParentKey(path: String, roots: List<Folder>): String? {
    val parent = path.parentPath()
    if (parent.isEmpty() || parent.isVolumeRoot()) return null
    return parent.takeIf { roots.findFolder(it) != null }
}

fun nextFolderQueue(roots: List<Folder>, currentDir: String): List<Track>? {
    val ordered = roots.flattenFolders().filter { it.allTracks().isNotEmpty() }
    if (ordered.isEmpty()) return null
    val index =
        ordered.indexOfFirst { it.path == currentDir }.takeIf { it >= 0 }
            ?: ordered.indexOfLast { currentDir == it.path || currentDir.startsWith("${it.path}/") }
    val next = ordered[(index + 1).mod(ordered.size)]
    return next.allTracks()
}

private fun String.parentPath(): String =
    substringBeforeLast('/', missingDelimiterValue = "")

private fun String.isVolumeRoot(): Boolean {
    if (this == "/storage/emulated/0" || this == "/sdcard" || this == "/mnt/sdcard") {
        return true
    }
    if (!startsWith("/storage/")) return false
    val rest = removePrefix("/storage/")
    return rest.isNotEmpty() && '/' !in rest && rest != "emulated"
}

fun List<Track>.toArtists(): List<Artist> =
    groupBy { it.primaryArtist.lowercase() }
        .map { (_, tracks) ->
            Artist(
                name = tracks.groupingBy { it.primaryArtist }
                    .eachCount()
                    .maxBy { it.value }
                    .key,
                albums = tracks.map { it.album }.distinct().size,
                tracks = tracks.sortedWith(
                    compareBy({ it.album.lowercase() }, { it.discNumber }, { it.trackNumber }),
                ),
            )
        }
        .sortedBy { it.name.lowercase() }

fun List<Track>.toGenres(): List<Genre> =
    groupBy { it.genre?.takeIf(String::isNotBlank)?.lowercase() ?: UNKNOWN_GENRE }
        .map { (_, tracks) ->
            Genre(name = tracks.first().genre?.takeIf(String::isNotBlank) ?: UNKNOWN_GENRE, tracks = tracks)
        }
        .sortedBy { it.name.lowercase() }

fun List<Track>.toAlbums(): List<Album> =
    groupBy { it.album }
        .map { (name, tracks) ->
            val artists = tracks.map { it.primaryArtist }.distinctBy { it.lowercase() }
            Album(
                name = name,
                artist = artists.singleOrNull() ?: "various artists",
                tracks = tracks.sortedWith(compareBy({ it.discNumber }, { it.trackNumber })),
            )
        }
        .sortedBy { it.name.lowercase() }
