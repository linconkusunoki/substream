package com.example.substream.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.substream.data.api.AlbumDetail
import com.example.substream.data.api.Song
import org.koin.androidx.compose.koinViewModel
import java.util.Locale

// =====================================================================
// 1. MAIN SCREEN COMPOSABLE
// =====================================================================

/**
 * Album Detail Screen displaying the album header and full tracklist.
 *
 * @param albumId The ID of the album to load.
 * @param onBackClick Optional callback for back navigation.
 * @param viewModel The Koin-injected AlbumDetailViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    albumId: String,
    onBackClick: (() -> Unit)? = null,
    viewModel: AlbumDetailViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    // Trigger loading album details whenever albumId changes
    LaunchedEffect(albumId) {
        viewModel.loadAlbumDetail(albumId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Detalhes do Álbum") },
                navigationIcon = {
                    onBackClick?.let { onClick ->
                        IconButton(onClick = onClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Voltar",
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is AlbumDetailState.Idle, is AlbumDetailState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is AlbumDetailState.Error -> {
                    ErrorContent(
                        message = state.message,
                        onRetry = { viewModel.loadAlbumDetail(albumId) },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is AlbumDetailState.Success -> {
                    AlbumDetailContent(
                        albumDetail = state.albumDetail,
                        getCoverArtUrl = { coverArtId -> viewModel.getCoverArtUrl(coverArtId) }
                    )
                }
            }
        }
    }
}

// =====================================================================
// 2. ALBUM DETAIL CONTENT COMPOSABLE
// =====================================================================

/**
 * Main content layout rendering header info and LazyColumn for tracklist.
 */
@Composable
private fun AlbumDetailContent(
    albumDetail: AlbumDetail,
    getCoverArtUrl: (String?) -> String?,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize()
    ) {
        // Album Header (Cover Art + Title + Artist)
        item {
            AlbumHeader(
                albumDetail = albumDetail,
                coverArtUrl = getCoverArtUrl(albumDetail.coverArt)
            )
        }

        // Section Title
        item {
            Text(
                text = "Faixas",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }

        // Song Tracklist
        itemsIndexed(
            items = albumDetail.song,
            key = { _, song -> song.id }
        ) { index, song ->
            SongRowItem(
                index = index + 1,
                song = song
            )
        }
    }
}

// =====================================================================
// 3. ALBUM HEADER COMPOSABLE
// =====================================================================

/**
 * Header component featuring a large cover image, album name, artist, and metadata.
 */
@Composable
private fun AlbumHeader(
    albumDetail: AlbumDetail,
    coverArtUrl: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Large Album Cover Art
        AsyncImage(
            model = coverArtUrl,
            contentDescription = albumDetail.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(220.dp)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Album Title
        Text(
            text = albumDetail.name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Artist Name
        Text(
            text = albumDetail.artist ?: "Artista Desconhecido",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Metadata Subtitle (Year & Song Count)
        val metadataText = listOfNotNull(
            albumDetail.year?.toString(),
            "${albumDetail.song.size} faixas"
        ).joinToString(" • ")

        if (metadataText.isNotEmpty()) {
            Text(
                text = metadataText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center
            )
        }
    }
}

// =====================================================================
// 4. SONG ROW ITEM COMPOSABLE
// =====================================================================

/**
 * Individual track row component in the tracklist.
 */
@Composable
private fun SongRowItem(
    index: Int,
    song: Song,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Track number
        Text(
            text = song.track?.toString() ?: index.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(32.dp),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Song Title and Artist
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (!song.artist.isNullOrEmpty()) {
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Formatted Duration (mm:ss)
        Text(
            text = formatDuration(song.duration),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

// =====================================================================
// 5. HELPER FUNCTIONS & ERROR UI
// =====================================================================

/**
 * Helper function to format duration in seconds to "mm:ss" format.
 */
private fun formatDuration(seconds: Int?): String {
    if (seconds == null || seconds <= 0) return "--:--"
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, remainingSeconds)
}

/**
 * Error display layout with retry button.
 */
@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
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
            Text("Tentar Novamente")
        }
    }
}
