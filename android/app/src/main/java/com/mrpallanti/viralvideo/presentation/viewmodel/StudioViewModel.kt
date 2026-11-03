package com.mrpallanti.viralvideo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrpallanti.viralvideo.domain.model.AuthUser
import com.mrpallanti.viralvideo.domain.model.CropPlacement
import com.mrpallanti.viralvideo.domain.model.GeneratedShortItem
import com.mrpallanti.viralvideo.domain.model.OverlayConfig
import com.mrpallanti.viralvideo.domain.model.RenderJob
import com.mrpallanti.viralvideo.domain.model.TextOverlayPosition
import com.mrpallanti.viralvideo.domain.model.TrimRange
import com.mrpallanti.viralvideo.domain.model.VideoMetadata
import com.mrpallanti.viralvideo.domain.model.ViralClip
import com.mrpallanti.viralvideo.domain.model.WatermarkPosition
import com.mrpallanti.viralvideo.domain.repository.AuthRepository
import com.mrpallanti.viralvideo.domain.usecase.AnalyzeViralHooksUseCase
import com.mrpallanti.viralvideo.domain.usecase.DeleteGeneratedShortUseCase
import com.mrpallanti.viralvideo.domain.usecase.DownloadYoutubeVideoUseCase
import com.mrpallanti.viralvideo.domain.usecase.ExtractYoutubeStreamUseCase
import com.mrpallanti.viralvideo.domain.usecase.GetRecentShortsUseCase
import com.mrpallanti.viralvideo.domain.usecase.RenderVerticalShortUseCase
import com.mrpallanti.viralvideo.domain.usecase.SaveGeneratedShortUseCase
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

enum class StudioTab {
    STUDIO,    // AI Hooks, virality ranking, 1-tap presets
    CLIPPER,   // In-App Player + interactive custom duration scrubber
    BRANDING,  // Watermark logo, custom hook text, opacity, positions
    LIBRARY    // Recently generated videos with instant preview & share
}

