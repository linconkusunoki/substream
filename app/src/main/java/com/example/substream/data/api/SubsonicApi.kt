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
    val album: AlbumDetail? = null,
    val searchResult3: SearchResult3? = null
)

// Alias for backward compatibility with existing ping references
typealias SubsonicPingData = SubsonicResponse

@Serializable
data class AlbumListContainer(
    val album: List<Album> = emptyList()
)

@Serializable
data class SearchResult3(
    val artist: List<Artist> = emptyList(),
    val album: List<Album> = emptyList(),
    val song: List<Song> = emptyList()
)

typealias SearchResult = SearchResult3

@Serializable
data class Artist(
    val id: String,
    val name: String,
    val coverArt: String? = null,
    val artistImageUrl: String? = null,
    val albumCount: Int? = null,
    val starred: String? = null
) {
    val isStarred: Boolean get() = starred != null
}

@Serializable
data class Album(
    val id: String,
    val name: String,
    val artist: String? = null,
    val coverArt: String? = null,
    val songCount: Int? = null,
    val year: Int? = null,
    val starred: String? = null
) {
    val isStarred: Boolean get() = starred != null
}

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
    val song: List<Song> = emptyList(),
    val starred: String? = null
) {
    val isStarred: Boolean get() = starred != null
}

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
    val artistId: String? = null,
    val starred: String? = null
) {
    val isStarred: Boolean get() = starred != null
}

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

    @GET("rest/search3.view")
    suspend fun search3(
        @Query("u") user: String,
        @Query("t") token: String,
        @Query("s") salt: String,
        @Query("query") query: String,
        @Query("artistCount") artistCount: Int = 20,
        @Query("albumCount") albumCount: Int = 20,
        @Query("songCount") songCount: Int = 20,
        @Query("v") version: String = "1.16.1",
        @Query("c") client: String = "SubStream",
        @Query("f") format: String = "json"
    ): SubsonicResponseWrapper

    @GET("rest/star.view")
    suspend fun star(
        @Query("u") user: String,
        @Query("t") token: String,
        @Query("s") salt: String,
        @Query("id") id: String? = null,
        @Query("albumId") albumId: String? = null,
        @Query("artistId") artistId: String? = null,
        @Query("v") version: String = "1.16.1",
        @Query("c") client: String = "SubStream",
        @Query("f") format: String = "json"
    ): SubsonicResponseWrapper

    @GET("rest/unstar.view")
    suspend fun unstar(
        @Query("u") user: String,
        @Query("t") token: String,
        @Query("s") salt: String,
        @Query("id") id: String? = null,
        @Query("albumId") albumId: String? = null,
        @Query("artistId") artistId: String? = null,
        @Query("v") version: String = "1.16.1",
        @Query("c") client: String = "SubStream",
        @Query("f") format: String = "json"
    ): SubsonicResponseWrapper
}
