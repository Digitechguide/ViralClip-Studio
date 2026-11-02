package com.mrpallanti.viralvideo.domain.repository

import com.mrpallanti.viralvideo.domain.model.GeneratedShortItem
import com.mrpallanti.viralvideo.domain.model.OverlayConfig
import com.mrpallanti.viralvideo.domain.model.RenderJob
import com.mrpallanti.viralvideo.domain.model.TrimRange
import com.mrpallanti.viralvideo.domain.model.VideoMetadata
import com.mrpallanti.viralvideo.domain.model.ViralClip
import kotlinx.coroutines.flow.Flow
import java.io.File

interface VideoStreamRepository {
    suspend fun extractMetadata(url: String): Result<VideoMetadata>
    suspend fun downloadDirectVideo(
        url: String,
        outputDir: File,
        onProgress: (Float, String) -> Unit
    ): Result<File>
}

interface AiViralityRepository {
    suspend fun analyzeViralHooks(
        title: String,
        description: String,
        totalDurationSeconds: Long,
        targetDurationSeconds: Int = 30
    ): Result<List<ViralClip>>
}

interface VideoRendererRepository {
    fun renderShort(
        inputUri: String,
        trimRange: TrimRange,
        hookText: String,
        overlayConfig: OverlayConfig = OverlayConfig()
    ): Flow<RenderJob>
}

interface GeneratedShortsRepository {
    fun getRecentShorts(): Flow<List<GeneratedShortItem>>
    suspend fun saveShort(item: GeneratedShortItem)
    suspend fun deleteShort(id: String)
}
