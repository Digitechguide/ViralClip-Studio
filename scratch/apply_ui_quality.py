import os

def update_custom_clipper():
    path = "android/app/src/main/java/com/mrpallanti/viralvideo/presentation/ui/components/CustomClipperView.kt"
    with open(path, "r", encoding="utf-8") as f:
        content = f.read()

    # 1. Add import
    target_import = "import com.mrpallanti.viralvideo.domain.model.CropPlacement\n"
    replace_import = "import com.mrpallanti.viralvideo.domain.model.CropPlacement\nimport com.mrpallanti.viralvideo.domain.model.VideoQuality\n"
    if "VideoQuality" not in content:
        assert target_import in content, "target_import not found"
        content = content.replace(target_import, replace_import, 1)

    # 2. Update status pill
    target_pill = """                    Surface(
                        color = if (hasLocalFile) SuccessGreen.copy(alpha = 0.15f) else PrimaryPurple.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (hasLocalFile) "🟢 1080p Offline" else "🌐 Live Stream",
                            color = if (hasLocalFile) SuccessGreen else PrimaryPurple,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }"""
    
    replace_pill = """                    Surface(
                        color = if (hasLocalFile) SuccessGreen.copy(alpha = 0.15f) else PrimaryPurple.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (hasLocalFile) SuccessGreen.copy(alpha = 0.4f) else PrimaryPurple.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = if (hasLocalFile) "🟢 ${state.selectedQuality.shortLabel} Ready" else "📥 Caching ${state.selectedQuality.shortLabel}...",
                            color = if (hasLocalFile) SuccessGreen else PrimaryPurple,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }"""
    assert target_pill in content, "target_pill not found"
    content = content.replace(target_pill, replace_pill, 1)

    # 3. Add Quality Selector right under Video Display Surface (after line 335)
    target_box_end = """                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No playable stream available", color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Playback Position Bar & Transport Controls"""

    replace_box_end = """                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No playable stream available", color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                }

                // ----------------------------------------------------
                // Output Resolution & Cache Quality Selector Chips
                // ----------------------------------------------------
                Spacer(modifier = Modifier.height(10.dp))
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
                        text = "${state.selectedQuality.width}x${state.selectedQuality.height}",
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
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = if (isSelected) AccentCyan else BorderSubtle,
                                selectedBorderColor = AccentCyan,
                                borderWidth = if (isSelected) 1.5.dp else 1.dp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Playback Position Bar & Transport Controls"""
    assert target_box_end in content, "target_box_end not found"
    content = content.replace(target_box_end, replace_box_end, 1)

    # 4. Update Export button
    target_export = """                        Text(
                            text = "Export 9:16 Custom Short (${String.format("%.0f", durationSec)}s)",
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            fontSize = 15.sp
                        )"""

    replace_export = """                        Text(
                            text = "Export 9:16 Short (${String.format("%.0f", durationSec)}s • ${state.selectedQuality.shortLabel})",
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            fontSize = 15.sp
                        )"""
    assert target_export in content, "target_export not found"
    content = content.replace(target_export, replace_export, 1)

    with open(path, "w", encoding="utf-8") as f:
        f.write(content)
    print("CustomClipperView.kt updated successfully.")

def update_studio_screen():
    path = "android/app/src/main/java/com/mrpallanti/viralvideo/presentation/ui/StudioScreen.kt"
    with open(path, "r", encoding="utf-8") as f:
        content = f.read()

    # 1. Add import
    target_import = "import com.mrpallanti.viralvideo.domain.model.ViralClip\n"
    replace_import = "import com.mrpallanti.viralvideo.domain.model.VideoQuality\nimport com.mrpallanti.viralvideo.domain.model.ViralClip\n"
    if "VideoQuality" not in content:
        assert target_import in content, "target_import not found in StudioScreen"
        content = content.replace(target_import, replace_import, 1)

    # 2. Add quality selection above Open Video Cropper Button
    target_btn = """                                Spacer(modifier = Modifier.height(14.dp))

                                // Open Video Cropper Action Button
                                Button("""

    replace_btn = """                                Spacer(modifier = Modifier.height(12.dp))

                                // Video Quality Preference Selector
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Target Render Quality:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = state.selectedQuality.label,
                                        fontSize = 11.sp,
                                        color = AccentCyan,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    VideoQuality.values().forEach { quality ->
                                        val isSelected = state.selectedQuality == quality
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { viewModel.setVideoQuality(quality, context.cacheDir) },
                                            label = {
                                                Text(
                                                    text = quality.shortLabel,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = PrimaryPurple.copy(alpha = 0.35f),
                                                selectedLabelColor = Color.White
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Open Video Cropper Action Button
                                Button("""

    assert target_btn in content, "target_btn not found in StudioScreen"
    content = content.replace(target_btn, replace_btn, 1)

    with open(path, "w", encoding="utf-8") as f:
        f.write(content)
    print("StudioScreen.kt updated successfully.")

if __name__ == "__main__":
    update_custom_clipper()
    update_studio_screen()
