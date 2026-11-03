package com.mrpallanti.viralvideo.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                    Text(
                        text = if (hasLocalFile) "📱 Local Video Player" else "▶️ In-App YouTube Player",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
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
                    // Local Gallery / Pre-downloaded video player
                    InAppVideoPlayer(
                        uriString = video.localUri!!,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp),
                        onPositionUpdate = { pos ->
                            playerPositionSec = pos
                            viewModel.updatePlayerPosition(pos)
                        }
                    )
                } else if (isYouTubeVideo) {
                    // Embedded YouTube WebView player with live JS bridge
                    InAppYouTubePlayer(
                        videoId = video.id,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp),
                        onPositionUpdate = { pos ->
                            playerPositionSec = pos
                            viewModel.updatePlayerPosition(pos)
                        }
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

                Spacer(modifier = Modifier.height(10.dp))

                // Playback Position Bar & Set Start/End Quick Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Playhead: ${formatSec(playerPositionSec)} / ${formatSec(totalDurationSec)}",
                        color = AccentCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilledTonalButton(
                            onClick = { viewModel.setStartToCurrentPlayback(playerPositionSec) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Set Start [${formatSec(playerPositionSec)}]", fontSize = 11.sp)
                        }
                        FilledTonalButton(
                            onClick = { viewModel.setEndToCurrentPlayback(playerPositionSec) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Set End [${formatSec(playerPositionSec)}]", fontSize = 11.sp)
                        }
                    }
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Crop Place & Framing",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Position of the 9:16 vertical Short in 16:9 video",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Surface(
                        color = PrimaryPurple.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = when (currentPlacement) {
                                CropPlacement.AUTO_FACE -> "🤖 Auto Face"
                                CropPlacement.LEFT -> "👈 Left (16%)"
                                CropPlacement.CENTER -> "🎯 Center (50%)"
                                CropPlacement.RIGHT -> "👉 Right (84%)"
                                CropPlacement.CUSTOM -> "🎚️ Custom (${(currentOffset * 100).toInt()}%)"
                            },
                            color = PrimaryPurple,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

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

                // Placement Selection Chips
                Text("Select Framing Place:", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = currentPlacement == CropPlacement.AUTO_FACE,
                        onClick = { viewModel.setCropPlacement(CropPlacement.AUTO_FACE) },
                        label = { Text("🤖 Auto Face", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryPurple.copy(alpha = 0.3f),
                            selectedLabelColor = TextPrimary
                        )
                    )
                    FilterChip(
                        selected = currentPlacement == CropPlacement.LEFT,
                        onClick = { viewModel.setCropPlacement(CropPlacement.LEFT) },
                        label = { Text("👈 Left", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryPurple.copy(alpha = 0.3f),
                            selectedLabelColor = TextPrimary
                        )
                    )
                    FilterChip(
                        selected = currentPlacement == CropPlacement.CENTER,
                        onClick = { viewModel.setCropPlacement(CropPlacement.CENTER) },
                        label = { Text("🎯 Center", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryPurple.copy(alpha = 0.3f),
                            selectedLabelColor = TextPrimary
                        )
                    )
                    FilterChip(
                        selected = currentPlacement == CropPlacement.RIGHT,
                        onClick = { viewModel.setCropPlacement(CropPlacement.RIGHT) },
                        label = { Text("👉 Right", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryPurple.copy(alpha = 0.3f),
                            selectedLabelColor = TextPrimary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

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
                            onClick = { viewModel.updateTrimRange(startSec - 1f, endSec) },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("-1s", fontSize = 11.sp, color = TextPrimary)
                        }
                        OutlinedButton(
                            onClick = { viewModel.updateTrimRange(startSec + 1f, endSec) },
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
                    onValueChange = { newStart -> viewModel.updateTrimRange(newStart, endSec) },
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
                            onClick = { viewModel.updateTrimRange(startSec, endSec - 1f) },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("-1s", fontSize = 11.sp, color = TextPrimary)
                        }
                        OutlinedButton(
                            onClick = { viewModel.updateTrimRange(startSec, endSec + 1f) },
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
                    onValueChange = { newEnd -> viewModel.updateTrimRange(startSec, newEnd) },
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
                    modifier = Modifier.fillMaxWidth(),
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
