package com.example.substream.data.repository

import com.example.substream.data.api.Album
import com.example.substream.data.api.AlbumDetail
import com.example.substream.data.api.Artist
import com.example.substream.data.api.Playlist
import com.example.substream.data.api.SearchResult
import com.example.substream.data.api.SearchResult3
import com.example.substream.data.api.SubsonicApiService
import com.example.substream.data.api.SubsonicPingData
import com.example.substream.data.preferences.ServerPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Repository responsible for handling data operations related to the Subsonic API.
 * This class abstracts the network logic from the UI layer.
 *
 * Credentials are never passed in as arguments: they live in [ServerPreferences] and
 * reach every call through the OkHttp auth interceptor, so no screen can accidentally
 * talk to the wrong server or with stale credentials.
 */
class SubsonicRepository(
    private val api: SubsonicApiService,
    private val serverPreferences: ServerPreferences,
) {

    /**
     * Executes the ping request against the active server to verify connectivity and
     * that the stored credentials are accepted.
     *
     * @return A Result containing the PingData if successful, or an Exception describing
     * why the server refused the credentials.
     */
    suspend fun pingServer(): Result<SubsonicPingData> = call {
        val response = api.ping().subsonicResponse
        if (response.status != "ok") {
            throw IOException(response.error?.message ?: "The server rejected these credentials")
        }
        response
    }

    /**
     * Fetches the list of albums from the Navidrome/Subsonic server.
     */
    suspend fun getAlbums(): Result<List<Album>> = call {
        api.getAlbums().subsonicResponse.albumList2?.album.orEmpty()
    }

    /**
     * Fetches details of a specific album, including its tracklist.
     *
     * @param albumId The unique ID of the album.
     */
    suspend fun getAlbumDetails(albumId: String): Result<AlbumDetail> = call {
        api.getAlbum(albumId = albumId).subsonicResponse.album
            ?: throw IOException("Album details not found in server response")
    }

    /**
     * Executes global search for artists, albums, and songs matching the given query string.
     *
     * @param query The search term entered by the user.
     */
    suspend fun search(query: String): Result<SearchResult> = call {
        api.search3(query = query).subsonicResponse.searchResult3 ?: SearchResult3()
    }

    /**
     * Fetches the artists index from the server.
     *
     * Navidrome does not implement getArtists.view, so on failure we fall back to the
     * unique artists found in the album list. ponytail: replace with a real artist index
     * once the server exposes one.
     */
    suspend fun getArtists(): Result<List<Artist>> {
        val result = call {
            api.getArtists().subsonicResponse.artists?.index
                ?.flatMap { it.artist }
                .orEmpty()
                .sortedBy { it.name.lowercase() }
        }
        if (result.isSuccess) return result

        val fallback = getAlbums().getOrNull().orEmpty()
            .mapNotNull { album ->
                album.artist?.let { name ->
                    Artist(id = album.id, name = name, coverArt = album.coverArt)
                }
            }
            .distinctBy { it.name.lowercase() }
            .sortedBy { it.name.lowercase() }
        return if (fallback.isEmpty()) result else Result.success(fallback)
    }

    /**
     * Fetches the playlists stored on the server.
     */
    suspend fun getPlaylists(): Result<List<Playlist>> = call {
        api.getPlaylists().subsonicResponse.playlists?.playlist.orEmpty()
    }

    /**
     * Fetches starred (favorite) albums via getStarred2.view.
     */
    suspend fun getStarredAlbums(): Result<List<Album>> = call {
        api.getStarred().subsonicResponse.starred2?.album.orEmpty()
    }

    /**
     * Toggles the favorite / starred status of a song or album on the Subsonic server.
     *
     * @param id The unique ID of the item (song, album, or artist).
     * @param isStarred The CURRENT starred state. If true, it will unstar; if false, it will star.
     * @param isAlbum Set to true if starring an album ID instead of a song ID.
     * @param isArtist Set to true if starring an artist ID instead of a song ID.
     * @return A Result containing the new boolean starred status if successful.
     */
    suspend fun toggleStar(
        id: String,
        isStarred: Boolean,
        isAlbum: Boolean = false,
        isArtist: Boolean = false,
    ): Result<Boolean> = call {
        val targetStarred = !isStarred
        val response = if (targetStarred) {
            api.star(
                id = if (isAlbum || isArtist) null else id,
                albumId = if (isAlbum) id else null,
                artistId = if (isArtist) id else null
            )
        } else {
            api.unstar(
                id = if (isAlbum || isArtist) null else id,
                albumId = if (isAlbum) id else null,
                artistId = if (isArtist) id else null
            )
        }
        if (response.subsonicResponse.status != "ok") {
            throw IOException("Error toggling star status")
        }
        targetStarred
    }

    /**
     * Constructs a full authenticated URL to fetch cover art for a given coverArt ID.
     * Returns null when nobody is signed in.
     */
    fun getCoverArtUrl(coverArtId: String): String? =
        serverPreferences.activeServer?.authenticatedUrl("getCoverArt.view", "id" to coverArtId)

    private suspend fun <T> call(block: suspend () -> T): Result<T> = withContext(Dispatchers.IO) {
        try {
            Result.success(block())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}