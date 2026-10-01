package com.example.substream.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.substream.data.api.Album
import org.koin.androidx.compose.koinViewModel

/**
 * Library hub: the bottom-nav "Library" destination with an internal tab bar
 * (Albums / Artists / Playlists / Favorites).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onAlbumClick: (Album) -> Unit = {},
    viewModel: LibraryViewModel = koinViewModel()
) {
    val artistsState by viewModel.artists.collectAsState()
    val playlistsState by viewModel.playlists.collectAsState()
    val starredState by viewModel.starred.collectAsState()

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    // Load every section once when the hub enters composition: tab labels show the counts.
    LaunchedEffect(Unit) {
        viewModel.loadArtists()
        viewModel.loadPlaylists()
        viewModel.loadStarred()
    }

    val counts = listOf(
        null,
        (artistsState as? LoadState.Success)?.items?.size,
        (playlistsState as? LoadState.Success)?.items?.size,
        (starredState as? LoadState.Success)?.items?.size,
    )

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Library", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 16.dp,
                    containerColor = MaterialTheme.colorScheme.surface,
                ) {
                    LibraryTab.entries.forEachIndexed { index, tab ->
                        val count = counts[index]
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = if (count != null) "${tab.label} ($count)" else tab.label,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (LibraryTab.entries[selectedTab]) {
                LibraryTab.Albums -> AlbumsScreen(
                    onAlbumClick = onAlbumClick,
                    showTopBar = false,
                )

                LibraryTab.Artists -> ArtistsScreen(
                    state = artistsState,
                    onRetry = viewModel::loadArtists,
                    getCoverArtUrl = viewModel::getCoverArtUrl,
                )

                LibraryTab.Playlists -> PlaylistsScreen(
                    state = playlistsState,
                    onRetry = viewModel::loadPlaylists,
                    getCoverArtUrl = viewModel::getCoverArtUrl,
                )

                LibraryTab.Favorites -> FavoritesScreen(
                    state = starredState,
                    onRetry = viewModel::loadStarred,
                    onRemove = viewModel::removeStarred,
                    onAlbumClick = onAlbumClick,
                    getCoverArtUrl = viewModel::getCoverArtUrl,
                )
            }
        }
    }
}

/** Internal tabs of the Library hub. */
private enum class LibraryTab(val label: String) {
    Albums("Albums"),
    Artists("Artists"),
    Playlists("Playlists"),
    Favorites("Favorites"),
}

/**
 * Renders a [LoadState]: spinner while loading, error with retry, empty message or content.
 */
@Composable
fun <T> LoadStateContent(
    state: LoadState<T>,
    emptyMessage: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (List<T>) -> Unit,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (state) {
            is LoadState.Idle, is LoadState.Loading -> CircularProgressIndicator()

            is LoadState.Error -> ErrorContent(
                message = state.message,
                onRetry = onRetry
            )

            is LoadState.Success -> {
                if (state.items.isEmpty()) {
                    Text(
                        text = emptyMessage,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    content(state.items)
                }
            }
        }
    }
}