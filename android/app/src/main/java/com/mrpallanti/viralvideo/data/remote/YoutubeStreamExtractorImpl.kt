package com.mrpallanti.viralvideo.data.remote

import android.content.Context
import android.util.Log
import com.maxrave.kotlinyoutubeextractor.State
import com.maxrave.kotlinyoutubeextractor.YTExtractor
import com.mrpallanti.viralvideo.domain.model.VideoMetadata
import com.mrpallanti.viralvideo.domain.repository.VideoStreamRepository
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.regex.Pattern

class YoutubeStreamExtractorImpl(
    private val context: Context,
    private val client: OkHttpClient = OkHttpClient()
) : VideoStreamRepository {

    override suspend fun extractMetadata(url: String): Result<VideoMetadata> = withContext(Dispatchers.IO) {
        try {
            val videoId = extractVideoId(url)
                ?: return@withContext Result.failure(IllegalArgumentException("Invalid YouTube URL: $url"))

            var title = "YouTube Video ($videoId)"
            var author = "YouTube Creator"
            var durationSeconds = 180L
            val thumbnailUrl = "https://img.youtube.com/vi/$videoId/maxresdefault.jpg"
            var directStreamUrl: String? = null

            // 1. Fast attempt: Query YouTube oEmbed API for instant title and creator name
            try {
                val oembedUrl = "https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v=$videoId&format=json"
                val request = Request.Builder()
                    .url(oembedUrl)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14)")
                    .build()

                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: ""

                if (response.isSuccessful && body.isNotBlank()) {
                    val json = JSONObject(body)
                    title = json.optString("title", title)
                    author = json.optString("author_name", author)
                }
            } catch (t: Throwable) {
                // Continue to extractors
            }

            // 2. Client-side stream check via YTExtractor
            try {
                val yt = YTExtractor(
                    con = context,
                    CACHING = false,
                    LOGGING = false,
                    retryCount = 1
                )
                yt.extract(videoId)

                if (yt.state == State.SUCCESS) {
                    val ytFiles = yt.getYTFiles()
                    if (ytFiles != null) {
                        for (i in 0 until ytFiles.size()) {
                            val file = ytFiles.valueAt(i)
                            if (file != null) {
                                val format = file.meta
                                val ext = format?.ext
                                val stream = file.url
                                if (ext == "mp4" && !stream.isNullOrBlank()) {
                                    directStreamUrl = stream
                                    if (format?.isDashContainer == false) {
                                        break
                                    }
                                }
                            }
                        }
                    }

                    val meta = yt.getVideoMeta()
                    if (meta != null) {
                        if (meta.videoLength > 0) {
                            durationSeconds = meta.videoLength
                        }
                        val metaTitle = meta.title
                        if (!metaTitle.isNullOrBlank()) {
                            title = metaTitle
                        }
                        val metaAuthor = meta.author
                        if (!metaAuthor.isNullOrBlank()) {
                            author = metaAuthor
                        }
                    }
                }
            } catch (t: Throwable) {
                // Ignore extraction failures, oEmbed metadata is already loaded
            }

            Result.success(
                VideoMetadata(
                    id = videoId,
                    title = title,
                    author = author,
                    durationSeconds = durationSeconds,
                    thumbnailUrl = thumbnailUrl,
                    streamUrl = directStreamUrl
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun downloadDirectVideo(
        url: String,
        outputDir: File,
        onProgress: (Float, String) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }

            val videoId = extractVideoId(url) ?: "video_${System.currentTimeMillis()}"
            val expectedOutputFile = File(outputDir, "${videoId}.mp4")

            // Check if already downloaded and valid
            if (expectedOutputFile.exists() && expectedOutputFile.length() > 500_000) {
                onProgress(100f, "Cached video ready!")
                return@withContext Result.success(expectedOutputFile)
            }

            // Ensure embedded engines are ready
            try {
                YoutubeDL.getInstance().init(context)
                FFmpeg.getInstance().init(context)
            } catch (t: Throwable) {
                Log.d("YoutubeStreamExtractor", "Init status: ${t.message}")
            }

            fun createYtDlpRequest(): YoutubeDLRequest {
                return YoutubeDLRequest(url).apply {
                    // Best video up to 1080p + best audio, automatically muxed into mp4
                    addOption("-f", "bestvideo[height<=1080]+bestaudio/best[height<=1080]/best")
                    addOption("--merge-output-format", "mp4")
                    addOption("-o", File(outputDir, "${videoId}.%(ext)s").absolutePath)
                    addOption("--no-mtime")
                    addOption("--no-playlist")
                    addOption("--force-overwrites")
                    addOption("--no-warnings")
                    addOption("--no-check-certificates")
                    // Android client emulation bypasses YouTube Botguard / PO-token challenges on mobile IPs
                    addOption("--extractor-args", "youtube:player_client=android,web")
                }
            }

            onProgress(5f, "Connecting to YouTube stream via embedded engine...")

            var executionOutput = ""
            try {
                val response = YoutubeDL.getInstance().execute(createYtDlpRequest(), "dl_$videoId") { progress, etaInSeconds, line ->
                    val statusText = line ?: "Downloading... ${progress.toInt()}%"
                    onProgress(progress.coerceIn(0f, 100f), statusText)
                }
                executionOutput = response.out ?: ""
            } catch (initialErr: Throwable) {
                Log.w("YoutubeStreamExtractor", "First attempt hit warning/error: ${initialErr.message}. Triggering yt-dlp update...")
                onProgress(10f, "Updating on-device yt-dlp engine to latest version...")
                try {
                    val updateResult = YoutubeDL.getInstance().updateYoutubeDL(context, YoutubeDL.UpdateChannel.STABLE)
                    Log.d("YoutubeStreamExtractor", "Dynamic update completed: $updateResult")
                } catch (updateErr: Throwable) {
                    Log.w("YoutubeStreamExtractor", "Update attempt note: ${updateErr.message}")
                }

                onProgress(15f, "Retrying direct download with latest engine...")
                val retryResponse = YoutubeDL.getInstance().execute(createYtDlpRequest(), "dl_${videoId}_retry") { progress, etaInSeconds, line ->
                    val statusText = line ?: "Downloading... ${progress.toInt()}%"
                    onProgress(progress.coerceIn(0f, 100f), statusText)
                }
                executionOutput = retryResponse.out ?: ""
            }

            if (expectedOutputFile.exists() && expectedOutputFile.length() > 50_000) {
                onProgress(100f, "Download complete!")
                Result.success(expectedOutputFile)
            } else {
                // Find any matching file in directory starting with videoId
                val matched = outputDir.listFiles()?.firstOrNull { it.name.startsWith(videoId) && it.length() > 50_000 }
                if (matched != null) {
                    onProgress(100f, "Download complete!")
                    Result.success(matched)
                } else {
                    Result.failure(Exception("Download failed: output file not found. Response: $executionOutput"))
                }
            }
        } catch (t: Throwable) {
            Log.e("YoutubeStreamExtractor", "Embedded yt-dlp error: ${t.message}", t)
            Result.failure(t)
        }
    }

    companion object {
        fun extractVideoId(url: String): String? {
            val patterns = listOf(
                "(?:/shorts/|/embed/|/v/|v=)([a-zA-Z0-9_-]{11})",
                "^([a-zA-Z0-9_-]{11})$",
                "youtu\\.be/([a-zA-Z0-9_-]{11})"
            )
            for (patternStr in patterns) {
                val pattern = Pattern.compile(patternStr)
                val matcher = pattern.matcher(url)
                if (matcher.find()) {
                    return matcher.group(1)
                }
            }
            return null
        }
    }
}
