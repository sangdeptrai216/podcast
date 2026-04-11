package com.example.podcast4.presentation.player

import android.content.Intent
import android.os.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private var sleepTimerJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
            .setUsage(C.USAGE_MEDIA)
            .build()
            
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .build()
            
        mediaSession = MediaSession.Builder(this, player)
            .setCallback(object : MediaSession.Callback {
                override fun onConnect(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo
                ): MediaSession.ConnectionResult {
                    val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                        .add(SessionCommand("SET_SLEEP_TIMER", Bundle.EMPTY))
                        .add(SessionCommand("CANCEL_SLEEP_TIMER", Bundle.EMPTY))
                        .build()
                    return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                        .setAvailableSessionCommands(sessionCommands)
                        .build()
                }

                override fun onCustomCommand(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    customCommand: SessionCommand,
                    args: Bundle
                ): ListenableFuture<SessionResult> {
                    when (customCommand.customAction) {
                        "SET_SLEEP_TIMER" -> {
                            val minutes = args.getInt("MINUTES", 0)
                            startSleepTimer(session, minutes)
                            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                        }
                        "CANCEL_SLEEP_TIMER" -> {
                            cancelSleepTimer()
                            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                        }
                    }
                    return super.onCustomCommand(session, controller, customCommand, args)
                }
            })
            .build()
    }

    private fun startSleepTimer(session: MediaSession, minutes: Int) {
        sleepTimerJob?.cancel()
        
        if (minutes <= 0) {
            updateSleepTimerExtras(0L)
            return
        }

        val totalMillis = minutes * 60 * 1000L
        val endTime = System.currentTimeMillis() + totalMillis
        
        updateSleepTimerExtras(endTime)

        sleepTimerJob = serviceScope.launch {
            delay(totalMillis)
            session.player.pause()
            updateSleepTimerExtras(0L)
        }
    }

    private fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        updateSleepTimerExtras(0L)
    }

    private fun updateSleepTimerExtras(endTime: Long) {
        val extras = Bundle().apply {
            putLong("SLEEP_TIMER_END_TIME", endTime)
        }
        mediaSession?.setSessionExtras(extras)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        sleepTimerJob?.cancel()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
