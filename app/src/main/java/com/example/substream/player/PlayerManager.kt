package com.example.substream.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.substream.BuildConfig
import com.example.substream.data.api.Song
import com.example.substream.data.api.SubsonicAuthUtil
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
 * Manager class encapsulating ExoPlayer logic for streaming audio from Navidrome.
 * Registered as a Singleton in Koin.
 */
class PlayerManager(
    context: Context,
) {

    private val player: ExoPlayer by lazy {
        ExoPlayer.Builder(context).build().apply {
            addListener(playerListener)
        }
    }

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playerState.value = _playerState.value.copy(isPlaying = isPlaying)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val isBuffering = playbackState == Player.STATE_BUFFERING
            _playerState.value = _playerState.value.copy(
                isBuffering = isBuffering,
                durationMs = player.duration.coerceAtLeast(0L)
            )
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            _playerState.value = _playerState.value.copy(
                durationMs = player.duration.coerceAtLeast(0L)
            )
        }
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
     * Plays a song by preparing and starting ExoPlayer with the Navidrome stream URL.
     */
    fun playSong(song: Song) {
        val streamUrl = getStreamUrl(song.id)

        val mediaMetadata = MediaMetadata.Builder()
            .setTitle(song.title)
            .setArtist(song.artist ?: "Artista Desconhecido")
            .setAlbumTitle(song.album ?: "")
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

        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }

    fun play() {
        if (player.playbackState == Player.STATE_IDLE) {
            player.prepare()
        }
        player.play()
    }

    fun pause() {
        player.pause()
    }

    fun togglePlayPause() {
        if (player.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    fun stop() {
        player.stop()
        _playerState.value = PlayerState()
    }

    fun release() {
        player.removeListener(playerListener)
        player.release()
    }
}
