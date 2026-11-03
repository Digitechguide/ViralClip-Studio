package com.mrpallanti.viralvideo.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.exoplayer.ExoPlayer
import com.mrpallanti.viralvideo.domain.model.CropPlacement
import com.mrpallanti.viralvideo.presentation.ui.theme.*
import com.mrpallanti.viralvideo.presentation.viewmodel.StudioUiState
import com.mrpallanti.viralvideo.presentation.viewmodel.StudioViewModel

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
    var ytControllerInstance by remember { mutableStateOf<YouTubePlayerController?>(null) }
    var isPlaybackPaused by remember { mutableStateOf(false) }

    if (video == null) {
        // Empty State: No video loaded yet
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCut,
                    contentDescription = null,
                    tint = PrimaryPurple,
                    modifier = Modifier.size(54.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Video Loaded",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Enter a YouTube link or pick a video from your gallery to play, scrub custom length, and choose your 9:16 crop framing place.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onPickFromGallery,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select from Gallery")
                }
            }
        }
        return
    }

    val totalDurationSec = video.durationSeconds.toFloat().coerceAtLeast(10f)
    val startSec = state.trimRange.startSeconds.coerceIn(0f, totalDurationSec - 1f)
    val endSec = state.trimRange.endSeconds.coerceIn(startSec + 1f, totalDurationSec)
    val durationSec = endSec - startSec

    val isYouTubeVideo = !video.id.startsWith("local_") && video.id.isNotBlank()
    val hasLocalFile = !video.localUri.isNullOrBlank()
    val playableUri = video.localUri ?: video.streamUrl

    val currentPlacement = state.overlayConfig.cropPlacement
    val currentOffset = state.overlayConfig.cropHorizontalOffset

    // Auto-stop preview when playhead reaches endSec
    LaunchedEffect(playerPositionSec, endSec) {
        if (!isPlaybackPaused && playerPositionSec >= endSec && endSec > startSec) {
            exoPlayerInstance?.pause()
            ytControllerInstance?.pause()
            isPlaybackPaused = true
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ----------------------------------------------------
        // 1. IN-APP VIDEO PLAYER (Dual Engine: Local ExoPlayer / In-App YouTube)
        // ----------------------------------------------------
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (hasLocalFile) Icons.Default.PhoneAndroid else Icons.Default.PlayCircle,
                            contentDescription = null,
                            tint = if (hasLocalFile) SuccessGreen else PrimaryPurple,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (hasLocalFile) "Offline Local Player" else "In-App Stream Player",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Surface(
                        color = if (hasLocalFile) SuccessGreen.copy(alpha = 0.15f) else PrimaryPurple.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (hasLocalFile) "Offline Ready" else "Live Stream",
                            color = if (hasLocalFile) SuccessGreen else PrimaryPurple,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (hasLocalFile) {
                    // Local Gallery / Pre-downloaded video player (ExoPlayer)
                    InAppVideoPlayer(
                        uriString = video.localUri!!,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp),
                        onPositionUpdate = { pos ->
                            playerPositionSec = pos
                            viewModel.updatePlayerPosition(pos)
                        },
                        onPlayerReady = { player ->
                            exoPlayerInstance = player
                        }
                    )
                } else if (isYouTubeVideo) {
                    // Embedded YouTube WebView player with live JS bridge & error recovery
                    InAppYouTubePlayer(
                        videoId = video.id,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp),
                        onPositionUpdate = { pos ->
                            playerPositionSec = pos
                            viewModel.updatePlayerPosition(pos)
                        },
                        onPlayerReady = { controller ->
                            ytControllerInstance = controller
                        },
                        onCacheVideo = {
                            viewModel.cacheVideoForPlayback(context.cacheDir)
                        },
                        onPickFromGallery = onPickFromGallery
                    )
                } else if (!playableUri.isNullOrBlank()) {
                    InAppVideoPlayer(
                        uriString = playableUri,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp),
                        onPositionUpdate = { pos ->
                            playerPositionSec = pos
                            viewModel.updatePlayerPosition(pos)
                        },
                        onPlayerReady = { player ->
                            exoPlayerInstance = player
                        }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(Color.Black, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No playable stream available", color = TextSecondary, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Playback Position Row + Pause/Play toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Playhead: ${formatSec(playerPositionSec)} / ${formatSec(totalDurationSec)}",
                            color = AccentCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (isPlaybackPaused) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "PAUSED",
                                    color = Color(0xFFF59E0B),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilledTonalIconButton(
                            onClick = {
                                if (isPlaybackPaused) {
                                    exoPlayerInstance?.play()
                                    ytControllerInstance?.play()
                                    isPlaybackPaused = false
                                } else {
                                    exoPlayerInstance?.pause()
                                    ytControllerInstance?.pause()
                                    isPlaybackPaused = true
                                }
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlaybackPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = if (isPlaybackPaused) "Play" else "Pause",
                                tint = TextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        if (!hasLocalFile) {
                            Spacer(modifier = Modifier.width(6.dp))
                            FilledTonalButton(
                                onClick = { viewModel.cacheVideoForPlayback(context.cacheDir) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(containerColor = PrimaryPurple.copy(alpha = 0.25f))
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(12.dp), tint = PrimaryPurple)
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Cache", fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Set Start & Set End Big Action Buttons (PAUSES PLAYBACK IMMEDIATELY)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            // 1. Immediately pause playback so timer stops!
                            exoPlayerInstance?.pause()
                            ytControllerInstance?.pause()
                            isPlaybackPaused = true

                            // 2. Lock start timestamp and seek
                            val currentPos = playerPositionSec
                            viewModel.setStartToCurrentPlayback(currentPos)
                            exoPlayerInstance?.seekTo((currentPos * 1000).toLong())
                            ytControllerInstance?.seekTo(currentPos)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                    ) {
                        Icon(Icons.Default.PauseCircle, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Set Start & Pause", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Start: ${formatSec(startSec)}", fontSize = 10.sp, color = AccentCyan)
                        }
                    }

                    Button(
                        onClick = {
                            // 1. Immediately pause playback so timer stops!
                            exoPlayerInstance?.pause()
                            ytControllerInstance?.pause()
                            isPlaybackPaused = true

                            // 2. Lock end timestamp and seek
                            val currentPos = playerPositionSec
                            viewModel.setEndToCurrentPlayback(currentPos)
                            exoPlayerInstance?.seekTo((currentPos * 1000).toLong())
                            ytControllerInstance?.seekTo(currentPos)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
                    ) {
                        Icon(Icons.Default.PauseCircle, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                        Spacer(modifier = Modifier.width(4.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Set End & Pause", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            Text("End: ${formatSec(endSec)}", fontSize = 10.sp, color = Color.Black.copy(alpha = 0.8f))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Preview Range Button (Start -> End with auto-pause at End)
                OutlinedButton(
                    onClick = {
                        exoPlayerInstance?.seekTo((startSec * 1000).toLong())
                        exoPlayerInstance?.play()
                        ytControllerInstance?.seekTo(startSec)
                        ytControllerInstance?.play()
                        isPlaybackPaused = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryPurple.copy(alpha = 0.6f))
                ) {
                    Icon(Icons.Default.FastForward, contentDescription = null, modifier = Modifier.size(16.dp), tint = PrimaryPurple)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "▶ Preview Range (${formatSec(startSec)} ➔ ${formatSec(endSec)})",
                        fontSize = 12.sp,
                        color = TextPrimary,
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
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Header with placement title & badge (No wrap)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Crop Place & Framing",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 15.sp
                    )
                    Surface(
                        color = PrimaryPurple.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = when (currentPlacement) {
                                CropPlacement.AUTO_FACE -> "🤖 Auto Face"
                                CropPlacement.LEFT -> "👈 Left"
                                CropPlacement.CENTER -> "🎯 Center"
                                CropPlacement.RIGHT -> "👉 Right"
                                CropPlacement.CUSTOM -> "🎚️ Custom"
                            },
                            color = PrimaryPurple,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Position of the 9:16 vertical Short across 16:9 widescreen",
                    color = TextSecondary,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Interactive 16:9 Widescreen Mockup with sliding 9:16 Crop Window
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF070B14))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
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

                    val boxOffsetX = maxTravel * resolvedFraction

                    // Background 16:9 Guide lines
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(BorderSubtle.copy(alpha = 0.4f)))
                        Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(BorderSubtle.copy(alpha = 0.4f)))
                    }

                    // Sliding 9:16 Framing Window
                    Box(
                        modifier = Modifier
                            .offset(x = boxOffsetX)
                            .width(cropBoxWidth)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .background(PrimaryPurple.copy(alpha = 0.30f))
                            .border(2.dp, PrimaryPurple, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = if (currentPlacement == CropPlacement.AUTO_FACE) Icons.Default.Face else Icons.Default.CropPortrait,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "9:16 CROP",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }

                    if (currentPlacement == CropPlacement.AUTO_FACE) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(6.dp)
                                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("🤖 Auto Tracking Speaker Face", color = AccentCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Placement Selection Chips (Horizontally Scrollable - Never clipped)
                Text("Select Framing Place:", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
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
                        label = { Text("🤖 Auto Face", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryPurple.copy(alpha = 0.3f),
                            selectedLabelColor = TextPrimary
                        )
                    )
                    FilterChip(
                        selected = currentPlacement == CropPlacement.LEFT,
                        onClick = { viewModel.setCropPlacement(CropPlacement.LEFT) },
                        label = { Text("👈 Left (16%)", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryPurple.copy(alpha = 0.3f),
                            selectedLabelColor = TextPrimary
                        )
                    )
                    FilterChip(
                        selected = currentPlacement == CropPlacement.CENTER,
                        onClick = { viewModel.setCropPlacement(CropPlacement.CENTER) },
                        label = { Text("🎯 Center (50%)", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryPurple.copy(alpha = 0.3f),
                            selectedLabelColor = TextPrimary
                        )
                    )
                    FilterChip(
                        selected = currentPlacement == CropPlacement.RIGHT,
                        onClick = { viewModel.setCropPlacement(CropPlacement.RIGHT) },
                        label = { Text("👉 Right (84%)", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryPurple.copy(alpha = 0.3f),
                            selectedLabelColor = TextPrimary
                        )
                    )
                    FilterChip(
                        selected = currentPlacement == CropPlacement.CUSTOM,
                        onClick = { viewModel.setCropPlacement(CropPlacement.CUSTOM) },
                        label = { Text("🎚️ Custom Slider", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryPurple.copy(alpha = 0.3f),
                            selectedLabelColor = TextPrimary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Custom Slider option
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Fine Horizontal Placement Slider:", color = TextSecondary, fontSize = 11.sp)
                    Text("${(currentOffset * 100).toInt()}%", color = AccentCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
                Slider(
                    value = currentOffset,
                    onValueChange = { viewModel.setCropHorizontalOffset(it) },
                    valueRange = 0.0f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = PrimaryPurple,
                        activeTrackColor = PrimaryPurple
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("0% (Left Edge)", fontSize = 10.sp, color = TextSecondary)
                    Text("50% (Center)", fontSize = 10.sp, color = TextSecondary)
                    Text("100% (Right Edge)", fontSize = 10.sp, color = TextSecondary)
                }
            }
        }

        // ----------------------------------------------------
        // 3. CUSTOM LENGTH SELECTOR & SCRUBBER
        // ----------------------------------------------------
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Custom Length Selector",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Trim precise start & end timestamps",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Surface(
                        color = if (durationSec in 15f..60f) SuccessGreen.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "${String.format("%.1f", durationSec)}s Duration",
                            color = if (durationSec in 15f..60f) SuccessGreen else TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Start Time Scrubber
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Start: ${formatSec(startSec)}", color = TextSecondary, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedButton(
                            onClick = {
                                viewModel.updateTrimRange(startSec - 1f, endSec)
                                exoPlayerInstance?.seekTo(((startSec - 1f).coerceAtLeast(0f) * 1000).toLong())
                                ytControllerInstance?.seekTo((startSec - 1f).coerceAtLeast(0f))
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("-1s", fontSize = 11.sp, color = TextPrimary)
                        }
                        OutlinedButton(
                            onClick = {
                                viewModel.updateTrimRange(startSec + 1f, endSec)
                                exoPlayerInstance?.seekTo(((startSec + 1f) * 1000).toLong())
                                ytControllerInstance?.seekTo(startSec + 1f)
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("+1s", fontSize = 11.sp, color = TextPrimary)
                        }
                    }
                }
                Slider(
                    value = startSec,
                    onValueChange = { newStart ->
                        viewModel.updateTrimRange(newStart, endSec)
                        exoPlayerInstance?.seekTo((newStart * 1000).toLong())
                        ytControllerInstance?.seekTo(newStart)
                    },
                    valueRange = 0f..totalDurationSec,
                    colors = SliderDefaults.colors(
                        thumbColor = PrimaryPurple,
                        activeTrackColor = PrimaryPurple
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // End Time Scrubber
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("End: ${formatSec(endSec)}", color = TextSecondary, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedButton(
                            onClick = {
                                viewModel.updateTrimRange(startSec, endSec - 1f)
                                exoPlayerInstance?.seekTo(((endSec - 1f).coerceAtLeast(0f) * 1000).toLong())
                                ytControllerInstance?.seekTo((endSec - 1f).coerceAtLeast(0f))
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("-1s", fontSize = 11.sp, color = TextPrimary)
                        }
                        OutlinedButton(
                            onClick = {
                                viewModel.updateTrimRange(startSec, endSec + 1f)
                                exoPlayerInstance?.seekTo(((endSec + 1f) * 1000).toLong())
                                ytControllerInstance?.seekTo(endSec + 1f)
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("+1s", fontSize = 11.sp, color = TextPrimary)
                        }
                    }
                }
                Slider(
                    value = endSec,
                    onValueChange = { newEnd ->
                        viewModel.updateTrimRange(startSec, newEnd)
                        exoPlayerInstance?.seekTo((newEnd * 1000).toLong())
                        ytControllerInstance?.seekTo(newEnd)
                    },
                    valueRange = 0f..totalDurationSec,
                    colors = SliderDefaults.colors(
                        thumbColor = AccentCyan,
                        activeTrackColor = AccentCyan
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Quick Preset Duration Chips
                Text("Quick Duration Presets:", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(15f to "15s (TikTok)", 30f to "30s (Viral)", 45f to "45s", 60f to "60s (Deep)").forEach { (dur, label) ->
                        FilterChip(
                            selected = kotlin.math.abs(durationSec - dur) < 1.0f,
                            onClick = { viewModel.setPresetDuration(dur) },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryPurple.copy(alpha = 0.3f),
                                selectedLabelColor = TextPrimary
                            )
                        )
                    }
                }
            }
        }

        // ----------------------------------------------------
        // 4. CLIP CAPTION OVERLAY
        // ----------------------------------------------------
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Clip Caption Overlay",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 14.sp
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
            }
        }

        // ----------------------------------------------------
        // 5. RENDER CTA BUTTON
        // ----------------------------------------------------
        Button(
            onClick = { viewModel.startRenderShort(context.cacheDir) },
            enabled = !state.isRendering,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
        ) {
            if (state.isRendering) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("Rendering Custom Short...", fontWeight = FontWeight.Bold)
            } else {
                Icon(Icons.Default.Movie, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Render 9:16 Custom Short (${String.format("%.0f", durationSec)}s)", fontWeight = FontWeight.Bold)
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
