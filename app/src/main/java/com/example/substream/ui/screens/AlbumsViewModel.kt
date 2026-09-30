package com.example.substream.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.substream.BuildConfig
import com.example.substream.data.api.Album
import com.example.substream.data.repository.SubsonicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// =====================================================================
// 1. THE STATE (UI Representation)
// =====================================================================

/**
 * Represents the distinct states the Albums Screen can be in.
 */
sealed class AlbumsState {
    object Idle : AlbumsState()
    object Loading : AlbumsState()
    data class Success(val albums: List<Album>) : AlbumsState()
    data class Error(val message: String) : AlbumsState()
}

// =====================================================================
// 2. THE VIEWMODEL (Logic & State Management)
// =====================================================================

/**
 * ViewModel responsible for managing album data and UI state for the AlbumsScreen.
 * Fully compatible with Koin dependency injection.
 */
class AlbumsViewModel(
    private val repository: SubsonicRepository
) : ViewModel() {

    // Internal mutable state flow
    private val _uiState = MutableStateFlow<AlbumsState>(AlbumsState.Idle)

    // Public immutable state flow exposed to the UI
    val uiState: StateFlow<AlbumsState> = _uiState.asStateFlow()

    /**
     * Triggers loading of albums using server credentials from BuildConfig.
     */
    fun loadAlbums() {
        _uiState.value = AlbumsState.Loading

        viewModelScope.launch {
            val user = BuildConfig.NAVIDROME_USER
            val pass = BuildConfig.NAVIDROME_PASS

            val result = repository.getAlbums(user, pass)

            result.fold(
                onSuccess = { albums ->
                    _uiState.value = AlbumsState.Success(albums)
                },
                onFailure = { error ->
                    _uiState.value = AlbumsState.Error(
                        error.message ?: "Failed to load albums from Navidrome"
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
