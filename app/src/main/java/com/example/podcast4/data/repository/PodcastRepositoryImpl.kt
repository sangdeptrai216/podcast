package com.example.podcast4.data.repository
//Code Kien đã sửa
import com.example.podcast4.data.local.PodcastDao
import com.example.podcast4.data.local.PodcastEntity
import com.example.podcast4.data.local.EpisodeEntity
import com.example.podcast4.data.remote.PodcastRssParser
import com.example.podcast4.data.remote.iTunesApiService
import com.example.podcast4.domain.models.Episode
import com.example.podcast4.domain.models.Podcast
import com.example.podcast4.domain.repository.PodcastRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PodcastRepositoryImpl @Inject constructor(
    private val api: iTunesApiService,
    private val rssParser: PodcastRssParser,
    private val dao: PodcastDao
) : PodcastRepository {

    override suspend fun searchPodcasts(query: String): Result<List<Podcast>> {
        return try {
            val response = api.searchPodcasts(query)
            val domainModels = response.results.mapNotNull { dto ->
                if (dto.feedUrl == null) null
                else Podcast(
                    id = dto.collectionId.toString(),
                    title = dto.collectionName,
                    artist = dto.artistName,
                    artworkUrl = dto.artworkUrl600 ?: "",
                    feedUrl = dto.feedUrl
                )
            }
            Result.success(domainModels)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTrendingPodcasts(): Result<List<Podcast>> {
        return searchPodcasts("technology")
    }

    override suspend fun getEpisodesForPodcast(feedUrl: String, podcastId: String, userId: String): Result<List<Episode>> {
        return try {
            val episodesFromRss = rssParser.parse(feedUrl, podcastId)
            
            // Lấy tất cả các tập của podcast này trong DB (để giữ lại thông tin download của user này và các user khác)
            val allExistingInDb = dao.getAllEpisodesForPodcastSync(podcastId)
            
            // Lấy các tập đã download của user hiện tại
            val userDownloads = allExistingInDb.filter { it.userId == userId && it.isDownloaded }.associateBy { it.id }

            val entitiesToInsert = episodesFromRss.map { episode ->
                val userDownload = userDownloads[episode.id]
                EpisodeEntity(
                    id = episode.id,
                    podcastId = episode.podcastId,
                    userId = if (userDownload != null) userId else "", // Nếu đã download thì lưu theo userId, nếu chưa thì để trống (mặc định)
                    title = userDownload?.title ?: episode.title, 
                    originalTitle = episode.originalTitle,
                    artist = episode.artist,
                    artworkUrl = episode.artworkUrl,
                    description = episode.description,
                    audioUrl = episode.audioUrl,
                    duration = episode.duration,
                    pubDate = episode.pubDate,
                    isDownloaded = userDownload?.isDownloaded ?: false,
                    localAudioPath = userDownload?.localAudioPath
                )
            }
            dao.insertEpisodes(entitiesToInsert)
            
            Result.success(episodesFromRss.map { episode ->
                val userDownload = userDownloads[episode.id]
                episode.copy(
                    isDownloaded = userDownload?.isDownloaded ?: false,
                    userId = if (userDownload != null) userId else ""
                )
            })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getLocalEpisodes(podcastId: String, userId: String): Flow<List<Episode>> {
        return dao.getEpisodesForPodcast(podcastId, userId).map { entities ->
            entities.map { mapToDomain(it) }
        }
    }

    override fun getAllPodcasts(): Flow<List<Podcast>> {
        return dao.getAllPodcasts().map { entities ->
            entities.map {
                Podcast(
                    id = it.id,
                    title = it.title,
                    artist = it.artist,
                    artworkUrl = it.artworkUrl,
                    feedUrl = it.feedUrl
                )
            }
        }
    }

    override suspend fun savePodcast(podcast: Podcast) {
        dao.insertPodcast(
            PodcastEntity(
                id = podcast.id,
                title = podcast.title,
                artist = podcast.artist,
                artworkUrl = podcast.artworkUrl,
                feedUrl = podcast.feedUrl
            )
        )
    }

    override suspend fun markEpisodeAsDownloaded(episodeId: String, localPath: String, userId: String) {
        // Tìm tập phim gốc (userId = "")
        val originalEpisode = dao.getEpisodeByIdAndUser(episodeId, "")
        
        if (originalEpisode != null) {
            // Tạo một bản sao mới cho user này với trạng thái đã tải xuống
            val downloadedEntity = originalEpisode.copy(
                userId = userId,
                isDownloaded = true,
                localAudioPath = localPath
            )
            dao.upsertEpisode(downloadedEntity)
        }
    }

    override fun getDownloadedEpisodes(userId: String): Flow<List<Episode>> {
        return dao.getDownloadedEpisodes(userId).map { entities ->
            entities.map { mapToDomain(it) }
        }
    }

    override suspend fun removeDownload(episodeId: String, userId: String) {
        // Thay vì xóa hoàn toàn, chúng ta đánh dấu là chưa tải xuống cho user này
        // Hoặc xóa bản ghi của user này nếu nó không phải là bản ghi mặc định (userId != "")
        if (userId.isNotEmpty()) {
            dao.removeDownload(episodeId, userId)
        }
    }

    override suspend fun updateEpisodeTitle(episodeId: String, userId: String, newTitle: String) {
        dao.updateEpisodeTitle(episodeId, userId, newTitle)
    }

    private fun mapToDomain(entity: EpisodeEntity): Episode {
        return Episode(
            id = entity.id,
            podcastId = entity.podcastId,
            userId = entity.userId,
            title = entity.title,
            originalTitle = entity.originalTitle,
            artist = entity.artist,
            artworkUrl = entity.artworkUrl,
            description = entity.description,
            audioUrl = entity.audioUrl,
            duration = entity.duration,
            pubDate = entity.pubDate,
            isDownloaded = entity.isDownloaded,
            localAudioPath = entity.localAudioPath
        )
    }
}
