package com.example.podcast4.data.repository

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.podcast4.data.remote.PodcastRssParser
import com.example.podcast4.domain.repository.PodcastRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.firstOrNull

@HiltWorker
class AutoSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: PodcastRepository,
    private val rssParser: PodcastRssParser
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val dbPodcasts = repository.getAllPodcasts().firstOrNull() ?: emptyList()

            for (podcast in dbPodcasts) {
                // Get the latest from RSS
                val newEpisodes = rssParser.parse(podcast.feedUrl, podcast.id)
                // Get local ones to identify missing episodes
                val localEpisodes = repository.getLocalEpisodes(podcast.id, "").firstOrNull() ?: emptyList()
                val localIds = localEpisodes.map { it.id }.toSet()

                for (episode in newEpisodes) {
                    if (!localIds.contains(episode.id)) {
                        // Enqueue download for the un-synced missing episode
                        val downloadData = Data.Builder()
                            .putString("episodeId", episode.id)
                            .putString("audioUrl", episode.audioUrl)
                            .putString("podcastId", podcast.id)
                            .putString("userId", "")
                            .build()

                        val downloadWork = OneTimeWorkRequestBuilder<EpisodeDownloadWorker>()
                            .setInputData(downloadData)
                            .build()

                        WorkManager.getInstance(applicationContext).enqueue(downloadWork)
                    }
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
