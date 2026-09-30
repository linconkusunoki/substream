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
    @SerialName("subsonic-response") val subsonicResponse: SubsonicPingData
)

@Serializable
data class SubsonicPingData(
    val status: String,
    val version: String
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
}