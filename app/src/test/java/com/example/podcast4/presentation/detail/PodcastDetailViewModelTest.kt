package com.example.podcast4.presentation.detail

import androidx.lifecycle.SavedStateHandle
import com.example.podcast4.domain.models.Episode
import com.example.podcast4.domain.repository.PodcastRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

@OptIn(ExperimentalCoroutinesApi::class)
class PodcastDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: PodcastDetailViewModel
    private lateinit var repository: PodcastRepository
    private lateinit var savedStateHandle: SavedStateHandle

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mock(PodcastRepository::class.java)
        savedStateHandle = SavedStateHandle(
            mapOf(
                "feedUrl" to "https%3A%2F%2Ftest.com%2Ffeed.xml",
                "podcastId" to "12345"
            )
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadEpisodes updates state when DB has local data`() = runTest {
        val dummyEpisodes = listOf(
            Episode(
                id = "1",
                podcastId = "12345",
                title = "Test Ep",
                description = "Desc",
                audioUrl = "audio",
                duration = 0,
                pubDate = "Date"
            )
        )
        `when`(repository.getLocalEpisodes("12345")).thenReturn(flowOf(dummyEpisodes))
        `when`(repository.getEpisodesForPodcast("https://test.com/feed.xml", "12345"))
            .thenReturn(Result.success(emptyList()))

        viewModel = PodcastDetailViewModel(repository, savedStateHandle)
        advanceUntilIdle() 

        assertEquals(dummyEpisodes, viewModel.episodes.value)
        assertFalse(viewModel.isLoading.value)
        verify(repository).getEpisodesForPodcast("https://test.com/feed.xml", "12345")
    }

    @Test
    fun `loadEpisodes loading state toggles correctly`() = runTest {
        `when`(repository.getLocalEpisodes("12345")).thenReturn(flowOf(emptyList()))
        `when`(repository.getEpisodesForPodcast("https://test.com/feed.xml", "12345"))
            .thenReturn(Result.success(emptyList()))
        
        viewModel = PodcastDetailViewModel(repository, savedStateHandle)
        
        assertTrue(viewModel.isLoading.value)

        advanceUntilIdle()
        
        assertFalse(viewModel.isLoading.value)
    }
}
