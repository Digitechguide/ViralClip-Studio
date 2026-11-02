package com.mrpallanti.viralvideo.presentation.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.mrpallanti.viralvideo.domain.model.ViralClip
import com.mrpallanti.viralvideo.presentation.ui.theme.*
import com.mrpallanti.viralvideo.presentation.viewmodel.StudioViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioScreen(
    viewModel: StudioViewModel
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Video Picker Launcher for Local Gallery Videos
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, uri)
                val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                val durationSec = (durStr?.toLongOrNull() ?: 30000L) / 1000L
                retriever.release()

                val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "Selected Video"
                viewModel.onLocalVideoSelected(
                    uriString = uri.toString(),
                    title = fileName,
                    durationSeconds = durationSec
                )
            } catch (e: Exception) {
                viewModel.onLocalVideoSelected(
                    uriString = uri.toString(),
                    title = "Gallery Video",
                    durationSeconds = 60
                )
            }
        }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(state.successMessage) {
        state.successMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
    }

    // Source Video Required Dialog (When YouTube protected stream needs local video selection)
    if (state.showVideoPickerPrompt) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissVideoPickerPrompt() },
            icon = {
                Icon(
                    Icons.Default.VideoLibrary,
                    contentDescription = null,
                    tint = PrimaryPurple,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    "Select Source Video",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Column {
                    Text(
                        "YouTube stream protection prevented direct in-app downloading for this clip.",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Tap below to select the downloaded video or screen recording from your phone's Gallery to render the 9:16 short directly!",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.dismissVideoPickerPrompt()
                        videoPickerLauncher.launch("video/*")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Select from Gallery")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        val blueprint = viewModel.getViralBlueprintText()
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Viral Blueprint", blueprint))
                        Toast.makeText(context, "📋 Copied Viral Blueprint! Ready for CapCut.", Toast.LENGTH_SHORT).show()
                        viewModel.dismissVideoPickerPrompt()
                    }
                ) {
                    Text("Copy Blueprint", color = TextSecondary)
                }
            },
            containerColor = CardDark
        )
    }

    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Sign Out", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = { Text("Are you sure you want to sign out of ViralClip Studio?", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Sign Out", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CardDark
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Logo",
                            tint = PrimaryPurple,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "ViralClip Studio",
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    fontSize = 17.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = PrimaryPurple.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "v${com.mrpallanti.viralvideo.BuildConfig.APP_VERSION_NAME}",
                                        color = PrimaryPurple,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            state.currentUser?.let { user ->
                                Text(
                                    text = user.email,
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Sign Out",
                            tint = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BgDark
                )
            )
        },
        containerColor = BgDark
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. YouTube Link & Gallery Picker Input Section
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.border(1.dp, BorderDark, RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Source Video Input",
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                fontSize = 15.sp
                            )

                            // Pick from gallery pill button
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = PrimaryPurple.copy(alpha = 0.15f),
                                modifier = Modifier.clickable {
                                    videoPickerLauncher.launch("video/*")
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.VideoFile,
                                        contentDescription = null,
                                        tint = PrimaryPurple,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Pick Local Video",
                                        color = PrimaryPurple,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = state.urlInput,
                            onValueChange = { viewModel.onUrlChanged(it) },
                            placeholder = { Text("Paste YouTube or Shorts URL...", color = TextSecondary) },
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                                    if (clip.isNotBlank()) {
                                        viewModel.onUrlChanged(clip)
                                    }
                                }) {
                                    Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = PrimaryPurple)
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryPurple,
                                unfocusedBorderColor = BorderDark,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { viewModel.fetchAndAnalyze() },
                            enabled = !state.isLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (state.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(state.loadingMessage.ifBlank { "Processing..." })
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Fetch & Analyze with Gemini AI")
                            }
                        }
                    }
                }
            }

            // 2. 9:16 Mobile Mockup Frame & Live Hook Preview
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .width(220.dp)
                            .height(390.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(Color.Black)
                            .border(3.dp, BorderDark, RoundedCornerShape(28.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (state.currentVideo != null) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(CardDark)
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.PlayCircle,
                                    contentDescription = null,
                                    tint = PrimaryPurple,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = state.currentVideo?.title ?: "",
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                    fontSize = 12.sp,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    color = if (state.currentVideo?.localUri != null) SuccessGreen.copy(alpha = 0.2f) else PrimaryPurple.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if (state.currentVideo?.localUri != null) "📱 Local File Ready" else "🌐 YouTube Hook Ready",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (state.currentVideo?.localUri != null) SuccessGreen else PrimaryPurple,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "9:16 Shorts Preview",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }

                        // Live Hook Banner Overlay
                        if (state.hookText.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 40.dp, start = 12.dp, end = 12.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.75f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = state.hookText,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        // Duration badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp)
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${state.trimRange.durationSeconds.toInt()}s Short",
                                color = Color.White,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            // 3. Trimmer, Hook Customizer & 1-Click Hardware Render
            if (state.currentVideo != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardDark),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.border(1.dp, BorderDark, RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Viral Hook Caption Overlay",
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = state.hookText,
                                onValueChange = { viewModel.onHookTextChanged(it) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryPurple,
                                    unfocusedBorderColor = BorderDark,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // 1-Click Hardware Render Button
                            Button(
                                onClick = { viewModel.startRenderShort(context.cacheDir) },
                                enabled = !state.isRendering,
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.horizontalGradient(listOf(PrimaryPurple, AccentIndigo)),
                                        RoundedCornerShape(12.dp)
                                    )
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (state.currentVideo?.localUri != null) "⚡ Render & Save 9:16 to Gallery" else "⚡ 1-Click Render Video to Gallery",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            // If only YouTube link is loaded, offer 1-tap gallery video picker
                            if (state.currentVideo?.localUri == null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { videoPickerLauncher.launch("video/*") },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                    border = ButtonDefaults.outlinedButtonBorder.copy(
                                        brush = Brush.horizontalGradient(listOf(BorderDark, PrimaryPurple))
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.FolderOpen, contentDescription = null, tint = PrimaryPurple)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("📁 Select Video File from Gallery")
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // 1-Tap Copy Blueprint Button
                            OutlinedButton(
                                onClick = {
                                    val blueprint = viewModel.getViralBlueprintText()
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Viral Blueprint", blueprint))
                                    Toast.makeText(context, "📋 Copied Viral Blueprint! Ready to paste into CapCut or Shorts.", Toast.LENGTH_LONG).show()
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = PrimaryPurple)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("📋 Copy Viral Blueprint & Hook Script")
                            }

                            if (state.isRendering) {
                                Spacer(modifier = Modifier.height(12.dp))
                                val progressVal = if (state.renderJob != null) {
                                    (state.renderJob.progressPercent) / 100f
                                } else {
                                    0.5f // Indeterminate or downloading pulse
                                }
                                LinearProgressIndicator(
                                    progress = progressVal,
                                    modifier = Modifier.fillMaxWidth(),
                                    color = SuccessGreen
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                val statusText = if (state.loadingMessage.isNotBlank()) {
                                    state.loadingMessage
                                } else {
                                    "Processing video with Android Hardware Acceleration: ${state.renderJob?.progressPercent ?: 0}%"
                                }
                                Text(
                                    text = statusText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = SuccessGreen,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }

            // 4. Suggested AI Viral Clips
            if (state.viralClips.isNotEmpty()) {
                item {
                    Text(
                        text = "🔥 AI Viral Clip Recommendations (${state.viralClips.size})",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 16.sp
                    )
                }

                items(state.viralClips) { clip ->
                    ViralClipCard(
                        clip = clip,
                        isSelected = state.selectedClip?.id == clip.id,
                        onApply = { viewModel.applyClip(clip) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun ViralClipCard(
    clip: ViralClip,
    isSelected: Boolean,
    onApply: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) CardDark.copy(alpha = 0.9f) else CardDark
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.5.dp,
                if (isSelected) PrimaryPurple else BorderDark,
                RoundedCornerShape(14.dp)
            )
            .clickable { onApply() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Virality Pill
                Surface(
                    color = PrimaryPurple.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = "🔥 ${clip.viralityScore}% Virality",
                        color = PrimaryPurple,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // Duration Pill
                Surface(
                    color = Color.Black.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = "${clip.startSeconds.toInt()}s - ${clip.endSeconds.toInt()}s (${(clip.endSeconds - clip.startSeconds).toInt()}s)",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = clip.title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = clip.hookReason,
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Caption Hook Box
            Surface(
                color = BgDark,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Hook:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = PrimaryPurple
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = clip.captionOverlay,
                        fontSize = 11.sp,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
