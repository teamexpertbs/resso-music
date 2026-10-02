package com.resso.craka.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media.app.NotificationCompat.MediaStyle
import com.resso.craka.MainActivity
import com.resso.craka.R
import com.resso.craka.util.ArtworkUrls
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class PlaybackService : Service() {
    private var wakeLock: PowerManager.WakeLock? = null
    private lateinit var mediaSession: MediaSessionCompat
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var artJob: Job? = null
    private var artBitmap: Bitmap? = null
    private var artUrlLoaded: String? = null
    private var playbackPositionMs: Long = 0L
    private var playbackDurationMs: Long = 1L

    override fun onCreate() {
        super.onCreate()
        createChannel()
        mediaSession = MediaSessionCompat(this, "RessoMusic").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    commands.onPlay?.invoke()
                }

                override fun onPause() {
                    commands.onPause?.invoke()
                }

                override fun onSkipToNext() {
                    commands.onNext?.invoke()
                }

                override fun onSkipToPrevious() {
                    commands.onPrevious?.invoke()
                }

                override fun onSeekTo(pos: Long) {
                    commands.onSeek?.invoke(pos)
                }
            })
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )
            isActive = true
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> commands.onPlay?.invoke()
            ACTION_PAUSE -> commands.onPause?.invoke()
            ACTION_NEXT -> commands.onNext?.invoke()
            ACTION_PREV -> commands.onPrevious?.invoke()
            ACTION_STOP -> {
                releaseWakeLock()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                val title = intent?.getStringExtra(EXTRA_TITLE) ?: "Resso"
                val artist = intent?.getStringExtra(EXTRA_ARTIST) ?: ""
                val playing = intent?.getBooleanExtra(EXTRA_PLAYING, false) ?: false
                val positionMs = intent?.getLongExtra(EXTRA_POSITION, 0L) ?: 0L
                val durationMs = intent?.getLongExtra(EXTRA_DURATION, 1L) ?: 1L
                val artUrl = intent?.getStringExtra(EXTRA_ART).orEmpty()
                val songId = intent?.getStringExtra(EXTRA_SONG_ID).orEmpty()

                playbackPositionMs = positionMs
                if (durationMs > 1000L) {
                    playbackDurationMs = durationMs
                }

                val notification = buildNotification(title, artist, playing, artBitmap)
                startForeground(NOTIFICATION_ID, notification)
                updateSession(title, artist, playing, artBitmap, positionMs, playbackDurationMs)
                if (playing) holdWakeLock() else releaseWakeLock()
                loadArtwork(title, artist, playing, songId, artUrl)
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        artJob?.cancel()
        scope.cancel()
        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        mediaSession.isActive = false
        mediaSession.release()
        markStopped()
        super.onDestroy()
    }

    private fun loadArtwork(title: String, artist: String, playing: Boolean, songId: String, artUrl: String) {
        val resolved = ArtworkUrls.candidates(songId, artUrl).firstOrNull().orEmpty()
        if (resolved.isBlank() || resolved == artUrlLoaded) return
        artJob?.cancel()
        artJob = scope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                ArtworkUrls.candidates(songId, artUrl).firstNotNullOfOrNull { downloadArtwork(it) }
            }
            if (bitmap == null) return@launch
            artBitmap = bitmap
            artUrlLoaded = resolved
            val manager = getSystemService(NotificationManager::class.java)
            manager.notify(NOTIFICATION_ID, buildNotification(title, artist, playing, bitmap))
            updateSession(title, artist, playing, bitmap, playbackPositionMs, playbackDurationMs)
        }
    }

    private fun downloadArtwork(url: String): Bitmap? {
        return try {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                instanceFollowRedirects = true
                setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36"
                )
                if (url.contains("saavncdn.com") || url.contains("jiosaavn.com")) {
                    setRequestProperty("Referer", "https://www.jiosaavn.com/")
                }
                if (url.contains("ytimg") || url.contains("ggpht")) {
                    setRequestProperty("Referer", "https://www.youtube.com/")
                }
            }
            connection.inputStream.use { stream ->
                val decoded = BitmapFactory.decodeStream(stream) ?: return null
                val maxSide = 512
                if (decoded.width <= maxSide && decoded.height <= maxSide) decoded
                else {
                    val scale = maxSide.toFloat() / maxOf(decoded.width, decoded.height)
                    Bitmap.createScaledBitmap(
                        decoded,
                        (decoded.width * scale).toInt().coerceAtLeast(1),
                        (decoded.height * scale).toInt().coerceAtLeast(1),
                        true
                    )
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun updateSession(
        title: String,
        artist: String,
        playing: Boolean,
        artwork: Bitmap? = null,
        positionMs: Long = -1L,
        durationMs: Long = 1L
    ) {
        if (positionMs >= 0) playbackPositionMs = positionMs
        if (durationMs > 1000L) playbackDurationMs = durationMs

        val state = if (playing) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        mediaSession.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_PLAY_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackStateCompat.ACTION_SEEK_TO or
                        PlaybackStateCompat.ACTION_STOP
                )
                .setState(state, playbackPositionMs, if (playing) 1.0f else 0.0f)
                .build()
        )
        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, "Resso Music")
            .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, title)
            .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, artist)
            .putLong(
                MediaMetadataCompat.METADATA_KEY_DURATION,
                if (playbackDurationMs > 1000L) playbackDurationMs else 180_000L
            )
        if (artwork != null) {
            metadata.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, artwork)
            metadata.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, artwork)
            metadata.putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, artwork)
        }
        mediaSession.setMetadata(metadata.build())
    }

    private fun buildNotification(title: String, artist: String, playing: Boolean, artwork: Bitmap?): android.app.Notification {
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val playPauseAction = if (playing) {
            NotificationCompat.Action(android.R.drawable.ic_media_pause, "Pause", serviceIntent(ACTION_PAUSE))
        } else {
            NotificationCompat.Action(android.R.drawable.ic_media_play, "Play", serviceIntent(ACTION_PLAY))
        }
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(artist)
            .setSubText("Resso Music • VIP")
            .setColorized(true)
            .setColor(0xFFE91E63.toInt()) // Vibrant Resso Neon Pink/Red theme accent
            .setContentIntent(openApp)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setOngoing(playing)
        if (artwork != null) {
            builder.setLargeIcon(artwork)
        }
        return builder
            .addAction(NotificationCompat.Action(android.R.drawable.ic_media_previous, "Previous", serviceIntent(ACTION_PREV)))
            .addAction(playPauseAction)
            .addAction(NotificationCompat.Action(android.R.drawable.ic_media_next, "Next", serviceIntent(ACTION_NEXT)))
            .setStyle(
                MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .build()
    }

    private fun serviceIntent(action: String): PendingIntent {
        val intent = Intent(this, PlaybackService::class.java).apply { this.action = action }
        return PendingIntent.getService(
            this,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun holdWakeLock() {
        if (wakeLock == null) {
            val power = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Resso:playback").apply {
                setReferenceCounted(false)
            }
        }
        if (wakeLock?.isHeld != true) {
            wakeLock?.acquire(4 * 60 * 60 * 1000L)
        }
    }

    private fun releaseWakeLock() {
        if (wakeLock?.isHeld == true) wakeLock?.release()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Playback",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Lock screen and background playback"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "resso_playback"
        private const val NOTIFICATION_ID = 42
        private const val ACTION_UPDATE = "com.resso.craka.action.UPDATE"
        private const val ACTION_PLAY = "com.resso.craka.action.PLAY"
        private const val ACTION_PAUSE = "com.resso.craka.action.PAUSE"
        private const val ACTION_NEXT = "com.resso.craka.action.NEXT"
        private const val ACTION_PREV = "com.resso.craka.action.PREV"
        private const val ACTION_STOP = "com.resso.craka.action.STOP"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_ARTIST = "artist"
        private const val EXTRA_PLAYING = "playing"
        private const val EXTRA_ART = "art"
        private const val EXTRA_SONG_ID = "songId"
        private const val EXTRA_POSITION = "position"
        private const val EXTRA_DURATION = "duration"

        val commands = PlaybackCommands()

        @Volatile
        private var started = false

        fun update(
            context: Context,
            title: String,
            artist: String,
            playing: Boolean,
            artUrl: String = "",
            songId: String = "",
            positionMs: Long = 0L,
            durationMs: Long = 1L
        ) {
            if (!playing && !started) return
            started = true
            val intent = Intent(context, PlaybackService::class.java).apply {
                action = ACTION_UPDATE
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_ARTIST, artist)
                putExtra(EXTRA_PLAYING, playing)
                putExtra(EXTRA_ART, artUrl)
                putExtra(EXTRA_SONG_ID, songId)
                putExtra(EXTRA_POSITION, positionMs)
                putExtra(EXTRA_DURATION, durationMs)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            started = false
            context.stopService(Intent(context, PlaybackService::class.java))
        }

        internal fun markStopped() {
            started = false
        }
    }
}

class PlaybackCommands {
    var onPlay: (() -> Unit)? = null
    var onPause: (() -> Unit)? = null
    var onNext: (() -> Unit)? = null
    var onPrevious: (() -> Unit)? = null
    var onSeek: ((Long) -> Unit)? = null
}
