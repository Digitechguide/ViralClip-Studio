package in.mrpallanti.viralvideo.domain.usecase

import in.mrpallanti.viralvideo.domain.model.RenderJob
import in.mrpallanti.viralvideo.domain.model.TrimRange
import in.mrpallanti.viralvideo.domain.model.VideoMetadata
import in.mrpallanti.viralvideo.domain.model.ViralClip
import in.mrpallanti.viralvideo.domain.repository.AiViralityRepository
import in.mrpallanti.viralvideo.domain.repository.VideoRendererRepository
import in.mrpallanti.viralvideo.domain.repository.VideoStreamRepository
import kotlinx.coroutines.flow.Flow

class ExtractYoutubeStreamUseCase(
    private val repository: VideoStreamRepository
) {
    suspend operator fun invoke(url: String): Result<VideoMetadata> {
        if (url.isBlank()) {
            return Result.failure(IllegalArgumentException("URL cannot be empty"))
        }
        return repository.extractMetadata(url.trim())
    }
}

class AnalyzeViralHooksUseCase(
    private val repository: AiViralityRepository
) {
    suspend operator fun invoke(
        title: String,
        description: String = "",
        totalDurationSeconds: Long,
        targetDurationSeconds: Int = 30
    ): Result<List<ViralClip>> {
        return repository.analyzeViralHooks(
            title = title,
            description = description,
            totalDurationSeconds = totalDurationSeconds,
            targetDurationSeconds = targetDurationSeconds
        )
    }
}

class RenderVerticalShortUseCase(
    private val repository: VideoRendererRepository
) {
    operator fun invoke(
        inputUri: String,
        trimRange: TrimRange,
        hookText: String
    ): Flow<RenderJob> {
        return repository.renderShort(inputUri, trimRange, hookText)
    }
}
