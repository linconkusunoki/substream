package com.example.substream.data.api

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

    fun createService(baseUrl: String): SubsonicApiService {
        // Setup logger to see requests in Logcat
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        val networkJson = Json { ignoreUnknownKeys = true }

        val retrofit = Retrofit.Builder()
            .baseUrl(if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/")
            .client(client)
            .addConverterFactory(networkJson.asConverterFactory("application/json".toMediaType()))
            .build()

        return retrofit.create(SubsonicApiService::class.java)
    }
}