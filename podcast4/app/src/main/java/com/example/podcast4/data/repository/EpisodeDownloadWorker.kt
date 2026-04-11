package com.example.podcast4.data.repository

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.podcast4.domain.repository.PodcastRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

@HiltWorker
class EpisodeDownloadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: PodcastRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val episodeId = inputData.getString("episodeId") ?: return Result.failure()
        val audioUrl = inputData.getString("audioUrl") ?: return Result.failure()
        val podcastId = inputData.getString("podcastId") ?: return Result.failure()
        val userId = inputData.getString("userId") ?: "" // Mặc định là chuỗi rỗng nếu không có userId

        Log.d("DownloadWorker", "Starting download for: $episodeId")

        return try {
            val url = URL(audioUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.setRequestProperty("User-Agent", "Mozilla/5.0")
            connection.connect()

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                Log.e("DownloadWorker", "Server returned code: $responseCode")
                return Result.retry()
            }

            val fileSize = connection.contentLength
            val dir = File(applicationContext.filesDir, "downloads/$podcastId")
            if (!dir.exists()) dir.mkdirs()
            
            // Tạo tên file an toàn (MD5 hash) để tránh ký tự đặc biệt trong URL/ID
            val safeFileName = try {
                val bytes = MessageDigest.getInstance("MD5").digest(episodeId.toByteArray())
                bytes.joinToString("") { "%02x".format(it) } + ".mp3"
            } catch (e: Exception) {
                "${episodeId.hashCode()}.mp3"
            }
            
            val outputFile = File(dir, safeFileName)
            
            connection.inputStream.use { input ->
                FileOutputStream(outputFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalBytesRead = 0L
                    
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead
                        if (fileSize > 0) {
                            val progress = (totalBytesRead * 100 / fileSize).toInt()
                            setProgress(workDataOf("progress" to progress))
                        }
                    }
                }
            }

            Log.d("DownloadWorker", "Download finished: ${outputFile.absolutePath}")
            repository.markEpisodeAsDownloaded(episodeId, outputFile.absolutePath, userId)
            Result.success()
        } catch (e: Exception) {
            Log.e("DownloadWorker", "Error downloading episode $episodeId", e)
            Result.retry()
        }
    }
}
