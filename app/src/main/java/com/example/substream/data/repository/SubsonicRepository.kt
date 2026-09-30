package com.example.substream.data.repository

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
        // withContext(Dispatchers.IO) moves this execution to a background thread pool,
        // specifically optimized for Network/Disk operations, keeping the UI thread smooth.
        return withContext(Dispatchers.IO) {
            try {
                // 1. Generate token and salt
                val authParams = SubsonicAuthUtil.generateTokenAndSalt(pass)

                // 2. Make the API call
                val response = api.ping(
                    user = user,
                    token = authParams.token,
                    salt = authParams.salt
                )

                // 3. Return success with the actual payload
                Result.success(response.subsonicResponse)
            } catch (e: Exception) {
                // Return failure if network is down, URL is wrong, or JSON parsing fails
                Result.failure(e)
            }
        }
    }
}