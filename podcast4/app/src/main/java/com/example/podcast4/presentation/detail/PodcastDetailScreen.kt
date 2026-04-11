package com.example.podcast4.presentation.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.media3.session.MediaController
import android.os.Bundle
import com.example.podcast4.data.repository.AuthRepository
import com.example.podcast4.domain.models.Episode
import com.example.podcast4.playEpisodes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PodcastDetailScreen(
    onBack: () -> Unit,
    onPlayEpisode: (Episode) -> Unit,
    onNavigateToLogin: () -> Unit,
    mediaController: MediaController?,
    viewModel: PodcastDetailViewModel = hiltViewModel()
) {
    val episodes by viewModel.episodes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    
    var endTime by remember { mutableLongStateOf(0L) }
    var sleepTimerRemaining by remember { mutableIntStateOf(0) }
    var showTimerDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(mediaController) {
        val controller = mediaController ?: return@LaunchedEffect
        while (true) {
            endTime = controller.sessionExtras.getLong("SLEEP_TIMER_END_TIME", 0L)
            if (endTime > 0) {
                val remaining = ((endTime - System.currentTimeMillis()) / 1000).toInt()
                sleepTimerRemaining = if (remaining > 0) remaining else 0
            } else {
                sleepTimerRemaining = 0
            }
            delay(1000)
        }
    }

    if (showTimerDialog) {
        SleepTimerDialog(
            onDismiss = { showTimerDialog = false },
            onSetTimer = { minutes ->
                val args = Bundle().apply { putInt("MINUTES", minutes) }
                mediaController?.sendCustomCommand(
                    androidx.media3.session.SessionCommand("SET_SLEEP_TIMER", Bundle.EMPTY),
                    args
                )
                showTimerDialog = false
            }
        )
    }

    val workInfos by WorkManager.getInstance(context)
        .getWorkInfosByTagLiveData("EpisodeDownload")
        .observeAsState(initial = emptyList<WorkInfo>())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            MediumTopAppBar(
                title = { Text("Danh sách Podcast", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (sleepTimerRemaining > 0) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Timer,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = formatRemaining(sleepTimerRemaining),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                IconButton(
                                    onClick = {
                                        mediaController?.sendCustomCommand(
                                            androidx.media3.session.SessionCommand("CANCEL_SLEEP_TIMER", Bundle.EMPTY),
                                            Bundle.EMPTY
                                        )
                                    },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                    IconButton(onClick = { showTimerDialog = true }) {
                        Icon(Icons.Default.Timer, contentDescription = "Sleep Timer")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isLoading && episodes.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(episodes) { index, episode ->
                        val workInfo = workInfos.find { 
                            it.tags.contains("episode_${episode.id}") &&
                            (it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED)
                        }
                        
                        val progress = if (workInfo?.state == WorkInfo.State.RUNNING) {
                            workInfo.progress.getInt("progress", 0)
                        } else if (workInfo?.state == WorkInfo.State.ENQUEUED) 0 else -1

                        ModernEpisodeItem(
                            episode = episode, 
                            progress = progress,
                            onPlay = { playEpisodes(mediaController, episodes, index) },
                            onDownload = { 
                                if (isLoggedIn) {
                                    viewModel.downloadEpisode(episode)
                                } else {
                                    scope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = "Vui lòng đăng nhập để tải xuống!",
                                            actionLabel = "Đăng nhập",
                                            duration = SnackbarDuration.Short
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            onNavigateToLogin()
                                        }
                                    }
                                }
                            },
                            onCancel = { viewModel.cancelDownload(episode.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SleepTimerDialog(onDismiss: () -> Unit, onSetTimer: (Int) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Hẹn giờ tắt") },
        text = {
            Column {
                listOf(5, 15, 30, 45, 60).forEach { minutes ->
                    TextButton(
                        onClick = { onSetTimer(minutes) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("$minutes phút")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}

private fun formatRemaining(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return if (m > 0) "${m}m ${s}s" else "${s}s"
}

@Composable
fun ModernEpisodeItem(
    episode: Episode, 
    progress: Int,
    onPlay: () -> Unit, 
    onDownload: () -> Unit,
    onCancel: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onPlay),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(episode.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 2)
                Text("${episode.pubDate} • ${episode.duration / 60} phút", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!episode.isDownloaded) {
                    if (progress in 0..100) {
                        IconButton(onClick = onCancel) {
                            CircularProgressIndicator(progress = { progress / 100f }, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            Icon(Icons.Default.Close, null, modifier = Modifier.size(12.dp))
                        }
                    } else {
                        IconButton(onClick = onDownload) {
                            Icon(Icons.Default.Download, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                } else {
                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(24.dp))
                }

                Spacer(modifier = Modifier.width(8.dp))

                FloatingActionButton(
                    onClick = onPlay,
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(12.dp),
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play")
                }
            }
        }
    }
}
