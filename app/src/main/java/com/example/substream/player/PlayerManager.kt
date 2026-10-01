package com.example.substream.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.substream.BuildConfig
import com.example.substream.data.api.Song
import com.example.substream.data.api.SubsonicAuthUtil
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// =====================================================================
// 1. PLAYER STATE
// =====================================================================

/**
 * State representing the current playback status of the media player.
 */
data class PlayerState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val durationMs: Long = 0L,
)

// =====================================================================
// 2. PLAYER MANAGER
// =====================================================================

/**
 * Manager class connecting to PlaybackService via MediaController.
 * Registered as a Singleton in Koin.
 */
class PlayerManager(
    private val context: Context,
) {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playerState.value = _playerState.value.copy(isPlaying = isPlaying)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val isBuffering = playbackState == Player.STATE_BUFFERING
            val duration = controller?.duration?.coerceAtLeast(0L) ?: 0L
            _playerState.value = _playerState.value.copy(
                isBuffering = isBuffering,
                durationMs = duration
            )
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val duration = controller?.duration?.coerceAtLeast(0L) ?: 0L
            _playerState.value = _playerState.value.copy(
                durationMs = duration
            )
        }
    }

    init {
        initializeController()
    }

    private fun initializeController() {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, PlaybackService::class.java)
        )
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                val mediaController = controllerFuture?.get()
                controller = mediaController
                mediaController?.addListener(playerListener)

                mediaController?.let { p ->
                    _playerState.value = _playerState.value.copy(
                        isPlaying = p.isPlaying,
                        isBuffering = p.playbackState == Player.STATE_BUFFERING,
                        durationMs = p.duration.coerceAtLeast(0L)
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * Constructs the authenticated streaming URL for a given song ID.
     */
    fun getStreamUrl(songId: String): String {
        val baseUrl = BuildConfig.NAVIDROME_URL
        val user = BuildConfig.NAVIDROME_USER
        val pass = BuildConfig.NAVIDROME_PASS

        val authParams = SubsonicAuthUtil.generateTokenAndSalt(pass)
        val cleanBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

        return "${cleanBaseUrl}rest/stream.view?id=$songId&u=$user&t=${authParams.token}&s=${authParams.salt}&v=1.16.1&c=SubStream"
    }

    /**
     * Constructs the authenticated cover art URL for a given coverArt ID.
     */
    fun getCoverArtUrl(coverArtId: String?): String? {
        if (coverArtId.isNullOrEmpty()) return null
        val baseUrl = BuildConfig.NAVIDROME_URL
        val user = BuildConfig.NAVIDROME_USER
        val pass = BuildConfig.NAVIDROME_PASS
        val authParams = SubsonicAuthUtil.generateTokenAndSalt(pass)
        val cleanBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return "${cleanBaseUrl}rest/getCoverArt.view?id=$coverArtId&u=$user&t=${authParams.token}&s=${authParams.salt}&v=1.16.1&c=SubStream"
    }

    /**
     * Plays a song by preparing and starting the MediaController with the Navidrome stream URL.
     */
    fun playSong(song: Song) {
        val streamUrl = getStreamUrl(song.id)

        val mediaMetadata = MediaMetadata.Builder()
            .setTitle(song.title)
            .setArtist(song.artist ?: "Artista Desconhecido")
            .setAlbumTitle(song.album ?: "")
            .setArtworkUri(getCoverArtUrl(song.coverArt)?.let { Uri.parse(it) })
            .build()

        val mediaItem = MediaItem.Builder()
            .setUri(streamUrl)
            .setMediaId(song.id)
            .setMediaMetadata(mediaMetadata)
            .build()

        _playerState.value = _playerState.value.copy(
            currentSong = song,
            isBuffering = true
        )

        controller?.let { player ->
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
        }
    }

    fun play() {
        controller?.let { player ->
            if (player.playbackState == Player.STATE_IDLE) {
                player.prepare()
            }
            player.play()
        }
    }

    fun pause() {
        controller?.pause()
    }

    fun togglePlayPause() {
        controller?.let { player ->
            if (player.isPlaying) {
                player.pause()
            } else {
                player.play()
            }
        }
    }

    fun stop() {
        controller?.stop()
        _playerState.value = PlayerState()
    }

    fun release() {
        controller?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
    }
}
