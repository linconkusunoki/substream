package com.substream.ui.screens

import androidx.compose.runtime.Composable
import com.substream.data.api.Album

/**
 * Starred albums served by getStarred2.view.
 * Rendered inside LibraryScreen, so it has no TopAppBar of its own.
 */
@Composable
fun FavoritesScreen(
    state: LoadState<Album>,
    onRetry: () -> Unit,
    onRemove: (Album) -> Unit,
    onAlbumClick: (Album) -> Unit,
    getCoverArtUrl: (String?) -> String?,
) {
    LoadStateContent(
        state = state,
        emptyMessage = "Your favorite items will show up here.",
        onRetry = onRetry,
    ) { albums ->
        AlbumGrid(
            albums = albums,
            onAlbumClick = onAlbumClick,
            onToggleStar = onRemove,
            getCoverArtUrl = getCoverArtUrl,
        )
    }
}