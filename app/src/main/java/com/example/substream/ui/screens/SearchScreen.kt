package com.example.substream.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.substream.data.api.Album
import com.example.substream.data.api.SearchResult
import com.example.substream.data.api.Song
import com.example.substream.player.PlayerManager
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.util.Locale

// =====================================================================
// 1. MAIN SEARCH SCREEN COMPOSABLE
// =====================================================================

/**
 * Main Composable function for the Search Screen.
 * Provides a reactive search text input, tabbed results for Albums and Songs, and favorite toggles.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onAlbumClick: (Album) -> Unit = {},
    onBackClick: (() -> Unit)? = null,
    viewModel: SearchViewModel = koinViewModel(),
    playerManager: PlayerManager = koinInject()
) {
    val query by viewModel.query.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    onBackClick?.let { onClick ->
                        IconButton(onClick = onClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Text Field Input
            SearchInputField(
                query = query,
                onQueryChange = { viewModel.onQueryChanged(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Search Content / Results Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (val state = uiState) {
                    is SearchState.Idle -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Type a song or album name to search.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(32.dp)
                            )
                        }
                    }

                    is SearchState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    is SearchState.Error -> {
                        ErrorContent(
                            message = state.message,
                            onRetry = { viewModel.search() },
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    is SearchState.Success -> {
                        SearchResultsTabbedContent(
                            searchResult = state.result,
                            onAlbumClick = onAlbumClick,
                            onSongClick = { song -> playerManager.playSong(song) },
                            onToggleStarAlbum = { album -> viewModel.toggleStarAlbum(album) },
                            onToggleStarSong = { song -> viewModel.toggleStarSong(song) },
                            getCoverArtUrl = { coverArt -> viewModel.getCoverArtUrl(coverArt) }
                        )
                    }
                }
            }
        }
    }
}

// =====================================================================
// 2. SEARCH INPUT FIELD COMPOSABLE
// =====================================================================

@Composable
private fun SearchInputField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Artists, albums, songs...") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search"
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear"
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(24.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = modifier
    )
}

// =====================================================================
// 3. TABBED RESULTS COMPOSABLE
// =====================================================================

@Composable
private fun SearchResultsTabbedContent(
    searchResult: SearchResult,
    onAlbumClick: (Album) -> Unit,
    onSongClick: (Song) -> Unit,
    onToggleStarAlbum: (Album) -> Unit,
    onToggleStarSong: (Song) -> Unit,
    getCoverArtUrl: (String?) -> String?,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        "Albums (${searchResult.album.size})",
        "Songs (${searchResult.song.size})"
    )

    if (searchResult.album.isEmpty() && searchResult.song.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No results found.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        when (selectedTabIndex) {
            0 -> {
                if (searchResult.album.isEmpty()) {
                    EmptySectionText(message = "No albums found.")
                } else {
                    SearchAlbumsGrid(
                        albums = searchResult.album,
                        onAlbumClick = onAlbumClick,
                        onToggleStar = onToggleStarAlbum,
                        getCoverArtUrl = getCoverArtUrl
                    )
                }
            }

            1 -> {
                if (searchResult.song.isEmpty()) {
                    EmptySectionText(message = "No songs found.")
                } else {
                    SearchSongsList(
                        songs = searchResult.song,
                        onSongClick = onSongClick,
                        onToggleStar = onToggleStarSong,
                        getCoverArtUrl = getCoverArtUrl
                    )
                }
            }
        }
    }
}

// =====================================================================
// 4. ALBUMS GRID COMPOSABLE
// =====================================================================

@Composable
private fun SearchAlbumsGrid(
    albums: List<Album>,
    onAlbumClick: (Album) -> Unit,
    onToggleStar: (Album) -> Unit,
    getCoverArtUrl: (String?) -> String?
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(
            items = albums,
            key = { album -> album.id }
        ) { album ->
            SearchAlbumCard(
                album = album,
                coverArtUrl = getCoverArtUrl(album.coverArt),
                onClick = { onAlbumClick(album) },
                onToggleStar = { onToggleStar(album) }
            )
        }
    }
}

@Composable
private fun SearchAlbumCard(
    album: Album,
    coverArtUrl: String?,
    onClick: () -> Unit,
    onToggleStar: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box {
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

            Column(modifier = Modifier.padding(12.dp)) {
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
// 5. SONGS LIST COMPOSABLE
// =====================================================================

@Composable
private fun SearchSongsList(
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
    onToggleStar: (Song) -> Unit,
    getCoverArtUrl: (String?) -> String?
) {
    LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(
            items = songs,
            key = { song -> song.id }
        ) { song ->
            SearchSongRowItem(
                song = song,
                coverArtUrl = getCoverArtUrl(song.coverArt),
                onSongClick = { onSongClick(song) },
                onToggleStar = { onToggleStar(song) }
            )
        }
    }
}

@Composable
private fun SearchSongRowItem(
    song: Song,
    coverArtUrl: String?,
    onSongClick: () -> Unit,
    onToggleStar: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSongClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail Image
        AsyncImage(
            model = coverArtUrl,
            contentDescription = song.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Title and Artist/Album Details
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            val subtitle = listOfNotNull(song.artist, song.album).joinToString(" • ")
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Song Duration
        Text(
            text = formatDuration(song.duration),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )

        // Favorite Star Button
        IconButton(onClick = onToggleStar) {
            Icon(
                imageVector = if (song.isStarred) Icons.Filled.Star else Icons.Outlined.StarOutline,
                contentDescription = if (song.isStarred) "Unfavorite" else "Favorite",
                tint = if (song.isStarred) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// =====================================================================
// 6. HELPER COMPONENTS
// =====================================================================

@Composable
private fun EmptySectionText(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatDuration(seconds: Int?): String {
    if (seconds == null || seconds <= 0) return "--:--"
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, remainingSeconds)
}
