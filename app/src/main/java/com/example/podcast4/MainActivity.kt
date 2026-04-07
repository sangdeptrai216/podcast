package com.example.podcast4

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.podcast4.data.repository.AuthRepository
import com.example.podcast4.presentation.auth.LoginScreen
import com.example.podcast4.presentation.detail.PodcastDetailScreen
import com.example.podcast4.presentation.downloads.DownloadsScreen
import com.example.podcast4.presentation.home.HomeScreen
import com.example.podcast4.presentation.player.components.MiniPlayer
import com.example.podcast4.presentation.player.rememberMediaController
import com.example.podcast4.ui.theme.Podcast4Theme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Podcast4Theme {
                val context = LocalContext.current
                val mediaController = rememberMediaController(context)
                val navController = rememberNavController()
                
                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    bottomBar = {
                        Column {
                            MiniPlayer(mediaController, onNavigateToPlayer = {})
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 8.dp
                            ) {
                                val navBackStackEntry by navController.currentBackStackEntryAsState()
                                val currentDestination = navBackStackEntry?.destination
                                
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                                    label = { Text("Home") },
                                    selected = currentDestination?.hierarchy?.any { it.route == "home" } == true,
                                    onClick = {
                                        navController.navigate("home") {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Download, contentDescription = null) },
                                    label = { Text("Downloads") },
                                    selected = currentDestination?.hierarchy?.any { it.route == "downloads" } == true,
                                    onClick = {
                                        navController.navigate("downloads") {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                            }
                        }
                    }
                ) { padding ->
                    Surface(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        AppNavigation(navController, mediaController, authRepository)
                    }
                }
            }
        }
    }
}

@Composable
fun AppNavigation(
    navController: androidx.navigation.NavHostController,
    mediaController: MediaController?,
    authRepository: AuthRepository
) {
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onNavigateToDetail = { podcastId, feedUrl ->
                    navController.navigate("detail/$podcastId/$feedUrl")
                },
                onNavigateToLogin = {
                    navController.navigate("login")
                },
                authRepository = authRepository
            )
        }
        
        composable("login") {
            val isLoggedIn by authRepository.isLoggedIn.collectAsState()
            if (isLoggedIn) {
                // Tự động quay về home khi đã login
                LaunchedEffect(Unit) {
                    navController.popBackStack()
                }
            } else {
                LoginScreen(onLoginSuccess = { navController.popBackStack() })
            }
        }
        
        composable("downloads") {
            DownloadsScreen(
                onBack = { navController.popBackStack() },
                onPlayEpisode = { episode -> playEpisodes(mediaController, listOf(episode), 0) }
            )
        }
        
        composable(
            route = "detail/{podcastId}/{feedUrl}",
            arguments = listOf(
                navArgument("podcastId") { type = NavType.StringType },
                navArgument("feedUrl") { type = NavType.StringType }
            )
        ) {
            PodcastDetailScreen(
                onBack = { navController.popBackStack() },
                onPlayEpisode = { episode -> playEpisodes(mediaController, listOf(episode), 0) },
                onNavigateToLogin = { navController.navigate("login") },
                mediaController = mediaController
            )
        }
    }
}

fun playEpisodes(mediaController: MediaController?, episodes: List<com.example.podcast4.domain.models.Episode>, startIndex: Int) {
    mediaController?.let { controller ->
        val mediaItems = episodes.map { episode ->
            val uriToPlay = if (episode.isDownloaded && !episode.localAudioPath.isNullOrEmpty()) {
                Uri.parse(episode.localAudioPath)
            } else {
                Uri.parse(episode.audioUrl)
            }
            MediaItem.Builder()
                .setMediaId(episode.id)
                .setUri(uriToPlay)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(episode.title)
                        .setArtist(episode.artist)
                        .setArtworkUri(Uri.parse(episode.artworkUrl))
                        .build()
                ).build()
        }
        controller.setMediaItems(mediaItems, startIndex, 0L)
        controller.prepare()
        controller.play()
    }
}
