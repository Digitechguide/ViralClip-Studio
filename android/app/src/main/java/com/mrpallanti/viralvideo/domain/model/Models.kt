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
    val cropHorizontalOffset: Float = 0.50f
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
