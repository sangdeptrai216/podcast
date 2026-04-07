package com.example.podcast4.presentation.player

import android.content.ComponentName
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken

@Composable
fun rememberMediaController(context: Context): MediaController? {
    val mediaControllerState = remember { mutableStateOf<MediaController?>(null) }

    DisposableEffect(context) {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()

        controllerFuture.addListener(
            { mediaControllerState.value = controllerFuture.get() },
            ContextCompat.getMainExecutor(context)
        )

        onDispose {
            MediaController.releaseFuture(controllerFuture)
            mediaControllerState.value?.release()
        }
    }

    return mediaControllerState.value
}
