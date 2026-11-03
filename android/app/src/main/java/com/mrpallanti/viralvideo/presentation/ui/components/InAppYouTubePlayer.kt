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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun InAppYouTubePlayer(
    videoId: String,
    modifier: Modifier = Modifier,
    onPositionUpdate: (Float) -> Unit = {}
) {
    val handler = Handler(Looper.getMainLooper())

    val htmlContent = remember(videoId) {
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <style>
                * { margin: 0; padding: 0; box-sizing: border-box; background: #000; overflow: hidden; }
                html, body { width: 100%; height: 100%; }
                #player { width: 100%; height: 100%; }
            </style>
        </head>
        <body>
            <div id="player"></div>
            <script>
                var tag = document.createElement('script');
                tag.src = "https://www.youtube.com/iframe_api";
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
                            'origin': 'https://www.youtube.com'
                        },
                        events: {
                            'onReady': onPlayerReady
                        }
                    });
                }

                function onPlayerReady(event) {
                    setInterval(function() {
                        if (player && typeof player.getCurrentTime === 'function') {
                            var t = player.getCurrentTime();
                            if (window.AndroidInterface) {
                                window.AndroidInterface.onTimeUpdate(t);
                            }
                        }
                    }, 400);
                }
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black)
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
                        mediaPlaybackRequiresUserGesture = false
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
                    }, "AndroidInterface")

                    loadDataWithBaseURL("https://www.youtube.com", htmlContent, "text/html", "UTF-8", null)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
