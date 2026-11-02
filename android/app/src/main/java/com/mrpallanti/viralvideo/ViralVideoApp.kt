package com.mrpallanti.viralvideo

import android.app.Application
import android.util.Log
import com.mrpallanti.viralvideo.data.local.AuthSessionManager
import com.mrpallanti.viralvideo.data.local.GeneratedShortsRepositoryImpl
import com.mrpallanti.viralvideo.data.local.Media3TransformerVideoRendererImpl
import com.mrpallanti.viralvideo.data.remote.AuthRepositoryImpl
import com.mrpallanti.viralvideo.data.remote.GeminiFlashApiClient
import com.mrpallanti.viralvideo.data.remote.YoutubeStreamExtractorImpl
import com.mrpallanti.viralvideo.domain.repository.AiViralityRepository
import com.mrpallanti.viralvideo.domain.repository.AuthRepository
import com.mrpallanti.viralvideo.domain.repository.GeneratedShortsRepository
import com.mrpallanti.viralvideo.domain.repository.VideoRendererRepository
import com.mrpallanti.viralvideo.domain.repository.VideoStreamRepository
import com.mrpallanti.viralvideo.domain.usecase.AnalyzeViralHooksUseCase
import com.mrpallanti.viralvideo.domain.usecase.DeleteGeneratedShortUseCase
import com.mrpallanti.viralvideo.domain.usecase.DownloadYoutubeVideoUseCase
import com.mrpallanti.viralvideo.domain.usecase.ExtractYoutubeStreamUseCase
import com.mrpallanti.viralvideo.domain.usecase.GetRecentShortsUseCase
import com.mrpallanti.viralvideo.domain.usecase.RenderVerticalShortUseCase
import com.mrpallanti.viralvideo.domain.usecase.SaveGeneratedShortUseCase
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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

    val generatedShortsRepository: GeneratedShortsRepository by lazy {
        GeneratedShortsRepositoryImpl(this)
    }

    // UseCases
    val extractYoutubeStreamUseCase: ExtractYoutubeStreamUseCase by lazy {
        ExtractYoutubeStreamUseCase(videoStreamRepository)
    }

    val downloadYoutubeVideoUseCase: DownloadYoutubeVideoUseCase by lazy {
        DownloadYoutubeVideoUseCase(videoStreamRepository)
    }

    val analyzeViralHooksUseCase: AnalyzeViralHooksUseCase by lazy {
        AnalyzeViralHooksUseCase(aiViralityRepository)
    }

    val renderVerticalShortUseCase: RenderVerticalShortUseCase by lazy {
        RenderVerticalShortUseCase(videoRendererRepository)
    }

    val getRecentShortsUseCase: GetRecentShortsUseCase by lazy {
        GetRecentShortsUseCase(generatedShortsRepository)
    }

    val saveGeneratedShortUseCase: SaveGeneratedShortUseCase by lazy {
        SaveGeneratedShortUseCase(generatedShortsRepository)
    }

    val deleteGeneratedShortUseCase: DeleteGeneratedShortUseCase by lazy {
        DeleteGeneratedShortUseCase(generatedShortsRepository)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        initEmbeddedEngine()
    }

    private fun initEmbeddedEngine() {
        // Asynchronously initialize embedded yt-dlp & FFmpeg binaries to prevent main thread ANR
        CoroutineScope(Dispatchers.IO).launch {
            try {
                YoutubeDL.getInstance().init(this@ViralVideoApp)
                FFmpeg.getInstance().init(this@ViralVideoApp)
                Log.d("ViralVideoApp", "Embedded yt-dlp & FFmpeg engines initialized.")

                // Check and dynamically update yt-dlp to latest release to maintain YouTube cipher compatibility
                try {
                    val updateStatus = YoutubeDL.getInstance().updateYoutubeDL(this@ViralVideoApp, YoutubeDL.UpdateChannel.STABLE)
                    Log.d("ViralVideoApp", "yt-dlp background update status: $updateStatus")
                } catch (updateErr: Throwable) {
                    Log.w("ViralVideoApp", "yt-dlp auto-update check skipped/failed: ${updateErr.message}")
                }
            } catch (t: Throwable) {
                Log.e("ViralVideoApp", "Error initializing embedded yt-dlp: ${t.message}", t)
            }
        }
    }

    companion object {
        lateinit var instance: ViralVideoApp
            private set
    }
}
