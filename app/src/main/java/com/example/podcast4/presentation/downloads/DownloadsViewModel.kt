package com.example.podcast4.presentation.downloads

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.example.podcast4.data.repository.AuthRepository
import com.example.podcast4.domain.models.Episode
import com.example.podcast4.domain.repository.PodcastRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val repository: PodcastRepository,
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _downloadedEpisodes = MutableStateFlow<List<Episode>>(emptyList())
    val downloadedEpisodes: StateFlow<List<Episode>> = _downloadedEpisodes

    init {
        loadDownloads()
    }

    private fun loadDownloads() {
        viewModelScope.launch {
            authRepository.userName.flatMapLatest { userId ->
                if (!userId.isNullOrEmpty()) {
                    repository.getDownloadedEpisodes(userId)
                } else {
                    flowOf(emptyList())
                }
            }.collect {
                _downloadedEpisodes.value = it
            }
        }
    }

    fun cancelDownload(episodeId: String) {
        WorkManager.getInstance(context).cancelAllWorkByTag("episode_$episodeId")
    }

    fun removeDownload(episodeId: String) {
        viewModelScope.launch {
            val userId = authRepository.userName.value ?: ""
            repository.removeDownload(episodeId, userId)
        }
    }

    fun updateEpisodeTitle(episodeId: String, newTitle: String) {
        viewModelScope.launch {
            val userId = authRepository.userName.value ?: ""
            repository.updateEpisodeTitle(episodeId, userId, newTitle)
        }
    }
}
