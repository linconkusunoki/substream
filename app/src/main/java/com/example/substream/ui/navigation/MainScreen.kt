package com.example.substream.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.substream.data.preferences.ServerPreferences
import com.example.substream.player.PlayerManager
import com.example.substream.ui.components.PlayerBar
import com.example.substream.ui.screens.AlbumDetailScreen
import com.example.substream.ui.screens.AlbumsScreen
import com.example.substream.ui.screens.LibraryScreen
import com.example.substream.ui.screens.LoginScreen
import com.example.substream.ui.screens.NowPlayingScreen
import com.example.substream.ui.screens.SearchScreen
import com.example.substream.ui.screens.SettingsScreen
import org.koin.compose.koinInject

/**
 * Root of the app: shows [LoginScreen] until a server is stored, then the main
 * Material 3 Scaffold with Bottom Navigation Bar, fixed Mini Player above the bottom
 * bar, and a NavHost for app navigation.
 *
 * The branch is keyed on `ServerPreferences.config`, so saving credentials from the
 * login form (or logging out from Settings) swaps the whole tree without a navigation
 * event, and the main NavHost is disposed on logout.
 */
@Composable
fun MainScreen(
    serverPreferences: ServerPreferences = koinInject(),
) {
    val config by serverPreferences.config.collectAsState()

    if (config == null) {
        LoginScreen()
    } else {
        MainNavScreen()
    }
}

@Composable
private fun MainNavScreen(
    navController: NavHostController = rememberNavController(),
    playerManager: PlayerManager = koinInject(),
) {
    val playerState by playerManager.playerState.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    // List of bottom navigation bar tabs
    val bottomNavItems = listOf(
        BottomNavItem.Home,
        BottomNavItem.Library,
        BottomNavItem.Search,
        BottomNavItem.Settings,
    )

    // Show BottomBar on top-level screens (hide on full-screen player)
    val isNowPlayingRoute = currentRoute == Screen.NowPlaying.route
    val showBottomBar = !isNowPlayingRoute

    // Single entry point to switch tabs. Using it for *every* jump to a tab
    // (bottom bar clicks AND in-screen shortcuts) keeps ViewModel state alive
    // via saveState/restoreState instead of pushing duplicate destinations.
    val navigateToTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        // This Scaffold owns the window insets for everything below it, so destinations
        // pass contentWindowInsets = WindowInsets(0.dp) and manage only their TopAppBar:
        //  - status bar: handled here (zeroed) + again by each TopAppBar, hence the zero.
        //  - navigation bar: the bottomBar below already sits on it; letting a nested
        //    Scaffold add it too leaves a gap between the tab bar and every list, and
        //    clips the last row. Modifier.padding does not consume insets, which is why
        //    each destination has to opt out explicitly.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Column {
                // Mini Player Bar: visible when a song is loaded and not in full NowPlaying screen
                if (!isNowPlayingRoute) {
                    PlayerBar(
                        playerState = playerState,
                        onPlayPauseClick = { playerManager.togglePlayPause() },
                        onStopClick = { playerManager.stop() },
                        getCoverArtUrl = { coverArt -> playerManager.getCoverArtUrl(coverArt) },
                        onBarClick = { navController.navigate(Screen.NowPlaying.route) },
                    )
                }

                // Bottom Navigation Bar with animation
                AnimatedVisibility(
                    visible = showBottomBar,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it }),
                ) {
                    NavigationBar {
                        bottomNavItems.forEach { item ->
                            val isSelected = currentDestination?.hierarchy?.any { it.route == item.route } == true

                            NavigationBarItem(
                                selected = isSelected,
                                onClick = {
                                    if (!isSelected) navigateToTab(item.route)
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.title,
                                    )
                                },
                                label = { Text(text = item.title) },
                            )
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
            ) {
                // 1. Reconfigure the server (Settings > Change server)
                composable(Screen.Login.route) {
                    LoginScreen(onSaved = { navController.popBackStack() })
                }

                // 2. Home
                composable(Screen.Home.route) {
                    AlbumsScreen(
                        title = "Home",
                        onAlbumClick = { album ->
                            navController.navigate(Screen.AlbumDetail.createRoute(album.id))
                        },
                        onSearchClick = {
                            navigateToTab(Screen.Search.route)
                        },
                    )
                }

                // 2. Library (Albums / Artists / Playlists / Favorites)
                composable(Screen.Library.route) {
                    LibraryScreen(
                        onAlbumClick = { album ->
                            navController.navigate(Screen.AlbumDetail.createRoute(album.id))
                        },
                    )
                }

                // 3. Search
                composable(Screen.Search.route) {
                    SearchScreen(
                        onAlbumClick = { album ->
                            navController.navigate(Screen.AlbumDetail.createRoute(album.id))
                        },
                        onBackClick = { navController.popBackStack() },
                    )
                }

                // 4. Settings
                composable(Screen.Settings.route) {
                    SettingsScreen(
                        onChangeServer = { navController.navigate(Screen.Login.route) },
                    )
                }

                // 5. Album Detail (with albumId argument)
                composable(
                    route = Screen.AlbumDetail.route,
                    arguments = listOf(
                        navArgument(Screen.AlbumDetail.ARG_ALBUM_ID) { type = NavType.StringType },
                    ),
                ) { backStackEntry ->
                    val albumId = backStackEntry.arguments?.getString(Screen.AlbumDetail.ARG_ALBUM_ID) ?: ""
                    AlbumDetailScreen(
                        albumId = albumId,
                        onBackClick = { navController.popBackStack() },
                    )
                }

                // 6. Player Expandido (Now Playing)
                composable(Screen.NowPlaying.route) {
                    NowPlayingScreen(
                        onBackClick = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}
