package com.substream.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.substream.data.api.Album
import com.substream.data.api.Artist
import com.substream.data.api.Playlist
import com.substream.data.repository.SubsonicRepository
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
 * and starred albums and artists (getStarred2.view).
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

    private val _starredArtists = MutableStateFlow<LoadState<Artist>>(LoadState.Idle)
    val starredArtists: StateFlow<LoadState<Artist>> = _starredArtists.asStateFlow()

    fun loadArtists() = load(_artists, repository::getArtists, "Could not load artists")

    fun loadPlaylists() = load(_playlists, repository::getPlaylists, "Could not load playlists")

    fun loadStarred() {
        load(_starred, repository::getStarredAlbums, "Could not load favorites")
        load(_starredArtists, repository::getStarredArtists, "Could not load favorite artists")
    }

    /**
     * Toggles an artist's starred state in the Artists tab.
     */
    fun toggleStarArtist(artist: Artist) = toggleStarArtist(_artists, artist)

    /**
     * Toggles an artist's starred state in the Favorites tab, removing it from the list when
     * the toggle unstars it.
     */
    fun toggleStarredArtist(artist: Artist) = toggleStarArtist(_starredArtists, artist)

    /**
     * Stars or unstars an artist and reflects the new state in [target] without a refetch.
     */
    private fun toggleStarArtist(target: MutableStateFlow<LoadState<Artist>>, artist: Artist) {
        viewModelScope.launch {
            repository.toggleStar(id = artist.id, isStarred = artist.isStarred, isArtist = true)
                .onSuccess { newStarred ->
                    val current = target.value
                    if (current !is LoadState.Success) return@onSuccess
                    val starredArtist = artist.copy(starred = if (newStarred) "starred" else null)
                    target.value = LoadState.Success(
                        if (newStarred) {
                            (current.items + starredArtist).distinctBy { it.id }
                        } else {
                            current.items.filterNot { it.id == artist.id }
                        }
                    )
                }
        }
    }

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