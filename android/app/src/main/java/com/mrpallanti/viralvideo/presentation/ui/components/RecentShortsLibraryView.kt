package com.mrpallanti.viralvideo.presentation.ui.components

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mrpallanti.viralvideo.domain.model.GeneratedShortItem
import com.mrpallanti.viralvideo.presentation.ui.theme.*
import com.mrpallanti.viralvideo.presentation.viewmodel.StudioUiState
import com.mrpallanti.viralvideo.presentation.viewmodel.StudioViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecentShortsLibraryView(
    state: StudioUiState,
    viewModel: StudioViewModel
) {
    val context = LocalContext.current
    val recentList = state.recentShorts
    var itemToDelete by remember { mutableStateOf<GeneratedShortItem?>(null) }
    var playingItem by remember { mutableStateOf<GeneratedShortItem?>(null) }

    // In-App Video Playback Dialog
    playingItem?.let { item ->
        Dialog(onDismissRequest = { playingItem = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(560.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CardDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryPurple)
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.title,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { playingItem = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    InAppVideoPlayer(
                        uriString = item.outputUri,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Duration: ${String.format("%.1f", item.durationSeconds)}s",
                            color = AccentCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Button(
                            onClick = {
                                shareVideo(context, item.outputUri, item.title)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share to Shorts / Reels", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Delete Short", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Remove '${item.title}' from your generated library?", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteGeneratedShort(item.id)
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CardDark
        )
    }

    if (recentList.isEmpty()) {
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
                    imageVector = Icons.Default.VideoLibrary,
                    contentDescription = null,
                    tint = PrimaryPurple,
                    modifier = Modifier.size(54.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Generated Videos Yet",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Videos rendered via the Studio or Custom Clipper tabs will appear here with instant in-app playback and 1-tap social media sharing.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recently Generated Shorts (${recentList.size})",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 16.sp
                )
                Surface(
                    color = SuccessGreen.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Saved in Gallery",
                        color = SuccessGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            recentList.forEach { shortItem ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Thumbnail / Video Icon Box
                            Box(
                                modifier = Modifier
                                    .size(58.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF030712))
                                    .border(1.dp, PrimaryPurple.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                    .clickable { playingItem = shortItem },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayCircle,
                                    contentDescription = "Play",
                                    tint = PrimaryPurple,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Metadata Info
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = shortItem.title,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (shortItem.hookCaption.isNotBlank()) {
                                    Text(
                                        text = shortItem.hookCaption,
                                        color = TextSecondary,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "${String.format("%.1f", shortItem.durationSeconds)}s",
                                        color = AccentCyan,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "•",
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = formatTimestamp(shortItem.timestamp),
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            // Actions: Share & Delete
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { shareVideo(context, shortItem.outputUri, shortItem.title) }) {
                                    Icon(Icons.Default.Share, contentDescription = "Share", tint = AccentCyan)
                                }
                                IconButton(onClick = { itemToDelete = shortItem }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444).copy(alpha = 0.7f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    if (timestamp <= 0) return "Recently"
    val diff = System.currentTimeMillis() - timestamp
    val mins = diff / (1000 * 60)
    val hours = mins / 60
    val days = hours / 24

    return when {
        mins < 2 -> "Just now"
        mins < 60 -> "${mins}m ago"
        hours < 24 -> "${hours}h ago"
        days < 7 -> "${days}d ago"
        else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
    }
}

private fun shareVideo(context: android.content.Context, uriString: String, title: String) {
    try {
        val uri = Uri.parse(uriString)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "🎬 Check out this viral 9:16 short generated with ViralClip Studio! #Shorts #Reels")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share '$title' to..."))
    } catch (e: Exception) {
        // Fallback if direct content URI fails
        try {
            val textIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "Generated short: $title\n$uriString")
            }
            context.startActivity(Intent.createChooser(textIntent, "Share Video Link"))
        } catch (ignored: Exception) {}
    }
}
