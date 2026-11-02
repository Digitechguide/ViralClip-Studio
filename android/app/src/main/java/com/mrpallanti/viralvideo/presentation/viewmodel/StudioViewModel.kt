package com.mrpallanti.viralvideo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrpallanti.viralvideo.domain.model.RenderJob
import com.mrpallanti.viralvideo.domain.model.TrimRange
import com.mrpallanti.viralvideo.domain.model.VideoMetadata
import com.mrpallanti.viralvideo.domain.model.ViralClip
import com.mrpallanti.viralvideo.domain.usecase.AnalyzeViralHooksUseCase
import com.mrpallanti.viralvideo.domain.usecase.ExtractYoutubeStreamUseCase
import com.mrpallanti.viralvideo.domain.usecase.RenderVerticalShortUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StudioUiState(
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
    val successMessage: String? = null
)

class StudioViewModel(
    private val extractYoutubeStreamUseCase: ExtractYoutubeStreamUseCase,
    private val analyzeViralHooksUseCase: AnalyzeViralHooksUseCase,
    private val renderVerticalShortUseCase: RenderVerticalShortUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudioUiState())
    val uiState: StateFlow<StudioUiState> = _uiState.asStateFlow()

    fun onUrlChanged(newUrl: String) {
        _uiState.update { it.copy(urlInput = newUrl) }
    }

    fun onHookTextChanged(newText: String) {
        _uiState.update { it.copy(hookText = newText) }
    }

    fun updateTrimRange(start: Float, end: Float) {
        _uiState.update { it.copy(trimRange = TrimRange(start, end)) }
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
                    loadingMessage = "Extracting video details...",
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

    fun startRenderShort() {
        val video = _uiState.value.currentVideo ?: return
        val inputUri = video.localUri ?: video.streamUrl ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isRendering = true, errorMessage = null) }

            renderVerticalShortUseCase(
                inputUri = inputUri,
                trimRange = _uiState.value.trimRange,
                hookText = _uiState.value.hookText
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
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
