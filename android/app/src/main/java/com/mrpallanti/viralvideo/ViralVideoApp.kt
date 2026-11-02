package com.mrpallanti.viralvideo

import android.app.Application
import com.mrpallanti.viralvideo.data.local.FfmpegVideoRendererImpl
import com.mrpallanti.viralvideo.data.remote.GeminiFlashApiClient
import com.mrpallanti.viralvideo.data.remote.YoutubeStreamExtractorImpl
import com.mrpallanti.viralvideo.domain.repository.AiViralityRepository
import com.mrpallanti.viralvideo.domain.repository.VideoRendererRepository
import com.mrpallanti.viralvideo.domain.repository.VideoStreamRepository
import com.mrpallanti.viralvideo.domain.usecase.AnalyzeViralHooksUseCase
import com.mrpallanti.viralvideo.domain.usecase.ExtractYoutubeStreamUseCase
import com.mrpallanti.viralvideo.domain.usecase.RenderVerticalShortUseCase

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
