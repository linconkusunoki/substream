package com.substream.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.substream.data.api.Album
import org.koin.androidx.compose.koinViewModel

// =====================================================================
// 1. MAIN SCREEN COMPOSABLE
// =====================================================================

/**
 * Main Composable function for the Albums Screen.
 * Uses Koin to inject the AlbumsViewModel and loads albums automatically upon entry.
 *
 * Set [showTopBar] to false to embed the list inside another screen (e.g. the Library tab).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumsScreen(
    onAlbumClick: (Album) -> Unit = {},
    onSearchClick: () -> Unit = {},
    title: String = "Albums",
    showTopBar: Boolean = true,
    viewModel: AlbumsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Trigger initial load when the screen enters the Composition
    LaunchedEffect(Unit) {
        viewModel.loadAlbums()
    }

    val content: @Composable (PaddingValues) -> Unit = { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is AlbumsState.Idle, is AlbumsState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is AlbumsState.Error -> {
                    ErrorContent(
                        message = state.message,
                        onRetry = { viewModel.loadAlbums() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is AlbumsState.Success -> {
                    if (state.albums.isEmpty()) {
                        Text(
                            text = "No albums found.",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        AlbumGrid(
                            albums = state.albums,
                            onAlbumClick = onAlbumClick,
                            onToggleStar = { album -> viewModel.toggleStarAlbum(album) },
                            getCoverArtUrl = { coverArt -> viewModel.getCoverArtUrl(coverArt) }
                        )
                    }
                }
            }
        }
    }

    if (!showTopBar) {
        content(PaddingValues(0.dp))
        return
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        content(paddingValues)
    }
}

// =====================================================================
// 2. GRID COMPOSABLE
// =====================================================================

/**
 * Renders a 2-column grid of Album Cards using LazyVerticalGrid.
 */
@Composable
fun AlbumGrid(
    albums: List<Album>,
    onAlbumClick: (Album) -> Unit,
    onToggleStar: (Album) -> Unit,
    getCoverArtUrl: (String?) -> String?,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxSize()
    ) {
        items(
            items = albums,
            key = { album -> album.id }
        ) { album ->
            AlbumCard(
                album = album,
                coverArtUrl = getCoverArtUrl(album.coverArt),
                onClick = { onAlbumClick(album) },
                onToggleStar = { onToggleStar(album) }
            )
        }
    }
}

// =====================================================================
// 3. ITEM CARD COMPOSABLE
// =====================================================================

/**
 * Card component to represent an individual Album item.
 */
@Composable
fun AlbumCard(
    album: Album,
    coverArtUrl: String?,
    onClick: () -> Unit,
    onToggleStar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box {
                // Album Cover Art loaded via Coil
                AsyncImage(
                    model = coverArtUrl,
                    contentDescription = album.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                )

                // Favorite Star Button on Album Cover
                IconButton(
                    onClick = onToggleStar,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = if (album.isStarred) Icons.Filled.Star else Icons.Outlined.StarOutline,
                        contentDescription = if (album.isStarred) "Unfavorite" else "Favorite",
                        tint = if (album.isStarred) Color(0xFFFFC107) else Color.White
                    )
                }
            }

            // Album Details (Title and Artist)
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = album.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = album.artist ?: "Unknown Artist",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// =====================================================================
// 4. ERROR CONTENT COMPOSABLE
// =====================================================================

/**
 * Display component for error state with a retry button.
 */
@Composable
fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onRetry) {
            Text("Try Again")
        }
    }
}
