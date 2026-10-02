@file:OptIn(UnstableApi::class)

package com.substream.player

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.substream.MainActivity
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

        mediaSession = MediaSession.Builder(this, player)
            // Without this the media notification has no target and tapping it is a no-op.
            .setSessionActivity(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java).apply {
                        action = Intent.ACTION_MAIN
                        addCategory(Intent.CATEGORY_LAUNCHER)
                        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                    },
                    PendingIntent.FLAG_IMMUTABLE,
                )
            )
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    /**
     * Dismissing the app from recents means "I'm done": pause and tear the service down so
     * the media notification goes with it. Leaving the app by pressing home does not land
     * here, so background playback keeps playing with a live notification.
     */
    @OptIn(UnstableApi::class)
    override fun onTaskRemoved(rootIntent: Intent?) {
        pauseAllPlayersAndStopSelf()
    }

    /**
     * START_STICKY (the MediaSessionService default) makes Android restart the service once
     * it stops, which re-posts the media notification with actions bound to a player the
     * service no longer owns — the notification sits in the tray and its buttons do nothing.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int =
        super.onStartCommand(intent, flags, startId).let { START_NOT_STICKY }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
