package com.example.substream

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.substream.ui.screens.AlbumDetailScreen
import com.example.substream.ui.screens.AlbumsScreen
import com.example.substream.ui.theme.SubStreamTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SubStreamTheme {
                val navController = rememberNavController()

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
