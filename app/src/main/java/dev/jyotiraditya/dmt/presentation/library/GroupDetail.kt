package dev.jyotiraditya.dmt.presentation.library

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.jyotiraditya.dmt.core.common.ListRow
import dev.jyotiraditya.dmt.core.common.SubdirHeader
import dev.jyotiraditya.dmt.domain.model.Album
import dev.jyotiraditya.dmt.domain.model.Track
import dev.jyotiraditya.dmt.presentation.player.DmtAction
import dev.jyotiraditya.dmt.presentation.player.DmtState

@Composable
internal fun <T> GroupDetail(
    spec: GroupSpec<T>,
    item: T,
    state: DmtState,
    dispatch: (DmtAction) -> Unit,
) {
    var openAlbum by remember(spec.key(item)) { mutableStateOf<Album?>(null) }
    val album = openAlbum

    if (album != null) {
        TrackListDetail(
            title = album.name,
            meta = album.artist.lowercase(),
            countLead = "",
            tracks = album.tracks,
            trackMeta = spec.trackMeta,
            nowPlayingId = state.nowPlayingId,
            onBack = { openAlbum = null },
            dispatch = dispatch,
        )
        return
    }

    when (val children = spec.children(item)) {
        is GroupChildren.Albums -> AlbumListDetail(
            title = spec.title(item),
            meta = spec.detailMeta(item).lowercase(),
            countLead = spec.countLead(item),
            albums = children.albums,
            allTracks = children.flatten(),
            onBack = { dispatch(spec.open(null)) },
            onOpenAlbum = { openAlbum = it },
        )

        is GroupChildren.Tracks -> TrackListDetail(
            title = spec.title(item),
            meta = spec.detailMeta(item).lowercase(),
            countLead = spec.countLead(item),
            tracks = children.tracks,
            trackMeta = spec.trackMeta,
            nowPlayingId = state.nowPlayingId,
            onBack = { dispatch(spec.open(null)) },
            dispatch = dispatch,
        )
    }
}

@Composable
private fun AlbumListDetail(
    title: String,
    meta: String,
    countLead: String,
    albums: List<Album>,
    allTracks: List<Track>,
    onBack: () -> Unit,
    onOpenAlbum: (Album) -> Unit,
) {
    LazyColumn {
        item {
            SubdirHeader(
                title = title,
                meta = meta,
                counts = listOf(countLead, "${allTracks.size} trk", totalTime(allTracks))
                    .filter { it.isNotBlank() }
                    .joinToString(" · "),
                onBack = onBack,
            )
        }
        itemsIndexed(albums, key = { _, a -> a.name }) { index, a ->
            AlbumRow(
                index = index,
                album = a,
                onClick = { onOpenAlbum(a) },
                modifier = Modifier.animateItem(),
            )
        }
    }
}

@Composable
private fun TrackListDetail(
    title: String,
    meta: String,
    countLead: String,
    tracks: List<Track>,
    trackMeta: (Track) -> String,
    nowPlayingId: String?,
    onBack: () -> Unit,
    dispatch: (DmtAction) -> Unit,
) {
    LazyColumn {
        item {
            SubdirHeader(
                title = title,
                meta = meta,
                counts = listOf(countLead, "${tracks.size} trk", totalTime(tracks))
                    .filter { it.isNotBlank() }
                    .joinToString(" · "),
                onBack = onBack,
            )
        }
        itemsIndexed(tracks, key = { _, track -> track.id }) { index, track ->
            TrackRow(
                index = index,
                track = track,
                meta = trackMeta(track),
                current = track.id.toString() == nowPlayingId,
                onClick = { dispatch(DmtAction.PlayAt(tracks, index)) },
                modifier = Modifier.animateItem(),
            )
        }
    }
}

@Composable
private fun AlbumRow(
    index: Int,
    album: Album,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListRow(
        index = index,
        line1 = album.name,
        line2 = "${album.tracks.size} trk",
        current = false,
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
private fun TrackRow(
    index: Int,
    track: Track,
    meta: String,
    current: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListRow(
        index = index,
        line1 = track.title,
        line2 = meta,
        current = current,
        onClick = onClick,
        modifier = modifier,
    )
}