data class StudioUiState(
    // Auth State
    val isLoggedIn: Boolean = false,
    val currentUser: AuthUser? = null,
    val isAuthenticating: Boolean = false,
    val authError: String? = null,
    val pendingSharedUrl: String? = null,

    // Navigation Tab
    val currentTab: StudioTab = StudioTab.STUDIO,

    // Studio State
    val urlInput: String = "",
    val isLoading: Boolean = false,
    val loadingMessage: String = "",
    val currentVideo: VideoMetadata? = null,
    val viralClips: List<ViralClip> = emptyList(),
    val selectedClip: ViralClip? = null,
    val trimRange: TrimRange = TrimRange(0f, 30f),
    val hookText: String = "",

    // Custom Overlay & Branding
    val overlayConfig: OverlayConfig = OverlayConfig(),

    // Recently Generated Videos
    val recentShorts: List<GeneratedShortItem> = emptyList(),
    val playingShort: GeneratedShortItem? = null,

    // In-App Video Playback & Scrubber State
    val playerCurrentPositionSec: Float = 0f,

    // Rendering State
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
    private val renderVerticalShortUseCase: RenderVerticalShortUseCase,
    private val getRecentShortsUseCase: GetRecentShortsUseCase,
    private val saveGeneratedShortUseCase: SaveGeneratedShortUseCase,
    private val deleteGeneratedShortUseCase: DeleteGeneratedShortUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        StudioUiState(
            isLoggedIn = authRepository.isLoggedIn(),
            currentUser = authRepository.getCurrentUser()
        )
    )
    val uiState: StateFlow<StudioUiState> = _uiState.asStateFlow()

    private val httpClient = OkHttpClient()

    init {
        viewModelScope.launch {
            getRecentShortsUseCase().collect { list ->
                _uiState.update { it.copy(recentShorts = list) }
            }
        }
    }

    // ----------------------------------------------------
    // NAVIGATION
    // ----------------------------------------------------
    fun setTab(tab: StudioTab) {
        _uiState.update { it.copy(currentTab = tab) }
    }

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
        _uiState.update {
            it.copy(
                hookText = newText,
                overlayConfig = it.overlayConfig.copy(customText = newText)
            )
        }
    }

    fun updateTrimRange(start: Float, end: Float) {
        val maxDuration = _uiState.value.currentVideo?.durationSeconds?.toFloat() ?: 600f
        val cleanStart = start.coerceIn(0f, maxDuration - 1f)
        val cleanEnd = end.coerceIn(cleanStart + 1f, maxDuration)
        _uiState.update { it.copy(trimRange = TrimRange(cleanStart, cleanEnd)) }
    }

    fun setStartToCurrentPlayback(positionSec: Float) {
        val currentEnd = _uiState.value.trimRange.endSeconds
        val newStart = positionSec.coerceAtLeast(0f)
        val newEnd = if (newStart >= currentEnd) (newStart + 30f) else currentEnd
        updateTrimRange(newStart, newEnd)
        _uiState.update { it.copy(successMessage = "Start set to ${formatSeconds(newStart)}") }
    }

    fun setEndToCurrentPlayback(positionSec: Float) {
        val currentStart = _uiState.value.trimRange.startSeconds
        val newEnd = positionSec.coerceAtLeast(currentStart + 1f)
        updateTrimRange(currentStart, newEnd)
        _uiState.update { it.copy(successMessage = "End set to ${formatSeconds(newEnd)}") }
    }

    fun setPresetDuration(targetDurationSec: Float) {
        val currentStart = _uiState.value.trimRange.startSeconds
        val maxDuration = _uiState.value.currentVideo?.durationSeconds?.toFloat() ?: 600f
        val newEnd = (currentStart + targetDurationSec).coerceAtMost(maxDuration)
        updateTrimRange(currentStart, newEnd)
    }

    fun updatePlayerPosition(positionSec: Float) {
        _uiState.update { it.copy(playerCurrentPositionSec = positionSec) }
    }

    fun dismissVideoPickerPrompt() {
        _uiState.update { it.copy(showVideoPickerPrompt = false) }
    }

    // ----------------------------------------------------
    // BRANDING & CUSTOM OVERLAY CONTROLS
    // ----------------------------------------------------
    fun updateOverlayConfig(config: OverlayConfig) {
        _uiState.update { it.copy(overlayConfig = config) }
    }

    fun setCropPlacement(placement: CropPlacement) {
        _uiState.update {
            it.copy(
                overlayConfig = it.overlayConfig.copy(cropPlacement = placement),
                successMessage = when (placement) {
                    CropPlacement.AUTO_FACE -> "Auto Face Tracking activated"
                    CropPlacement.LEFT -> "Framed to Left side (0.16)"
                    CropPlacement.CENTER -> "Framed to Center (0.50)"
                    CropPlacement.RIGHT -> "Framed to Right side (0.84)"
                    CropPlacement.CUSTOM -> "Custom placement enabled"
                }
            )
        }
    }

    fun setCropHorizontalOffset(offset: Float) {
        val cleanOffset = offset.coerceIn(0.0f, 1.0f)
        _uiState.update {
            it.copy(
                overlayConfig = it.overlayConfig.copy(
                    cropPlacement = CropPlacement.CUSTOM,
                    cropHorizontalOffset = cleanOffset
                )
            )
        }
    }

    fun setCustomLogoUri(uriString: String?) {
        _uiState.update {
            it.copy(
                overlayConfig = it.overlayConfig.copy(logoUri = uriString),
                successMessage = if (uriString != null) "Logo attached!" else "Logo cleared"
            )
        }
    }

    fun setWatermarkText(text: String) {
        _uiState.update {
            it.copy(overlayConfig = it.overlayConfig.copy(watermarkText = text))
        }
    }

    fun setWatermarkPosition(position: WatermarkPosition) {
        _uiState.update {
            it.copy(overlayConfig = it.overlayConfig.copy(watermarkPosition = position))
        }
    }

    fun setWatermarkOpacity(opacity: Float) {
        _uiState.update {
            it.copy(overlayConfig = it.overlayConfig.copy(watermarkOpacity = opacity))
        }
    }

    fun setTextOverlayPosition(position: TextOverlayPosition) {
        _uiState.update {
            it.copy(overlayConfig = it.overlayConfig.copy(textPosition = position))
        }
    }

    fun setTextColorHex(hex: String) {
        _uiState.update {
            it.copy(overlayConfig = it.overlayConfig.copy(textColorHex = hex))
        }
    }

    fun setTextBgColorHex(hex: String) {
        _uiState.update {
            it.copy(overlayConfig = it.overlayConfig.copy(textBgColorHex = hex))
        }
    }

    // ----------------------------------------------------
    // RECENT SHORTS & LIBRARY
    // ----------------------------------------------------
    fun setPlayingShort(item: GeneratedShortItem?) {
        _uiState.update { it.copy(playingShort = item) }
    }

    fun deleteGeneratedShort(id: String) {
        viewModelScope.launch {
            deleteGeneratedShortUseCase(id)
            _uiState.update { it.copy(successMessage = "Short removed from history") }
        }
    }

    // ----------------------------------------------------
    // VIDEO LOADING & ANALYSIS
    // ----------------------------------------------------
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
                            overlayConfig = state.overlayConfig.copy(
                                customText = firstClip?.captionOverlay ?: state.overlayConfig.customText
                            ),
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
                                    overlayConfig = state.overlayConfig.copy(
                                        customText = firstClip?.captionOverlay ?: state.overlayConfig.customText
                                    ),
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

    fun loadVideoForManualCropping(url: String = _uiState.value.urlInput, cacheDir: File? = null) {
        val effectiveUrl = url.ifBlank { _uiState.value.urlInput }

        // Case 1: Video is already loaded
        if (_uiState.value.currentVideo != null && effectiveUrl.isBlank()) {
            val current = _uiState.value.currentVideo!!
            if (current.localUri.isNullOrBlank() && cacheDir != null) {
                cacheVideoForPlayback(cacheDir)
            }
            _uiState.update {
                it.copy(
                    currentTab = StudioTab.CLIPPER,
                    successMessage = "Switched to Custom Clipper: Select length & place"
                )
            }
            return
        }

        // Case 2: No URL provided and no video loaded
        if (effectiveUrl.isBlank()) {
            _uiState.update {
                it.copy(
                    currentTab = StudioTab.CLIPPER,
                    errorMessage = "Please paste a YouTube URL or select a video from Gallery"
                )
            }
            return
        }

        // Case 3: Load YouTube video and AUTO-CACHE OFFLINE immediately
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    loadingMessage = "Extracting video details...",
                    errorMessage = null
                )
            }

            extractYoutubeStreamUseCase(effectiveUrl).fold(
                onSuccess = { metadata ->
                    val defaultEnd = minOf(30f, metadata.durationSeconds.toFloat().coerceAtLeast(10f))

                    // If cacheDir is provided, automatically download with on-device engine now!
                    if (cacheDir != null && metadata.localUri.isNullOrBlank()) {
                        _uiState.update {
                            it.copy(
                                currentVideo = metadata,
                                loadingMessage = "Auto-caching video for smooth offline cropping: 0%..."
                            )
                        }

                        val dlResult = downloadYoutubeVideoUseCase(
                            url = effectiveUrl,
                            outputDir = cacheDir,
                            onProgress = { progress, status ->
                                _uiState.update {
                                    it.copy(
                                        loadingMessage = if (progress in 1f..99f) {
                                            "Auto-caching video: ${progress.toInt()}% (On-device engine)..."
                                        } else {
                                            status
                                        }
                                    )
                                }
                            }
                        )

                        dlResult.fold(
                            onSuccess = { localFile ->
                                _uiState.update { state ->
                                    state.copy(
                                        isLoading = false,
                                        currentVideo = metadata.copy(localUri = localFile.absolutePath, streamUrl = localFile.absolutePath),
                                        currentTab = StudioTab.CLIPPER,
                                        trimRange = TrimRange(0f, defaultEnd),
                                        hookText = state.hookText.ifBlank { "WAIT TILL THE END... 🤯" },
                                        successMessage = "Auto-cached offline! Frame-accurate scrubbing ready."
                                    )
                                }
                            },
                            onFailure = { err ->
                                _uiState.update { state ->
                                    state.copy(
                                        isLoading = false,
                                        currentVideo = metadata,
                                        currentTab = StudioTab.CLIPPER,
                                        trimRange = TrimRange(0f, defaultEnd),
                                        hookText = state.hookText.ifBlank { "WAIT TILL THE END... 🤯" },
                                        errorMessage = "Auto-cache note: ${err.message?.lines()?.firstOrNull() ?: "Using stream playback"}"
                                    )
                                }
                            }
                        )
                    } else {
                        _uiState.update { state ->
                            state.copy(
                                isLoading = false,
                                currentVideo = metadata,
                                currentTab = StudioTab.CLIPPER,
                                trimRange = TrimRange(0f, defaultEnd),
                                hookText = state.hookText.ifBlank { "WAIT TILL THE END... 🤯" },
                                successMessage = "Loaded '${metadata.title}'! Choose length & place below."
                            )
                        }
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = err.message ?: "Failed to load YouTube video"
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
                overlayConfig = it.overlayConfig.copy(customText = clip.captionOverlay),
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

    fun cacheVideoForPlayback(cacheDir: File) {
        val state = _uiState.value
        val video = state.currentVideo ?: return
        if (!video.localUri.isNullOrBlank()) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    loadingMessage = "Initializing on-device engine for offline caching...",
                    errorMessage = null
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
                                "Caching from YouTube: ${progress.toInt()}% (On-device engine)..."
                            } else {
                                status
                            }
                        )
                    }
                }
            )

            dlResult.fold(
                onSuccess = { file ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            currentVideo = it.currentVideo?.copy(localUri = file.absolutePath),
                            successMessage = "Cached on device! Smooth offline scrubbing enabled."
                        )
                    }
                },
                onFailure = { err ->
                    val cleanError = err.message
                        ?.lines()
                        ?.firstOrNull { line ->
                            val l = line.trim()
                            l.isNotBlank() && !l.startsWith("WARNING", ignoreCase = true)
                        }
                        ?: err.message
                        ?: "Could not download stream"

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            showVideoPickerPrompt = true,
                            errorMessage = "Caching notice: $cleanError"
                        )
                    }
                }
            )
        }
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
                            val cleanError = err.message
                                ?.lines()
                                ?.firstOrNull { line ->
                                    val l = line.trim()
                                    l.isNotBlank() && !l.startsWith("WARNING", ignoreCase = true)
                                }
                                ?: err.message
                                ?: "Could not download stream"

                            _uiState.update {
                                it.copy(
                                    isRendering = false,
                                    showVideoPickerPrompt = true,
                                    errorMessage = "Auto-download notice: $cleanError"
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
                    hookText = state.hookText,
                    overlayConfig = state.overlayConfig
                ).collect { job ->
                    _uiState.update {
                        it.copy(
                            renderJob = job,
                            isRendering = !job.isComplete && job.error == null,
                            successMessage = if (job.isComplete) "🎉 Saved directly to your Phone Gallery!" else it.successMessage,
                            errorMessage = job.error
                        )
                    }

                    if (job.isComplete && job.outputUri.isNotBlank()) {
                        val item = GeneratedShortItem(
                            id = job.id,
                            title = state.selectedClip?.title ?: state.currentVideo?.title ?: "Viral Short",
                            outputUri = job.outputUri,
                            durationSeconds = state.trimRange.durationSeconds,
                            timestamp = System.currentTimeMillis(),
                            fileSizeFormatted = "",
                            hookCaption = state.hookText
                        )
                        saveGeneratedShortUseCase(item)
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

    private fun formatSeconds(sec: Float): String {
        val totalSec = sec.toInt()
        val m = totalSec / 60
        val s = totalSec % 60
        return String.format("%02d:%02d", m, s)
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
