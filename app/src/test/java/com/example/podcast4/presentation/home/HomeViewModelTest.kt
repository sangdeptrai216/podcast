package com.example.podcast4.presentation.home

import com.example.podcast4.domain.models.Podcast
import com.example.podcast4.domain.repository.PodcastRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private lateinit var viewModel: HomeViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test initial state loads trending podcasts successfully`() = runTest {
        // Arrange
        val mockPodcasts = listOf(Podcast("1", "Podcast A", "Artist", "url", "feedUrl"))
        val fakeRepo = object : PodcastRepository {
            override suspend fun searchPodcasts(query: String) = Result.success(emptyList<Podcast>())
            override suspend fun getTrendingPodcasts() = Result.success(mockPodcasts)
            override suspend fun getEpisodesForPodcast(feedUrl: String, podcastId: String) = Result.success(emptyList<com.example.podcast4.domain.models.Episode>())
            override fun getLocalEpisodes(podcastId: String) = flowOf(emptyList<com.example.podcast4.domain.models.Episode>())
            override suspend fun savePodcast(podcast: Podcast) {}
            override suspend fun markEpisodeAsDownloaded(episodeId: String, localPath: String) {}
        }

        // Act
        viewModel = HomeViewModel(fakeRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        assertEquals(mockPodcasts, viewModel.podcasts.value)
        assertEquals(false, viewModel.isLoading.value)
    }
}
