package com.mrpallanti.viralvideo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrpallanti.viralvideo.domain.model.AuthUser
import com.mrpallanti.viralvideo.domain.model.RenderJob
import com.mrpallanti.viralvideo.domain.model.TrimRange
import com.mrpallanti.viralvideo.domain.model.VideoMetadata
import com.mrpallanti.viralvideo.domain.model.ViralClip
import com.mrpallanti.viralvideo.domain.repository.AuthRepository
import com.mrpallanti.viralvideo.domain.usecase.AnalyzeViralHooksUseCase
import com.mrpallanti.viralvideo.domain.usecase.DownloadYoutubeVideoUseCase
import com.mrpallanti.viralvideo.domain.usecase.ExtractYoutubeStreamUseCase
import com.mrpallanti.viralvideo.domain.usecase.RenderVerticalShortUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

data class StudioUiState(
    // Auth State
    val isLoggedIn: Boolean = false,
    val currentUser: AuthUser? = null,
    val isAuthenticating: Boolean = false,
    val authError: String? = null,
    val pendingSharedUrl: String? = null,

    // Studio State
    val urlInput: String = "",
    val isLoading: Boolean = false,
    val loadingMessage: String = "",
    val currentVideo: VideoMetadata? = null,
    val viralClips: List<ViralClip> = emptyList(),
    val selectedClip: ViralClip? = null,
    val trimRange: TrimRange = TrimRange(0f, 30f),
    val hookText: String = "",
    val isRendering: Boolean = false,
    val renderJob: RenderJob? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val showVideoPickerPrompt: Boolean = false
)

