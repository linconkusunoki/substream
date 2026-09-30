package com.example.substream.data.repository

import com.example.substream.BuildConfig
import com.example.substream.data.api.Album
import com.example.substream.data.api.AlbumDetail
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
     * Constructs a full authenticated URL to fetch cover art for a given coverArt ID.
     */
    fun getCoverArtUrl(coverArtId: String, user: String, pass: String): String {
        val authParams = SubsonicAuthUtil.generateTokenAndSalt(pass)
        val baseUrl = BuildConfig.NAVIDROME_URL
        val cleanBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return "${cleanBaseUrl}rest/getCoverArt.view?id=$coverArtId&u=$user&t=${authParams.token}&s=${authParams.salt}&v=1.16.1&c=SubStream"
    }
}
