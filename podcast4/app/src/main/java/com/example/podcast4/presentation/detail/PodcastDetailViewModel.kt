package com.example.podcast4.presentation.detail

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.podcast4.data.repository.AuthRepository
import com.example.podcast4.data.repository.EpisodeDownloadWorker
import com.example.podcast4.domain.models.Episode
import com.example.podcast4.domain.repository.PodcastRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.net.URLDecoder

@HiltViewModel
class PodcastDetailViewModel @Inject constructor(
    private val repository: PodcastRepository,
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val feedUrl: String = URLDecoder.decode(checkNotNull(savedStateHandle["feedUrl"]), "UTF-8")
    private val podcastId: String = checkNotNull(savedStateHandle["podcastId"])

    private val _episodes = MutableStateFlow<List<Episode>>(emptyList())
    val episodes: StateFlow<List<Episode>> = _episodes
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    val isLoggedIn: StateFlow<Boolean> = authRepository.isLoggedIn

    init {
        observeEpisodes()
    }

    private fun observeEpisodes() {
        viewModelScope.launch {
            authRepository.userName.collectLatest { userId ->
                val currentUserId = userId ?: ""
                repository.getLocalEpisodes(podcastId, currentUserId).collect { localList ->
                    _episodes.value = localList
                }
            }
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            val userId = authRepository.userName.value ?: ""
            repository.getEpisodesForPodcast(feedUrl, podcastId, userId)
            _isLoading.value = false
        }
    }

    fun downloadEpisode(episode: Episode) {
        val userId = authRepository.userName.value ?: ""
        val downloadData = Data.Builder()
            .putString("episodeId", episode.id)
            .putString("audioUrl", episode.audioUrl)
            .putString("podcastId", episode.podcastId)
            .putString("userId", userId)
            .build()

        val downloadWork = OneTimeWorkRequestBuilder<EpisodeDownloadWorker>()
            .addTag("EpisodeDownload")
            .addTag("episode_${episode.id}")
            .setInputData(downloadData)
            .build()

        WorkManager.getInstance(context).enqueue(downloadWork)
    }

    fun cancelDownload(episodeId: String) {
        WorkManager.getInstance(context).cancelAllWorkByTag("episode_$episodeId")
    }
}