class StudioViewModel(
    private val authRepository: AuthRepository,
    private val extractYoutubeStreamUseCase: ExtractYoutubeStreamUseCase,
    private val downloadYoutubeVideoUseCase: DownloadYoutubeVideoUseCase,
    private val analyzeViralHooksUseCase: AnalyzeViralHooksUseCase,
    private val renderVerticalShortUseCase: RenderVerticalShortUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        StudioUiState(
            isLoggedIn = authRepository.isLoggedIn(),
            currentUser = authRepository.getCurrentUser()
        )
    )
    val uiState: StateFlow<StudioUiState> = _uiState.asStateFlow()

    private val httpClient = OkHttpClient()

    // ----------------------------------------------------
    // AUTHENTICATION
    // ----------------------------------------------------
    fun login(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _uiState.update { it.copy(authError = "Please enter both email and password") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isAuthenticating = true, authError = null) }
            val result = authRepository.login(email.trim(), pass)

            result.fold(
                onSuccess = { user ->
                    val pendingUrl = _uiState.value.pendingSharedUrl
                    _uiState.update {
                        it.copy(
                            isAuthenticating = false,
                            isLoggedIn = true,
                            currentUser = user,
                            authError = null,
                            pendingSharedUrl = null,
                            successMessage = "Welcome, ${user.name}!"
                        )
                    }
                    if (!pendingUrl.isNullOrBlank()) {
                        onUrlChanged(pendingUrl)
                        fetchAndAnalyze(pendingUrl)
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isAuthenticating = false,
                            authError = err.message ?: "Authentication failed"
                        )
                    }
                }
            )
        }
    }

    fun logout() {
        authRepository.logout()
        _uiState.update {
            StudioUiState(
                isLoggedIn = false,
                currentUser = null,
                successMessage = "Logged out successfully"
            )
        }
    }

    fun clearAuthError() {
        _uiState.update { it.copy(authError = null) }
    }

    fun handleIncomingSharedUrl(sharedUrl: String) {
        if (_uiState.value.isLoggedIn) {
            onUrlChanged(sharedUrl)
            fetchAndAnalyze(sharedUrl)
        } else {
            _uiState.update { it.copy(pendingSharedUrl = sharedUrl) }
        }
    }

    // ----------------------------------------------------
    // STUDIO ACTIONS
    // ----------------------------------------------------
    fun onUrlChanged(newUrl: String) {
        _uiState.update { it.copy(urlInput = newUrl) }
    }

    fun onHookTextChanged(newText: String) {
        _uiState.update { it.copy(hookText = newText) }
    }

    fun updateTrimRange(start: Float, end: Float) {
        _uiState.update { it.copy(trimRange = TrimRange(start, end)) }
    }

    fun dismissVideoPickerPrompt() {
        _uiState.update { it.copy(showVideoPickerPrompt = false) }
    }

    fun onLocalVideoSelected(uriString: String, title: String, durationSeconds: Long) {
        val metadata = VideoMetadata(
            id = "local_${System.currentTimeMillis()}",
            title = title.ifBlank { "Local Video" },
            author = "Device Gallery",
            durationSeconds = durationSeconds.coerceAtLeast(10),
            thumbnailUrl = "",
            localUri = uriString,
            streamUrl = uriString
        )

        _uiState.update {
            it.copy(
                currentVideo = metadata,
                showVideoPickerPrompt = false,
                isLoading = true,
                loadingMessage = "Analyzing virality hooks for local video..."
            )
        }

        viewModelScope.launch {
            analyzeViralHooksUseCase(
                title = metadata.title,
                totalDurationSeconds = metadata.durationSeconds
            ).fold(
                onSuccess = { clips ->
                    val firstClip = clips.firstOrNull()
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            viralClips = clips,
                            selectedClip = firstClip,
                            trimRange = firstClip?.let { TrimRange(it.startSeconds, it.endSeconds) }
                                ?: TrimRange(0f, 30f),
                            hookText = firstClip?.captionOverlay ?: "WAIT TILL THE END... 🤯",
                            successMessage = "Loaded local video! ${clips.size} viral hooks suggested."
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "AI Hook error: ${err.message}"
                        )
                    }
                }
            )
        }
    }

    fun fetchAndAnalyze(url: String = _uiState.value.urlInput) {
        if (url.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter a YouTube URL") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    loadingMessage = "Extracting video details & streams...",
                    errorMessage = null
                )
            }

            extractYoutubeStreamUseCase(url).fold(
                onSuccess = { metadata ->
                    _uiState.update {
                        it.copy(
                            currentVideo = metadata,
                            loadingMessage = "Analyzing virality & retention curve with Gemini AI..."
                        )
                    }

                    // Run AI Virality Analysis
                    analyzeViralHooksUseCase(
                        title = metadata.title,
                        totalDurationSeconds = metadata.durationSeconds
                    ).fold(
                        onSuccess = { clips ->
                            val firstClip = clips.firstOrNull()
                            _uiState.update { state ->
                                state.copy(
                                    isLoading = false,
                                    viralClips = clips,
                                    selectedClip = firstClip,
                                    trimRange = firstClip?.let { TrimRange(it.startSeconds, it.endSeconds) }
                                        ?: TrimRange(0f, 30f),
                                    hookText = firstClip?.captionOverlay ?: "WAIT TILL THE END... 🤯",
                                    successMessage = "Analyzed with ${clips.size} viral clips ready!"
                                )
                            }
                        },
                        onFailure = { err ->
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = "AI Hook error: ${err.message}"
                                )
                            }
                        }
                    )
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = err.message ?: "Failed to load video"
                        )
                    }
                }
            )
        }
    }

    fun applyClip(clip: ViralClip) {
        _uiState.update {
            it.copy(
                selectedClip = clip,
                trimRange = TrimRange(clip.startSeconds, clip.endSeconds),
                hookText = clip.captionOverlay,
                successMessage = "Applied '${clip.title}'"
            )
        }
    }

    fun getViralBlueprintText(): String {
        val state = _uiState.value
        val clip = state.selectedClip
        val video = state.currentVideo
        val title = clip?.title ?: video?.title ?: "Viral Short"
        val startSec = state.trimRange.startSeconds.toInt()
        val endSec = state.trimRange.endSeconds.toInt()
        val durSec = state.trimRange.durationSeconds.toInt()
        val hook = state.hookText.ifBlank { "WAIT TILL THE END... 🤯" }
        val score = clip?.viralityScore ?: 95

        return """
🎬 VIRAL SHORT BLUEPRINT (ViralClip Studio)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📌 Title: $title
⏱️ Timestamp: ${startSec}s - ${endSec}s (${durSec}s total)
🔥 Virality Score: $score%
💡 Hook Caption: "$hook"
🏷️ Recommended Hashtags:
#Shorts #Viral #Trending #Reels #ContentCreator #ViralClipStudio
━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Ready for CapCut, YouTube Shorts, TikTok & IG Reels!
        """.trimIndent()
    }

    fun startRenderShort(cacheDir: File? = null) {
        val state = _uiState.value
        val video = state.currentVideo

        if (video == null) {
            _uiState.update { it.copy(errorMessage = "Please select or analyze a video first") }
            return
        }

        viewModelScope.launch {
            try {
                var targetUri = video.localUri

                // If local file is missing, automatically download full video using on-device yt-dlp engine!
                if (targetUri.isNullOrBlank() && cacheDir != null) {
                    _uiState.update {
                        it.copy(
                            isRendering = true,
                            loadingMessage = "Initializing on-device engine for YouTube download..."
                        )
                    }

                    val dlUrl = state.urlInput.ifBlank { "https://www.youtube.com/watch?v=${video.id}" }
                    val dlResult = downloadYoutubeVideoUseCase(
                        url = dlUrl,
                        outputDir = cacheDir,
                        onProgress = { progress, status ->
                            _uiState.update {
                                it.copy(
                                    loadingMessage = if (progress in 1f..99f) {
                                        "Downloading from YouTube: ${progress.toInt()}% (On-device engine)..."
                                    } else {
                                        status
                                    }
                                )
                            }
                        }
                    )

                    dlResult.fold(
                        onSuccess = { file ->
                            targetUri = file.absolutePath
                            _uiState.update {
                                it.copy(
                                    currentVideo = it.currentVideo?.copy(localUri = targetUri),
                                    loadingMessage = "Download complete! Now rendering 9:16 vertical Short..."
                                )
                            }
                        },
                        onFailure = { err ->
                            // If auto-download hits any restriction, fall back to gallery picker
                            _uiState.update {
                                it.copy(
                                    isRendering = false,
                                    showVideoPickerPrompt = true,
                                    errorMessage = "Auto-download notice: ${err.message}"
                                )
                            }
                            return@launch
                        }
                    )
                }

                // If targetUri is still null, prompt creator to pick from gallery
                val resolvedUri = targetUri
                if (resolvedUri.isNullOrBlank()) {
                    _uiState.update {
                        it.copy(
                            isRendering = false,
                            showVideoPickerPrompt = true
                        )
                    }
                    return@launch
                }

                _uiState.update { it.copy(isRendering = true, errorMessage = null) }

                renderVerticalShortUseCase(
                    inputUri = resolvedUri,
                    trimRange = state.trimRange,
                    hookText = state.hookText
                ).collect { job ->
                    _uiState.update {
                        it.copy(
                            renderJob = job,
                            isRendering = !job.isComplete && job.error == null,
                            successMessage = if (job.isComplete) "🎉 Saved directly to your Phone Gallery!" else it.successMessage,
                            errorMessage = job.error
                        )
                    }
                }
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(
                        isRendering = false,
                        errorMessage = "Rendering error: ${t.message ?: "Failed to process video"}"
                    )
                }
            }
        }
    }

    private suspend fun downloadDirectStream(
        streamUrl: String,
        cacheDir: File,
        videoId: String
    ): File? = withContext(Dispatchers.IO) {
        return@withContext try {
            val tempFile = File(cacheDir, "yt_${videoId}_source.mp4")
            val request = Request.Builder()
                .url(streamUrl)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14)")
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                response.body?.byteStream()?.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }
                if (tempFile.exists() && tempFile.length() > 1024) tempFile else null
            } else {
                null
            }
        } catch (t: Throwable) {
            null
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
