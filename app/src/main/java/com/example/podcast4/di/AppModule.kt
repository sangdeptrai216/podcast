package com.example.podcast4.di

import android.app.Application
import androidx.room.Room
import com.example.podcast4.data.local.AppDatabase
import com.example.podcast4.data.local.PodcastDao
import com.example.podcast4.data.remote.PodcastRssParser
import com.example.podcast4.data.remote.iTunesApiService
import com.example.podcast4.data.repository.PodcastRepositoryImpl
import com.example.podcast4.domain.repository.PodcastRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideITunesApi(): iTunesApiService {
        return Retrofit.Builder()
            .baseUrl("https://itunes.apple.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(iTunesApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideRssParser(): PodcastRssParser {
        return PodcastRssParser()
    }

    @Provides
    @Singleton
    fun provideDatabase(app: Application): AppDatabase {
        return Room.databaseBuilder(
            app,
            AppDatabase::class.java,
            "podcast_db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun providePodcastDao(db: AppDatabase): PodcastDao {
        return db.podcastDao
    }

    @Provides
    @Singleton
    fun providePodcastRepository(
        api: iTunesApiService,
        rssParser: PodcastRssParser,
        dao: PodcastDao
    ): PodcastRepository {
        return PodcastRepositoryImpl(api, rssParser, dao)
    }
}
