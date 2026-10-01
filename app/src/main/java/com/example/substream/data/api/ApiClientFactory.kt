package com.example.substream.data.api

import com.example.substream.data.preferences.ServerPreferences
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Factory object to build and configure the Retrofit instance.
 */
object ApiClientFactory {

    /**
     * Retrofit requires a base URL at build time. This placeholder is never dialled:
     * [AuthInterceptor] rewrites every request to the server stored in
     * [serverPreferences], so the service stays a singleton across logins.
     */
    private const val PLACEHOLDER_BASE_URL = "http://substream.invalid/"

    fun createService(serverPreferences: ServerPreferences): SubsonicApiService {
        // Setup logger to see requests in Logcat.
        // ponytail: BODY logs the auth token in the query string; drop to BASIC before
        // shipping a release build, or redact it.
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(serverPreferences))
            .addInterceptor(logging)
            .build()

        val networkJson = Json { ignoreUnknownKeys = true }

        val retrofit = Retrofit.Builder()
            .baseUrl(PLACEHOLDER_BASE_URL)
            .client(client)
            .addConverterFactory(networkJson.asConverterFactory("application/json".toMediaType()))
            .build()

        return retrofit.create(SubsonicApiService::class.java)
    }
}