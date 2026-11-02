package com.mrpallanti.viralvideo.presentation

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mrpallanti.viralvideo.ViralVideoApp
import com.mrpallanti.viralvideo.presentation.ui.StudioScreen
import com.mrpallanti.viralvideo.presentation.ui.theme.ViralVideoTheme
import com.mrpallanti.viralvideo.presentation.viewmodel.StudioViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: StudioViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = application as ViralVideoApp
                return StudioViewModel(
                    extractYoutubeStreamUseCase = app.extractYoutubeStreamUseCase,
                    analyzeViralHooksUseCase = app.analyzeViralHooksUseCase,
                    renderVerticalShortUseCase = app.renderVerticalShortUseCase
                ) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Handle YouTube shared text intent
        handleIncomingIntent(intent)

        setContent {
            ViralVideoTheme {
                StudioScreen(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
            if (sharedText.isNotBlank()) {
                viewModel.onUrlChanged(sharedText)
                viewModel.fetchAndAnalyze(sharedText)
            }
        }
    }
}
