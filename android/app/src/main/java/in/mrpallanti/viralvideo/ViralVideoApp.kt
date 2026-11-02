package in.mrpallanti.viralvideo

import android.app.Application
import in.mrpallanti.viralvideo.data.local.FfmpegVideoRendererImpl
import in.mrpallanti.viralvideo.data.remote.GeminiFlashApiClient
import in.mrpallanti.viralvideo.data.remote.YoutubeStreamExtractorImpl
import in.mrpallanti.viralvideo.domain.repository.AiViralityRepository
import in.mrpallanti.viralvideo.domain.repository.VideoRendererRepository
import in.mrpallanti.viralvideo.domain.repository.VideoStreamRepository
import in.mrpallanti.viralvideo.domain.usecase.AnalyzeViralHooksUseCase
import in.mrpallanti.viralvideo.domain.usecase.ExtractYoutubeStreamUseCase
import in.mrpallanti.viralvideo.domain.usecase.RenderVerticalShortUseCase

class ViralVideoApp : Application() {

    // Repositories
    val videoStreamRepository: VideoStreamRepository by lazy {
        YoutubeStreamExtractorImpl()
    }

    val aiViralityRepository: AiViralityRepository by lazy {
        GeminiFlashApiClient()
    }

    val videoRendererRepository: VideoRendererRepository by lazy {
        FfmpegVideoRendererImpl(this)
    }

    // UseCases
    val extractYoutubeStreamUseCase: ExtractYoutubeStreamUseCase by lazy {
        ExtractYoutubeStreamUseCase(videoStreamRepository)
    }

    val analyzeViralHooksUseCase: AnalyzeViralHooksUseCase by lazy {
        AnalyzeViralHooksUseCase(aiViralityRepository)
    }

    val renderVerticalShortUseCase: RenderVerticalShortUseCase by lazy {
        RenderVerticalShortUseCase(videoRendererRepository)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: ViralVideoApp
            private set
    }
}
