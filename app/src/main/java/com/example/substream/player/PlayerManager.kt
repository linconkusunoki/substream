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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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
    val currentPositionMs: Long = 0L,
    val shuffleModeEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
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

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var positionUpdateJob: Job? = null

    private var currentPlaylist: List<Song> = emptyList()

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playerState.value = _playerState.value.copy(isPlaying = isPlaying)
            if (isPlaying) {
                startPositionUpdates()
            } else {
                stopPositionUpdates()
                controller?.let { p ->
                    _playerState.value = _playerState.value.copy(
                        currentPositionMs = p.currentPosition.coerceAtLeast(0L)
                    )
                }
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val isBuffering = playbackState == Player.STATE_BUFFERING
            val duration = controller?.duration?.coerceAtLeast(0L) ?: 0L
            val currentPos = controller?.currentPosition?.coerceAtLeast(0L) ?: 0L
            _playerState.value = _playerState.value.copy(
                isBuffering = isBuffering,
                durationMs = duration,
                currentPositionMs = currentPos
            )
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val duration = controller?.duration?.coerceAtLeast(0L) ?: 0L
            val currentPos = controller?.currentPosition?.coerceAtLeast(0L) ?: 0L
            val mediaId = mediaItem?.mediaId
            val song = currentPlaylist.find { it.id == mediaId } ?: _playerState.value.currentSong
            _playerState.value = _playerState.value.copy(
                currentSong = song,
                durationMs = duration,
                currentPositionMs = currentPos
            )
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _playerState.value = _playerState.value.copy(repeatMode = repeatMode)
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _playerState.value = _playerState.value.copy(shuffleModeEnabled = shuffleModeEnabled)
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
                        durationMs = p.duration.coerceAtLeast(0L),
                        currentPositionMs = p.currentPosition.coerceAtLeast(0L),
                        shuffleModeEnabled = p.shuffleModeEnabled,
                        repeatMode = p.repeatMode
                    )
                    if (p.isPlaying) {
                        startPositionUpdates()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun startPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = scope.launch {
            while (isActive) {
                controller?.let { p ->
                    _playerState.value = _playerState.value.copy(
                        currentPositionMs = p.currentPosition.coerceAtLeast(0L),
                        durationMs = p.duration.coerceAtLeast(0L)
                    )
                }
                delay(500L)
            }
        }
    }

    private fun stopPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }

    private val coverArtCache = mutableMapOf<String, String>()

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
     * Caches generated URLs by coverArtId to ensure stable URLs and avoid
     * Coil image reloading/flickering on player state updates.
     */
    fun getCoverArtUrl(coverArtId: String?): String? {
        if (coverArtId.isNullOrEmpty()) return null
        return coverArtCache.getOrPut(coverArtId) {
            val baseUrl = BuildConfig.NAVIDROME_URL
            val user = BuildConfig.NAVIDROME_USER
            val pass = BuildConfig.NAVIDROME_PASS
            val authParams = SubsonicAuthUtil.generateTokenAndSalt(pass)
            val cleanBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            "${cleanBaseUrl}rest/getCoverArt.view?id=$coverArtId&u=$user&t=${authParams.token}&s=${authParams.salt}&v=1.16.1&c=SubStream"
        }
    }

    /**
     * Plays a list of songs, starting at the specified index.
     */
    fun playSongs(songs: List<Song>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        currentPlaylist = songs

        val mediaItems = songs.map { song ->
            val streamUrl = getStreamUrl(song.id)
            val mediaMetadata = MediaMetadata.Builder()
                .setTitle(song.title)
                .setArtist(song.artist ?: "Unknown Artist")
                .setAlbumTitle(song.album ?: "")
                .setArtworkUri(getCoverArtUrl(song.coverArt)?.let { Uri.parse(it) })
                .build()

            MediaItem.Builder()
                .setUri(streamUrl)
                .setMediaId(song.id)
                .setMediaMetadata(mediaMetadata)
                .build()
        }

        val initialSong = songs.getOrNull(startIndex) ?: songs[0]
        _playerState.value = _playerState.value.copy(
            currentSong = initialSong,
            isBuffering = true,
            currentPositionMs = 0L
        )

        controller?.let { player ->
            player.setMediaItems(mediaItems, startIndex, 0L)
            player.prepare()
            player.play()
        }
    }

    /**
     * Plays a single song by preparing and starting the MediaController with the Navidrome stream URL.
     */
    fun playSong(song: Song) {
        playSongs(listOf(song), 0)
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

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
        _playerState.value = _playerState.value.copy(currentPositionMs = positionMs)
    }

    fun skipToNext() {
        controller?.let { player ->
            if (player.hasNextMediaItem()) {
                player.seekToNextMediaItem()
            } else {
                player.seekToNext()
            }
        }
    }

    fun skipToPrevious() {
        controller?.let { player ->
            if (player.hasPreviousMediaItem()) {
                player.seekToPreviousMediaItem()
            } else {
                player.seekToPrevious()
            }
        }
    }

    fun toggleShuffle() {
        controller?.let { player ->
            val nextShuffle = !player.shuffleModeEnabled
            player.shuffleModeEnabled = nextShuffle
            _playerState.value = _playerState.value.copy(shuffleModeEnabled = nextShuffle)
        }
    }

    fun toggleRepeatMode() {
        controller?.let { player ->
            val nextMode = when (player.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
            player.repeatMode = nextMode
            _playerState.value = _playerState.value.copy(repeatMode = nextMode)
        }
    }

    fun stop() {
        stopPositionUpdates()
        controller?.stop()
        _playerState.value = PlayerState()
    }

    fun release() {
        stopPositionUpdates()
        scope.cancel()
        controller?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
    }
}
