package com.substream.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.substream.data.api.ArtistDetail
import com.substream.data.api.Song
import com.substream.data.repository.SubsonicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// =====================================================================
// 1. THE STATE (UI Representation)
// =====================================================================

/**
 * Represents the distinct states the Artist Detail Screen can be in.
 *
 * [Success.songs] is a [LoadState] of its own because the tracklist is fetched lazily, the first
 * time the Songs tab is opened: getArtist.view returns albums only, so the songs cost a second
 * search call that most visits to this screen never need.
 */
sealed class ArtistDetailState {
    object Idle : ArtistDetailState()
    object Loading : ArtistDetailState()
    data class Success(
        val artistDetail: ArtistDetail,
        val songs: LoadState<Song> = LoadState.Idle,
    ) : ArtistDetailState()
    data class Error(val message: String) : ArtistDetailState()
}

// =====================================================================
// 2. THE VIEWMODEL (Logic & State Management)
// =====================================================================

/**
 * ViewModel responsible for fetching and managing an artist's albums and songs.
 * Injectable via Koin with optional SavedStateHandle navigation support.
 */
class ArtistDetailViewModel(
    private val repository: SubsonicRepository,
    savedStateHandle: SavedStateHandle? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow<ArtistDetailState>(ArtistDetailState.Idle)
    val uiState: StateFlow<ArtistDetailState> = _uiState.asStateFlow()

    // The artist whose songs are cached in [ArtistDetailState.Success.songs], so switching tabs
    // back and forth refetches nothing. Reset by [loadArtist] when a different artist is opened.
    private var songsArtistId: String? = null

    init {
        val initialArtistId: String? = savedStateHandle?.get("artistId")
        if (!initialArtistId.isNullOrEmpty()) {
            loadArtist(initialArtistId)
        }
    }

    /**
     * Loads an artist and their albums.
     */
    fun loadArtist(artistId: String) {
        songsArtistId = null
        _uiState.value = ArtistDetailState.Loading

        viewModelScope.launch {
            repository.getArtistDetails(artistId).fold(
                onSuccess = { detail ->
                    _uiState.value = ArtistDetailState.Success(detail)
                },
                onFailure = { error ->
                    _uiState.value = ArtistDetailState.Error(
                        error.message ?: "Failed to load artist details"
                    )
                }
            )
        }
    }

    /**
     * Loads the artist's songs, but only the first time: after that the list lives in the UI
     * state and every later call is a no-op, so rotating or re-entering the Songs tab is free.
     */
    fun loadSongsIfNeeded() {
        val current = _uiState.value as? ArtistDetailState.Success ?: return
        if (songsArtistId == current.artistDetail.id) return

        songsArtistId = current.artistDetail.id
        _uiState.value = current.copy(songs = LoadState.Loading)

        viewModelScope.launch {
            val result = repository.getArtistSongs(
                artistId = current.artistDetail.id,
                artistName = current.artistDetail.name,
            )
            val latest = _uiState.value
            if (latest !is ArtistDetailState.Success) return@launch

            _uiState.value = latest.copy(
                songs = result.fold(
                    onSuccess = { LoadState.Success(it) },
                    // Not an error state worth shouting about: an artist whose songs the server
                    // cannot match by name simply shows as empty.
                    onFailure = { LoadState.Success(emptyList()) },
                )
            )
        }
    }

    /**
     * Toggles star/favorite state for the artist.
     */
    fun toggleStarArtist(artistDetail: ArtistDetail) {
        viewModelScope.launch {
            val result = repository.toggleStar(
                id = artistDetail.id,
                isStarred = artistDetail.isStarred,
                isArtist = true,
            )
            result.onSuccess { newStarred ->
                val currentState = _uiState.value
                if (currentState is ArtistDetailState.Success) {
                    _uiState.value = currentState.copy(
                        artistDetail = currentState.artistDetail.copy(
                            starred = if (newStarred) "starred" else null
                        )
                    )
                }
            }
        }
    }

    /**
     * Toggles star/favorite state for a song in the artist's tracklist.
     */
    fun toggleStarSong(song: Song) {
        viewModelScope.launch {
            val result = repository.toggleStar(id = song.id, isStarred = song.isStarred)
            result.onSuccess { newStarred ->
                val currentState = _uiState.value
                if (currentState !is ArtistDetailState.Success) return@onSuccess
                val currentSongs = currentState.songs
                if (currentSongs !is LoadState.Success) return@onSuccess

                _uiState.value = currentState.copy(
                    songs = LoadState.Success(
                        currentSongs.items.map {
                            if (it.id == song.id) {
                                it.copy(starred = if (newStarred) "starred" else null)
                            } else {
                                it
                            }
                        }
                    )
                )
            }
        }
    }

    /**
     * Generates a full authenticated cover art URL for a given coverArt ID.
     */
    fun getCoverArtUrl(coverArtId: String?): String? {
        if (coverArtId.isNullOrEmpty()) return null
        return repository.getCoverArtUrl(coverArtId)
    }
}