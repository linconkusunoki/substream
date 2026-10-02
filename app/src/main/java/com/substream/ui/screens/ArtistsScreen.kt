package com.substream.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.substream.data.api.Artist

/**
 * Grid of artists served by getArtists.view.
 * Rendered inside LibraryScreen, so it has no TopAppBar of its own.
 */
@Composable
fun ArtistsScreen(
    state: LoadState<Artist>,
    onRetry: () -> Unit,
    onArtistClick: (Artist) -> Unit,
    onToggleStar: (Artist) -> Unit,
    getCoverArtUrl: (String?) -> String?,
) {
    LoadStateContent(
        state = state,
        emptyMessage = "No artists found.",
        onRetry = onRetry,
    ) { artists ->
        ArtistsGrid(
            artists = artists,
            onArtistClick = onArtistClick,
            onToggleStar = onToggleStar,
            getCoverArtUrl = getCoverArtUrl,
        )
    }
}

/**
 * Renders a 2-column grid of Artist Cards. Shared by the Library tab, the Favorites tab and the
 * search results, so all three stay identical.
 */
@Composable
fun ArtistsGrid(
    artists: List<Artist>,
    onArtistClick: (Artist) -> Unit,
    onToggleStar: (Artist) -> Unit,
    getCoverArtUrl: (String?) -> String?,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxSize()
    ) {
        items(items = artists, key = { artist -> artist.id }) { artist ->
            ArtistCard(
                artist = artist,
                coverArtUrl = getCoverArtUrl(artist.coverArt),
                onClick = { onArtistClick(artist) },
                onToggleStar = { onToggleStar(artist) }
            )
        }
    }
}

@Composable
fun ArtistCard(
    artist: Artist,
    coverArtUrl: String?,
    onClick: () -> Unit,
    onToggleStar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box {
                // Search results can carry an artist with no artwork at all, so the box is never
                // left empty: the placeholder shows through until the image resolves.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(40.dp)
                    )
                    AsyncImage(
                        model = coverArtUrl,
                        contentDescription = artist.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                IconButton(
                    onClick = onToggleStar,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = if (artist.isStarred) Icons.Filled.Star else Icons.Outlined.StarOutline,
                        contentDescription = if (artist.isStarred) "Unfavorite" else "Favorite",
                        tint = if (artist.isStarred) Color(0xFFFFC107) else Color.White
                    )
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = artist.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                artist.albumCount?.let { count ->
                    Box(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (count == 1) "1 album" else "$count albums",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}