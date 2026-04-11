package com.example.podcast4.domain.models

data class Podcast(
    val id: String,
    val title: String,
    val artist: String,
    val artworkUrl: String,
    val feedUrl: String
)

data class Episode(
    val id: String,
    val podcastId: String,
    val userId: String = "",
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
