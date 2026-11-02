package in.mrpallanti.viralvideo.data.remote

import in.mrpallanti.viralvideo.domain.model.VideoMetadata
import in.mrpallanti.viralvideo.domain.repository.VideoStreamRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.regex.Pattern

class YoutubeStreamExtractorImpl(
    private val client: OkHttpClient = OkHttpClient()
) : VideoStreamRepository {

    override suspend fun extractMetadata(url: String): Result<VideoMetadata> = withContext(Dispatchers.IO) {
        try {
            val videoId = extractVideoId(url)
                ?: return@withContext Result.failure(IllegalArgumentException("Invalid YouTube URL: $url"))

            // Query YouTube oEmbed API for title and author
            val oembedUrl = "https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v=$videoId&format=json"
            val request = Request.Builder()
                .url(oembedUrl)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14)")
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            var title = "YouTube Video ($videoId)"
            var author = "YouTube Creator"

            if (response.isSuccessful && body.isNotBlank()) {
                val json = JSONObject(body)
                title = json.optString("title", title)
                author = json.optString("author_name", author)
            }

            val thumbnailUrl = "https://img.youtube.com/vi/$videoId/maxresdefault.jpg"

            // Direct playable stream URL or embed URL
            val streamUrl = "https://www.youtube-nocookie.com/embed/$videoId?autoplay=1"

            Result.success(
                VideoMetadata(
                    id = videoId,
                    title = title,
                    author = author,
                    durationSeconds = 180, // Default baseline if not probed
                    thumbnailUrl = thumbnailUrl,
                    streamUrl = streamUrl
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
