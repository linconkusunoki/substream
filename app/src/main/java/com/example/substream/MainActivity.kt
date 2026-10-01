package com.example.substream

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.substream.player.PlayerManager
import com.example.substream.ui.components.PlayerBar
import com.example.substream.ui.screens.AlbumDetailScreen
import com.example.substream.ui.screens.AlbumsScreen
import com.example.substream.ui.screens.NowPlayingScreen
import com.example.substream.ui.screens.SearchScreen
import com.example.substream.ui.theme.SubStreamTheme
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val playerManager: PlayerManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request notification permission for Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        setContent {
            SubStreamTheme {
                val navController = rememberNavController()
                val playerState by playerManager.playerState.collectAsState()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                Scaffold(
                    bottomBar = {
                        if (currentRoute != "now_playing") {
                            PlayerBar(
                                playerState = playerState,
                                onPlayPauseClick = { playerManager.togglePlayPause() },
                                onStopClick = { playerManager.stop() },
                                getCoverArtUrl = { coverArt -> playerManager.getCoverArtUrl(coverArt) },
                                onBarClick = { navController.navigate("now_playing") }
                            )
                        }
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
                                    },
                                    onSearchClick = {
                                        navController.navigate("search")
                                    }
                                )
                            }

                            composable("search") {
                                SearchScreen(
                                    onAlbumClick = { album ->
                                        navController.navigate("album_detail/${album.id}")
                                    },
                                    onBackClick = { navController.popBackStack() }
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

                            composable("now_playing") {
                                NowPlayingScreen(
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
