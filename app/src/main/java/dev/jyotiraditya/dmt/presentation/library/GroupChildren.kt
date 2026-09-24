package dev.jyotiraditya.dmt.presentation.library

import dev.jyotiraditya.dmt.domain.model.Album
import dev.jyotiraditya.dmt.domain.model.Folder
import dev.jyotiraditya.dmt.domain.model.Track
import dev.jyotiraditya.dmt.domain.model.allTracks

sealed interface GroupChildren {
    data class Tracks(val tracks: List<Track>) : GroupChildren
    data class Albums(val albums: List<Album>) : GroupChildren
    data class Folders(val folders: List<Folder>, val tracks: List<Track>) : GroupChildren

    fun flatten(): List<Track> = when (this) {
        is Tracks -> tracks
        is Albums -> albums.flatMap { it.tracks }
        is Folders -> tracks + folders.flatMap { it.allTracks() }
    }
}
