package com.resso.craka.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.resso.craka.service.PlaybackService
import com.resso.craka.RessoApplication
import com.resso.craka.data.model.CommentEntity
import com.resso.craka.data.model.LyricLine
import com.resso.craka.data.model.PlaylistEntity
import com.resso.craka.data.model.SongEntity
import com.resso.craka.data.model.VibeEntity
import com.resso.craka.flash.FlashSyncManager
import com.resso.craka.player.AppEqualizer
import com.resso.craka.player.StreamPlayerManager
import com.resso.craka.util.LyricsParser
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MusicViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as RessoApplication).repository
    val flashSyncManager = FlashSyncManager(application)
    val streamPlayerManager = StreamPlayerManager(application)

    private val player: ExoPlayer = ExoPlayer.Builder(application)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build(),
            true
        )
        .setHandleAudioBecomingNoisy(true)
        .setWakeMode(C.WAKE_MODE_NETWORK)
        .build()

    val allSongs: StateFlow<List<SongEntity>> = repository.getAllSongs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val likedSongs: StateFlow<List<SongEntity>> = repository.getLikedSongs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPlaylists: StateFlow<List<PlaylistEntity>> = repository.getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allVibes: StateFlow<List<VibeEntity>> = repository.getAllVibes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentSongIndex = MutableStateFlow(0)
    val currentSongIndex: StateFlow<Int> = _currentSongIndex.asStateFlow()

    private val _currentSong = MutableStateFlow<SongEntity?>(null)
    val currentSong: StateFlow<SongEntity?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _streamVisible = MutableStateFlow(false)
    val streamVisible: StateFlow<Boolean> = _streamVisible.asStateFlow()

    private val _openPlaylistSongs = MutableStateFlow<List<SongEntity>>(emptyList())
    val openPlaylistSongs: StateFlow<List<SongEntity>> = _openPlaylistSongs.asStateFlow()

    private val playerPrefs = application.getSharedPreferences("player_prefs", Context.MODE_PRIVATE)

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(1L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _lyrics = MutableStateFlow<List<LyricLine>>(emptyList())
    val lyrics: StateFlow<List<LyricLine>> = _lyrics.asStateFlow()

    private val _lyricsStatus = MutableStateFlow("")
    val lyricsStatus: StateFlow<String> = _lyricsStatus.asStateFlow()

    private val _activeLyricIndex = MutableStateFlow(0)
    val activeLyricIndex: StateFlow<Int> = _activeLyricIndex.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _repeatMode = MutableStateFlow(0) // 0: none, 1: all, 2: one
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    // Vibes
    private val _currentVibeUri = MutableStateFlow<String?>(null)
    val currentVibeUri: StateFlow<String?> = _currentVibeUri.asStateFlow()

    private val _currentVibeFilter = MutableStateFlow("Neon")
    val currentVibeFilter: StateFlow<String> = _currentVibeFilter.asStateFlow()

    // Flash Sync & Volume Booster
    private val _isFlashSyncEnabled = MutableStateFlow(false)
    val isFlashSyncEnabled: StateFlow<Boolean> = _isFlashSyncEnabled.asStateFlow()

    private val _isVolumeBoosterEnabled = MutableStateFlow(false)
    val isVolumeBoosterEnabled: StateFlow<Boolean> = _isVolumeBoosterEnabled.asStateFlow()

    // Comments Sheet
    private val _isCommentsSheetOpen = MutableStateFlow(false)
    val isCommentsSheetOpen: StateFlow<Boolean> = _isCommentsSheetOpen.asStateFlow()

    private val _commentsForCurrentSong = MutableStateFlow<List<CommentEntity>>(emptyList())
    val commentsForCurrentSong: StateFlow<List<CommentEntity>> = _commentsForCurrentSong.asStateFlow()

    // Lyric Poster Dialog
    private val _isPosterDialogOpen = MutableStateFlow(false)
    val isPosterDialogOpen: StateFlow<Boolean> = _isPosterDialogOpen.asStateFlow()

    private val _selectedPosterLyric = MutableStateFlow<LyricLine?>(null)
    val selectedPosterLyric: StateFlow<LyricLine?> = _selectedPosterLyric.asStateFlow()

    // Video MV mode & Lyrics visibility toggles
    private val _isVideoMode = MutableStateFlow(false)
    val isVideoMode: StateFlow<Boolean> = _isVideoMode.asStateFlow()

    fun toggleVideoMode() {
        _isVideoMode.value = !_isVideoMode.value
        streamPlayerManager.setHighQuality(_isVideoMode.value)
    }

    fun setVideoMode(enabled: Boolean) {
        _isVideoMode.value = enabled
        streamPlayerManager.setHighQuality(enabled)
    }

    private val _isLyricsVisible = MutableStateFlow(false)
    val isLyricsVisible: StateFlow<Boolean> = _isLyricsVisible.asStateFlow()

    fun toggleLyrics() {
        _isLyricsVisible.value = !_isLyricsVisible.value
    }

    fun setLyricsVisible(visible: Boolean) {
        _isLyricsVisible.value = visible
    }

    // Search and filter
    val searchQuery = MutableStateFlow("")
    val selectedMood = MutableStateFlow<String?>(null)

    private val _searchResults = MutableStateFlow<List<SongEntity>>(emptyList())
    val searchResults: StateFlow<List<SongEntity>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _searchError = MutableStateFlow<String?>(null)
    val searchError: StateFlow<String?> = _searchError.asStateFlow()

    private val _trendingSongs = MutableStateFlow<List<SongEntity>>(emptyList())
    val trendingSongs: StateFlow<List<SongEntity>> = _trendingSongs.asStateFlow()

    private val playbackQueue = MutableStateFlow<List<SongEntity>>(emptyList())
    val queue: StateFlow<List<SongEntity>> = playbackQueue.asStateFlow()

    private val _recentSongs = MutableStateFlow<List<SongEntity>>(emptyList())
    val recentSongs: StateFlow<List<SongEntity>> = _recentSongs.asStateFlow()

    private val _pendingSearch = MutableStateFlow<String?>(null)
    val pendingSearch: StateFlow<String?> = _pendingSearch.asStateFlow()

    fun openSearch(query: String) {
        _pendingSearch.value = query
    }

    fun consumePendingSearch() {
        _pendingSearch.value = null
    }

    private val _searchHistory = MutableStateFlow<List<String>>(emptyList())
    val searchHistory: StateFlow<List<String>> = _searchHistory.asStateFlow()

    private val _homeRows = MutableStateFlow<List<Pair<String, List<SongEntity>>>>(emptyList())
    val homeRows: StateFlow<List<Pair<String, List<SongEntity>>>> = _homeRows.asStateFlow()

    private val _lyricOffsetMs = MutableStateFlow(0)
    val lyricOffsetMs: StateFlow<Int> = _lyricOffsetMs.asStateFlow()

    private val _sleepMinutesLeft = MutableStateFlow(0)
    val sleepMinutesLeft: StateFlow<Int> = _sleepMinutesLeft.asStateFlow()

    private val _equalizerPreset = MutableStateFlow("Off")
    val equalizerPreset: StateFlow<String> = _equalizerPreset.asStateFlow()

    private val appEqualizer = AppEqualizer()
    private var sleepJob: Job? = null

    private var searchJob: Job? = null
    private var progressJob: Job? = null
    private var commentsJob: Job? = null

    // Network Auto-Resume Management (Resume from exact spot when internet reconnects)
    private var wasPlayingBeforeNetworkLost = false
    private var savedPositionBeforeLost = 0L
    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    private val _networkStatusMessage = MutableStateFlow<String?>(null)
    val networkStatusMessage: StateFlow<String?> = _networkStatusMessage.asStateFlow()

    fun clearNetworkStatusMessage() {
        _networkStatusMessage.value = null
    }

    init {
        PlaybackService.commands.onPlay = { resumePlayback() }
        PlaybackService.commands.onPause = { pausePlayback() }
        PlaybackService.commands.onNext = { playNextSong() }
        PlaybackService.commands.onPrevious = { playPreviousSong() }
        PlaybackService.commands.onSeek = { seekTo(it) }
        _recentSongs.value = repository.recentSongs()
        _searchHistory.value = playerPrefs.getString("search_history", "")
            .orEmpty()
            .split("\n")
            .filter { it.isNotBlank() }
        _isShuffle.value = playerPrefs.getBoolean("shuffle", false)
        _repeatMode.value = playerPrefs.getInt("repeat", 0)
        setupPlayerListener()
        setupStreamListener()
        startProgressTracking()
        loadTrendingSongs()
        loadHomeRows()
        setupNetworkConnectivityListener()
    }

    private fun setupNetworkConnectivityListener() {
        try {
            val cm = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            connectivityManager = cm
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onLost(network: Network) {
                    if (!_isPlaying.value) return
                    val position = _currentPositionMs.value
                    viewModelScope.launch {
                        delay(400)
                        if (hasInternet() || !_isPlaying.value) return@launch
                        wasPlayingBeforeNetworkLost = true
                        savedPositionBeforeLost = position
                        _networkStatusMessage.value = "⚠️ Internet कट गया! कनेक्शन आते ही गाना वहीं से चालू होगा 📡"
                    }
                }

                override fun onAvailable(network: Network) {
                    if (!wasPlayingBeforeNetworkLost) return
                    viewModelScope.launch {
                        delay(600)
                        if (!wasPlayingBeforeNetworkLost || !hasInternet()) return@launch
                        wasPlayingBeforeNetworkLost = false
                        val song = _currentSong.value ?: return@launch
                        _networkStatusMessage.value = "⚡ Internet वापस आ गया! गाना वहीं से जारी हो रहा है..."
                        if (song.id.startsWith("yt_")) {
                            startYoutube(song, autoPlay = true)
                            streamPlayerManager.seekTo(savedPositionBeforeLost)
                        } else {
                            player.seekTo(savedPositionBeforeLost)
                            player.play()
                            _isPlaying.value = true
                        }
                        publishPlayback(true)
                    }
                }
            }
            networkCallback = callback
            cm?.registerDefaultNetworkCallback(callback)
        } catch (e: Exception) {
            Log.w("MusicViewModel", "Network listener init: ${e.message}")
        }
    }

    private fun setupStreamListener() {
        streamPlayerManager.onVideoEnded = {
            handleSongEnded()
        }

        viewModelScope.launch {
            streamPlayerManager.isPlaying.collect { streamPlaying ->
                val song = _currentSong.value
                if (song != null && song.id.startsWith("yt_")) {
                    if (streamPlaying || !streamPlayerManager.wantsBackgroundPlayback()) {
                        _isPlaying.value = streamPlaying
                        publishPlayback(streamPlaying)
                    }
                    if (streamPlaying && _isFlashSyncEnabled.value) {
                        flashSyncManager.startSync(128)
                    } else if (!streamPlaying && !player.isPlaying) {
                        flashSyncManager.stopSync()
                    }
                }
            }
        }

        viewModelScope.launch {
            streamPlayerManager.currentTimeMs.collect { streamPos ->
                val song = _currentSong.value
                if (song != null && song.id.startsWith("yt_")) {
                    _currentPositionMs.value = streamPos
                    val currentLyrics = _lyrics.value
                    if (currentLyrics.isNotEmpty()) {
                        updateLyricIndex(streamPos)
                    }
                }
            }
        }

        viewModelScope.launch {
            streamPlayerManager.durationMs.collect { streamDur ->
                val song = _currentSong.value
                if (song != null && song.id.startsWith("yt_") && streamDur > 1000L) {
                    _durationMs.value = streamDur
                }
            }
        }
    }

    private fun handleSongEnded() {
        when (_repeatMode.value) {
            2 -> restartCurrentSong()
            1 -> playNextSong()
            else -> {
                val songs = currentQueue()
                val index = songs.indexOfFirst { it.id == _currentSong.value?.id }
                if (index in 0 until songs.lastIndex) {
                    playNextSong()
                } else {
                    _isPlaying.value = false
                }
            }
        }
    }

    private fun restartCurrentSong() {
        seekTo(0)
        val song = _currentSong.value ?: return
        if (song.id.startsWith("yt_")) {
            streamPlayerManager.play()
        } else {
            player.play()
        }
        _isPlaying.value = true
    }

    private fun currentQueue(): List<SongEntity> {
        val queued = playbackQueue.value
        if (queued.isNotEmpty()) return queued
        val library = allSongs.value
        if (library.isNotEmpty()) return library
        return _trendingSongs.value
    }

    private fun loadTrendingSongs() {
        val cached = repository.peekTrending()
        if (cached.isNotEmpty()) {
            _trendingSongs.value = cached
            if (playbackQueue.value.isEmpty()) playbackQueue.value = cached
        }
        viewModelScope.launch {
            try {
                val trending = repository.getTrendingSongs()
                if (trending.isEmpty()) return@launch
                _trendingSongs.value = trending
                if (playbackQueue.value.isEmpty() || playbackQueue.value.map { it.id } == cached.map { it.id }) {
                    playbackQueue.value = trending
                }
            } catch (e: Exception) {
                Log.w("MusicViewModel", "Error loading trending: ${e.message}")
            }
        }
    }

    fun searchMusic(query: String) {
        searchJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            _searchError.value = null
            return
        }

        searchJob = viewModelScope.launch {
            _isSearching.value = true
            _searchError.value = null
            try {
                // 1. Gather matching local songs
                val localMatches = repository.searchLocalSongs(trimmed)

                // 2. Search YouTube / Online songs (with full-length YouTube audio)
                val onlineMatches = repository.searchSongsOnline(trimmed)

                // 3. Combine without duplicate entries
                val combined = mutableListOf<SongEntity>()
                combined.addAll(localMatches)
                val existingKeys = localMatches.map { "${it.title.lowercase()}_${it.artist.lowercase()}" }.toSet()

                for (song in onlineMatches) {
                    val key = "${song.title.lowercase()}_${song.artist.lowercase()}"
                    if (key !in existingKeys && combined.none { it.id == song.id }) {
                        combined.add(song)
                    }
                }

                _searchResults.value = combined
                if (combined.isNotEmpty()) {
                    rememberSearch(trimmed)
                    _homeRows.value = buildHomeRows(allSongs.value)
                }
                if (combined.isEmpty()) {
                    _searchError.value = "No songs found for '$trimmed'"
                }
            } catch (e: Exception) {
                _searchError.value = "Search error: ${e.localizedMessage ?: "Unknown error"}"
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun playSongFromAnywhere(
        song: SongEntity,
        autoPlay: Boolean = true,
        queue: List<SongEntity>? = null
    ) {
        if (!queue.isNullOrEmpty()) {
            playbackQueue.value = queue
        }
        viewModelScope.launch {
            // Save to database so it exists in Room and persists across app restarts
            val existing = repository.getSongById(song.id)
            if (existing == null) {
                repository.insertCustomSong(song)
            }

            val allCurrent = allSongs.value
            val existingIndex = allCurrent.indexOfFirst { it.id == song.id }
            val indexToUse = if (existingIndex >= 0) existingIndex else allCurrent.size

            selectSong(song, indexToUse, autoPlay = autoPlay)
        }
    }

    private fun fetchAndApplyLyrics(song: SongEntity) {
        viewModelScope.launch {
            val fetchedLyrics = repository.fetchLyrics(song.artist, song.title)
            if (!fetchedLyrics.isNullOrBlank()) {
                repository.updateSongLyrics(song.id, fetchedLyrics)
                if (_currentSong.value?.id == song.id) {
                    _currentSong.value = _currentSong.value?.copy(lyricsLrc = fetchedLyrics)
                    _lyrics.value = LyricsParser.parse(fetchedLyrics)
                    _lyricsStatus.value = ""
                }
            } else if (_currentSong.value?.id == song.id) {
                _lyrics.value = emptyList()
                _lyricsStatus.value = "Lyrics not available for this song"
            }
        }
    }

    private fun needsRealLyrics(lrc: String): Boolean {
        if (lrc.isBlank()) return true
        return lrc.contains("High Fidelity") ||
            lrc.contains("Feel the rhythm") ||
            lrc.contains("Now playing:") ||
            lrc.contains("Gentle Piano") ||
            lrc.contains("Enjoying the vibe")
    }

    private fun setupPlayerListener() {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                val song = _currentSong.value
                if (song != null && !song.id.startsWith("yt_")) {
                    _isPlaying.value = playing
                    if (playing && _isFlashSyncEnabled.value) {
                        flashSyncManager.startSync(128)
                    } else if (!playing && !streamPlayerManager.isPlaying.value) {
                        flashSyncManager.stopSync()
                    }
                }
            }

            override fun onPlaybackStateChanged(state: Int) {
                val song = _currentSong.value
                if (song != null && !song.id.startsWith("yt_")) {
                    if (state == Player.STATE_READY) {
                        _durationMs.value = player.duration.coerceAtLeast(1L)
                        applyEqualizer()
                    } else if (state == Player.STATE_ENDED) {
                        handleSongEnded()
                    }
                }
            }
        })
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (isActive) {
                val song = _currentSong.value
                if (song != null && !song.id.startsWith("yt_") && player.isPlaying) {
                    val pos = player.currentPosition
                    _currentPositionMs.value = pos
                    val dur = player.duration
                    if (dur > 0) _durationMs.value = dur

                    // Update active lyric index
                    val currentLyrics = _lyrics.value
                    if (currentLyrics.isNotEmpty()) updateLyricIndex(pos)
                }
                delay(250)
            }
        }
    }

    fun selectSong(
        song: SongEntity,
        index: Int,
        autoPlay: Boolean = true,
        queue: List<SongEntity>? = null
    ) {
        if (!queue.isNullOrEmpty()) {
            playbackQueue.value = queue
        } else if (playbackQueue.value.none { it.id == song.id }) {
            playbackQueue.value = allSongs.value.ifEmpty { listOf(song) }
        }
        _currentSong.value = song
        _currentSongIndex.value = index
        repository.rememberRecent(song)
        _recentSongs.value = repository.recentSongs()
        _lyricOffsetMs.value = playerPrefs.getInt("offset_${song.id}", 0)
        if (needsRealLyrics(song.lyricsLrc)) {
            _lyrics.value = emptyList()
            _lyricsStatus.value = "Finding lyrics…"
        } else {
            _lyrics.value = LyricsParser.parse(song.lyricsLrc)
            _lyricsStatus.value = ""
        }
        _currentVibeUri.value = song.vibeVideoUri
        _activeLyricIndex.value = 0
        _currentPositionMs.value = 0L
        _durationMs.value = song.durationMs.coerceAtLeast(1000L)

        // Observe comments for this song
        observeCommentsForSong(song.id)

        if (song.id.startsWith("yt_")) {
            startYoutube(song, autoPlay)
        } else {
            // Standard media: pause Stream, play via ExoPlayer
            streamPlayerManager.setKeepPlayingInBackground(false)
            streamPlayerManager.pause()
            _isVideoMode.value = false
            try {
                val mediaItem = MediaItem.fromUri(mediaUri(song.audioUrl))
                player.setMediaItem(mediaItem)
                player.prepare()
                if (autoPlay) {
                    player.play()
                    _isPlaying.value = true
                } else {
                    _isPlaying.value = false
                }
            } catch (e: Exception) {
                Log.e("MusicViewModel", "Error playing audio: ${e.message}", e)
            }
        }

        if (needsRealLyrics(song.lyricsLrc)) {
            fetchAndApplyLyrics(song)
        }
        applyEqualizer()
        publishPlayback(autoPlay)
    }

    private fun startYoutube(song: SongEntity, autoPlay: Boolean) {
        if (player.isPlaying) player.stop()
        val videoId = song.id.removePrefix("yt_")
        if (!autoPlay) {
            streamPlayerManager.setKeepPlayingInBackground(false)
            streamPlayerManager.pause()
            _isPlaying.value = false
            return
        }
        _streamVisible.value = true
        if (streamPlayerManager.activeVideoId() == videoId && streamPlayerManager.hasWebView()) {
            streamPlayerManager.setKeepPlayingInBackground(true)
            streamPlayerManager.play()
        } else {
            streamPlayerManager.loadAndPlay(videoId, true)
        }
        if (_isVolumeBoosterEnabled.value) streamPlayerManager.setBoost(true)
        _isPlaying.value = true
    }

    private fun hasInternet(): Boolean {
        val cm = connectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun publishPlayback(playing: Boolean = _isPlaying.value) {
        val song = _currentSong.value ?: return
        if (playing && song.id.startsWith("yt_")) {
            streamPlayerManager.setKeepPlayingInBackground(true)
        }
        PlaybackService.update(
            context = getApplication(),
            title = song.title,
            artist = song.artist,
            playing = playing,
            artUrl = song.albumArtUrl,
            songId = song.id,
            positionMs = _currentPositionMs.value
        )
    }

    private fun resumePlayback() {
        val song = _currentSong.value ?: return
        if (song.id.startsWith("yt_")) {
            startYoutube(song, autoPlay = true)
        } else if (!player.isPlaying) {
            player.play()
            _isPlaying.value = true
        }
        publishPlayback(true)
    }

    private fun pausePlayback() {
        val song = _currentSong.value ?: return
        wasPlayingBeforeNetworkLost = false
        if (song.id.startsWith("yt_")) {
            streamPlayerManager.setKeepPlayingInBackground(false)
            streamPlayerManager.pause()
        } else {
            player.pause()
        }
        _isPlaying.value = false
        publishPlayback(false)
    }

    private fun observeCommentsForSong(songId: String) {
        commentsJob?.cancel()
        commentsJob = viewModelScope.launch {
            repository.getCommentsForSong(songId).collect { list ->
                _commentsForCurrentSong.value = list
            }
        }
    }

    fun togglePlayPause() {
        val song = _currentSong.value ?: return
        if (song.id.startsWith("yt_")) {
            val playing = _isPlaying.value || streamPlayerManager.isPlaying.value
            if (playing) pausePlayback() else resumePlayback()
            return
        } else if (player.isPlaying) {
            player.pause()
            _isPlaying.value = false
        } else {
            player.play()
            _isPlaying.value = true
        }
        publishPlayback(_isPlaying.value)
    }

    fun playNextSong() {
        val songs = currentQueue()
        if (songs.isEmpty()) return
        val currentIndex = songs.indexOfFirst { it.id == _currentSong.value?.id }.let { found ->
            if (found >= 0) found else _currentSongIndex.value.coerceIn(0, songs.lastIndex)
        }
        val nextIndex = if (_isShuffle.value) {
            songs.indices.filter { it != currentIndex }.randomOrNull() ?: 0
        } else {
            (currentIndex + 1) % songs.size
        }
        selectSong(songs[nextIndex], nextIndex, autoPlay = true)
    }

    fun playPreviousSong() {
        val songs = currentQueue()
        if (songs.isEmpty()) return
        val currentIndex = songs.indexOfFirst { it.id == _currentSong.value?.id }.let { found ->
            if (found >= 0) found else _currentSongIndex.value.coerceIn(0, songs.lastIndex)
        }
        val prevIndex = if (_currentPositionMs.value > 3000) {
            currentIndex
        } else if (currentIndex - 1 < 0) {
            songs.lastIndex
        } else {
            currentIndex - 1
        }
        selectSong(songs[prevIndex], prevIndex, autoPlay = true)
    }

    fun seekTo(positionMs: Long) {
        _currentPositionMs.value = positionMs
        val song = _currentSong.value
        if (song != null && song.id.startsWith("yt_")) {
            streamPlayerManager.seekTo(positionMs)
        } else {
            player.seekTo(positionMs)
        }
    }

    fun toggleShuffle() {
        _isShuffle.value = !_isShuffle.value
        playerPrefs.edit().putBoolean("shuffle", _isShuffle.value).apply()
    }

    fun toggleRepeat() {
        _repeatMode.value = (_repeatMode.value + 1) % 3
        playerPrefs.edit().putInt("repeat", _repeatMode.value).apply()
    }

    fun toggleLikeCurrentSong() {
        val song = _currentSong.value ?: return
        toggleLikeSong(song)
    }

    fun createPlaylist(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch { repository.createPlaylist(trimmed) }
    }

    fun addSongToPlaylist(playlistId: Long, song: SongEntity) {
        viewModelScope.launch { repository.addSongToPlaylist(playlistId, song) }
    }

    fun loadPlaylistSongs(playlist: PlaylistEntity) {
        viewModelScope.launch {
            _openPlaylistSongs.value = repository.songsInPlaylist(playlist)
        }
    }

    fun playPlaylist(playlist: PlaylistEntity) {
        viewModelScope.launch {
            val songs = repository.songsInPlaylist(playlist)
            if (songs.isNotEmpty()) {
                _openPlaylistSongs.value = songs
                selectSong(songs.first(), 0, autoPlay = true, queue = songs)
            }
        }
    }

    fun toggleLikeSong(song: SongEntity) {
        viewModelScope.launch {
            val existing = repository.getSongById(song.id)
            val newLikedState = !song.isLiked
            if (existing == null) {
                repository.insertCustomSong(song.copy(isLiked = newLikedState))
            } else {
                repository.toggleLike(song.id, song.isLiked)
            }
            if (_currentSong.value?.id == song.id) {
                _currentSong.value = _currentSong.value?.copy(isLiked = newLikedState)
            }
            _searchResults.value = _searchResults.value.map {
                if (it.id == song.id) it.copy(isLiked = newLikedState) else it
            }
            _trendingSongs.value = _trendingSongs.value.map {
                if (it.id == song.id) it.copy(isLiked = newLikedState) else it
            }
        }
    }

    // Flash sync toggle
    fun toggleFlashSync() {
        val newState = !_isFlashSyncEnabled.value
        _isFlashSyncEnabled.value = newState
        if (newState && _isPlaying.value) {
            flashSyncManager.startSync(128)
        } else {
            flashSyncManager.stopSync()
        }
    }

    // Volume Booster toggle
    fun toggleVolumeBooster() {
        val newState = !_isVolumeBoosterEnabled.value
        _isVolumeBoosterEnabled.value = newState
        player.volume = if (newState) 1.5f else 1.0f
        streamPlayerManager.setBoost(newState)
    }

    // Vibe Creator: Save custom video vibe associated with current song
    fun saveCustomSongVibe(
        songId: String,
        title: String,
        videoUri: String,
        startTrimMs: Long,
        endTrimMs: Long,
        filterType: String
    ) {
        viewModelScope.launch {
            val vibe = VibeEntity(
                songId = songId,
                title = title.ifBlank { "Custom Vibe" },
                videoUri = videoUri,
                startTrimMs = startTrimMs,
                endTrimMs = endTrimMs,
                filterType = filterType,
                creatorName = "You"
            )
            repository.saveVibe(vibe)
            repository.updateSongVibe(songId, videoUri)

            // Update current state if it's the currently playing song
            if (_currentSong.value?.id == songId) {
                _currentVibeUri.value = videoUri
                _currentVibeFilter.value = filterType
                _currentSong.value = _currentSong.value?.copy(vibeVideoUri = videoUri)
            }
        }
    }

    fun applyVibeToCurrent(vibe: VibeEntity) {
        _currentVibeUri.value = vibe.videoUri
        _currentVibeFilter.value = vibe.filterType
    }

    fun setVibeFilter(filter: String) {
        _currentVibeFilter.value = filter
    }

    fun clearVibeVideo() {
        _currentVibeUri.value = null
    }

    // Comments Sheet
    fun setCommentsSheetOpen(open: Boolean) {
        _isCommentsSheetOpen.value = open
    }

    fun addCommentToCurrentSong(content: String) {
        val song = _currentSong.value ?: return
        if (content.isBlank()) return
        viewModelScope.launch {
            val newComment = CommentEntity(
                songId = song.id,
                userName = "MusicLover",
                userAvatarColor = 0xFFFF2A6DL,
                content = content.trim(),
                songTimestampMs = _currentPositionMs.value,
                likesCount = 0,
                isLikedByMe = false
            )
            repository.addComment(newComment)
        }
    }

    fun toggleCommentLike(commentId: Long) {
        viewModelScope.launch {
            repository.toggleCommentLike(commentId)
        }
    }

    // Lyric Poster
    fun openLyricPosterDialog(line: LyricLine?) {
        _selectedPosterLyric.value = line ?: _lyrics.value.getOrNull(_activeLyricIndex.value)
        _isPosterDialogOpen.value = true
    }

    fun closeLyricPosterDialog() {
        _isPosterDialogOpen.value = false
    }

    private fun updateLyricIndex(positionMs: Long) {
        val adjusted = positionMs + _lyricOffsetMs.value
        val idx = _lyrics.value.indexOfLast { it.timeMs <= adjusted }
        _activeLyricIndex.value = if (idx >= 0) idx else 0
    }

    fun nudgeLyricOffset(deltaMs: Int) {
        val song = _currentSong.value ?: return
        val next = (_lyricOffsetMs.value + deltaMs).coerceIn(-10000, 10000)
        _lyricOffsetMs.value = next
        playerPrefs.edit().putInt("offset_${song.id}", next).apply()
        updateLyricIndex(_currentPositionMs.value)
    }

    fun seekToLyric(timeMs: Long) {
        seekTo((timeMs - _lyricOffsetMs.value).coerceAtLeast(0))
    }

    fun removeFromQueue(songId: String) {
        val wasCurrent = _currentSong.value?.id == songId
        val keepPlaying = wasCurrent && _isPlaying.value
        val remaining = playbackQueue.value.filter { it.id != songId }
        playbackQueue.value = remaining
        if (!wasCurrent) return
        val next = remaining.firstOrNull()
        if (next == null) {
            pausePlayback()
            return
        }
        selectSong(next, 0, autoPlay = keepPlaying, queue = remaining)
    }

    fun moveInQueue(index: Int, direction: Int) {
        val items = playbackQueue.value.toMutableList()
        val target = index + direction
        if (index !in items.indices || target !in items.indices) return
        val item = items.removeAt(index)
        items.add(target, item)
        playbackQueue.value = items
    }

    fun addToQueue(song: SongEntity) {
        if (playbackQueue.value.any { it.id == song.id }) {
            _networkStatusMessage.value = "Yeh gaana queue mein pehle se hai"
            return
        }
        playbackQueue.value = playbackQueue.value + song
        _networkStatusMessage.value = "Queue mein add ho gaya"
    }

    fun startSleepTimer(minutes: Int) {
        sleepJob?.cancel()
        _sleepMinutesLeft.value = minutes
        sleepJob = viewModelScope.launch {
            var left = minutes
            while (left > 0) {
                delay(60_000)
                left -= 1
                _sleepMinutesLeft.value = left
            }
            pausePlayback()
        }
    }

    fun cancelSleepTimer() {
        sleepJob?.cancel()
        _sleepMinutesLeft.value = 0
    }

    fun setEqualizerPreset(preset: String) {
        _equalizerPreset.value = preset
        if (preset == "Off") {
            appEqualizer.release()
            return
        }
        val song = _currentSong.value
        if (song?.id?.startsWith("yt_") == true) {
            appEqualizer.release()
            _networkStatusMessage.value = "Equalizer saved audio par chalta hai. YouTube playback par nahi lagta."
            return
        }
        applyEqualizer()
    }

    private fun applyEqualizer() {
        val preset = _equalizerPreset.value
        if (preset == "Off") {
            appEqualizer.release()
            return
        }
        val song = _currentSong.value
        if (song == null || song.id.startsWith("yt_")) {
            appEqualizer.release()
            return
        }
        val session = player.audioSessionId
        if (session == C.AUDIO_SESSION_ID_UNSET) return
        appEqualizer.attach(session)
        appEqualizer.apply(preset)
    }

    private fun mediaUri(audioUrl: String): Uri {
        return if (audioUrl.startsWith("/")) Uri.fromFile(java.io.File(audioUrl)) else Uri.parse(audioUrl)
    }

    fun saveCurrentOffline() {
        val song = _currentSong.value ?: return
        viewModelScope.launch {
            val message = repository.saveForOffline(song)
            _networkStatusMessage.value = message
            val updated = repository.getSongById(song.id)
            if (updated != null && _currentSong.value?.id == song.id) {
                _currentSong.value = updated
            }
        }
    }

    fun moveInPlaylist(playlistId: Long, index: Int, direction: Int) {
        viewModelScope.launch {
            repository.movePlaylistSong(playlistId, index, index + direction)
            val playlist = repository.getPlaylist(playlistId) ?: return@launch
            _openPlaylistSongs.value = repository.songsInPlaylist(playlist)
        }
    }

    private fun rememberSearch(query: String) {
        val next = listOf(query) + _searchHistory.value.filter { !it.equals(query, ignoreCase = true) }
        _searchHistory.value = next.take(8)
        playerPrefs.edit().putString("search_history", _searchHistory.value.joinToString("\n")).apply()
    }

    private fun loadHomeRows() {
        viewModelScope.launch {
            allSongs.collect { local ->
                _homeRows.value = buildHomeRows(local)
            }
        }
    }

    private fun buildHomeRows(local: List<SongEntity>): List<Pair<String, List<SongEntity>>> {
        val specs = listOf(
            Triple("Romantic", listOf("Romantic Hits", "Romantic Hindi Songs", "Romantic"), listOf("romance", "romantic", "soulful", "heartfelt")),
            Triple("Punjabi", listOf("Punjabi Beats", "Punjabi Hits", "Punjabi"), listOf("punjabi")),
            Triple("Arijit Singh", listOf("Arijit Singh"), listOf("arijit"))
        )
        return specs.mapNotNull { (title, queries, needles) ->
            val cached = repository.peekSearches(queries)
            val fromLibrary = local.filter { song ->
                val blob = "${song.title} ${song.artist} ${song.album} ${song.genre} ${song.mood}".lowercase()
                needles.any { blob.contains(it) }
            }
            val songs = (cached + fromLibrary).distinctBy { it.id }.take(8)
            if (songs.isEmpty()) null else title to songs
        }
    }

    override fun onCleared() {
        super.onCleared()
        appEqualizer.release()
        sleepJob?.cancel()
        try {
            networkCallback?.let { connectivityManager?.unregisterNetworkCallback(it) }
        } catch (_: Exception) {}
        progressJob?.cancel()
        commentsJob?.cancel()
        flashSyncManager.stopSync()
        player.release()
        streamPlayerManager.release()
        PlaybackService.commands.onPlay = null
        PlaybackService.commands.onPause = null
        PlaybackService.commands.onNext = null
        PlaybackService.commands.onPrevious = null
        PlaybackService.commands.onSeek = null
        PlaybackService.stop(getApplication())
    }
}
