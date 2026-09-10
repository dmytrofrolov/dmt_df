package dev.jyotiraditya.dmt.data.remote.artwork

import dev.jyotiraditya.dmt.BuildConfig
import dev.jyotiraditya.dmt.domain.model.Track
import dev.jyotiraditya.dmt.domain.model.UNKNOWN_ALBUM
import dev.jyotiraditya.dmt.domain.model.UNKNOWN_ARTIST
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

private const val BASE_URL = "https://artwork.m8tec.top/api/v1/artwork/search"
private const val USER_AGENT =
    "dmt v${BuildConfig.VERSION_NAME} (https://github.com/imjyotiraditya/dmt)"

private fun known(value: String, placeholder: String): String? =
    value.takeIf { it.isNotBlank() && it != placeholder }

@Singleton
class AnimatedArtworkApi @Inject constructor(
    private val client: HttpClient,
) {

    suspend fun fetchStreamUrl(track: Track): String? {
        val artist = known(track.albumArtist.ifBlank { track.artist }, UNKNOWN_ARTIST) ?: return null
        val album = known(track.album, UNKNOWN_ALBUM) ?: return null

        val response = runCatching {
            client.get(BASE_URL) {
                url {
                    parameters.append("artist", artist)
                    parameters.append("album", album)
                }
                header("User-Agent", USER_AGENT)
            }
        }.getOrNull() ?: return null
        if (!response.status.isSuccess()) return null

        val json = runCatching { JSONObject(response.bodyAsText()) }.getOrNull() ?: return null

        return if (json.isNull("url")) null else json.optString("url").ifBlank { null }
    }
}
