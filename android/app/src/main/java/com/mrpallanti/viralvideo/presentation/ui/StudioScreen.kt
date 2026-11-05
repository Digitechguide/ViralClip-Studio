package com.mrpallanti.viralvideo.presentation.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.mrpallanti.viralvideo.domain.model.ViralClip
import com.mrpallanti.viralvideo.presentation.ui.components.BrandingOverlayView
import com.mrpallanti.viralvideo.presentation.ui.components.CustomClipperView
import com.mrpallanti.viralvideo.presentation.ui.components.RecentShortsLibraryView
import com.mrpallanti.viralvideo.presentation.ui.components.StudioBottomNavigationBar
import com.mrpallanti.viralvideo.presentation.ui.theme.*
import com.mrpallanti.viralvideo.presentation.viewmodel.StudioTab
import com.mrpallanti.viralvideo.presentation.viewmodel.StudioViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioScreen(
    viewModel: StudioViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Clipboard link detection
    val clipboard = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager }
    var detectedClipboardUrl by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        try {
            val clip = clipboard?.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
            if (clip.contains("youtube.com") || clip.contains("youtu.be")) {
                detectedClipboardUrl = clip.trim()
            }
        } catch (_: Exception) {}
    }

    // Local Video Picker
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, uri)
                val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                val durMs = durStr?.toLongOrNull() ?: 60000L
                val title = uri.lastPathSegment ?: "Local Video"
                retriever.release()

                viewModel.onLocalVideoSelected(
                    uriString = uri.toString(),
                    title = title,
                    durationSeconds = durMs / 1000
                )
            } catch (e: Exception) {
                viewModel.onLocalVideoSelected(
                    uriString = uri.toString(),
                    title = "Imported Video",
                    durationSeconds = 60
                )
            }
        }
    }

    // Toast Notifications
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
                        val clipMgr = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipMgr.setPrimaryClip(ClipData.newPlainText("Viral Blueprint", blueprint))
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
            title = { Text("Sign Out", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to sign out of ViralClip Studio?", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("Sign Out")
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
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(listOf(PrimaryPurple, AccentCyan))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = "Logo",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "ViralClip Studio",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextPrimary,
                                    fontSize = 17.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = PrimaryPurple.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, PrimaryPurple.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = "PRO",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "🟢 Hardware Engine: 60fps Ready",
                                color = SuccessGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
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
                    containerColor = Color(0xFF070B14)
                )
            )
        },
        bottomBar = {
            StudioBottomNavigationBar(
                currentTab = state.currentTab,
                onTabSelected = { tab ->
                    if (tab == StudioTab.CLIPPER && state.currentVideo != null && state.currentVideo?.localUri.isNullOrBlank()) {
                        viewModel.cacheVideoForPlayback(context.cacheDir)
                    }
                    viewModel.setTab(tab)
                },
                recentCount = state.recentShorts.size
            )
        },
        containerColor = BgDark
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (state.currentTab) {
                StudioTab.STUDIO -> {
                    // 1. YouTube Link & Gallery Picker Hero Input Section
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CardDark),
                            shape = RoundedCornerShape(22.dp),
                            border = BorderStroke(
                                1.dp,
                                Brush.horizontalGradient(
                                    listOf(
                                        PrimaryPurple.copy(alpha = 0.4f),
                                        AccentCyan.copy(alpha = 0.3f),
                                        BorderDark
                                    )
                                )
                            )
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Source Video Input",
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            fontSize = 16.sp
                                        )
                                        Text(
                                            text = "Paste any YouTube link or pick from gallery",
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = AccentCyan.copy(alpha = 0.15f),
                                        border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.4f)),
                                        modifier = Modifier.clickable {
                                            videoPickerLauncher.launch("video/*")
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.FolderOpen,
                                                contentDescription = null,
                                                tint = AccentCyan,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Gallery",
                                                color = AccentCyan,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = state.urlInput,
                                    onValueChange = { viewModel.onUrlChanged(it) },
                                    placeholder = { Text("Paste YouTube, Shorts, or Podcast URL...", color = TextSecondary, fontSize = 13.sp) },
                                    singleLine = true,
                                    trailingIcon = {
                                        IconButton(onClick = {
                                            val clip = clipboard?.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
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
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Auto-clipboard detected pill
                                if (detectedClipboardUrl.isNotBlank() && state.urlInput != detectedClipboardUrl) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = PrimaryPurple.copy(alpha = 0.15f),
                                        border = BorderStroke(1.dp, PrimaryPurple.copy(alpha = 0.4f)),
                                        modifier = Modifier.clickable {
                                            viewModel.onUrlChanged(detectedClipboardUrl)
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.ContentPaste, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Detected Link: ${detectedClipboardUrl.take(30)}... (Tap to paste)",
                                                fontSize = 11.sp,
                                                color = PrimaryPurple,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Open Video Cropper Action Button
                                Button(
                                    onClick = { viewModel.loadVideoForManualCropping(cacheDir = context.cacheDir) },
                                    enabled = !state.isLoading,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                    shape = RoundedCornerShape(14.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(PrimaryPurple, AccentIndigo, AccentCyan)
                                                ),
                                                RoundedCornerShape(14.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            if (state.isLoading) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(20.dp),
                                                    color = Color.White,
                                                    strokeWidth = 2.dp
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    text = state.loadingMessage.ifBlank { "Loading Video..." },
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    fontSize = 14.sp
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.ContentCut,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Open Video Cropper & 9:16 Framing",
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color.White,
                                                    fontSize = 14.sp
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(
                                                    imageVector = Icons.Default.ArrowForward,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. Realistic 9:16 Mobile Mockup Frame & Live Hook Preview
                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(230.dp)
                                    .height(410.dp)
                                    .clip(RoundedCornerShape(32.dp))
                                    .background(Color(0xFF070B14))
                                    .border(
                                        BorderStroke(
                                            3.dp,
                                            Brush.verticalGradient(
                                                listOf(
                                                    PrimaryPurple.copy(alpha = 0.8f),
                                                    AccentCyan.copy(alpha = 0.5f),
                                                    BorderDark
                                                )
                                            )
                                        ),
                                        RoundedCornerShape(32.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                // Top Camera Notch Pill
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .padding(top = 10.dp)
                                        .width(60.dp)
                                        .height(5.dp)
                                        .background(Color.White.copy(alpha = 0.25f), CircleShape)
                                )

                                if (state.currentVideo != null) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(CardDark)
                                            .padding(14.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Surface(
                                            color = PrimaryPurple.copy(alpha = 0.2f),
                                            shape = CircleShape,
                                            modifier = Modifier.size(54.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Default.PlayCircle,
                                                    contentDescription = null,
                                                    tint = PrimaryPurple,
                                                    modifier = Modifier.size(36.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = state.currentVideo?.title ?: "",
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center,
                                            fontSize = 12.sp,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Surface(
                                            color = if (state.currentVideo?.localUri != null) SuccessGreen.copy(alpha = 0.2f) else PrimaryPurple.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = if (state.currentVideo?.localUri != null) "📱 Local File Ready" else "🌐 YouTube Stream Ready",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (state.currentVideo?.localUri != null) SuccessGreen else PrimaryPurple,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                } else {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            Icons.Default.CropPortrait,
                                            contentDescription = null,
                                            tint = TextSecondary.copy(alpha = 0.4f),
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "9:16 Shorts Mockup",
                                            color = TextSecondary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Paste a link to preview",
                                            color = TextSecondary.copy(alpha = 0.6f),
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                // Live Hook Caption Overlay on Mockup
                                if (state.hookText.isNotBlank()) {
                                    Surface(
                                        modifier = Modifier
                                            .align(Alignment.TopCenter)
                                            .padding(top = 28.dp, start = 12.dp, end = 12.dp),
                                        color = Color.Black.copy(alpha = 0.85f),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, PrimaryPurple.copy(alpha = 0.6f))
                                    ) {
                                        Text(
                                            text = state.hookText,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                        )
                                    }
                                }

                                // Duration & Aspect badge on Mockup
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(12.dp),
                                    color = Color.Black.copy(alpha = 0.75f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "${state.trimRange.durationSeconds.toInt()}s • 9:16",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
                                shape = RoundedCornerShape(22.dp),
                                border = BorderStroke(1.dp, BorderDark)
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Text(
                                        text = "Viral Hook Caption Overlay",
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        fontSize = 15.sp
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
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // 1-Click Hardware Render Button
                                    Button(
                                        onClick = { viewModel.startRenderShort(context.cacheDir) },
                                        enabled = !state.isRendering,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                        shape = RoundedCornerShape(14.dp),
                                        contentPadding = PaddingValues(0.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    Brush.horizontalGradient(listOf(PrimaryPurple, AccentIndigo)),
                                                    RoundedCornerShape(14.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Download, contentDescription = null, tint = Color.White)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = if (state.currentVideo?.localUri != null) "⚡ Render & Save 9:16 to Gallery" else "⚡ 1-Click Render Video to Gallery",
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color.White,
                                                    fontSize = 14.sp
                                                )
                                            }
                                        }
                                    }

                                    // Switch to Custom Clipper CTA
                                    Spacer(modifier = Modifier.height(10.dp))
                                    OutlinedButton(
                                        onClick = { viewModel.setTab(StudioTab.CLIPPER) },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.ContentCut, contentDescription = null, tint = AccentCyan)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("✂️ Manual Scrub & Custom Duration Clipper")
                                    }

                                    // Switch to Branding CTA
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedButton(
                                        onClick = { viewModel.setTab(StudioTab.BRANDING) },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.BrandingWatermark, contentDescription = null, tint = PrimaryPurple)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("🖼️ Custom Logo & Watermark Settings")
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // 1-Tap Copy Blueprint Button
                                    OutlinedButton(
                                        onClick = {
                                            val blueprint = viewModel.getViralBlueprintText()
                                            val clipMgr = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipMgr.setPrimaryClip(ClipData.newPlainText("Viral Blueprint", blueprint))
                                            Toast.makeText(context, "📋 Copied Viral Blueprint! Ready for CapCut.", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = TextSecondary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("📋 Copy Viral Blueprint & Hook Script")
                                    }

                                    if (state.isRendering) {
                                        Spacer(modifier = Modifier.height(14.dp))
                                        val currentJob = state.renderJob
                                        val progressVal = if (currentJob != null) {
                                            currentJob.progressPercent / 100f
                                        } else {
                                            0.5f
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
                                            "Processing video with Face-Centered Auto Framing: ${currentJob?.progressPercent ?: 0}%"
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
                }

                StudioTab.CLIPPER -> {
                    item {
                        CustomClipperView(
                            state = state,
                            viewModel = viewModel,
                            onPickFromGallery = { videoPickerLauncher.launch("video/*") }
                        )
                    }
                }

                StudioTab.BRANDING -> {
                    item {
                        BrandingOverlayView(
                            state = state,
                            viewModel = viewModel
                        )
                    }
                }

                StudioTab.LIBRARY -> {
                    item {
                        RecentShortsLibraryView(
                            state = state,
                            viewModel = viewModel
                        )
                    }
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
            containerColor = if (isSelected) Color(0xFF1B1438) else CardDark
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                BorderStroke(
                    1.5.dp,
                    if (isSelected) PrimaryPurple else BorderDark
                ),
                RoundedCornerShape(16.dp)
            )
            .clickable { onApply() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Virality Pill
                Surface(
                    color = PrimaryPurple.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, PrimaryPurple.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "🔥 ${clip.viralityScore}% Virality",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // Duration Pill
                Surface(
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, BorderSubtle)
                ) {
                    Text(
                        text = "${clip.startSeconds.toInt()}s - ${clip.endSeconds.toInt()}s (${(clip.endSeconds - clip.startSeconds).toInt()}s)",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = clip.title,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = clip.hookReason,
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Caption Hook Box
            Surface(
                color = BgDark,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Hook:",
                        fontWeight = FontWeight.ExtraBold,
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
