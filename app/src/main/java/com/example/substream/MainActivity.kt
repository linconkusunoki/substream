package com.example.substream

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.substream.player.PlayerManager
import com.example.substream.ui.components.PlayerBar
import com.example.substream.ui.screens.AlbumDetailScreen
import com.example.substream.ui.screens.AlbumsScreen
import com.example.substream.ui.theme.SubStreamTheme
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val playerManager: PlayerManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SubStreamTheme {
                val navController = rememberNavController()
                val playerState by playerManager.playerState.collectAsState()

                Scaffold(
                    bottomBar = {
                        PlayerBar(
                            playerState = playerState,
                            onPlayPauseClick = { playerManager.togglePlayPause() },
                            onStopClick = { playerManager.stop() },
                            getCoverArtUrl = { coverArt -> playerManager.getCoverArtUrl(coverArt) }
                        )
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        NavHost(
                            navController = navController,
                            startDestination = "albums"
                        ) {
                            composable("albums") {
                                AlbumsScreen(
                                    onAlbumClick = { album ->
                                        navController.navigate("album_detail/${album.id}")
                                    }
                                )
                            }

                            composable(
                                route = "album_detail/{albumId}",
                                arguments = listOf(
                                    navArgument("albumId") { type = NavType.StringType }
                                )
                            ) { backStackEntry ->
                                val albumId = backStackEntry.arguments?.getString("albumId") ?: ""
                                AlbumDetailScreen(
                                    albumId = albumId,
                                    onBackClick = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        playerManager.release()
    }
}
