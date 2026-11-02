package com.mrpallanti.viralvideo.data.local

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.Crop
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.Presentation
import androidx.media3.effect.TextureOverlay
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.google.common.collect.ImmutableList
import com.mrpallanti.viralvideo.domain.model.OverlayConfig
import com.mrpallanti.viralvideo.domain.model.RenderJob
import com.mrpallanti.viralvideo.domain.model.TrimRange
import com.mrpallanti.viralvideo.domain.repository.VideoRendererRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.io.FileInputStream
import java.util.UUID

@OptIn(UnstableApi::class)
class Media3TransformerVideoRendererImpl(
    private val context: Context
) : VideoRendererRepository {

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun renderShort(
        inputUri: String,
        trimRange: TrimRange,
        hookText: String,
        overlayConfig: OverlayConfig
    ): Flow<RenderJob> = callbackFlow {
        val jobId = UUID.randomUUID().toString().substring(0, 8)
        val tempOutput = File(context.cacheDir, "viral_short_${jobId}.mp4")

        trySend(
            RenderJob(
                id = jobId,
                inputUri = inputUri,
                outputUri = "",
                trimRange = trimRange,
                hookText = hookText,
                progressPercent = 5,
                overlayConfig = overlayConfig
            )
        )

        try {
            val startMs = (trimRange.startSeconds * 1000).toLong().coerceAtLeast(0L)
            val endMs = (trimRange.endSeconds * 1000).toLong().coerceAtLeast(startMs + 1000L)

            // Auto Face-Centered Framing (Option 1):
            // Analyze the clip frames to identify the primary speaker's horizontal center
            val speakerCenterX = FaceTrackingHelper.detectSpeakerHorizontalCenter(
                videoPath = inputUri,
                startMs = startMs,
                endMs = endMs
            )

            // Compute exact 9:16 crop window in NDC (-1.0f to 1.0f) centered on the speaker's face.
            // Horizontal fraction required to turn 16:9 into 9:16 is (9/16)/(16/9) = 81/256 ≈ 0.3164
            val halfCropWidthNdc = 0.3164f
            val speakerCenterNdc = (2.0f * speakerCenterX) - 1.0f
            val maxLeftNdc = 1.0f - (2.0f * halfCropWidthNdc)
            val leftNdc = (speakerCenterNdc - halfCropWidthNdc).coerceIn(-1.0f, maxLeftNdc)
            val rightNdc = leftNdc + (2.0f * halfCropWidthNdc)

            val cropEffect = Crop(leftNdc, rightNdc, -1.0f, 1.0f)
            val presentationEffect = Presentation.createForWidthAndHeight(
                1080,
                1920,
                Presentation.LAYOUT_SCALE_TO_FIT
            )

            val clippingConfiguration = MediaItem.ClippingConfiguration.Builder()
                .setStartPositionMs(startMs)
                .setEndPositionMs(endMs)
                .build()

            val mediaItem = MediaItem.Builder()
                .setUri(Uri.parse(inputUri))
                .setClippingConfiguration(clippingConfiguration)
                .build()

            val videoEffects = mutableListOf<androidx.media3.common.Effect>(cropEffect, presentationEffect)

            val overlayBitmap = VideoOverlayHelper.createOverlayBitmap(
                context = context,
                width = 1080,
                height = 1920,
                hookText = hookText,
                overlayConfig = overlayConfig
            )

            if (overlayBitmap != null) {
                try {
                    val bitmapOverlay: TextureOverlay = BitmapOverlay.createStaticBitmapOverlay(overlayBitmap)
                    val overlayEffect = OverlayEffect(ImmutableList.of<TextureOverlay>(bitmapOverlay))
                    videoEffects.add(overlayEffect)
                } catch (t: Throwable) {
                    // Safe fallback if overlay effect cannot be attached
                }
            }

            val editedMediaItem = EditedMediaItem.Builder(mediaItem)
                .setEffects(Effects(emptyList(), videoEffects))
                .build()

            var isFinished = false
            var transformerRef: Transformer? = null

            val progressHolder = ProgressHolder()
            val progressRunnable = object : Runnable {
                override fun run() {
                    if (!isFinished && transformerRef != null) {
                        try {
                            val state = transformerRef?.getProgress(progressHolder)
                            if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                                val currentProgress = progressHolder.progress.coerceIn(5, 95)
                                trySend(
                                    RenderJob(
                                        id = jobId,
                                        inputUri = inputUri,
                                        outputUri = "",
                                        trimRange = trimRange,
                                        hookText = hookText,
                                        progressPercent = currentProgress,
                                        overlayConfig = overlayConfig
                                    )
                                )
                            }
                        } catch (t: Throwable) {
                            // Ignore progress reporting errors
                        }
                        mainHandler.postDelayed(this, 500)
                    }
                }
            }

            val listener = object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    isFinished = true
                    mainHandler.removeCallbacks(progressRunnable)

                    // Save exported MP4 to Android MediaStore Gallery
                    val galleryUri = saveVideoToGallery(context, tempOutput, "ViralShort_${jobId}.mp4")
                    trySend(
                        RenderJob(
                            id = jobId,
                            inputUri = inputUri,
                            outputUri = galleryUri?.toString() ?: tempOutput.absolutePath,
                            trimRange = trimRange,
                            hookText = hookText,
                            progressPercent = 100,
                            isComplete = true,
                            overlayConfig = overlayConfig
                        )
                    )
                    channel.close()
                }

                override fun onError(
                    composition: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException
                ) {
                    isFinished = true
                    mainHandler.removeCallbacks(progressRunnable)
                    trySend(
                        RenderJob(
                            id = jobId,
                            inputUri = inputUri,
                            outputUri = "",
                            trimRange = trimRange,
                            hookText = hookText,
                            progressPercent = 0,
                            isComplete = false,
                            error = "Rendering error: ${exportException.message ?: "Encoding failed"}"
                        )
                    )
                    tempOutput.delete()
                    channel.close()
                }
            }

            mainHandler.post {
                try {
                    val transformer = Transformer.Builder(context)
                        .setVideoMimeType(MimeTypes.VIDEO_H264)
                        .setAudioMimeType(MimeTypes.AUDIO_AAC)
                        .addListener(listener)
                        .build()

                    transformerRef = transformer
                    transformer.start(editedMediaItem, tempOutput.absolutePath)
                    mainHandler.postDelayed(progressRunnable, 500)
                } catch (t: Throwable) {
                    isFinished = true
                    trySend(
                        RenderJob(
                            id = jobId,
                            inputUri = inputUri,
                            outputUri = "",
                            trimRange = trimRange,
                            hookText = hookText,
                            progressPercent = 0,
                            isComplete = false,
                            error = "Transformer initialization failed: ${t.message}"
                        )
                    )
                    channel.close()
                }
            }

            awaitClose {
                isFinished = true
                mainHandler.removeCallbacks(progressRunnable)
                mainHandler.post {
                    try {
                        transformerRef?.cancel()
                    } catch (t: Throwable) {
                        // Safe cancel
                    }
                }
            }
        } catch (t: Throwable) {
            trySend(
                RenderJob(
                    id = jobId,
                    inputUri = inputUri,
                    outputUri = "",
                    trimRange = trimRange,
                    hookText = hookText,
                    progressPercent = 0,
                    isComplete = false,
                    error = "Failed to start render: ${t.message}"
                )
            )
            channel.close()
        }
    }.flowOn(Dispatchers.IO)

    private fun saveVideoToGallery(context: Context, videoFile: File, displayName: String): Uri? {
        return try {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/ViralClips")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
            }

            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }

            val itemUri = resolver.insert(collection, contentValues) ?: return null

            resolver.openOutputStream(itemUri)?.use { out ->
                FileInputStream(videoFile).use { input ->
                    input.copyTo(out)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(itemUri, contentValues, null, null)
            }

            itemUri
        } catch (t: Throwable) {
            null
        }
    }
}
