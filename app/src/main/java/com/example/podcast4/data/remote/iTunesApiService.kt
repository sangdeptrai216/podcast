package com.example.podcast4.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface iTunesApiService {
    @GET("search")
    suspend fun searchPodcasts(
        @Query("term") term: String,
        @Query("entity") entity: String = "podcast",
        @Query("limit") limit: Int = 20
    ): iTunesSearchResponse
}

data class iTunesSearchResponse(
    val resultCount: Int,
    val results: List<iTunesPodcastDto>
)

data class iTunesPodcastDto(
    val collectionId: Long,
    val collectionName: String,
    val artistName: String,
    val artworkUrl600: String?,
    val feedUrl: String?
)
