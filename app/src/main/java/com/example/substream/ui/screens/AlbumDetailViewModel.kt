package com.example.substream.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.substream.BuildConfig
import com.example.substream.data.api.AlbumDetail
import com.example.substream.data.repository.SubsonicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// =====================================================================
// 1. THE STATE (UI Representation)
// =====================================================================

/**
 * Represents the distinct states the Album Detail Screen can be in.
 */
sealed class AlbumDetailState {
    object Idle : AlbumDetailState()
    object Loading : AlbumDetailState()
    data class Success(val albumDetail: AlbumDetail) : AlbumDetailState()
    data class Error(val message: String) : AlbumDetailState()
}

// =====================================================================
// 2. THE VIEWMODEL (Logic & State Management)
// =====================================================================

/**
 * ViewModel responsible for fetching and managing album details and tracklist.
 * Injectable via Koin with optional SavedStateHandle navigation support.
 */
class AlbumDetailViewModel(
    private val repository: SubsonicRepository,
    savedStateHandle: SavedStateHandle? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow<AlbumDetailState>(AlbumDetailState.Idle)
    val uiState: StateFlow<AlbumDetailState> = _uiState.asStateFlow()

    init {
        // Automatically load if albumId was passed via SavedStateHandle / Navigation args
        val initialAlbumId: String? = savedStateHandle?.get("albumId")
        if (!initialAlbumId.isNullOrEmpty()) {
            loadAlbumDetail(initialAlbumId)
        }
    }

    /**
     * Loads the details and song list for a given album ID.
     */
    fun loadAlbumDetail(albumId: String) {
        _uiState.value = AlbumDetailState.Loading

        viewModelScope.launch {
            val user = BuildConfig.NAVIDROME_USER
            val pass = BuildConfig.NAVIDROME_PASS

            val result = repository.getAlbumDetails(user, pass, albumId)

            result.fold(
                onSuccess = { detail ->
                    _uiState.value = AlbumDetailState.Success(detail)
                },
                onFailure = { error ->
                    _uiState.value = AlbumDetailState.Error(
                        error.message ?: "Failed to load album details"
                    )
                }
            )
        }
    }

    /**
     * Generates a full authenticated cover art URL for a given coverArt ID.
     */
    fun getCoverArtUrl(coverArtId: String?): String? {
        if (coverArtId.isNullOrEmpty()) return null
        val user = BuildConfig.NAVIDROME_USER
        val pass = BuildConfig.NAVIDROME_PASS
        return repository.getCoverArtUrl(coverArtId, user, pass)
    }
}
