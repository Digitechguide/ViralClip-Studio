package com.mrpallanti.viralvideo.presentation.ui.components

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(UnstableApi::class)
@Composable
fun InAppVideoPlayer(
    uriString: String,
    modifier: Modifier = Modifier,
    onPositionUpdate: (Float) -> Unit = {}
) {
    val context = LocalContext.current

    val exoPlayer = remember(uriString) {
        ExoPlayer.Builder(context).build().apply {
            try {
                val mediaItem = MediaItem.fromUri(Uri.parse(uriString))
                setMediaItem(mediaItem)
                prepare()
                playWhenReady = false
            } catch (ignored: Exception) {}
        }
    }

    LaunchedEffect(exoPlayer) {
        while (isActive) {
            try {
                if (exoPlayer.isPlaying) {
                    val posSec = (exoPlayer.currentPosition / 1000f).coerceAtLeast(0f)
                    onPositionUpdate(posSec)
                }
            } catch (ignored: Exception) {}
            delay(250)
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            try {
                exoPlayer.stop()
                exoPlayer.release()
            } catch (ignored: Exception) {}
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
