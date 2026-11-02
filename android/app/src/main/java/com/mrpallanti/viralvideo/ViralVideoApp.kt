package com.mrpallanti.viralvideo

import android.app.Application
import com.mrpallanti.viralvideo.data.local.AuthSessionManager
import com.mrpallanti.viralvideo.data.local.Media3TransformerVideoRendererImpl
import com.mrpallanti.viralvideo.data.remote.AuthRepositoryImpl
import com.mrpallanti.viralvideo.data.remote.GeminiFlashApiClient
import com.mrpallanti.viralvideo.data.remote.YoutubeStreamExtractorImpl
import com.mrpallanti.viralvideo.domain.repository.AiViralityRepository
import com.mrpallanti.viralvideo.domain.repository.AuthRepository
import com.mrpallanti.viralvideo.domain.repository.VideoRendererRepository
import com.mrpallanti.viralvideo.domain.repository.VideoStreamRepository
import com.mrpallanti.viralvideo.domain.usecase.AnalyzeViralHooksUseCase
import com.mrpallanti.viralvideo.domain.usecase.ExtractYoutubeStreamUseCase
import com.mrpallanti.viralvideo.domain.usecase.RenderVerticalShortUseCase

class ViralVideoApp : Application() {

    // Auth Session Manager & Repository
    val authSessionManager: AuthSessionManager by lazy {
        AuthSessionManager(this)
    }

    val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(authSessionManager)
    }

    // Video & AI Repositories
    val videoStreamRepository: VideoStreamRepository by lazy {
        YoutubeStreamExtractorImpl(this)
    }

    val aiViralityRepository: AiViralityRepository by lazy {
        GeminiFlashApiClient()
    }

    val videoRendererRepository: VideoRendererRepository by lazy {
        Media3TransformerVideoRendererImpl(this)
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
