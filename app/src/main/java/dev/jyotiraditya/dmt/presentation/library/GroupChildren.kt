package dev.jyotiraditya.dmt.presentation.library

import dev.jyotiraditya.dmt.domain.model.Album
import dev.jyotiraditya.dmt.domain.model.Track

sealed interface GroupChildren {
    data class Tracks(val tracks: List<Track>) : GroupChildren
    data class Albums(val albums: List<Album>) : GroupChildren

    fun flatten(): List<Track> = when (this) {
        is Tracks -> tracks
        is Albums -> albums.flatMap { it.tracks }
    }
}
