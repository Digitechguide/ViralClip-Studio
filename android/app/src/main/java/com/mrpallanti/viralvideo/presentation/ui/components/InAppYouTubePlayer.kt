package com.mrpallanti.viralvideo.presentation.ui.components

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.mrpallanti.viralvideo.presentation.ui.theme.CardDark
import com.mrpallanti.viralvideo.presentation.ui.theme.PrimaryPurple
import com.mrpallanti.viralvideo.presentation.ui.theme.TextPrimary
import com.mrpallanti.viralvideo.presentation.ui.theme.TextSecondary

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun InAppYouTubePlayer(
    videoId: String,
    modifier: Modifier = Modifier,
    onPositionUpdate: (Float) -> Unit = {},
    onCacheVideo: () -> Unit = {},
    onPickFromGallery: () -> Unit = {}
) {
    val handler = Handler(Looper.getMainLooper())
    var playerError by remember(videoId) { mutableStateOf<Int?>(null) }

    val htmlContent = remember(videoId) {
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <style>
                * { margin: 0; padding: 0; box-sizing: border-box; background: #000; overflow: hidden; }
                html, body { width: 100%; height: 100%; }
                #player { width: 100%; height: 100%; border: 0; }
            </style>
        </head>
        <body>
            <div id="player"></div>
            <script>
                var tag = document.createElement('script');
                tag.src = "https://www.youtube-nocookie.com/iframe_api";
                var firstScriptTag = document.getElementsByTagName('script')[0];
                firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);

                var player;
                function onYouTubeIframeAPIReady() {
                    player = new YT.Player('player', {
                        videoId: '$videoId',
                        playerVars: {
                            'playsinline': 1,
                            'autoplay': 0,
                            'controls': 1,
                            'rel': 0,
                            'modestbranding': 1,
                            'fs': 0,
                            'origin': 'https://viral.mrpallanti.in',
                            'enablejsapi': 1
                        },
                        events: {
                            'onReady': onPlayerReady,
                            'onError': onPlayerError
                        }
                    });
                }

                function onPlayerReady(event) {
                    setInterval(function() {
                        if (player && typeof player.getCurrentTime === 'function') {
                            var t = player.getCurrentTime();
                            if (window.AndroidInterface && typeof window.AndroidInterface.onTimeUpdate === 'function') {
                                window.AndroidInterface.onTimeUpdate(t);
                            }
                        }
                    }, 400);
                }

                function onPlayerError(event) {
                    if (window.AndroidInterface && typeof window.AndroidInterface.onPlayerError === 'function') {
                        window.AndroidInterface.onPlayerError(event.data);
                    }
                }
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        mediaPlaybackRequiresUserGesture = false
                        userAgentString = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
                        cacheMode = WebSettings.LOAD_DEFAULT
                        allowFileAccess = true
                        builtInZoomControls = false
                        displayZoomControls = false
                    }
                    webChromeClient = WebChromeClient()
                    webViewClient = WebViewClient()

                    addJavascriptInterface(object {
                        @JavascriptInterface
                        fun onTimeUpdate(seconds: Float) {
                            handler.post {
                                onPositionUpdate(seconds)
                            }
                        }

                        @JavascriptInterface
                        fun onPlayerError(errorCode: Int) {
                            handler.post {
                                playerError = errorCode
                            }
                        }
                    }, "AndroidInterface")

                    loadDataWithBaseURL("https://viral.mrpallanti.in", htmlContent, "text/html", "UTF-8", null)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay fallback when YouTube restricts iframe embedding (error 150, 152, 101, etc.)
        if (playerError != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.92f))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "YouTube Embed Restricted (Code $playerError)",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "This creator restricted browser playback. Cache on-device with our built-in engine for smooth, unrestricted scrubbing!",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onCacheVideo,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cache on Device", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = onPickFromGallery,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Gallery", fontSize = 12.sp, color = TextPrimary)
                        }
                    }
                }
            }
        }
    }
}
