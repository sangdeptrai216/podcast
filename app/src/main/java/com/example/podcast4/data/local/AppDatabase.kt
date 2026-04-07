package com.example.podcast4.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [PodcastEntity::class, EpisodeEntity::class], version = 5, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract val podcastDao: PodcastDao
}
