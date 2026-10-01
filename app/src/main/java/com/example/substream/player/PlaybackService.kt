@file:OptIn(UnstableApi::class)

package com.example.substream.player

import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Foreground MediaSessionService enabling background audio playback
 * and integration with Android system notifications and lockscreen controls.
 */
class PlaybackService : MediaSessionService(), KoinComponent {

    private var mediaSession: MediaSession? = null

    // Injeção do CacheDataSource.Factory via Koin
    private val cacheDataSourceFactory: CacheDataSource.Factory by inject()

    override fun onCreate() {
        super.onCreate()

        // Configures the DefaultMediaSourceFactory with the injected CacheDataSource.Factory
        val mediaSourceFactory = DefaultMediaSourceFactory(this)
            .setDataSourceFactory(cacheDataSourceFactory)

        // Inicializa o ExoPlayer utilizando o MediaSourceFactory com suporte a Cache
        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()

        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player != null && (!player.playWhenReady)) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
