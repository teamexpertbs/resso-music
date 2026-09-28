package com.resso.craka.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
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

class PlaybackService : Service() {
    private var wakeLock: PowerManager.WakeLock? = null
    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null
    private lateinit var mediaSession: MediaSessionCompat

    override fun onCreate() {
        super.onCreate()
        createChannel()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
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
            })
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
                releaseAudio()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                val title = intent?.getStringExtra(EXTRA_TITLE) ?: "Resso"
                val artist = intent?.getStringExtra(EXTRA_ARTIST) ?: ""
                val playing = intent?.getBooleanExtra(EXTRA_PLAYING, false) ?: false
                val notification = buildNotification(title, artist, playing)
                startForeground(NOTIFICATION_ID, notification)
                updateSession(title, artist, playing)
                if (playing) holdAudio() else releaseWakeLock()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        releaseAudio()
        stopForeground(STOP_FOREGROUND_REMOVE)
        mediaSession.isActive = false
        mediaSession.release()
        super.onDestroy()
    }

    private fun updateSession(title: String, artist: String, playing: Boolean) {
        val state = if (playing) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        mediaSession.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                )
                .setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, if (playing) 1f else 0f)
                .build()
        )
        mediaSession.setMetadata(
            MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, "Resso")
                .build()
        )
    }

    private fun buildNotification(title: String, artist: String, playing: Boolean): android.app.Notification {
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
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(artist)
            .setContentIntent(openApp)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setOngoing(playing)
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

    private fun holdAudio() {
        if (wakeLock == null) {
            val power = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Resso:playback").apply {
                setReferenceCounted(false)
            }
        }
        if (wakeLock?.isHeld != true) {
            wakeLock?.acquire(4 * 60 * 60 * 1000L)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setOnAudioFocusChangeListener { }
                .build()
            focusRequest = request
            audioManager?.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            audioManager?.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }
    }

    private fun releaseWakeLock() {
        if (wakeLock?.isHeld == true) wakeLock?.release()
    }

    private fun releaseAudio() {
        releaseWakeLock()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager?.abandonAudioFocus(null)
        }
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

        val commands = PlaybackCommands()

        fun update(context: Context, title: String, artist: String, playing: Boolean) {
            val intent = Intent(context, PlaybackService::class.java).apply {
                action = ACTION_UPDATE
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_ARTIST, artist)
                putExtra(EXTRA_PLAYING, playing)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, PlaybackService::class.java))
        }
    }
}

class PlaybackCommands {
    var onPlay: (() -> Unit)? = null
    var onPause: (() -> Unit)? = null
    var onNext: (() -> Unit)? = null
    var onPrevious: (() -> Unit)? = null
}
