package com.mrpallanti.viralvideo.data.remote

import android.content.Context
import com.maxrave.kotlinyoutubeextractor.State
import com.maxrave.kotlinyoutubeextractor.YTExtractor
import com.mrpallanti.viralvideo.domain.model.VideoMetadata
import com.mrpallanti.viralvideo.domain.repository.VideoStreamRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
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

            // 1. First attempt: Query YouTube oEmbed API for fast title and author
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
                // Continue to on-device stream extractor
            }

            // 2. Second attempt: On-Device YTExtractor for direct playable MP4 stream & exact duration
            try {
                val yt = YTExtractor(
                    con = context,
                    CACHING = false,
                    LOGGING = false,
                    retryCount = 2
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
