package com.example.player

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class YouTubePlayerManager(private val context: Context) {
    private var webView: WebView? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private var currentVideoId: String? = null
    private var isPlayerReady = false
    private var pendingVideoId: String? = null
    private var pendingAutoPlay: Boolean = true

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _currentTimeMs = MutableStateFlow(0L)
    val currentTimeMs = _currentTimeMs.asStateFlow()

    private val _durationMs = MutableStateFlow(1L)
    val durationMs = _durationMs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    var onVideoEnded: (() -> Unit)? = null

    init {
        mainHandler.post {
            initWebView()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        if (webView != null) return

        try {
            val wv = WebView(context.applicationContext)
            wv.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            val settings = wv.settings
            settings.javaScriptEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

            wv.webChromeClient = WebChromeClient()
            wv.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    Log.d("YouTubePlayerManager", "Player page loaded successfully")
                }
            }

            wv.addJavascriptInterface(AndroidBridge(), "AndroidBridge")
            wv.loadDataWithBaseURL("https://www.youtube-nocookie.com", getPlayerHtml(), "text/html", "UTF-8", null)

            webView = wv
        } catch (e: Exception) {
            Log.e("YouTubePlayerManager", "Error initializing WebView: ${e.message}", e)
        }
    }

    fun getWebView(): WebView {
        if (webView == null) {
            initWebView()
        }
        return webView!!
    }

    fun loadAndPlay(videoId: String, autoPlay: Boolean = true) {
        currentVideoId = videoId
        _isLoading.value = true
        _currentTimeMs.value = 0L

        mainHandler.post {
            if (isPlayerReady && webView != null) {
                val action = if (autoPlay) "loadVideoById" else "cueVideoById"
                webView?.evaluateJavascript("if (player && player.$action) { player.$action('$videoId'); }", null)
            } else {
                pendingVideoId = videoId
                pendingAutoPlay = autoPlay
                if (webView == null) {
                    initWebView()
                }
            }
        }
    }

    fun play() {
        mainHandler.post {
            webView?.evaluateJavascript("if (player && player.playVideo) { player.playVideo(); }", null)
        }
    }

    fun pause() {
        mainHandler.post {
            webView?.evaluateJavascript("if (player && player.pauseVideo) { player.pauseVideo(); }", null)
        }
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            pause()
        } else {
            play()
        }
    }

    fun seekTo(positionMs: Long) {
        _currentTimeMs.value = positionMs
        val seconds = positionMs / 1000f
        mainHandler.post {
            webView?.evaluateJavascript("if (player && player.seekTo) { player.seekTo($seconds, true); }", null)
        }
    }

    fun release() {
        mainHandler.post {
            try {
                (webView?.parent as? ViewGroup)?.removeView(webView)
                webView?.destroy()
                webView = null
            } catch (_: Exception) {}
        }
    }

    private inner class AndroidBridge {
        @JavascriptInterface
        fun onReady() {
            isPlayerReady = true
            _isLoading.value = false
            mainHandler.post {
                val pending = pendingVideoId
                if (pending != null) {
                    val action = if (pendingAutoPlay) "loadVideoById" else "cueVideoById"
                    webView?.evaluateJavascript("if (player && player.$action) { player.$action('$pending'); }", null)
                    pendingVideoId = null
                }
            }
        }

        @JavascriptInterface
        fun onStateChange(state: Int) {
            mainHandler.post {
                // YT.PlayerState:
                // -1: unstarted, 0: ended, 1: playing, 2: paused, 3: buffering, 5: video cued
                when (state) {
                    1 -> {
                        _isPlaying.value = true
                        _isLoading.value = false
                    }
                    2 -> {
                        _isPlaying.value = false
                        _isLoading.value = false
                    }
                    3 -> {
                        _isLoading.value = true
                    }
                    0 -> {
                        _isPlaying.value = false
                        _isLoading.value = false
                        onVideoEnded?.invoke()
                    }
                }
            }
        }

        @JavascriptInterface
        fun onTimeUpdate(currentTimeSec: Float, durationSec: Float) {
            val currMs = (currentTimeSec * 1000).toLong()
            val durMs = (durationSec * 1000).toLong()
            if (durMs > 1000L) {
                _durationMs.value = durMs
            }
            if (currMs >= 0L) {
                _currentTimeMs.value = currMs
            }
        }

        @JavascriptInterface
        fun onError(errorCode: Int) {
            Log.e("YouTubePlayerManager", "YouTube Player Error: $errorCode")
            _isLoading.value = false
        }
    }

    private fun getPlayerHtml(): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <style>
                    * { box-sizing: border-box; }
                    html, body {
                        margin: 0;
                        padding: 0;
                        width: 100%;
                        height: 100%;
                        background-color: #000000;
                        overflow: hidden;
                    }
                    #player {
                        width: 100%;
                        height: 100%;
                        position: absolute;
                        top: 0;
                        left: 0;
                    }
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
                    var isReady = false;

                    function onYouTubeIframeAPIReady() {
                        player = new YT.Player('player', {
                            height: '100%',
                            width: '100%',
                            playerVars: {
                                'autoplay': 1,
                                'controls': 0,
                                'playsinline': 1,
                                'rel': 0,
                                'fs': 0,
                                'modestbranding': 1,
                                'origin': 'https://www.youtube-nocookie.com'
                            },
                            events: {
                                'onReady': onPlayerReady,
                                'onStateChange': onPlayerStateChange,
                                'onError': onPlayerError
                            }
                        });
                    }

                    function onPlayerReady(event) {
                        isReady = true;
                        if (window.AndroidBridge) {
                            window.AndroidBridge.onReady();
                        }
                    }

                    function onPlayerStateChange(event) {
                        if (window.AndroidBridge) {
                            window.AndroidBridge.onStateChange(event.data);
                        }
                    }

                    function onPlayerError(event) {
                        if (window.AndroidBridge) {
                            window.AndroidBridge.onError(event.data);
                        }
                    }

                    setInterval(function() {
                        if (player && isReady && player.getCurrentTime) {
                            try {
                                var curr = player.getCurrentTime();
                                var dur = player.getDuration();
                                if (window.AndroidBridge && curr !== undefined && dur !== undefined) {
                                    window.AndroidBridge.onTimeUpdate(curr, dur);
                                }
                            } catch(e) {}
                        }
                    }, 250);
                </script>
            </body>
            </html>
        """.trimIndent()
    }
}
