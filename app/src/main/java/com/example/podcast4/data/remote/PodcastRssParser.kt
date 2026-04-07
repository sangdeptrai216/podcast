package com.example.podcast4.data.remote

import android.util.Xml
import com.example.podcast4.domain.models.Episode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

class PodcastRssParser {

    suspend fun parse(feedUrl: String, podcastId: String): List<Episode> = withContext(Dispatchers.IO) {
        val url = URL(feedUrl)
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        connection.requestMethod = "GET"

        val inputStream = connection.inputStream
        val episodes = mutableListOf<Episode>()

        try {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(inputStream, null)

            var eventType = parser.eventType
            var isInsideItem = false
            var channelTitle: String? = null
            var channelArtworkUrl: String? = null

            var title: String? = null
            var description: String? = null
            var audioUrl: String? = null
            var pubDate: String? = null
            var duration: Long = 0L
            var episodeArtworkUrl: String? = null

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val name = parser.name

                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (name.equals("item", ignoreCase = true)) {
                            isInsideItem = true
                            title = null
                            description = null
                            audioUrl = null
                            pubDate = null
                            duration = 0L
                            episodeArtworkUrl = null
                        } else if (isInsideItem) {
                            when (name.lowercase()) {
                                "title" -> title = readText(parser)
                                "description" -> description = readText(parser)
                                "pubdate" -> pubDate = readText(parser)
                                "enclosure" -> {
                                    audioUrl = parser.getAttributeValue(null, "url")
                                }
                                "itunes:duration" -> {
                                    val durationStr = readText(parser)
                                    duration = parseDuration(durationStr)
                                }
                                "itunes:image" -> {
                                    episodeArtworkUrl = parser.getAttributeValue(null, "href")
                                }
                            }
                        } else if (name.equals("title", ignoreCase = true)) {
                            channelTitle = readText(parser)
                        } else if (name.equals("itunes:image", ignoreCase = true)) {
                            channelArtworkUrl = parser.getAttributeValue(null, "href")
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (name.equals("item", ignoreCase = true) && isInsideItem) {
                            if (title != null && audioUrl != null) {
                                episodes.add(
                                    Episode(
                                        id = audioUrl ?: UUID.randomUUID().toString(),
                                        podcastId = podcastId,
                                        title = title ?: "",
                                        originalTitle = title ?: "",
                                        artist = channelTitle ?: "Podcast",
                                        artworkUrl = episodeArtworkUrl ?: channelArtworkUrl ?: "",
                                        description = description ?: "",
                                        audioUrl = audioUrl ?: "",
                                        duration = duration,
                                        pubDate = pubDate ?: ""
                                    )
                                )
                            }
                            isInsideItem = false
                        }
                    }
                }
                eventType = parser.next()
            }
        } finally {
            inputStream.close()
            connection.disconnect()
        }
        return@withContext episodes
    }

    private fun readText(parser: XmlPullParser): String {
        var result = ""
        try {
            if (parser.next() == XmlPullParser.TEXT) {
                result = parser.text
                parser.nextTag()
            }
        } catch (e: Exception) {
            // Handle cases where next() might not be TEXT or nextTag() fails
        }
        return result
    }

    private fun parseDuration(durationStr: String): Long {
        if (durationStr.isBlank()) return 0L
        return try {
            val parts = durationStr.split(":")
            if (parts.size == 3) {
                // hh:mm:ss
                (parts[0].toLong() * 3600) + (parts[1].toLong() * 60) + parts[2].toLong()
            } else if (parts.size == 2) {
                // mm:ss
                (parts[0].toLong() * 60) + parts[1].toLong()
            } else {
                // raw seconds
                durationStr.toLongOrNull() ?: 0L
            }
        } catch (e: Exception) {
            0L
        }
    }
}
