package com.example.substream.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.substream.data.api.Album
import com.example.substream.data.api.Artist
import com.example.substream.data.api.Playlist
import com.example.substream.data.repository.SubsonicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Generic async list state shared by the Library sections (artists, playlists, favorites).
 */
sealed interface LoadState<out T> {
    data object Idle : LoadState<Nothing>
    data object Loading : LoadState<Nothing>
    data class Success<T>(val items: List<T>) : LoadState<T>
    data class Error(val message: String) : LoadState<Nothing>
}

/**
 * ViewModel backing LibraryScreen: artists (getArtists.view), playlists (getPlaylists.view)
 * and starred albums (getStarred2.view).
 */
class LibraryViewModel(
    private val repository: SubsonicRepository
) : ViewModel() {

    private val _artists = MutableStateFlow<LoadState<Artist>>(LoadState.Idle)
    val artists: StateFlow<LoadState<Artist>> = _artists.asStateFlow()

    private val _playlists = MutableStateFlow<LoadState<Playlist>>(LoadState.Idle)
    val playlists: StateFlow<LoadState<Playlist>> = _playlists.asStateFlow()

    private val _starred = MutableStateFlow<LoadState<Album>>(LoadState.Idle)
    val starred: StateFlow<LoadState<Album>> = _starred.asStateFlow()

    fun loadArtists() = load(_artists, repository::getArtists, "Could not load artists")

    fun loadPlaylists() = load(_playlists, repository::getPlaylists, "Could not load playlists")

    fun loadStarred() = load(_starred, repository::getStarredAlbums, "Could not load favorites")

    /**
     * Unstars an album from the favorites tab and removes it from the list.
     */
    fun removeStarred(album: Album) {
        viewModelScope.launch {
            repository.toggleStar(id = album.id, isStarred = true, isAlbum = true)
                .onSuccess {
                    val current = _starred.value
                    if (current is LoadState.Success) {
                        _starred.value = LoadState.Success(current.items.filterNot { it.id == album.id })
                    }
                }
        }
    }

    fun getCoverArtUrl(coverArtId: String?): String? {
        if (coverArtId.isNullOrEmpty()) return null
        return repository.getCoverArtUrl(coverArtId)
    }

    private fun <T> load(
        target: MutableStateFlow<LoadState<T>>,
        block: suspend () -> Result<List<T>>,
        errorMessage: String,
    ) {
        target.value = LoadState.Loading
        viewModelScope.launch {
            block().fold(
                onSuccess = { items -> target.value = LoadState.Success(items) },
                onFailure = { error ->
                    target.value = LoadState.Error(error.message ?: errorMessage)
                }
            )
        }
    }
}