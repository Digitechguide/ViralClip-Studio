package com.mrpallanti.viralvideo.presentation.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mrpallanti.viralvideo.domain.model.TextOverlayPosition
import com.mrpallanti.viralvideo.domain.model.WatermarkPosition
import com.mrpallanti.viralvideo.presentation.ui.theme.*
import com.mrpallanti.viralvideo.presentation.viewmodel.StudioUiState
import com.mrpallanti.viralvideo.presentation.viewmodel.StudioViewModel

@Composable
fun BrandingOverlayView(
    state: StudioUiState,
    viewModel: StudioViewModel
) {
    val context = LocalContext.current
    val overlayConfig = state.overlayConfig

    val logoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.setCustomLogoUri(uri.toString())
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Branding Header & Summary
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.BrandingWatermark,
                        contentDescription = null,
                        tint = PrimaryPurple,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Branding & Watermark Studio",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 16.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Add your channel logo, username watermark, and customizable caption overlays directly onto the exported 9:16 video.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }

        // 2. Custom Logo Image Picker Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Channel Logo (PNG / JPG)",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                if (overlayConfig.logoUri != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Image, contentDescription = null, tint = SuccessGreen)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Custom Logo Attached", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("Active on all rendered shorts", color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                        IconButton(onClick = { viewModel.setCustomLogoUri(null) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove Logo", tint = Color(0xFFEF4444))
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = { logoPickerLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pick Logo Image from Storage")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Text Watermark (Fallback or Channel Handle)
                Text(
                    text = "Or Text Watermark (Handle)",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = overlayConfig.watermarkText,
                    onValueChange = { viewModel.setWatermarkText(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("@your_channel_name", color = TextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = PrimaryPurple,
                        unfocusedBorderColor = BorderSubtle
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Watermark Position
                Text("Watermark Position:", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        WatermarkPosition.TOP_RIGHT to "Top Right",
                        WatermarkPosition.TOP_LEFT to "Top Left",
                        WatermarkPosition.BOTTOM_RIGHT to "Bottom Right",
                        WatermarkPosition.BOTTOM_LEFT to "Bottom Left"
                    ).forEach { (pos, label) ->
                        FilterChip(
                            selected = overlayConfig.watermarkPosition == pos,
                            onClick = { viewModel.setWatermarkPosition(pos) },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryPurple.copy(alpha = 0.3f),
                                selectedLabelColor = TextPrimary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Watermark Opacity Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Watermark Opacity", color = TextSecondary, fontSize = 12.sp)
                    Text("${(overlayConfig.watermarkOpacity * 100).toInt()}%", color = AccentCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = overlayConfig.watermarkOpacity,
                    onValueChange = { viewModel.setWatermarkOpacity(it) },
                    valueRange = 0.2f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = AccentCyan,
                        activeTrackColor = AccentCyan
                    )
                )
            }
        }

        // 3. Custom Text / Hook Caption Overlay Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Hook Text Overlay Position & Style",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                Text("Text Position on 9:16 Frame:", color = TextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        TextOverlayPosition.TOP to "Top (Header)",
                        TextOverlayPosition.CENTER to "Center",
                        TextOverlayPosition.BOTTOM to "Bottom (Sub)"
                    ).forEach { (pos, label) ->
                        FilterChip(
                            selected = overlayConfig.textPosition == pos,
                            onClick = { viewModel.setTextOverlayPosition(pos) },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryPurple.copy(alpha = 0.3f),
                                selectedLabelColor = TextPrimary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text("Caption Accent Color:", color = TextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(
                        "#FFFFFF" to "White",
                        "#FBBF24" to "Yellow Hook",
                        "#38BDF8" to "Cyan Neon",
                        "#A855F7" to "Purple"
                    ).forEach { (hex, name) ->
                        Surface(
                            onClick = { viewModel.setTextColorHex(hex) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (overlayConfig.textColorHex == hex) PrimaryPurple.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.05f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (overlayConfig.textColorHex == hex) PrimaryPurple else BorderSubtle
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(Color(android.graphics.Color.parseColor(hex)))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(name, fontSize = 11.sp, color = TextPrimary)
                            }
                        }
                    }
                }
            }
        }

        // 4. Live 9:16 Mockup Preview Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Live 9:16 Overlay Preview",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Mini 9:16 phone frame
                Box(
                    modifier = Modifier
                        .width(180.dp)
                        .height(320.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF030712))
                        .border(2.dp, PrimaryPurple.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(8.dp)
                ) {
                    // Video placeholder
                    Text(
                        text = "9:16 Video\n(Auto-Face Centered)",
                        color = Color.White.copy(alpha = 0.25f),
                        fontSize = 11.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center)
                    )

                    // Watermark Preview
                    val wmText = if (!overlayConfig.logoUri.isNullOrBlank()) "🖼️ LOGO" else overlayConfig.watermarkText.ifBlank { "@channel" }
                    val wmAlignment = when (overlayConfig.watermarkPosition) {
                        WatermarkPosition.TOP_LEFT -> Alignment.TopStart
                        WatermarkPosition.TOP_RIGHT -> Alignment.TopEnd
                        WatermarkPosition.BOTTOM_LEFT -> Alignment.BottomStart
                        WatermarkPosition.BOTTOM_RIGHT -> Alignment.BottomEnd
                    }
                    Surface(
                        color = Color.Black.copy(alpha = overlayConfig.watermarkOpacity * 0.7f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .align(wmAlignment)
                            .padding(4.dp)
                    ) {
                        Text(
                            text = wmText,
                            fontSize = 8.sp,
                            color = Color.White.copy(alpha = overlayConfig.watermarkOpacity),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }

                    // Caption Overlay Preview
                    val caption = overlayConfig.customText.ifBlank { state.hookText.ifBlank { "HOOK CAPTION PREVIEW" } }
                    val capAlignment = when (overlayConfig.textPosition) {
                        TextOverlayPosition.TOP -> Alignment.TopCenter
                        TextOverlayPosition.CENTER -> Alignment.Center
                        TextOverlayPosition.BOTTOM -> Alignment.BottomCenter
                    }
                    Surface(
                        color = Color.Black.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryPurple.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .align(capAlignment)
                            .padding(
                                top = if (overlayConfig.textPosition == TextOverlayPosition.TOP) 28.dp else 0.dp,
                                bottom = if (overlayConfig.textPosition == TextOverlayPosition.BOTTOM) 24.dp else 0.dp
                            )
                            .fillMaxWidth(0.92f)
                    ) {
                        Text(
                            text = caption,
                            color = try { Color(android.graphics.Color.parseColor(overlayConfig.textColorHex)) } catch (e: Exception) { Color.White },
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                            maxLines = 2
                        )
                    }
                }
            }
        }
    }
}
