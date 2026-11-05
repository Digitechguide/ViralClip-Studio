package com.mrpallanti.viralvideo.domain.model

import kotlinx.serialization.Serializable

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

@Serializable
enum class WatermarkPosition {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT
}

@Serializable
enum class TextOverlayPosition {
    TOP,
    CENTER,
    BOTTOM
}

@Serializable
enum class VideoQuality(
    val label: String,
    val shortLabel: String,
    val width: Int,
    val height: Int,
    val targetHeight: Int
) {
    FULL_HD_1080P("1080p Full HD", "1080p", 1080, 1920, 1080),
    HD_720P("720p HD", "720p", 720, 1280, 720),
    QUAD_HD_1440P("1440p 2K QHD", "2K", 1440, 2560, 1440),
    SD_480P("480p SD", "480p", 480, 854, 480)
}

@Serializable
enum class CropPlacement {
    AUTO_FACE, // AI Face-Centered Auto Framing
    LEFT,      // Left side of 16:9 frame (0.16)
    CENTER,    // Center of 16:9 frame (0.50)
    RIGHT,     // Right side of 16:9 frame (0.84)
    CUSTOM     // Custom user-defined horizontal offset (0.0 to 1.0)
}

@Serializable
data class OverlayConfig(
    val customText: String = "",
    val textPosition: TextOverlayPosition = TextOverlayPosition.TOP,
    val textColorHex: String = "#FFFFFF",
    val textBgColorHex: String = "#B3000000",
    val logoUri: String? = null,
    val watermarkText: String = "",
    val watermarkPosition: WatermarkPosition = WatermarkPosition.TOP_RIGHT,
    val watermarkOpacity: Float = 0.85f,
    val cropPlacement: CropPlacement = CropPlacement.AUTO_FACE,
    val cropHorizontalOffset: Float = 0.50f,
    val videoQuality: VideoQuality = VideoQuality.FULL_HD_1080P
)

@Serializable
data class GeneratedShortItem(
    val id: String,
    val title: String,
    val outputUri: String,
    val durationSeconds: Float,
    val timestamp: Long,
    val fileSizeFormatted: String = "",
    val hookCaption: String = ""
)

data class RenderJob(
    val id: String,
    val inputUri: String,
    val outputUri: String,
    val trimRange: TrimRange,
    val hookText: String,
    val progressPercent: Int = 0,
    val isComplete: Boolean = false,
    val error: String? = null,
    val overlayConfig: OverlayConfig = OverlayConfig()
)
