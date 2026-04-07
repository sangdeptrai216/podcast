package com.example.podcast4.presentation.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.podcast4.domain.models.Episode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    onBack: () -> Unit,
    onPlayEpisode: (Episode) -> Unit,
    viewModel: DownloadsViewModel = hiltViewModel()
) {
    val downloadedEpisodes by viewModel.downloadedEpisodes.collectAsState()
    val context = LocalContext.current
    
    val workInfos by WorkManager.getInstance(context)
        .getWorkInfosByTagLiveData("EpisodeDownload")
        .observeAsState(emptyList())

    var episodeToRename by remember { mutableStateOf<Episode?>(null) }
    var newTitle by remember { mutableStateOf("") }

    if (episodeToRename != null) {
        AlertDialog(
            onDismissRequest = { episodeToRename = null },
            title = { Text("Đổi tên podcast") },
            text = {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    label = { Text("Tên mới") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    episodeToRename?.let { viewModel.updateEpisodeTitle(it.id, newTitle) }
                    episodeToRename = null
                }) { Text("Lưu") }
            },
            dismissButton = {
                TextButton(onClick = { episodeToRename = null }) { Text("Hủy") }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "Danh sách tải xuống",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
    ) { padding ->
        if (downloadedEpisodes.isEmpty() && workInfos.none { it.state == WorkInfo.State.RUNNING }) {
            EmptyDownloadsState(Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "${downloadedEpisodes.size} tập podcast đã tải",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                
                items(downloadedEpisodes) { episode ->
                    val workInfo = workInfos.find { 
                        it.tags.contains("episode_${episode.id}") && it.state == WorkInfo.State.RUNNING 
                    }
                    val progress = workInfo?.progress?.getInt("progress", -1) ?: -1

                    PremiumEpisodeItem(
                        episode = episode,
                        progress = progress,
                        onPlay = { onPlayEpisode(episode) },
                        onCancel = { viewModel.cancelDownload(episode.id) },
                        onRemove = { viewModel.removeDownload(episode.id) },
                        onRename = { 
                            episodeToRename = episode
                            newTitle = episode.title
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PremiumEpisodeItem(
    episode: Episode,
    progress: Int,
    onPlay: () -> Unit,
    onCancel: () -> Unit,
    onRemove: () -> Unit,
    onRename: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onPlay),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Podcasts,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = episode.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${episode.duration / 60} phút • ${episode.pubDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Box {
                if (progress in 0..100) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { progress / 100f },
                            modifier = Modifier.size(40.dp),
                            strokeWidth = 3.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = onCancel, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel", modifier = Modifier.size(16.dp))
                        }
                    }
                } else {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Phát") },
                        onClick = { 
                            showMenu = false
                            onPlay()
                        },
                        leadingIcon = { Icon(Icons.Default.PlayArrow, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Đổi tên") },
                        onClick = { 
                            showMenu = false
                            onRename()
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, null) }
                    )
                    Divider()
                    DropdownMenuItem(
                        text = { Text("Xóa bản tải xuống", color = MaterialTheme.colorScheme.error) },
                        onClick = { 
                            showMenu = false
                            onRemove()
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyDownloadsState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Surface(
                modifier = Modifier.size(120.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.CloudDownload,
                        contentDescription = null,
                        modifier = Modifier.size(60.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Chưa có tập phim nào",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Các tập phim bạn tải xuống để nghe ngoại tuyến sẽ xuất hiện tại đây.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
