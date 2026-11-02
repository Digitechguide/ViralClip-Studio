package com.mrpallanti.viralvideo.domain.model

data class VideoMetadata(
    val id: String,
    val title: String,
    val author: String,
    val durationSeconds: Long,
    val thumbnailUrl: String,
    val streamUrl: String?,
    val localUri: String? = null
)

data class ViralClip(
    val id: Int,
    val title: String,
    val startSeconds: Float,
    val endSeconds: Float,
    val duration: Float,
    val viralityScore: Int,
    val hookReason: String,
    val captionOverlay: String,
    val hashtags: List<String>
)

data class TrimRange(
    val startSeconds: Float,
    val endSeconds: Float
) {
    val durationSeconds: Float get() = (endSeconds - startSeconds).coerceAtLeast(1f)
}

data class RenderJob(
    val id: String,
    val inputUri: String,
    val outputUri: String,
    val trimRange: TrimRange,
    val hookText: String,
    val progressPercent: Int = 0,
    val isComplete: Boolean = false,
    val error: String? = null
)
