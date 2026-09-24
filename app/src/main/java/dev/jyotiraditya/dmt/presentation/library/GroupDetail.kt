package dev.jyotiraditya.dmt.presentation.library

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.core.common.ListRow
import dev.jyotiraditya.dmt.core.common.SubdirHeader
import dev.jyotiraditya.dmt.domain.model.Album
import dev.jyotiraditya.dmt.domain.model.Folder
import dev.jyotiraditya.dmt.domain.model.Track
import dev.jyotiraditya.dmt.domain.model.allTracks
import dev.jyotiraditya.dmt.presentation.player.DmtAction
import dev.jyotiraditya.dmt.presentation.player.DmtState
import dev.jyotiraditya.dmt.ui.theme.TuiFaint

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
            onBack = { dispatch(spec.close(item)) },
            onOpenAlbum = { openAlbum = it },
        )

        is GroupChildren.Tracks -> TrackListDetail(
            title = spec.title(item),
            meta = spec.detailMeta(item).lowercase(),
            countLead = spec.countLead(item),
            tracks = children.tracks,
            trackMeta = spec.trackMeta,
            nowPlayingId = state.nowPlayingId,
            onBack = { dispatch(spec.close(item)) },
            dispatch = dispatch,
        )

        is GroupChildren.Folders -> FolderListDetail(
            title = spec.title(item),
            meta = spec.detailMeta(item).lowercase(),
            folders = children.folders,
            tracks = children.tracks,
            trackMeta = spec.trackMeta,
            nowPlayingId = state.nowPlayingId,
            onBack = { dispatch(spec.close(item)) },
            onOpenFolder = { dispatch(DmtAction.OpenFolder(it.path)) },
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
private fun FolderListDetail(
    title: String,
    meta: String,
    folders: List<Folder>,
    tracks: List<Track>,
    trackMeta: (Track) -> String,
    nowPlayingId: String?,
    onBack: () -> Unit,
    onOpenFolder: (Folder) -> Unit,
    dispatch: (DmtAction) -> Unit,
) {
    val allTracks = tracks + folders.flatMap { it.allTracks() }
    val folderOffset = 2
    LazyColumn {
        item {
            SubdirHeader(
                title = title,
                meta = meta,
                counts = listOf(
                    folders.takeIf { it.isNotEmpty() }?.let { "${it.size} dir" }.orEmpty(),
                    "${allTracks.size} trk",
                    totalTime(allTracks),
                ).filter { it.isNotBlank() }.joinToString(" · "),
                onBack = onBack,
            )
        }
        item(key = "folder-up") {
            ListRow(
                index = 0,
                line1 = stringResource(R.string.folder_up),
                line2 = stringResource(R.string.folder_up_meta),
                current = false,
                onClick = onBack,
                modifier = Modifier.animateItem(),
            )
        }
        item(key = "folder-play") {
            ListRow(
                index = 1,
                line1 = stringResource(R.string.folder_play),
                line2 = "${allTracks.size} trk",
                current = false,
                onClick = {
                    if (allTracks.isNotEmpty()) {
                        dispatch(DmtAction.PlayAt(allTracks, 0))
                    }
                },
                modifier = Modifier.animateItem(),
            )
        }
        itemsIndexed(folders, key = { _, folder -> folder.path }) { index, folder ->
            FolderRow(
                index = folderOffset + index,
                folder = folder,
                onClick = { onOpenFolder(folder) },
                modifier = Modifier.animateItem(),
            )
        }
        itemsIndexed(tracks, key = { _, track -> track.id }) { index, track ->
            TrackRow(
                index = folderOffset + folders.size + index,
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
private fun FolderRow(
    index: Int,
    folder: Folder,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dirs = folder.children.size
    val trackCount = folder.allTracks().size
    ListRow(
        index = index,
        line1 = folder.name,
        line2 = buildString {
            if (dirs > 0) append("$dirs dir · ")
            append("$trackCount trk")
        }.lowercase(),
        current = false,
        onClick = onClick,
        trailing = {
            Text(
                text = stringResource(R.string.open_album),
                style = MaterialTheme.typography.labelMedium,
                color = TuiFaint,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        },
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
