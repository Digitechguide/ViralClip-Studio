package com.mrpallanti.viralvideo.data.local

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
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

class FfmpegVideoRendererImpl(
    private val context: Context
) : VideoRendererRepository {

    override fun renderShort(
        inputUri: String,
        trimRange: TrimRange,
        hookText: String
    ): Flow<RenderJob> = callbackFlow {
        val jobId = UUID.randomUUID().toString().substring(0, 8)
        val tempOutput = File(context.cacheDir, "short_${jobId}_916.mp4")

        trySend(
            RenderJob(
                id = jobId,
                inputUri = inputUri,
                outputUri = "",
                trimRange = trimRange,
                hookText = hookText,
                progressPercent = 5
            )
        )

        val duration = trimRange.durationSeconds
        val vfFilter = StringBuilder("crop=ih*9/16:ih:(iw-ow)/2:0,scale=1080:1920:flags=lanczos")

        if (hookText.isNotBlank()) {
            val cleanHook = hookText.replace("'", "").replace(":", "").replace("\\", "").trim()
            vfFilter.append(",drawbox=y=160:color=black@0.65:width=iw:height=140:t=fill")
            vfFilter.append(",drawtext=text='$cleanHook':fontcolor=white:fontsize=46:x=(w-text_w)/2:y=210")
        }

        // Build native FFmpeg command string
        val command = StringBuilder().apply {
            append("-y ")
            append("-ss ${trimRange.startSeconds} ")
            append("-t $duration ")
            append("-i \"$inputUri\" ")
            append("-vf \"$vfFilter\" ")
            append("-c:v libx264 ")
            append("-preset veryfast ")
            append("-crf 22 ")
            append("-c:a aac ")
            append("-b:a 128k ")
            append("\"${tempOutput.absolutePath}\"")
        }.toString()

        val session = FFmpegKit.executeAsync(
            command,
            { completeSession ->
                val returnCode = completeSession.returnCode
                if (ReturnCode.isSuccess(returnCode)) {
                    // Save to Gallery via Android MediaStore
                    val galleryUri = saveVideoToGallery(context, tempOutput, "viral_short_$jobId.mp4")
                    trySend(
                        RenderJob(
                            id = jobId,
                            inputUri = inputUri,
                            outputUri = galleryUri.toString(),
                            trimRange = trimRange,
                            hookText = hookText,
                            progressPercent = 100,
                            isComplete = true
                        )
                    )
                } else {
                    val logs = completeSession.allLogsAsString
                    trySend(
                        RenderJob(
                            id = jobId,
                            inputUri = inputUri,
                            outputUri = "",
                            trimRange = trimRange,
                            hookText = hookText,
                            progressPercent = 0,
                            isComplete = false,
                            error = "Rendering failed (code $returnCode): $logs"
                        )
                    )
                }
                tempOutput.delete()
                channel.close()
            },
            { log ->
                // Log progress
            },
            { statistics ->
                val timeInMs = statistics.time
                if (duration > 0 && timeInMs > 0) {
                    val progress = ((timeInMs / (duration * 1000f)) * 90f).toInt().coerceIn(5, 95)
                    trySend(
                        RenderJob(
                            id = jobId,
                            inputUri = inputUri,
                            outputUri = "",
                            trimRange = trimRange,
                            hookText = hookText,
                            progressPercent = progress
                        )
                    )
                }
            }
        )

        awaitClose {
            FFmpegKit.cancel(session.sessionId)
        }
    }.flowOn(Dispatchers.IO)

    private fun saveVideoToGallery(context: Context, videoFile: File, displayName: String): Uri? {
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

        resolver.openOutputStream(itemUri)?.use { outStream ->
            FileInputStream(videoFile).use { inStream ->
                inStream.copyTo(outStream)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
            resolver.update(itemUri, contentValues, null, null)
        }

        return itemUri
    }
}
