package com.mrpallanti.viralvideo.data.local

import android.graphics.Bitmap
import android.graphics.PointF
import android.media.FaceDetector
import android.media.MediaMetadataRetriever
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Auto Face-Centered Framing Helper
 * Uses Android's built-in hardware-accelerated FaceDetector to identify the primary
 * speaker's horizontal coordinates and dynamically align the 9:16 vertical crop.
 */
object FaceTrackingHelper {

    private const val TAG = "FaceTrackingHelper"

    /**
     * Analyzes video frames across the clip's timestamp range to calculate the normalized
     * horizontal center (0.0 = far left, 0.5 = dead center, 1.0 = far right) of the main subject.
     */
    suspend fun detectSpeakerHorizontalCenter(
        videoPath: String,
        startMs: Long,
        endMs: Long
    ): Float = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(videoPath)
            val durationMs = endMs - startMs
            if (durationMs <= 0) return@withContext 0.5f

            // Sample keyframe points across the clip duration to find speaker position
            val samplePercentages = listOf(0.15f, 0.35f, 0.60f, 0.85f)
            val detectedXList = mutableListOf<Float>()

            for (pct in samplePercentages) {
                val targetUs = (startMs + (durationMs * pct).toLong()) * 1000L
                val frame = retriever.getFrameAtTime(targetUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?: continue

                val faceX = detectPrimaryFaceX(frame)
                if (faceX != null) {
                    detectedXList.add(faceX)
                }
                frame.recycle()
            }

            if (detectedXList.isNotEmpty()) {
                val avgX = detectedXList.average().toFloat()
                Log.d(TAG, "Speaker Face Detection Success: horizontal center = $avgX (from ${detectedXList.size} frames)")
                avgX.coerceIn(0.16f, 0.84f)
            } else {
                Log.d(TAG, "No face detected in sampled frames; defaulting to 0.5 (center crop)")
                0.5f
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Face detection fallback: ${t.message}")
            0.5f
        } finally {
            try {
                retriever.release()
            } catch (t: Throwable) {
                // Ignore cleanup error
            }
        }
    }

    private fun detectPrimaryFaceX(source: Bitmap): Float? {
        return try {
            // Downsample frame to 640px wide for ultra-fast on-device processing (<15ms)
            val targetW = 640
            val targetH = (source.height * (640f / source.width)).toInt().coerceAtLeast(360)
            val scaled = Bitmap.createScaledBitmap(source, targetW, targetH, true)

            // android.media.FaceDetector requires RGB_565 color format
            val rgb565 = if (scaled.config != Bitmap.Config.RGB_565) {
                val converted = scaled.copy(Bitmap.Config.RGB_565, false)
                scaled.recycle()
                converted
            } else {
                scaled
            }

            val maxFaces = 3
            val detector = FaceDetector(rgb565.width, rgb565.height, maxFaces)
            val faces = arrayOfNulls<FaceDetector.Face>(maxFaces)
            val count = detector.findFaces(rgb565, faces)

            var bestFaceX: Float? = null
            var maxConfidence = 0.0f

            for (i in 0 until count) {
                val face = faces[i] ?: continue
                val conf = face.confidence()
                if (conf > 0.35f && conf > maxConfidence) {
                    val pt = PointF()
                    face.getMidPoint(pt)
                    bestFaceX = pt.x / rgb565.width
                    maxConfidence = conf
                }
            }

            rgb565.recycle()
            bestFaceX
        } catch (t: Throwable) {
            null
        }
    }
}
