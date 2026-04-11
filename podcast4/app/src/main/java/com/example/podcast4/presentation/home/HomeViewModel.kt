package com.example.podcast4.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.podcast4.domain.models.Podcast
import com.example.podcast4.domain.repository.PodcastRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: PodcastRepository
) : ViewModel() {

    private val _podcasts = MutableStateFlow<List<Podcast>>(emptyList())
    val podcasts: StateFlow<List<Podcast>> = _podcasts
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadTrendingPodcasts()
    }

    private fun loadTrendingPodcasts() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getTrendingPodcasts()
                .onSuccess { _podcasts.value = it }
            _isLoading.value = false
        }
    }

    fun searchPodcasts(query: String) {
        if (query.isBlank()) {
            loadTrendingPodcasts()
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            repository.searchPodcasts(query)
                .onSuccess { _podcasts.value = it }
            _isLoading.value = false
        }
    }
}
