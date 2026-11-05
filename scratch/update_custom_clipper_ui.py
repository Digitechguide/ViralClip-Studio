import os

new_code = '''package com.mrpallanti.viralvideo.presentation.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.exoplayer.ExoPlayer
import com.mrpallanti.viralvideo.domain.model.CropPlacement
import com.mrpallanti.viralvideo.domain.model.VideoQuality
import com.mrpallanti.viralvideo.presentation.ui.theme.*
import com.mrpallanti.viralvideo.presentation.viewmodel.StudioUiState
import com.mrpallanti.viralvideo.presentation.viewmodel.StudioViewModel

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun CustomClipperView(
    state: StudioUiState,
    viewModel: StudioViewModel,
    onPickFromGallery: () -> Unit
) {
    val context = LocalContext.current
    val video = state.currentVideo
    var playerPositionSec by remember { mutableFloatStateOf(0f) }

    var exoPlayerInstance by remember { mutableStateOf<ExoPlayer?>(null) }
    var isPlaybackPaused by remember { mutableStateOf(false) }

    if (video == null) {
        // Empty State: No video loaded yet
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(36.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(PrimaryPurple.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCut,
                        contentDescription = null,
                        tint = PrimaryPurple,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "No Video Selected",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Paste a YouTube link in the Studio tab or pick a local video from your device to begin custom length & place cropping.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onPickFromGallery,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select from Gallery", fontWeight = FontWeight.Bold)
                }
            }
        }
        return
    }

    val totalDurationSec = video.durationSeconds.toFloat().coerceAtLeast(10f)
    val startSec = state.trimRange.startSeconds.coerceIn(0f, totalDurationSec - 1f)
    val endSec = state.trimRange.endSeconds.coerceIn(startSec + 1f, totalDurationSec)
    val durationSec = endSec - startSec

    val hasLocalFile = !video.localUri.isNullOrBlank()
    val playableUri = video.localUri ?: video.streamUrl

    val currentPlacement = state.overlayConfig.cropPlacement
    val currentOffset = state.overlayConfig.cropHorizontalOffset

    // Auto-cache YouTube video offline immediately if not cached yet
    LaunchedEffect(video.id, hasLocalFile) {
        if (!hasLocalFile && !video.id.startsWith("local_")) {
            viewModel.cacheVideoForPlayback(context.cacheDir)
        }
    }

    // Auto-stop preview when playhead reaches endSec
    LaunchedEffect(playerPositionSec, endSec) {
        if (!isPlaybackPaused && playerPositionSec >= endSec && endSec > startSec) {
            exoPlayerInstance?.pause()
            isPlaybackPaused = true
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ----------------------------------------------------
        // 1. IN-APP STUDIO VIDEO PLAYER & TIMELINE CONSOLE
        // ----------------------------------------------------
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(
                1.dp,
                Brush.horizontalGradient(
                    listOf(
                        PrimaryPurple.copy(alpha = 0.45f),
                        AccentCyan.copy(alpha = 0.35f),
                        BorderSubtle
                    )
                )
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header: Status Badge & Video Info
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            color = if (hasLocalFile) SuccessGreen.copy(alpha = 0.15f) else AccentCyan.copy(alpha = 0.15f),
                            shape = CircleShape,
                            modifier = Modifier.size(30.dp),
                            border = BorderStroke(1.dp, if (hasLocalFile) SuccessGreen.copy(alpha = 0.4f) else AccentCyan.copy(alpha = 0.4f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (hasLocalFile) Icons.Default.CheckCircle else Icons.Default.DownloadCloud,
                                    contentDescription = null,
                                    tint = if (hasLocalFile) SuccessGreen else AccentCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = video.title,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (hasLocalFile) "Offline Local Engine • 60fps Scrubbing" else "Auto-extracting high-bitrate stream...",
                                color = if (hasLocalFile) SuccessGreen else TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        color = if (hasLocalFile) SuccessGreen.copy(alpha = 0.15f) else PrimaryPurple.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (hasLocalFile) SuccessGreen.copy(alpha = 0.4f) else PrimaryPurple.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = if (hasLocalFile) "🟢 ${state.selectedQuality.shortLabel} Ready" else "📥 Caching ${state.selectedQuality.shortLabel}...",
                            color = if (hasLocalFile) SuccessGreen else PrimaryPurple,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // Video Display Surface
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                ) {
                    if (hasLocalFile) {
                        InAppVideoPlayer(
                            uriString = video.localUri!!,
                            modifier = Modifier.fillMaxSize(),
                            onPositionUpdate = { pos ->
                                playerPositionSec = pos
                                viewModel.updatePlayerPosition(pos)
                            },
                            onIsPlayingChanged = { isPlaying ->
                                isPlaybackPaused = !isPlaying
                            },
                            onPlayerReady = { player ->
                                exoPlayerInstance = player
                            }
                        )
                    } else if (!video.id.startsWith("local_")) {
                        // High-tech Auto-Caching Indicator Surface (Bypasses YouTube Error 152 completely)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            Color(0xFF1E1035),
                                            Color(0xFF090D1A)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(36.dp),
                                    color = AccentCyan,
                                    strokeWidth = 3.dp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Auto-Caching Video Offline...",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (state.isLoading) state.loadingMessage else "Extracting ${state.selectedQuality.label} stream for zero-lag scrubbing...",
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = AccentCyan.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.4f))
                                    ) {
                                        Text(
                                            text = "⚡ Zero Buffer Scrubbing",
                                            color = AccentCyan,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Surface(
                                        color = PrimaryPurple.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, PrimaryPurple.copy(alpha = 0.4f))
                                    ) {
                                        Text(
                                            text = "🛡️ Direct Stream Extraction",
                                            color = PrimaryPurple,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    } else if (!playableUri.isNullOrBlank()) {
                        InAppVideoPlayer(
                            uriString = playableUri,
                            modifier = Modifier.fillMaxSize(),
                            onPositionUpdate = { pos ->
                                playerPositionSec = pos
                                viewModel.updatePlayerPosition(pos)
                            },
                            onIsPlayingChanged = { isPlaying ->
                                isPlaybackPaused = !isPlaying
                            },
                            onPlayerReady = { player ->
                                exoPlayerInstance = player
                            }
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No playable stream available", color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ----------------------------------------------------
                // Output Resolution & Cache Quality Selector Chips
                // ----------------------------------------------------
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.HighQuality,
                            contentDescription = null,
                            tint = AccentCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Output Quality & Cache:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    Text(
                        text = "${state.selectedQuality.width} × ${state.selectedQuality.height}",
                        fontSize = 11.sp,
                        color = AccentCyan,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VideoQuality.values().forEach { quality ->
                        val isSelected = state.selectedQuality == quality
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (!isSelected) {
                                    viewModel.setVideoQuality(quality, context.cacheDir)
                                }
                            },
                            label = {
                                Text(
                                    text = quality.label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                                )
                            },
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = AccentCyan
                                    )
                                }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentCyan.copy(alpha = 0.2f),
                                selectedLabelColor = Color.White,
                                containerColor = BgDark,
                                labelColor = TextSecondary
                            ),
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) AccentCyan else BorderSubtle
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ----------------------------------------------------
                // Real-Time Video Timeline Scrubber
                // ----------------------------------------------------
                Slider(
                    value = playerPositionSec.coerceIn(0f, totalDurationSec),
                    onValueChange = { pos ->
                        playerPositionSec = pos
                        exoPlayerInstance?.seekTo((pos * 1000).toLong())
                        viewModel.updatePlayerPosition(pos)
                    },
                    valueRange = 0f..totalDurationSec,
                    colors = SliderDefaults.colors(
                        thumbColor = AccentCyan,
                        activeTrackColor = AccentCyan,
                        inactiveTrackColor = Color(0xFF1E293B)
                    ),
                    modifier = Modifier.fillMaxWidth().height(24.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Playback Position Bar & Transport Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Playhead timestamp
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = BgDark,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, BorderSubtle)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = formatSec(playerPositionSec),
                                    color = AccentCyan,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = " / ${formatSec(totalDurationSec)}",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = if (isPlaybackPaused) Color(0xFFF59E0B).copy(alpha = 0.15f) else SuccessGreen.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, if (isPlaybackPaused) Color(0xFFF59E0B).copy(alpha = 0.35f) else SuccessGreen.copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = if (isPlaybackPaused) "PAUSED" else "PLAYING",
                                color = if (isPlaybackPaused) Color(0xFFF59E0B) else SuccessGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Transport Actions: -5s / Play-Pause / +5s
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalIconButton(
                            onClick = {
                                val target = (playerPositionSec - 5f).coerceAtLeast(0f)
                                exoPlayerInstance?.seekTo((target * 1000).toLong())
                            },
                            modifier = Modifier.size(36.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("-5s", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }

                        FilledIconButton(
                            onClick = {
                                if (isPlaybackPaused) {
                                    exoPlayerInstance?.play()
                                    isPlaybackPaused = false
                                } else {
                                    exoPlayerInstance?.pause()
                                    isPlaybackPaused = true
                                }
                            },
                            modifier = Modifier.size(42.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = if (isPlaybackPaused) PrimaryPurple else AccentCyan
                            ),
                            shape = CircleShape
                        ) {
                            Icon(
                                imageVector = if (isPlaybackPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = if (isPlaybackPaused) "Play" else "Pause",
                                tint = if (isPlaybackPaused) Color.White else Color.Black,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        FilledTonalIconButton(
                            onClick = {
                                val target = (playerPositionSec + 5f).coerceAtMost(totalDurationSec)
                                exoPlayerInstance?.seekTo((target * 1000).toLong())
                            },
                            modifier = Modifier.size(36.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("+5s", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ----------------------------------------------------
                // Quick Duration Presets (15s, 30s, 45s, 60s)
                // ----------------------------------------------------
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Quick Duration Presets:", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                    Surface(
                        color = PrimaryPurple.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "✂️ ${String.format("%.1f", durationSec)}s Selected",
                            fontSize = 11.sp,
                            color = PrimaryPurple,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        15f to "🎵 15s (TikTok)",
                        30f to "🔥 30s (Viral)",
                        45f to "⏱️ 45s (Reels)",
                        60f to "🧠 60s (Deep)"
                    ).forEach { (dur, label) ->
                        val isPresetActive = kotlin.math.abs(durationSec - dur) < 1.0f
                        FilterChip(
                            selected = isPresetActive,
                            onClick = {
                                val newEnd = (startSec + dur).coerceAtMost(totalDurationSec)
                                viewModel.updateTrimRange(startSec, newEnd)
                            },
                            label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryPurple.copy(alpha = 0.35f),
                                selectedLabelColor = Color.White
                            ),
                            border = BorderStroke(1.dp, if (isPresetActive) PrimaryPurple else BorderSubtle)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // DUAL START & END TRIM CARDS (Tapping Pauses Video Immediately)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Start Trim Card
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1220)),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🟢 Start Time", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    FilledTonalIconButton(
                                        onClick = {
                                            val t = (startSec - 1f).coerceAtLeast(0f)
                                            viewModel.updateTrimRange(t, endSec)
                                            exoPlayerInstance?.seekTo((t * 1000).toLong())
                                        },
                                        modifier = Modifier.size(24.dp),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text("-1s", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                    FilledTonalIconButton(
                                        onClick = {
                                            val t = (startSec + 1f).coerceAtMost(endSec - 1f)
                                            viewModel.updateTrimRange(t, endSec)
                                            exoPlayerInstance?.seekTo((t * 1000).toLong())
                                        },
                                        modifier = Modifier.size(24.dp),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text("+1s", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formatSec(startSec),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = SuccessGreen
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = {
                                    exoPlayerInstance?.pause()
                                    isPlaybackPaused = true
                                    val currentPos = playerPositionSec
                                    viewModel.setStartToCurrentPlayback(currentPos)
                                    exoPlayerInstance?.seekTo((currentPos * 1000).toLong())
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(34.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                            ) {
                                Icon(Icons.Default.PauseCircle, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Black)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Set Start & Pause", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }
                    }

                    // End Trim Card
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1220)),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🔴 End Time", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    FilledTonalIconButton(
                                        onClick = {
                                            val t = (endSec - 1f).coerceAtLeast(startSec + 1f)
                                            viewModel.updateTrimRange(startSec, t)
                                            exoPlayerInstance?.seekTo((t * 1000).toLong())
                                        },
                                        modifier = Modifier.size(24.dp),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text("-1s", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                    FilledTonalIconButton(
                                        onClick = {
                                            val t = (endSec + 1f).coerceAtMost(totalDurationSec)
                                            viewModel.updateTrimRange(startSec, t)
                                            exoPlayerInstance?.seekTo((t * 1000).toLong())
                                        },
                                        modifier = Modifier.size(24.dp),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text("+1s", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formatSec(endSec),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = AccentCyan
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = {
                                    exoPlayerInstance?.pause()
                                    isPlaybackPaused = true
                                    val currentPos = playerPositionSec
                                    viewModel.setEndToCurrentPlayback(currentPos)
                                    exoPlayerInstance?.seekTo((currentPos * 1000).toLong())
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(34.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                            ) {
                                Icon(Icons.Default.PauseCircle, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Black)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Set End & Pause", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Preview Range Button (Start -> End with auto-pause at End)
                Button(
                    onClick = {
                        exoPlayerInstance?.seekTo((startSec * 1000).toLong())
                        exoPlayerInstance?.play()
                        isPlaybackPaused = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryPurple.copy(alpha = 0.15f)
                    ),
                    border = BorderStroke(1.dp, PrimaryPurple.copy(alpha = 0.7f))
                ) {
                    Icon(Icons.Default.FastForward, contentDescription = null, modifier = Modifier.size(18.dp), tint = PrimaryPurple)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "▶ Preview Short: ${formatSec(startSec)} ➔ ${formatSec(endSec)} (${String.format("%.1f", durationSec)}s)",
                        fontSize = 13.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // ----------------------------------------------------
        // 2. CROP PLACEMENT ("PLACE" SELECTOR & 16:9 VISUAL PREVIEW)
        // ----------------------------------------------------
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Header with placement title & badge (With weight to prevent 2-line wrapping)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Crop Place & Framing",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Position of 9:16 vertical short across 16:9 frame",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        color = PrimaryPurple.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, PrimaryPurple.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = when (currentPlacement) {
                                CropPlacement.AUTO_FACE -> "🤖 Auto Face-Track"
                                CropPlacement.LEFT -> "👈 Left Focus (16%)"
                                CropPlacement.CENTER -> "🎯 Center (50%)"
                                CropPlacement.RIGHT -> "👉 Right Focus (84%)"
                                CropPlacement.CUSTOM -> "🎚️ Custom (${(currentOffset * 100).toInt()}%)"
                            },
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Interactive 16:9 Widescreen Mockup with sliding 9:16 Crop Window
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF050811))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                ) {
                    val fullWidth = maxWidth
                    // 9:16 short takes ~31.64% width of a 16:9 widescreen frame
                    val cropBoxWidth = fullWidth * 0.3164f
                    val maxTravel = fullWidth - cropBoxWidth

                    val resolvedFraction = when (currentPlacement) {
                        CropPlacement.AUTO_FACE -> 0.50f
                        CropPlacement.LEFT -> 0.0f
                        CropPlacement.CENTER -> 0.50f
                        CropPlacement.RIGHT -> 1.0f
                        CropPlacement.CUSTOM -> currentOffset.coerceIn(0.0f, 1.0f)
                    }

                    val animatedFraction by animateFloatAsState(
                        targetValue = resolvedFraction,
                        animationSpec = tween(durationMillis = 250),
                        label = "cropWindowAnimation"
                    )

                    val boxOffsetX = maxTravel * animatedFraction

                    // Rule-of-Thirds Grid Guides
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(BorderSubtle.copy(alpha = 0.35f)))
                        Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(BorderSubtle.copy(alpha = 0.35f)))
                    }

                    // Zone Labels (Left, Center, Right)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Text("Left Speaker", fontSize = 10.sp, color = TextSecondary.copy(alpha = 0.6f))
                        Text("Solo Center", fontSize = 10.sp, color = TextSecondary.copy(alpha = 0.6f))
                        Text("Right Speaker", fontSize = 10.sp, color = TextSecondary.copy(alpha = 0.6f))
                    }

                    // Sliding 9:16 Framing Window
                    Box(
                        modifier = Modifier
                            .offset(x = boxOffsetX)
                            .width(cropBoxWidth)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        PrimaryPurple.copy(alpha = 0.35f),
                                        AccentCyan.copy(alpha = 0.20f)
                                    )
                                )
                            )
                            .border(2.dp, PrimaryPurple, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = if (currentPlacement == CropPlacement.AUTO_FACE) Icons.Default.Face else Icons.Default.CropPortrait,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "9:16 CROP",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.sp
                            )
                        }
                    }

                    if (currentPlacement == CropPlacement.AUTO_FACE) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(6.dp)
                                .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                                .border(1.dp, AccentCyan.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text("🤖 AI Active Face Tracking", color = AccentCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Placement Selection Chips (Horizontally Scrollable - Never clipped)
                Text("Select Framing Preset:", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = currentPlacement == CropPlacement.AUTO_FACE,
                        onClick = { viewModel.setCropPlacement(CropPlacement.AUTO_FACE) },
                        label = { Text("🤖 Auto Face-Track", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryPurple.copy(alpha = 0.35f),
                            selectedLabelColor = Color.White
                        ),
                        border = BorderStroke(1.dp, if (currentPlacement == CropPlacement.AUTO_FACE) PrimaryPurple else BorderSubtle)
                    )
                    FilterChip(
                        selected = currentPlacement == CropPlacement.LEFT,
                        onClick = { viewModel.setCropPlacement(CropPlacement.LEFT) },
                        label = { Text("👈 Left Focus (16%)", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryPurple.copy(alpha = 0.35f),
                            selectedLabelColor = Color.White
                        ),
                        border = BorderStroke(1.dp, if (currentPlacement == CropPlacement.LEFT) PrimaryPurple else BorderSubtle)
                    )
                    FilterChip(
                        selected = currentPlacement == CropPlacement.CENTER,
                        onClick = { viewModel.setCropPlacement(CropPlacement.CENTER) },
                        label = { Text("🎯 Center (50%)", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryPurple.copy(alpha = 0.35f),
                            selectedLabelColor = Color.White
                        ),
                        border = BorderStroke(1.dp, if (currentPlacement == CropPlacement.CENTER) PrimaryPurple else BorderSubtle)
                    )
                    FilterChip(
                        selected = currentPlacement == CropPlacement.RIGHT,
                        onClick = { viewModel.setCropPlacement(CropPlacement.RIGHT) },
                        label = { Text("👉 Right Focus (84%)", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryPurple.copy(alpha = 0.35f),
                            selectedLabelColor = Color.White
                        ),
                        border = BorderStroke(1.dp, if (currentPlacement == CropPlacement.RIGHT) PrimaryPurple else BorderSubtle)
                    )
                    FilterChip(
                        selected = currentPlacement == CropPlacement.CUSTOM,
                        onClick = { viewModel.setCropPlacement(CropPlacement.CUSTOM) },
                        label = { Text("🎚️ Fine Slider", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryPurple.copy(alpha = 0.35f),
                            selectedLabelColor = Color.White
                        ),
                        border = BorderStroke(1.dp, if (currentPlacement == CropPlacement.CUSTOM) PrimaryPurple else BorderSubtle)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Custom Slider option
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Continuous Horizontal Placement:", color = TextSecondary, fontSize = 11.sp)
                    Text("${(currentOffset * 100).toInt()}%", color = AccentCyan, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                }
                Slider(
                    value = currentOffset,
                    onValueChange = { viewModel.setCropHorizontalOffset(it) },
                    valueRange = 0.0f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = AccentCyan,
                        activeTrackColor = AccentCyan,
                        inactiveTrackColor = Color(0xFF1E293B)
                    )
                )
            }
        }

        // ----------------------------------------------------
        // 3. CLIP CAPTION OVERLAY & VIRAL HOOKS
        // ----------------------------------------------------
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Viral Hook Caption Overlay",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 15.sp
                )
                Text(
                    text = "High-retention caption rendered on top of the vertical short",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = state.hookText,
                    onValueChange = { viewModel.onHookTextChanged(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. The 1 Habit That Changed Everything...", color = TextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = PrimaryPurple,
                        unfocusedBorderColor = BorderSubtle
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Inspiration Hook Chips
                Text("Tap for instant viral inspiration:", color = TextSecondary, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "WAIT TILL THE END... 🤯",
                        "The 1 Secret Nobody Tells You 🤫",
                        "Watch what happens next! 😱",
                        "Top 3 Things You Need To Know 👇"
                    ).forEach { suggestion ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = BgDark,
                            border = BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier.clickable {
                                viewModel.onHookTextChanged(suggestion)
                            }
                        ) {
                            Text(
                                text = suggestion,
                                fontSize = 11.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // ----------------------------------------------------
        // 4. HARDWARE RENDER CTA BUTTON
        // ----------------------------------------------------
        Button(
            onClick = { viewModel.startRenderShort(context.cacheDir) },
            enabled = !state.isRendering,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues(0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                PrimaryPurple,
                                AccentIndigo,
                                AccentCyan
                            )
                        ),
                        RoundedCornerShape(18.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (state.isRendering) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Hardware Encoding 9:16 Short...",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 15.sp
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Movie, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Export 9:16 Short (${String.format("%.0f", durationSec)}s • ${state.selectedQuality.shortLabel})",
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

private fun formatSec(sec: Float): String {
    val total = sec.toInt()
    val m = total / 60
    val s = total % 60
    return String.format("%02d:%02d", m, s)
}
'''

path = "android/app/src/main/java/com/mrpallanti/viralvideo/presentation/ui/components/CustomClipperView.kt"
with open(path, "w", encoding="utf-8") as f:
    f.write(new_code)
print("Updated CustomClipperView.kt successfully!")
