package com.example.substream.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

// =====================================================================
// 1. DATA CLASSES (The Types)
// =====================================================================

@Serializable
data class SubsonicResponseWrapper(
    @SerialName("subsonic-response") val subsonicResponse: SubsonicResponse
)

@Serializable
data class SubsonicResponse(
    val status: String,
    val version: String,
    val albumList2: AlbumListContainer? = null,
    val album: AlbumDetail? = null
)

// Alias for backward compatibility with existing ping references
typealias SubsonicPingData = SubsonicResponse

@Serializable
data class AlbumListContainer(
    val album: List<Album> = emptyList()
)

@Serializable
data class Album(
    val id: String,
    val name: String,
    val artist: String? = null,
    val coverArt: String? = null,
    val songCount: Int? = null,
    val year: Int? = null
)

@Serializable
data class AlbumDetail(
    val id: String,
    val name: String,
    val artist: String? = null,
    val artistId: String? = null,
    val coverArt: String? = null,
    val songCount: Int? = null,
    val duration: Int? = null,
    val year: Int? = null,
    val song: List<Song> = emptyList()
)

@Serializable
data class Song(
    val id: String,
    val title: String,
    val album: String? = null,
    val artist: String? = null,
    val track: Int? = null,
    val year: Int? = null,
    val genre: String? = null,
    val coverArt: String? = null,
    val duration: Int? = null,
    val size: Long? = null,
    val contentType: String? = null,
    val suffix: String? = null,
    val bitRate: Int? = null,
    val path: String? = null,
    val albumId: String? = null,
    val artistId: String? = null
)

// =====================================================================
// 2. THE INTERFACE (The Endpoints)
// =====================================================================

interface SubsonicApiService {

    @GET("rest/ping.view")
    suspend fun ping(
        @Query("u") user: String,
        @Query("t") token: String,
        @Query("s") salt: String,
        @Query("v") version: String = "1.16.1",
        @Query("c") client: String = "SubStream",
        @Query("f") format: String = "json"
    ): SubsonicResponseWrapper

    @GET("rest/getAlbumList2.view")
    suspend fun getAlbums(
        @Query("u") user: String,
        @Query("t") token: String,
        @Query("s") salt: String,
        @Query("type") type: String = "newest",
        @Query("size") size: Int = 20,
        @Query("v") version: String = "1.16.1",
        @Query("c") client: String = "SubStream",
        @Query("f") format: String = "json"
    ): SubsonicResponseWrapper

    @GET("rest/getAlbum.view")
    suspend fun getAlbum(
        @Query("u") user: String,
        @Query("t") token: String,
        @Query("s") salt: String,
        @Query("id") albumId: String,
        @Query("v") version: String = "1.16.1",
        @Query("c") client: String = "SubStream",
        @Query("f") format: String = "json"
    ): SubsonicResponseWrapper
}
