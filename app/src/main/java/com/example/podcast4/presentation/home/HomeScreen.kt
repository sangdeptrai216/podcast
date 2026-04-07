package com.example.podcast4.presentation.home

import java.net.URLEncoder
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Login
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.podcast4.domain.models.Podcast
import com.example.podcast4.data.repository.AuthRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToDetail: (String, String) -> Unit,
    onNavigateToLogin: () -> Unit,
    authRepository: AuthRepository,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val podcasts by viewModel.podcasts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoggedIn by authRepository.isLoggedIn.collectAsState()
    val userName by authRepository.userName.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var showLogoutDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Đăng xuất") },
            text = { Text("Bạn có chắc chắn muốn đăng xuất khỏi tài khoản $userName?") },
            confirmButton = {
                TextButton(onClick = {
                    authRepository.logout()
                    showLogoutDialog = false
                    Toast.makeText(context, "Đã đăng xuất", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Đăng xuất", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        Text(
                            "Khám phá", 
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 26.sp
                            )
                        )
                        Text(
                            if (isLoggedIn) "Chào mừng, $userName" else "Nghe những gì bạn yêu thích", 
                            style = MaterialTheme.typography.labelMedium, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        if (isLoggedIn) {
                            showLogoutDialog = true
                        } else {
                            onNavigateToLogin()
                        }
                    }) {
                        Icon(
                            imageVector = if (isLoggedIn) Icons.Default.Logout else Icons.Default.Login, 
                            contentDescription = if (isLoggedIn) "Logout" else "Login",
                            tint = if (isLoggedIn) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
        ) {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item {
                    SearchBarModern(
                        query = searchQuery,
                        onQueryChange = {
                            searchQuery = it
                            viewModel.searchPodcasts(it)
                        }
                    )
                }

                if (isLoading && podcasts.isEmpty()) {
                    item {
                        Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                } else {
                    item {
                        SectionHeader("Thịnh hành")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(podcasts.take(5)) { podcast ->
                                FeaturedPodcastItem(podcast) {
                                    val encodedUrl = URLEncoder.encode(podcast.feedUrl, "UTF-8")
                                    onNavigateToDetail(podcast.id, encodedUrl)
                                }
                            }
                        }
                    }

                    item {
                        SectionHeader("Dành cho bạn")
                    }

                    items(podcasts.drop(5).chunked(2)) { pair ->
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            pair.forEach { podcast ->
                                ModernPodcastCard(
                                    podcast = podcast,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        val encodedUrl = URLEncoder.encode(podcast.feedUrl, "UTF-8")
                                        onNavigateToDetail(podcast.id, encodedUrl)
                                    }
                                )
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.Bold,
            color = Color.Black
        ),
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 12.dp)
    )
}

@Composable
fun SearchBarModern(query: String, onQueryChange: (String) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Tìm kiếm podcast...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun FeaturedPodcastItem(podcast: Podcast, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(280.dp)
            .height(180.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box {
            AsyncImage(
                model = podcast.artworkUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(20.dp)
            ) {
                Text(
                    text = podcast.title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = podcast.artist,
                    color = Color.White.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun ModernPodcastCard(podcast: Podcast, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        AsyncImage(
            model = podcast.artworkUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(24.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = podcast.title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                lineHeight = 20.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = podcast.artist,
            style = MaterialTheme.typography.bodySmall.copy(
                lineHeight = 16.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 4.dp)
        )
    }
}
