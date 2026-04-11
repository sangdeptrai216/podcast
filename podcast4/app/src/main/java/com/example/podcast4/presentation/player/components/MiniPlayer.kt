package com.example.podcast4.presentation.player.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun MiniPlayer(
    mediaController: MediaController?,
    onNavigateToPlayer: () -> Unit
) {
    if (mediaController == null) return

    var isPlaying by remember { mutableStateOf(mediaController.isPlaying) }
    var currentMediaItem by remember { mutableStateOf(mediaController.currentMediaItem) }
    var playbackSpeed by remember { mutableStateOf(mediaController.playbackParameters.speed) }
    var playbackState by remember { mutableIntStateOf(mediaController.playbackState) }
    
    var showTimerMenu by remember { mutableStateOf(false) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var showVolumeMenu by remember { mutableStateOf(false) }
    var showCustomTimerDialog by remember { mutableStateOf(false) }
    var customMinutes by remember { mutableStateOf("") }
    
    var currentPosition by remember { mutableLongStateOf(mediaController.currentPosition) }
    var duration by remember { mutableLongStateOf(mediaController.duration) }
    var endTime by remember { mutableLongStateOf(0L) }
    var remainingSeconds by remember { mutableIntStateOf(0) }
    
    var volume by remember { mutableFloatStateOf(mediaController.volume) }
    var isMuted by remember { mutableStateOf(false) }
    var preMuteVolume by remember { mutableFloatStateOf(1.0f) }

    var offsetX by remember { mutableFloatStateOf(0f) }

    DisposableEffect(mediaController) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                isPlaying = isPlayingNow
            }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                currentMediaItem = mediaItem
                duration = mediaController.duration
            }
            override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
                playbackSpeed = playbackParameters.speed
            }
            override fun onPlaybackStateChanged(state: Int) {
                playbackState = state
            }
            override fun onVolumeChanged(volumeNow: Float) {
                volume = volumeNow
            }
        }
        mediaController.addListener(listener)
        isPlaying = mediaController.isPlaying
        currentMediaItem = mediaController.currentMediaItem
        playbackSpeed = mediaController.playbackParameters.speed
        duration = mediaController.duration
        volume = mediaController.volume
        playbackState = mediaController.playbackState
        
        onDispose {
            mediaController.removeListener(listener)
        }
    }

    LaunchedEffect(mediaController, isPlaying) {
        while (true) {
            currentPosition = mediaController.currentPosition
            duration = if (mediaController.duration > 0) mediaController.duration else 0L
            endTime = mediaController.sessionExtras.getLong("SLEEP_TIMER_END_TIME", 0L)
            if (endTime > 0) {
                val remaining = ((endTime - System.currentTimeMillis()) / 1000).toInt()
                remainingSeconds = if (remaining > 0) remaining else 0
            } else {
                remainingSeconds = 0
            }
            delay(1000)
        }
    }

    if (currentMediaItem == null) return

    val title = currentMediaItem?.mediaMetadata?.title?.toString() ?: "Unknown Episode"
    val subtitle = currentMediaItem?.mediaMetadata?.artist?.toString() ?: "Podcast"
    val artworkUrl = currentMediaItem?.mediaMetadata?.artworkUri?.toString()

    if (showCustomTimerDialog) {
        AlertDialog(
            onDismissRequest = { showCustomTimerDialog = false },
            title = { Text("Hẹn giờ tắt (phút)") },
            text = {
                OutlinedTextField(
                    value = customMinutes,
                    onValueChange = { if (it.all { char -> char.isDigit() }) customMinutes = it },
                    label = { Text("Nhập số phút") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val mins = customMinutes.toIntOrNull() ?: 0
                    if (mins > 0) {
                        val bundle = Bundle().apply { putInt("MINUTES", mins) }
                        mediaController.sendCustomCommand(SessionCommand("SET_SLEEP_TIMER", Bundle.EMPTY), bundle)
                    }
                    showCustomTimerDialog = false
                    customMinutes = ""
                }) { Text("Bắt đầu") }
            },
            dismissButton = {
                TextButton(onClick = { showCustomTimerDialog = false }) { Text("Hủy") }
            }
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .offset { IntOffset(offsetX.roundToInt(), 0) }
            .draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState { delta -> offsetX += delta },
                onDragStopped = {
                    if (offsetX > 150) { 
                        if (mediaController.hasPreviousMediaItem()) {
                            mediaController.seekToPreviousMediaItem()
                            mediaController.prepare()
                            mediaController.play()
                        } else {
                            mediaController.seekTo(0)
                        }
                    }
                    else if (offsetX < -150) { 
                        if (mediaController.hasNextMediaItem()) {
                            mediaController.seekToNextMediaItem()
                            mediaController.prepare()
                            mediaController.play()
                        }
                    }
                    offsetX = 0f
                }
            )
            .clickable { onNavigateToPlayer() },
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 8.dp
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(contentAlignment = Alignment.Center) {
                    AsyncImage(
                        model = artworkUrl,
                        contentDescription = "Artwork",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(40.dp).clip(MaterialTheme.shapes.small)
                    )
                    if (playbackState == Player.STATE_BUFFERING) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    }
                }
                
                Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                    Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${formatMillis(currentPosition)} / ${formatMillis(duration)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Volume Menu
                    Box {
                        IconButton(onClick = { showVolumeMenu = true }, modifier = Modifier.size(32.dp)) {
                            Icon(if (volume == 0f || isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp, null, modifier = Modifier.size(18.dp))
                        }
                        DropdownMenu(expanded = showVolumeMenu, onDismissRequest = { showVolumeMenu = false }) {
                            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    if (isMuted) { mediaController.volume = preMuteVolume; isMuted = false }
                                    else { preMuteVolume = mediaController.volume; mediaController.volume = 0f; isMuted = true }
                                }) { Icon(if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp, null) }
                                Slider(value = volume, onValueChange = { mediaController.volume = it; if (it > 0) isMuted = false }, modifier = Modifier.width(120.dp))
                            }
                        }
                    }

                    // Speed Menu
                    Box {
                        TextButton(onClick = { showSpeedMenu = true }, contentPadding = PaddingValues(0.dp), modifier = Modifier.height(32.dp).width(40.dp)) {
                            Text("${if (playbackSpeed % 1f == 0f) playbackSpeed.toInt() else playbackSpeed}x", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        DropdownMenu(expanded = showSpeedMenu, onDismissRequest = { showSpeedMenu = false }) {
                            listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f).forEach { speed ->
                                DropdownMenuItem(text = { Text("${speed}x") }, onClick = { mediaController.setPlaybackSpeed(speed); showSpeedMenu = false })
                            }
                        }
                    }

                    // Timer Menu
                    Box {
                        IconButton(onClick = { showTimerMenu = true }, modifier = Modifier.size(32.dp)) {
                            BadgedBox(badge = { if (remainingSeconds > 0) { Badge { Text(formatRemaining(remainingSeconds), fontSize = 7.sp) } } }) {
                                Icon(Icons.Default.Timer, null, modifier = Modifier.size(18.dp), tint = if (remainingSeconds > 0) MaterialTheme.colorScheme.primary else LocalContentColor.current)
                            }
                        }
                        DropdownMenu(expanded = showTimerMenu, onDismissRequest = { showTimerMenu = false }) {
                            if (remainingSeconds > 0) {
                                DropdownMenuItem(
                                    text = { Text("Hủy hẹn giờ", color = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        val bundle = Bundle().apply { putInt("MINUTES", 0) }
                                        mediaController.sendCustomCommand(SessionCommand("SET_SLEEP_TIMER", Bundle.EMPTY), bundle)
                                        showTimerMenu = false
                                    },
                                    leadingIcon = { Icon(Icons.Default.TimerOff, null, tint = MaterialTheme.colorScheme.error) }
                                )
                                Divider()
                            }
                            listOf(15, 30, 60).forEach { mins ->
                                DropdownMenuItem(text = { Text("$mins phút") }, onClick = {
                                    val bundle = Bundle().apply { putInt("MINUTES", mins) }
                                    mediaController.sendCustomCommand(SessionCommand("SET_SLEEP_TIMER", Bundle.EMPTY), bundle)
                                    showTimerMenu = false
                                })
                            }
                            DropdownMenuItem(text = { Text("Tùy chỉnh...") }, onClick = { showTimerMenu = false; showCustomTimerDialog = true })
                        }
                    }
                }
            }

            // Dòng nút điều khiển chính
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { 
                    if (mediaController.hasPreviousMediaItem()) {
                        mediaController.seekToPreviousMediaItem()
                        mediaController.prepare()
                        mediaController.play()
                    } else {
                        mediaController.seekTo(0)
                        mediaController.play()
                    }
                }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.SkipPrevious, "Prev")
                }
                IconButton(onClick = { 
                    val targetPos = (mediaController.currentPosition - 10000).coerceAtLeast(0)
                    mediaController.seekTo(targetPos)
                    mediaController.play()
                }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Replay10, "-10s", modifier = Modifier.size(22.dp))
                }
                IconButton(onClick = { if (isPlaying) mediaController.pause() else mediaController.play() }, modifier = Modifier.size(44.dp)) {
                    Icon(if (isPlaying) Icons.Default.PauseCircleFilled else Icons.Default.PlayCircleFilled, "Play/Pause", modifier = Modifier.size(38.dp), tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { 
                    val targetPos = (mediaController.currentPosition + 10000).coerceAtMost(mediaController.duration)
                    mediaController.seekTo(targetPos)
                    mediaController.play()
                }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Forward10, "+10s", modifier = Modifier.size(22.dp))
                }
                IconButton(onClick = { 
                    if (mediaController.hasNextMediaItem()) {
                        mediaController.seekToNextMediaItem()
                        mediaController.prepare()
                        mediaController.play()
                    }
                }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.SkipNext, "Next")
                }
            }
            
            if (duration > 0) {
                LinearProgressIndicator(
                    progress = { (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(3.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                )
            }
        }
    }
}

private fun formatMillis(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%02d:%02d".format(minutes, seconds)
}

private fun formatRemaining(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return if (m > 0) "${m}m" else "${s}s"
}
