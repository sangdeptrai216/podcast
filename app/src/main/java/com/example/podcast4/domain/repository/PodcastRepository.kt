package com.example.podcast4.domain.repository

import com.example.podcast4.domain.models.Episode
import com.example.podcast4.domain.models.Podcast
import kotlinx.coroutines.flow.Flow

interface PodcastRepository {
    suspend fun searchPodcasts(query: String): Result<List<Podcast>>
    suspend fun getTrendingPodcasts(): Result<List<Podcast>>
    suspend fun getEpisodesForPodcast(feedUrl: String, podcastId: String, userId: String): Result<List<Episode>>
    fun getLocalEpisodes(podcastId: String, userId: String): Flow<List<Episode>>
    fun getAllPodcasts(): Flow<List<Podcast>>
    suspend fun savePodcast(podcast: Podcast)
    suspend fun markEpisodeAsDownloaded(episodeId: String, localPath: String, userId: String)
    fun getDownloadedEpisodes(userId: String): Flow<List<Episode>>
    suspend fun removeDownload(episodeId: String, userId: String)
    suspend fun updateEpisodeTitle(episodeId: String, userId: String, newTitle: String)
}
