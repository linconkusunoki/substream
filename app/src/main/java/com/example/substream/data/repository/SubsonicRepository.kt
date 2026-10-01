package com.example.substream.data.repository

import com.example.substream.BuildConfig
import com.example.substream.data.api.Album
import com.example.substream.data.api.AlbumDetail
import com.example.substream.data.api.Artist
import com.example.substream.data.api.Playlist
import com.example.substream.data.api.SearchResult
import com.example.substream.data.api.SearchResult3
import com.example.substream.data.api.SubsonicApiService
import com.example.substream.data.api.SubsonicAuthUtil
import com.example.substream.data.api.SubsonicPingData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository responsible for handling data operations related to the Subsonic API.
 * This class abstracts the network and authentication logic from the UI layer.
 */
class SubsonicRepository(
    private val api: SubsonicApiService
) {

    /**
     * Executes the ping request to check server connectivity and credentials.
     *
     * @param user The username for the Navidrome server.
     * @param pass The plain text password.
     * @return A Result containing the PingData if successful, or an Exception if it fails.
     */
    suspend fun pingServer(user: String, pass: String): Result<SubsonicPingData> {
        return withContext(Dispatchers.IO) {
            try {
                val authParams = SubsonicAuthUtil.generateTokenAndSalt(pass)
                val response = api.ping(
                    user = user,
                    token = authParams.token,
                    salt = authParams.salt
                )
                Result.success(response.subsonicResponse)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Fetches the list of albums from the Navidrome/Subsonic server.
     *
     * @param user The username for the Navidrome server.
     * @param pass The plain text password.
     * @return A Result containing a list of Albums if successful, or an Exception if it fails.
     */
    suspend fun getAlbums(user: String, pass: String): Result<List<Album>> {
        return withContext(Dispatchers.IO) {
            try {
                val authParams = SubsonicAuthUtil.generateTokenAndSalt(pass)
                val response = api.getAlbums(
                    user = user,
                    token = authParams.token,
                    salt = authParams.salt
                )
                val albums = response.subsonicResponse.albumList2?.album ?: emptyList()
                Result.success(albums)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Fetches details of a specific album, including its tracklist.
     *
     * @param user The username for the Navidrome server.
     * @param pass The plain text password.
     * @param albumId The unique ID of the album.
     * @return A Result containing AlbumDetail if successful, or an Exception if it fails.
     */
    suspend fun getAlbumDetails(user: String, pass: String, albumId: String): Result<AlbumDetail> {
        return withContext(Dispatchers.IO) {
            try {
                val authParams = SubsonicAuthUtil.generateTokenAndSalt(pass)
                val response = api.getAlbum(
                    user = user,
                    token = authParams.token,
                    salt = authParams.salt,
                    albumId = albumId
                )
                val detail = response.subsonicResponse.album
                    ?: throw Exception("Album details not found in server response")
                Result.success(detail)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Executes global search for artists, albums, and songs matching the given query string.
     *
     * @param query The search term entered by the user.
     * @param user The username for the Navidrome server (defaults to BuildConfig value).
     * @param pass The plain text password (defaults to BuildConfig value).
     * @return A Result containing SearchResult if successful, or an Exception if it fails.
     */
    suspend fun search(
        query: String,
        user: String = BuildConfig.NAVIDROME_USER,
        pass: String = BuildConfig.NAVIDROME_PASS
    ): Result<SearchResult> {
        return withContext(Dispatchers.IO) {
            try {
                val authParams = SubsonicAuthUtil.generateTokenAndSalt(pass)
                val response = api.search3(
                    user = user,
                    token = authParams.token,
                    salt = authParams.salt,
                    query = query
                )
                val searchResult = response.subsonicResponse.searchResult3
                    ?: SearchResult3()
                Result.success(searchResult)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Fetches the artists index from the server.
     *
     * Navidrome does not implement getArtists.view, so on failure we fall back to the
     * unique artists found in the album list. ponytail: replace with a real artist index
     * once the server exposes one.
     */
    suspend fun getArtists(
        user: String = BuildConfig.NAVIDROME_USER,
        pass: String = BuildConfig.NAVIDROME_PASS
    ): Result<List<Artist>> {
        return withContext(Dispatchers.IO) {
            try {
                val authParams = SubsonicAuthUtil.generateTokenAndSalt(pass)
                val response = api.getArtists(
                    user = user,
                    token = authParams.token,
                    salt = authParams.salt
                )
                Result.success(
                    response.subsonicResponse.artists?.index
                        ?.flatMap { it.artist }
                        .orEmpty()
                        .sortedBy { it.name.lowercase() }
                )
            } catch (e: Exception) {
                val fallback = getAlbums(user, pass).getOrNull().orEmpty()
                    .mapNotNull { album ->
                        album.artist?.let { name ->
                            Artist(id = album.id, name = name, coverArt = album.coverArt)
                        }
                    }
                    .distinctBy { it.name.lowercase() }
                    .sortedBy { it.name.lowercase() }
                if (fallback.isEmpty()) Result.failure(e) else Result.success(fallback)
            }
        }
    }

    /**
     * Fetches the playlists stored on the server.
     */
    suspend fun getPlaylists(
        user: String = BuildConfig.NAVIDROME_USER,
        pass: String = BuildConfig.NAVIDROME_PASS
    ): Result<List<Playlist>> {
        return withContext(Dispatchers.IO) {
            try {
                val authParams = SubsonicAuthUtil.generateTokenAndSalt(pass)
                val response = api.getPlaylists(
                    user = user,
                    token = authParams.token,
                    salt = authParams.salt
                )
                Result.success(response.subsonicResponse.playlists?.playlist.orEmpty())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Fetches starred (favorite) albums via getStarred2.view.
     */
    suspend fun getStarredAlbums(
        user: String = BuildConfig.NAVIDROME_USER,
        pass: String = BuildConfig.NAVIDROME_PASS
    ): Result<List<Album>> {
        return withContext(Dispatchers.IO) {
            try {
                val authParams = SubsonicAuthUtil.generateTokenAndSalt(pass)
                val response = api.getStarred(
                    user = user,
                    token = authParams.token,
                    salt = authParams.salt
                )
                Result.success(response.subsonicResponse.starred2?.album.orEmpty())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Toggles the favorite / starred status of a song or album on the Subsonic server.
     *
     * @param id The unique ID of the item (song, album, or artist).
     * @param isStarred The CURRENT starred state. If true, it will unstar; if false, it will star.
     * @param isAlbum Set to true if starring an album ID instead of a song ID.
     * @param isArtist Set to true if starring an artist ID instead of a song ID.
     * @param user The username for the Navidrome server (defaults to BuildConfig value).
     * @param pass The plain text password (defaults to BuildConfig value).
     * @return A Result containing the new boolean starred status if successful.
     */
    suspend fun toggleStar(
        id: String,
        isStarred: Boolean,
        isAlbum: Boolean = false,
        isArtist: Boolean = false,
        user: String = BuildConfig.NAVIDROME_USER,
        pass: String = BuildConfig.NAVIDROME_PASS
    ): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val authParams = SubsonicAuthUtil.generateTokenAndSalt(pass)
                val targetStarred = !isStarred

                val songId = if (!isAlbum && !isArtist) id else null
                val albumIdParam = if (isAlbum) id else null
                val artistIdParam = if (isArtist) id else null

                val response = if (targetStarred) {
                    api.star(
                        user = user,
                        token = authParams.token,
                        salt = authParams.salt,
                        id = songId,
                        albumId = albumIdParam,
                        artistId = artistIdParam
                    )
                } else {
                    api.unstar(
                        user = user,
                        token = authParams.token,
                        salt = authParams.salt,
                        id = songId,
                        albumId = albumIdParam,
                        artistId = artistIdParam
                    )
                }

                if (response.subsonicResponse.status == "ok") {
                    Result.success(targetStarred)
                } else {
                    Result.failure(Exception("Error toggling star status"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Constructs a full authenticated URL to fetch cover art for a given coverArt ID.
     */
    fun getCoverArtUrl(coverArtId: String, user: String, pass: String): String {
        val authParams = SubsonicAuthUtil.generateTokenAndSalt(pass)
        val baseUrl = BuildConfig.NAVIDROME_URL
        val cleanBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return "${cleanBaseUrl}rest/getCoverArt.view?id=$coverArtId&u=$user&t=${authParams.token}&s=${authParams.salt}&v=1.16.1&c=SubStream"
    }
}
