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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                    text = "Load a YouTube link in the Studio tab or pick a video from your gallery to play and manually scrub custom short ranges.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
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

    val playableUri = video.localUri ?: video.streamUrl

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. In-App Video Player or Download Banner
        if (!playableUri.isNullOrBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardDark),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    InAppVideoPlayer(
                        uriString = playableUri,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        onPositionUpdate = { pos ->
                            playerPositionSec = pos
                            viewModel.updatePlayerPosition(pos)
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Playback: ${formatSec(playerPositionSec)} / ${formatSec(totalDurationSec)}",
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
        } else {
            // Need source file
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardDark),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryPurple.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Stream Playback Needs Local Source", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                        Text("Pick video from gallery for frame-accurate scrubbing", color = TextSecondary, fontSize = 12.sp)
                    }
                    Button(
                        onClick = onPickFromGallery,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Pick", fontSize = 12.sp)
                    }
                }
            }
        }

        // 2. Interactive Scrubber & Duration Control Card
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
                    Text(
                        text = "Custom Range Selector",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 15.sp
                    )
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

        // 3. Custom Hook Text for this Clip
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

        // 4. Render CTA Button
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
