package in.mrpallanti.viralvideo.domain.repository

import in.mrpallanti.viralvideo.domain.model.RenderJob
import in.mrpallanti.viralvideo.domain.model.TrimRange
import in.mrpallanti.viralvideo.domain.model.VideoMetadata
import in.mrpallanti.viralvideo.domain.model.ViralClip
import kotlinx.coroutines.flow.Flow

interface VideoStreamRepository {
    suspend fun extractMetadata(url: String): Result<VideoMetadata>
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
        hookText: String
    ): Flow<RenderJob>
}
