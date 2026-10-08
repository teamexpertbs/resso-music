package com.resso.craka.player

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import com.resso.craka.data.model.Song
import com.resso.craka.data.model.SongEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object YouTubeMusicPlayerManager {
    private const val TAG = "YouTubeMusicPlayer"

    private var youTubePlayerView: YouTubePlayerView? = null
    private var activePlayer: YouTubePlayer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentSong = MutableStateFlow<SongEntity?>(null)
    val currentSong: StateFlow<SongEntity?> = _currentSong.asStateFlow()

    private val _currentVideoId = MutableStateFlow<String?>(null)
    val currentVideoId: StateFlow<String?> = _currentVideoId.asStateFlow()

    private val _currentPositionSec = MutableStateFlow(0f)
    val currentPositionSec: StateFlow<Float> = _currentPositionSec.asStateFlow()

    private val _totalDurationSec = MutableStateFlow(0f)
    val totalDurationSec: StateFlow<Float> = _totalDurationSec.asStateFlow()

    private var consecutiveErrorCount = 0
    private var lastErrorTimeMs = 0L

    // Callback when a track naturally finishes
    var onSongEnded: (() -> Unit)? = null
    var onPlaybackStateChanged: ((Boolean) -> Unit)? = null

    /**
     * Recursively traverses views to enable essential WebView settings for autoplay
     * without user gestures.
     */
    fun configureWebViewSettings(view: View?) {
        if (view == null) return
        if (view is WebView) {
            try {
                view.settings.mediaPlaybackRequiresUserGesture = false
                view.settings.javaScriptEnabled = true
                view.settings.domStorageEnabled = true
                view.settings.databaseEnabled = true
                view.settings.allowContentAccess = true
                view.settings.allowFileAccess = false
                Log.d(TAG, "Configured WebView for gesture-free media playback")
            } catch (e: Exception) {
                Log.w(TAG, "WebView configuration warning: ${e.message}")
            }
        } else if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                configureWebViewSettings(view.getChildAt(i))
            }
        }
    }

    fun attachPlayerView(playerView: YouTubePlayerView) {
        this.youTubePlayerView = playerView
        playerView.enableAutomaticInitialization = false
        playerView.enableBackgroundPlayback(true)

        playerView.setOnHierarchyChangeListener(object : ViewGroup.OnHierarchyChangeListener {
            override fun onChildViewAdded(parent: View?, child: View?) {
                configureWebViewSettings(child)
            }
            override fun onChildViewRemoved(parent: View?, child: View?) {}
        })

        // Configure WebView child once attached to window
        playerView.post {
            configureWebViewSettings(playerView)
        }

        val options = IFramePlayerOptions.Builder()
            .controls(0)
            .autoplay(1)
            .rel(0)
            .ivLoadPolicy(3)
            .ccLoadPolicy(0)
            .build()

        playerView.initialize(object : AbstractYouTubePlayerListener() {
            override fun onReady(youTubePlayer: YouTubePlayer) {
                activePlayer = youTubePlayer
                Log.d(TAG, "YouTubePlayer onReady received")
                // Re-apply WebView settings after initial frame load
                playerView.post { configureWebViewSettings(playerView) }

                // If a song was queued prior to onReady
                _currentVideoId.value?.let { videoId ->
                    if (videoId.isNotBlank()) {
                        youTubePlayer.loadVideo(videoId, 0f)
                        _isPlaying.value = true
                        onPlaybackStateChanged?.invoke(true)
                    }
                }
            }

            override fun onStateChange(youTubePlayer: YouTubePlayer, state: PlayerConstants.PlayerState) {
                Log.d(TAG, "YouTubePlayer onStateChange: $state")
                when (state) {
                    PlayerConstants.PlayerState.PLAYING -> {
                        _isPlaying.value = true
                        consecutiveErrorCount = 0
                        onPlaybackStateChanged?.invoke(true)
                    }
                    PlayerConstants.PlayerState.PAUSED -> {
                        _isPlaying.value = false
                        onPlaybackStateChanged?.invoke(false)
                    }
                    PlayerConstants.PlayerState.ENDED -> {
                        _isPlaying.value = false
                        consecutiveErrorCount = 0
                        onPlaybackStateChanged?.invoke(false)
                        Log.i(TAG, "Track finished naturally. Advancing to next song.")
                        onSongEnded?.invoke()
                    }
                    PlayerConstants.PlayerState.BUFFERING -> {
                        // Keep state as playing during brief network buffer
                        _isPlaying.value = true
                    }
                    else -> {}
                }
            }

            override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                _currentPositionSec.value = second
            }

            override fun onVideoDuration(youTubePlayer: YouTubePlayer, duration: Float) {
                _totalDurationSec.value = duration
            }

            override fun onError(youTubePlayer: YouTubePlayer, error: PlayerConstants.PlayerError) {
                Log.e(TAG, "YouTubePlayer onError: $error")
                val now = System.currentTimeMillis()

                // Drop bursts of errors within 2 seconds
                if (now - lastErrorTimeMs < 2000L) {
                    return
                }
                lastErrorTimeMs = now
                consecutiveErrorCount++

                if (consecutiveErrorCount >= 3) {
                    Log.w(TAG, "Limit of consecutive errors reached ($consecutiveErrorCount). Pausing to prevent skip loop.")
                    _isPlaying.value = false
                    consecutiveErrorCount = 0
                    onPlaybackStateChanged?.invoke(false)
                    return
                }

                // Instead of instantly skipping, wait 2.5 seconds with debounce
                mainHandler.postDelayed({
                    if (consecutiveErrorCount in 1..2) {
                        Log.i(TAG, "Delayed fallback skip after YouTube error")
                        onSongEnded?.invoke()
                    }
                }, 2500L)
            }
        }, true, options)
    }

    fun playSong(song: SongEntity) {
        _currentSong.value = song
        val videoId = extractVideoId(song)
        _currentVideoId.value = videoId
        Log.d(TAG, "Playing song: ${song.title} with videoId: $videoId")

        val player = activePlayer
        if (player != null && videoId.isNotBlank()) {
            player.loadVideo(videoId, 0f)
            _isPlaying.value = true
            onPlaybackStateChanged?.invoke(true)
        }
    }

    fun playSong(song: Song) {
        playSong(song.toSongEntity())
    }

    fun togglePlayPause() {
        val player = activePlayer ?: return
        if (_isPlaying.value) {
            player.pause()
            _isPlaying.value = false
            onPlaybackStateChanged?.invoke(false)
        } else {
            player.play()
            _isPlaying.value = true
            onPlaybackStateChanged?.invoke(true)
        }
    }

    fun pause() {
        activePlayer?.pause()
        _isPlaying.value = false
        onPlaybackStateChanged?.invoke(false)
    }

    fun resume() {
        activePlayer?.play()
        _isPlaying.value = true
        onPlaybackStateChanged?.invoke(true)
    }

    fun seekTo(seconds: Float) {
        activePlayer?.seekTo(seconds)
    }

    fun release() {
        mainHandler.removeCallbacksAndMessages(null)
        try {
            youTubePlayerView?.release()
        } catch (_: Exception) {}
        youTubePlayerView = null
        activePlayer = null
    }

    fun extractVideoId(song: SongEntity): String {
        val id = song.id.removePrefix("yt_").trim()
        if (id.length == 11 && !id.contains(" ") && !id.contains("/")) {
            return id
        }
        val url = song.audioUrl
        if (url.contains("youtu.be/")) {
            val candidate = url.substringAfter("youtu.be/").substringBefore("?").substringBefore("&").trim()
            if (candidate.length == 11) return candidate
        }
        if (url.contains("v=")) {
            val candidate = url.substringAfter("v=").substringBefore("&").substringBefore("?").trim()
            if (candidate.length == 11) return candidate
        }
        val regex = Regex("[a-zA-Z0-9_-]{11}")
        regex.find(id)?.value?.let { return it }
        regex.find(url)?.value?.let { return it }
        return id
    }
}
