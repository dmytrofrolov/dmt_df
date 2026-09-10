package dev.jyotiraditya.dmt.data.repository

import dev.jyotiraditya.dmt.data.remote.artwork.AnimatedArtworkApi
import dev.jyotiraditya.dmt.domain.model.Track
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

private const val CACHE_LIMIT = 64

@Singleton
class AnimatedArtworkRepository @Inject constructor(
    private val api: AnimatedArtworkApi,
) {

    private val lock = Mutex()
    private val known = mutableMapOf<String, String?>()

    suspend fun streamUrlFor(track: Track): String? {
        val key = "${track.albumArtist.ifBlank { track.artist }} ${track.album}".lowercase()

        lock.withLock {
            if (known.containsKey(key)) return known[key]
        }

        val url = api.fetchStreamUrl(track)

        return lock.withLock {
            if (known.size >= CACHE_LIMIT) known.clear()
            known.getOrPut(key) { url }
        }
    }
}
