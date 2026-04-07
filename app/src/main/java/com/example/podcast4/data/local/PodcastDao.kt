package com.example.podcast4.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "podcasts")
data class PodcastEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val artworkUrl: String,
    val feedUrl: String
)

@Entity(tableName = "episodes", primaryKeys = ["id", "userId"])
data class EpisodeEntity(
    val id: String,
    val podcastId: String,
    val userId: String = "", // Mặc định là chuỗi rỗng cho các dữ liệu từ RSS hoặc chưa tải
    val title: String,
    val originalTitle: String,
    val artist: String,
    val artworkUrl: String,
    val description: String,
    val audioUrl: String,
    val duration: Long,
    val pubDate: String,
    val isDownloaded: Boolean = false,
    val localAudioPath: String? = null
)

@Dao
interface PodcastDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPodcasts(podcasts: List<PodcastEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPodcast(podcast: PodcastEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEpisodes(episodes: List<EpisodeEntity>)

    @Query("SELECT * FROM podcasts")
    fun getAllPodcasts(): Flow<List<PodcastEntity>>

    @Query("SELECT * FROM podcasts WHERE id = :id")
    suspend fun getPodcastById(id: String): PodcastEntity?

    @Query("""
        SELECT * FROM episodes 
        WHERE podcastId = :podcastId 
        AND (userId = :userId OR userId = '')
        AND (
            userId = :userId 
            OR id NOT IN (SELECT id FROM episodes WHERE podcastId = :podcastId AND userId = :userId)
        )
        ORDER BY pubDate DESC
    """)
    fun getEpisodesForPodcast(podcastId: String, userId: String): Flow<List<EpisodeEntity>>

    @Query("SELECT * FROM episodes WHERE podcastId = :podcastId")
    suspend fun getAllEpisodesForPodcastSync(podcastId: String): List<EpisodeEntity>

    @Query("SELECT * FROM episodes WHERE id = :episodeId AND userId = :userId")
    suspend fun getEpisodeByIdAndUser(episodeId: String, userId: String): EpisodeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEpisode(episode: EpisodeEntity)

    @Query("SELECT * FROM episodes WHERE isDownloaded = 1 AND userId = :userId ORDER BY pubDate DESC")
    fun getDownloadedEpisodes(userId: String): Flow<List<EpisodeEntity>>

    @Query("SELECT * FROM episodes WHERE isDownloaded = 1 AND userId = :userId")
    suspend fun getDownloadedEpisodesSync(userId: String): List<EpisodeEntity>

    @Query("DELETE FROM episodes WHERE id = :episodeId AND userId = :userId")
    suspend fun removeDownload(episodeId: String, userId: String)

    @Query("UPDATE episodes SET isDownloaded = 0, localAudioPath = NULL WHERE id = :episodeId AND userId = :userId")
    suspend fun markAsNotDownloaded(episodeId: String, userId: String)

    @Query("UPDATE episodes SET title = :newTitle WHERE id = :episodeId AND userId = :userId")
    suspend fun updateEpisodeTitle(episodeId: String, userId: String, newTitle: String)
}
