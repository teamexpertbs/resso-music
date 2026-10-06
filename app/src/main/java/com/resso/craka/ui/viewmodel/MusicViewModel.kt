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
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import android.media.audiofx.LoudnessEnhancer
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.AudioSink
import com.resso.craka.player.Spatial8DAudioProcessor
import com.resso.craka.player.BeatDetectionAudioProcessor
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
import com.resso.craka.util.SongDeduplicator
import com.resso.craka.util.deduplicate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
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
    val eightDAudioProcessor = Spatial8DAudioProcessor()
    val beatDetectionAudioProcessor = BeatDetectionAudioProcessor {
        flashSyncManager.pulseOnce()
    }
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var loudnessSessionId: Int = C.AUDIO_SESSION_ID_UNSET

    private val player: ExoPlayer = run {
        val renderersFactory = object : DefaultRenderersFactory(application) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink? {
                return DefaultAudioSink.Builder(context)
                    .setAudioProcessors(arrayOf(beatDetectionAudioProcessor, eightDAudioProcessor))
                    .build()
            }
        }
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(15000)
        val dataSourceFactory = DefaultDataSource.Factory(application, httpDataSourceFactory)
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

        ExoPlayer.Builder(application, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
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
    }

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
    private val playedHistoryIds = java.util.Collections.synchronizedSet(
        LinkedHashSet(playerPrefs.getStringSet("played_history_ids", emptySet()) ?: emptySet())
    )
    private val playedHistoryKeys = java.util.Collections.synchronizedSet(
        LinkedHashSet(playerPrefs.getStringSet("played_history_keys", emptySet()) ?: emptySet())
    )
    private val playbackBackStack = java.util.ArrayDeque<SongEntity>()
    private val playbackForwardStack = java.util.ArrayDeque<SongEntity>()
    private var isNavigatingHistory = false

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

    private val _firestoreSearchResults = MutableStateFlow<List<SongEntity>>(emptyList())
    val firestoreSearchResults: StateFlow<List<SongEntity>> = _firestoreSearchResults.asStateFlow()

    private val _isFirestoreSearching = MutableStateFlow(false)
    val isFirestoreSearching: StateFlow<Boolean> = _isFirestoreSearching.asStateFlow()

    private var firestoreSearchJob: Job? = null
    private var activeSearchRequestId: Long = 0L

    private val _searchFilterType = MutableStateFlow(com.resso.craka.ui.components.SearchFilterType.ALL)
    val searchFilterType: StateFlow<com.resso.craka.ui.components.SearchFilterType> = _searchFilterType.asStateFlow()

    fun setSearchFilterType(type: com.resso.craka.ui.components.SearchFilterType) {
        _searchFilterType.value = type
    }

    fun searchFirestoreRealtime(query: String) {
        firestoreSearchJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            _firestoreSearchResults.value = emptyList()
            _isFirestoreSearching.value = false
            return
        }
        firestoreSearchJob = viewModelScope.launch {
            try {
                repository.searchSongsInFirestoreRealtime(trimmed).collect { list ->
                    _firestoreSearchResults.value = list
                    _isFirestoreSearching.value = false
                }
            } catch (_: Throwable) {
                // Graceful fallback if Cloud Firestore API is disabled or permissions not granted
                _firestoreSearchResults.value = emptyList()
                _isFirestoreSearching.value = false
            }
        }
    }

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

    private val _is8DAudioEnabled = MutableStateFlow(false)
    val is8DAudioEnabled: StateFlow<Boolean> = _is8DAudioEnabled.asStateFlow()

    private val _isCrossfadeEnabled = MutableStateFlow(true)
    val isCrossfadeEnabled: StateFlow<Boolean> = _isCrossfadeEnabled.asStateFlow()

    private var isCrossfading = false
    private var crossfadeTriggeredForCurrentSong = false

    private val appEqualizer = AppEqualizer()
    private val shuffleEngine = com.resso.craka.player.PlaybackShuffleEngine()
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
                        player.seekTo(savedPositionBeforeLost)
                        player.play()
                        _isPlaying.value = true
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

    private fun isUsingStreamPlayer(song: SongEntity?): Boolean {
        if (song == null) return false
        return song.audioUrl.isBlank() && song.id.startsWith("yt_")
    }

    private fun setupStreamListener() {
        streamPlayerManager.onVideoEnded = {
            handleSongEnded()
        }

        viewModelScope.launch {
            streamPlayerManager.isPlaying.collect { streamPlaying ->
                val song = _currentSong.value
                if (isUsingStreamPlayer(song)) {
                    if (streamPlaying || !streamPlayerManager.wantsBackgroundPlayback()) {
                        _isPlaying.value = streamPlaying
                        publishPlayback(streamPlaying)
                    }
                    if (streamPlaying && _isFlashSyncEnabled.value) {
                        flashSyncManager.startFallbackRhythm()
                    } else if (!streamPlaying && !player.isPlaying) {
                        flashSyncManager.stopSync()
                    }
                }
            }
        }

        viewModelScope.launch {
            streamPlayerManager.currentTimeMs.collect { streamPos ->
                val song = _currentSong.value
                if (isUsingStreamPlayer(song)) {
                    _currentPositionMs.value = streamPos
                    val currentLyrics = _lyrics.value
                    if (currentLyrics.isNotEmpty()) {
                        updateLyricIndex(streamPos)
                    }

                    // Seamless Crossfade & DJ Transition in last 3.5s
                    if (_isCrossfadeEnabled.value && _isPlaying.value && _durationMs.value > 15_000L && !isCrossfading && !crossfadeTriggeredForCurrentSong) {
                        val remainingMs = _durationMs.value - streamPos
                        if (remainingMs in 500L..3500L) {
                            crossfadeTriggeredForCurrentSong = true
                            triggerCrossfade()
                        }
                    }
                }
            }
        }

        viewModelScope.launch {
            streamPlayerManager.durationMs.collect { streamDur ->
                val song = _currentSong.value
                if (isUsingStreamPlayer(song) && streamDur > 1000L) {
                    _durationMs.value = streamDur
                }
            }
        }
    }

    private fun handleSongEnded() {
        if (isCrossfading) {
            // Already transitioning smoothly via crossfade
            return
        }
        if (_repeatMode.value == 2) {
            restartCurrentSong()
        } else {
            playNextSong()
        }
    }

    private fun restartCurrentSong() {
        seekTo(0)
        val song = _currentSong.value ?: return
        if (isUsingStreamPlayer(song)) {
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
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val trending = repository.getTrendingSongs()
                if (trending.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        _trendingSongs.value = trending
                        if (playbackQueue.value.isEmpty()) {
                            val (shuffledQueue, _) = if (_isShuffle.value) {
                                shuffleEngine.onNewSongsFetched(emptyList(), trending, null)
                            } else {
                                trending to 0
                            }
                            playbackQueue.value = shuffledQueue
                            restoreLastPlaybackOrInitial(shuffledQueue)
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        if (playbackQueue.value.isEmpty()) {
                            restoreLastPlaybackOrInitial(emptyList())
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("MusicViewModel", "Error loading trending: ${e.message}")
                withContext(Dispatchers.Main) {
                    if (playbackQueue.value.isEmpty()) {
                        restoreLastPlaybackOrInitial(emptyList())
                    }
                }
            }
        }
    }

    private fun restoreLastPlaybackOrInitial(trendingList: List<SongEntity>) {
        if (_currentSong.value != null) return
        viewModelScope.launch {
            val pool = (trendingList + _trendingSongs.value + allSongs.value).deduplicate()
            if (pool.isEmpty()) return@launch

            // Always pick a brand new song that has not been heard yet in previous sessions!
            val unplayed = pool.filter { 
                it.id !in playedHistoryIds && SongDeduplicator.canonicalKey(it) !in playedHistoryKeys 
            }
            val songToSelect = unplayed.shuffled().firstOrNull()
                ?: pool.shuffled().firstOrNull()
                ?: pool.first()

            if (_currentSong.value == null) {
                val otherPool = pool.filter { !SongDeduplicator.isSameOrDuplicate(it, songToSelect) }.shuffled()
                val randomizedQueue = (listOf(songToSelect) + otherPool).deduplicate()
                selectSong(songToSelect, 0, autoPlay = false, queue = randomizedQueue)
            }
        }
    }

    fun scanDeviceAudio() {
        viewModelScope.launch {
            repository.scanDeviceAudio()
        }
    }

    fun searchMusic(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            searchJob?.cancel()
            _searchResults.value = emptyList()
            _isSearching.value = false
            _searchError.value = null
            return
        }

        searchJob?.cancel()
        val requestId = ++activeSearchRequestId

        searchJob = viewModelScope.launch {
            _isSearching.value = true
            _searchError.value = null

            // 1. Immediately display local matching songs without waiting for network
            val localMatches = repository.searchLocalSongs(trimmed).filter { it.audioUrl.startsWith("http") }
            if (localMatches.isNotEmpty() && requestId == activeSearchRequestId) {
                _searchResults.value = localMatches
            }

            // 2. Query online music catalog off the main thread
            try {
                val onlineMatches = withContext(Dispatchers.IO) {
                    repository.searchSongsOnline(trimmed).filter { it.audioUrl.startsWith("http") }
                }

                // Check if this response is still the latest active search
                if (requestId != activeSearchRequestId || !isActive) return@launch

                val combined = mutableListOf<SongEntity>()
                val seenIds = mutableSetOf<String>()

                for (song in onlineMatches) {
                    if (seenIds.add(song.id)) {
                        combined.add(song)
                    }
                }
                for (song in localMatches) {
                    if (seenIds.add(song.id)) {
                        combined.add(song)
                    }
                }

                _searchResults.value = combined
                if (combined.isNotEmpty()) {
                    rememberSearch(trimmed)
                    _homeRows.value = buildHomeRows(allSongs.value)
                } else {
                    _searchError.value = "No songs found for '$trimmed'"
                }
            } catch (e: Exception) {
                if (requestId == activeSearchRequestId) {
                    if (_searchResults.value.isEmpty()) {
                        _searchError.value = "Unable to load songs for '$trimmed'. Please check connection and retry."
                    }
                }
            } finally {
                if (requestId == activeSearchRequestId) {
                    _isSearching.value = false
                }
            }
        }
    }

    fun playSongFromAnywhere(
        song: SongEntity,
        autoPlay: Boolean = true,
        queue: List<SongEntity>? = null
    ) {
        val effectiveQueue = if (!queue.isNullOrEmpty()) {
            queue
        } else {
            val fallback = (_trendingSongs.value + allSongs.value).filter { it.id != song.id }
            listOf(song) + fallback
        }
        playbackQueue.value = effectiveQueue

        viewModelScope.launch {
            // Save to database so it exists in Room and persists across app restarts
            val existing = repository.getSongById(song.id)
            if (existing == null) {
                repository.insertCustomSong(song)
            }

            val queueIndex = effectiveQueue.indexOfFirst { it.id == song.id }.let {
                if (it >= 0) it else 0
            }

            selectSong(song, queueIndex, autoPlay = autoPlay, queue = effectiveQueue)

            // Autoplay buffer: Pre-fetch upcoming similar tracks
            ensureAutoplayQueue(song)
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
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                if (audioSessionId != C.AUDIO_SESSION_ID_UNSET && audioSessionId > 0) {
                    if (_isVolumeBoosterEnabled.value) {
                        applyVolumeBooster(audioSessionId)
                    }
                    if (_equalizerPreset.value != "Off") {
                        applyEqualizer(audioSessionId)
                    }
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                val song = _currentSong.value
                if (!isUsingStreamPlayer(song)) {
                    _isPlaying.value = playing
                    if (playing && _isFlashSyncEnabled.value) {
                        flashSyncManager.startDirectSync()
                        beatDetectionAudioProcessor.isEnabled = true
                    } else if (!playing && !streamPlayerManager.isPlaying.value) {
                        beatDetectionAudioProcessor.isEnabled = false
                        flashSyncManager.stopSync()
                    }
                    publishPlayback(playing)
                }
            }

            override fun onPlaybackStateChanged(state: Int) {
                val song = _currentSong.value
                if (!isUsingStreamPlayer(song)) {
                    if (state == Player.STATE_READY) {
                        _durationMs.value = player.duration.coerceAtLeast(1L)
                        applyEqualizer()
                        applyVolumeBooster()
                    } else if (state == Player.STATE_ENDED) {
                        handleSongEnded()
                    }
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                Log.e("MusicViewModel", "ExoPlayer playback error: ${error.errorCodeName} - ${error.message}", error)
                val current = _currentSong.value
                val audioUrl = current?.audioUrl.orEmpty()

                // Automatic bitrate fallback for 404 / HTTP errors (try 160kbps, then 96kbps)
                if (audioUrl.contains("_320.mp4")) {
                    val fallback160 = audioUrl.replace("_320.mp4", "_160.mp4")
                    _currentSong.value = current?.copy(audioUrl = fallback160)
                    try {
                        player.setMediaItem(MediaItem.fromUri(mediaUri(fallback160)))
                        player.prepare()
                        player.play()
                        _isPlaying.value = true
                        return
                    } catch (_: Exception) {}
                } else if (audioUrl.contains("_160.mp4")) {
                    val fallback96 = audioUrl.replace("_160.mp4", "_96.mp4")
                    _currentSong.value = current?.copy(audioUrl = fallback96)
                    try {
                        player.setMediaItem(MediaItem.fromUri(mediaUri(fallback96)))
                        player.prepare()
                        player.play()
                        _isPlaying.value = true
                        return
                    } catch (_: Exception) {}
                }

                // If fallback not applicable or failed, smoothly skip to next track without crashing
                _isPlaying.value = false
                viewModelScope.launch {
                    delay(300)
                    playNextSong()
                }
            }
        })
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (isActive) {
                val song = _currentSong.value
                if (!isUsingStreamPlayer(song) && player.isPlaying) {
                    val pos = player.currentPosition
                    _currentPositionMs.value = pos
                    val dur = player.duration
                    if (dur > 0) _durationMs.value = dur

                    // Update active lyric index
                    val currentLyrics = _lyrics.value
                    if (currentLyrics.isNotEmpty()) updateLyricIndex(pos)

                    // Seamless Crossfade & DJ Transition in last 3.5s for normal ExoPlayer songs
                    if (_isCrossfadeEnabled.value && dur > 15_000L && !isCrossfading && !crossfadeTriggeredForCurrentSong) {
                        val remainingMs = dur - pos
                        if (remainingMs in 500L..3500L) {
                            crossfadeTriggeredForCurrentSong = true
                            triggerCrossfade()
                        }
                    }
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
        val currentQueue = playbackQueue.value
        val effectiveQueue = when {
            queue != null && queue.isNotEmpty() -> queue
            currentQueue.any { SongDeduplicator.isSameOrDuplicate(it, song) } -> currentQueue
            currentQueue.isNotEmpty() -> listOf(song) + currentQueue
            else -> listOf(song) + (_trendingSongs.value + allSongs.value)
        }.deduplicate()

        playbackQueue.value = effectiveQueue

        if (!isNavigatingHistory) {
            // User manually selected a new song or tapped a track:
            // Clear forward stack because user initiated a new branch of playback
            synchronized(playbackForwardStack) {
                playbackForwardStack.clear()
            }
            val prev = _currentSong.value
            if (prev != null && !SongDeduplicator.isSameOrDuplicate(prev, song)) {
                synchronized(playbackBackStack) {
                    if (playbackBackStack.isEmpty() || !SongDeduplicator.isSameOrDuplicate(playbackBackStack.peekLast(), prev)) {
                        playbackBackStack.addLast(prev)
                        if (playbackBackStack.size > 50) playbackBackStack.removeFirst()
                    }
                }
            }
        }

        _currentSong.value = song
        _currentSongIndex.value = effectiveQueue.indexOfFirst { SongDeduplicator.isSameOrDuplicate(it, song) }.coerceAtLeast(0)
        crossfadeTriggeredForCurrentSong = false

        // Anti-repeat session memory (never repeats in current or future sessions)
        val canonicalKey = SongDeduplicator.canonicalKey(song)
        synchronized(playedHistoryIds) {
            playedHistoryIds.add(song.id)
            if (canonicalKey.isNotEmpty()) {
                playedHistoryKeys.add(canonicalKey)
            }
            if (playedHistoryIds.size > 250) {
                val toRemove = playedHistoryIds.take(50).toSet()
                playedHistoryIds.removeAll(toRemove)
            }
            if (playedHistoryKeys.size > 250) {
                val toRemove = playedHistoryKeys.take(50).toSet()
                playedHistoryKeys.removeAll(toRemove)
            }
            playerPrefs.edit()
                .putStringSet("played_history_ids", java.util.HashSet(playedHistoryIds))
                .putStringSet("played_history_keys", java.util.HashSet(playedHistoryKeys))
                .apply()
        }

        // Persist recent & last played so app launch restores exact song & artwork
        repository.rememberRecent(song)
        _recentSongs.value = repository.recentSongs()
        playerPrefs.edit()
            .putString("last_played_song_id", song.id)
            .putString("last_played_title", song.title)
            .putString("last_played_artist", song.artist)
            .putString("last_played_album", song.album)
            .putString("last_played_art", song.albumArtUrl)
            .putString("last_played_url", song.audioUrl)
            .putLong("last_played_duration", song.durationMs)
            .putLong("last_position_ms", 0L)
            .apply()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (repository.getSongById(song.id) == null) {
                    repository.insertCustomSong(song)
                }
            } catch (_: Exception) {}
        }

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

        _isVideoMode.value = false
        if (isUsingStreamPlayer(song)) {
            try { player.pause() } catch (_: Exception) {}
            _streamVisible.value = true
            streamPlayerManager.loadAndPlay(song.id.removePrefix("yt_"), autoPlay)
            _isPlaying.value = autoPlay
        } else {
            try {
                streamPlayerManager.pause()
                streamPlayerManager.setKeepPlayingInBackground(false)
            } catch (_: Exception) {}
            _streamVisible.value = false
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
        applyEqualizer(); applyVolumeBooster()
        publishPlayback(autoPlay)

        // Proactively replenish live radio buffer ahead of time
        ensureAutoplayRadioBuffer(song)
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
        if (playing && isUsingStreamPlayer(song)) {
            streamPlayerManager.setKeepPlayingInBackground(true)
        }
        PlaybackService.update(
            context = getApplication(),
            title = song.title,
            artist = song.artist,
            playing = playing,
            artUrl = song.albumArtUrl,
            songId = song.id,
            positionMs = _currentPositionMs.value,
            durationMs = _durationMs.value
        )
    }

    private fun resumePlayback() {
        val song = _currentSong.value ?: return
        if (isUsingStreamPlayer(song)) {
            streamPlayerManager.setKeepPlayingInBackground(true)
            streamPlayerManager.play()
            _isPlaying.value = true
        } else {
            if (!player.isPlaying) {
                player.play()
                _isPlaying.value = true
            }
        }
        publishPlayback(true)
    }

    private fun pausePlayback() {
        val song = _currentSong.value ?: return
        wasPlayingBeforeNetworkLost = false
        if (isUsingStreamPlayer(song)) {
            streamPlayerManager.setKeepPlayingInBackground(false)
            streamPlayerManager.pause()
        } else {
            player.pause()
        }
        _isPlaying.value = false
        playerPrefs.edit()
            .putString("last_played_song_id", song.id)
            .putLong("last_position_ms", _currentPositionMs.value)
            .apply()
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
        if (isUsingStreamPlayer(song)) {
            val playing = _isPlaying.value || streamPlayerManager.isPlaying.value
            if (playing) pausePlayback() else resumePlayback()
            return
        } else if (player.isPlaying) {
            player.pause()
            _isPlaying.value = false
            playerPrefs.edit()
                .putString("last_played_song_id", song.id)
                .putLong("last_position_ms", _currentPositionMs.value)
                .apply()
        } else {
            player.play()
            _isPlaying.value = true
            playerPrefs.edit()
                .putString("last_played_song_id", song.id)
                .putLong("last_position_ms", _currentPositionMs.value)
                .apply()
        }
        publishPlayback(_isPlaying.value)
    }

    fun playNextSong() {
        val rawQueue = playbackQueue.value
        if (rawQueue.isEmpty()) return
        val queue = rawQueue.deduplicate()

        val currentSong = _currentSong.value

        // 1. Check forward navigation history first (e.g. user pressed Previous earlier)
        val forwardSong = synchronized(playbackForwardStack) {
            if (playbackForwardStack.isNotEmpty()) {
                playbackForwardStack.removeLast()
            } else {
                null
            }
        }

        if (forwardSong != null) {
            isNavigatingHistory = true
            try {
                if (currentSong != null) {
                    synchronized(playbackBackStack) {
                        if (playbackBackStack.isEmpty() || !SongDeduplicator.isSameOrDuplicate(playbackBackStack.peekLast(), currentSong)) {
                            playbackBackStack.addLast(currentSong)
                            if (playbackBackStack.size > 50) playbackBackStack.removeFirst()
                        }
                    }
                }
                val targetIndex = queue.indexOfFirst { SongDeduplicator.isSameOrDuplicate(it, forwardSong) }.let { if (it >= 0) it else 0 }
                selectSong(forwardSong, targetIndex, autoPlay = true)
                ensureAutoplayRadioBuffer(forwardSong)
            } finally {
                isNavigatingHistory = false
            }
            return
        }

        // 2. Compute candidate track based on current queue and mode
        val currentIndex = queue.indexOfFirst { SongDeduplicator.isSameOrDuplicate(it, currentSong) }

        val candidate: SongEntity?
        val targetIndex: Int

        if (_isShuffle.value) {
            // Shuffle mode: pick an unplayed track if available, or any other non-duplicate track
            val unplayed = queue.filter { 
                !SongDeduplicator.isSameOrDuplicate(it, currentSong) && 
                    it.id !in playedHistoryIds && 
                    SongDeduplicator.canonicalKey(it) !in playedHistoryKeys 
            }
            val chosen = if (unplayed.isNotEmpty()) {
                unplayed.random()
            } else {
                val others = queue.filter { !SongDeduplicator.isSameOrDuplicate(it, currentSong) }
                if (others.isNotEmpty()) others.random() else queue.firstOrNull()
            }
            candidate = chosen
            targetIndex = if (candidate != null) queue.indexOfFirst { it.id == candidate.id }.coerceAtLeast(0) else 0
        } else {
            // Normal sequential mode: deterministic track progression with zero duplicates
            if (currentIndex >= 0 && currentIndex + 1 < queue.size) {
                targetIndex = currentIndex + 1
                candidate = queue[targetIndex]
            } else if (_repeatMode.value == 1 && queue.isNotEmpty()) {
                // Repeat All: wrap to beginning of queue
                targetIndex = 0
                candidate = queue[0]
            } else {
                // Repeat Off and at end of queue:
                val trending = _trendingSongs.value.deduplicate()
                val existingKeys = queue.map { SongDeduplicator.canonicalKey(it) }.toSet()
                val nextTrending = trending.firstOrNull { 
                    SongDeduplicator.canonicalKey(it) !in existingKeys && 
                        SongDeduplicator.canonicalKey(it) !in playedHistoryKeys &&
                        !SongDeduplicator.isSameOrDuplicate(it, currentSong)
                } ?: trending.firstOrNull { !SongDeduplicator.isSameOrDuplicate(it, currentSong) }

                if (nextTrending != null) {
                    playbackQueue.value = (queue + nextTrending).deduplicate()
                    targetIndex = queue.size
                    candidate = nextTrending
                } else if (queue.isNotEmpty() && _repeatMode.value != 0) {
                    targetIndex = 0
                    candidate = queue[0]
                } else {
                    targetIndex = 0
                    candidate = queue.firstOrNull { !SongDeduplicator.isSameOrDuplicate(it, currentSong) } ?: queue.firstOrNull()
                }
            }
        }

        // Proactive replenishment buffer if nearing end of queue
        val unplayedRemaining = queue.count { 
            !SongDeduplicator.isSameOrDuplicate(it, currentSong) && 
                it.id !in playedHistoryIds && 
                SongDeduplicator.canonicalKey(it) !in playedHistoryKeys 
        }
        if (unplayedRemaining <= 3) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val freshSongs = repository.getTrendingSongs().deduplicate()
                    if (freshSongs.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            val currentQ = playbackQueue.value
                            val existingKeys = currentQ.map { SongDeduplicator.canonicalKey(it) }.toSet()
                            val newTracks = freshSongs.filter { 
                                it.id !in playedHistoryIds && 
                                    SongDeduplicator.canonicalKey(it) !in existingKeys && 
                                    SongDeduplicator.canonicalKey(it) !in playedHistoryKeys 
                            }
                            if (newTracks.isNotEmpty()) {
                                playbackQueue.value = (currentQ + newTracks).deduplicate()
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
        }

        if (candidate != null) {
            if (currentSong != null) {
                synchronized(playbackBackStack) {
                    if (playbackBackStack.isEmpty() || !SongDeduplicator.isSameOrDuplicate(playbackBackStack.peekLast(), currentSong)) {
                        playbackBackStack.addLast(currentSong)
                        if (playbackBackStack.size > 50) playbackBackStack.removeFirst()
                    }
                }
            }
            selectSong(candidate, targetIndex, autoPlay = true)
            ensureAutoplayRadioBuffer(candidate)
        }
    }

    fun playPreviousSong() {
        val queue = playbackQueue.value
        if (queue.isEmpty()) return

        val currentSong = _currentSong.value

        // 1. Pop from playbackBackStack first for real navigation back-stack
        val previousSong = synchronized(playbackBackStack) {
            if (playbackBackStack.isNotEmpty()) {
                playbackBackStack.removeLast()
            } else {
                null
            }
        }

        if (previousSong != null) {
            isNavigatingHistory = true
            try {
                // Push current song to forward stack so pressing Next returns to it!
                if (currentSong != null) {
                    synchronized(playbackForwardStack) {
                        if (playbackForwardStack.isEmpty() || playbackForwardStack.peekLast()?.id != currentSong.id) {
                            playbackForwardStack.addLast(currentSong)
                            if (playbackForwardStack.size > 50) playbackForwardStack.removeFirst()
                        }
                    }
                }
                val targetIndex = queue.indexOfFirst { it.id == previousSong.id }.let { if (it >= 0) it else 0 }
                selectSong(previousSong, targetIndex, autoPlay = true)
                ensureAutoplayRadioBuffer(previousSong)
            } finally {
                isNavigatingHistory = false
            }
            return
        }

        // 2. If back stack is empty:
        // If song played for more than 3 seconds, restart it from 0:00
        if (_currentPositionMs.value > 3000L && currentSong != null) {
            seekTo(0L)
            if (!_isPlaying.value) togglePlayPause()
            return
        }

        // Otherwise go to previous sequential track in queue
        val currentId = currentSong?.id
        val currentIndex = queue.indexOfFirst { it.id == currentId }

        val fallbackIndex = if (currentIndex > 0) {
            currentIndex - 1
        } else if (_repeatMode.value == 1 && queue.isNotEmpty()) {
            queue.lastIndex
        } else {
            0
        }

        val fallbackCandidate = queue.getOrNull(fallbackIndex)
        if (fallbackCandidate != null && fallbackCandidate.id != currentId) {
            if (currentSong != null) {
                synchronized(playbackForwardStack) {
                    playbackForwardStack.addLast(currentSong)
                }
            }
            selectSong(fallbackCandidate, fallbackIndex, autoPlay = true)
            ensureAutoplayRadioBuffer(fallbackCandidate)
        } else {
            seekTo(0L)
        }
    }

    fun seekTo(positionMs: Long) {
        _currentPositionMs.value = positionMs
        val song = _currentSong.value
        if (isUsingStreamPlayer(song)) {
            streamPlayerManager.seekTo(positionMs)
        } else {
            player.seekTo(positionMs)
        }
        playerPrefs.edit().putLong("last_position_ms", positionMs).apply()
    }

    fun toggleShuffle() {
        val next = !_isShuffle.value
        _isShuffle.value = next
        playerPrefs.edit().putBoolean("shuffle", next).apply()
        synchronized(playbackForwardStack) {
            playbackForwardStack.clear()
        }
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
        val song = _currentSong.value
        val isStream = isUsingStreamPlayer(song)
        if (newState && (_isPlaying.value || streamPlayerManager.isPlaying.value)) {
            if (isStream) {
                beatDetectionAudioProcessor.isEnabled = false
                flashSyncManager.startFallbackRhythm()
                _networkStatusMessage.value = "⚡ Flash Beat Sync: ON (Estimated rhythm pulse for stream)"
            } else {
                flashSyncManager.startDirectSync()
                beatDetectionAudioProcessor.isEnabled = true
                _networkStatusMessage.value = "⚡ Flash Beat Sync: ON (Direct PCM Hardware Beat Detection)"
            }
        } else {
            beatDetectionAudioProcessor.isEnabled = false
            flashSyncManager.stopSync()
            _networkStatusMessage.value = if (!newState) "Flash Beat Sync: OFF" else "Flash Beat Sync: Ready (play music to start)"
        }
    }

    private fun applyVolumeBooster(targetSessionId: Int = player.audioSessionId) {
        val enabled = _isVolumeBoosterEnabled.value
        if (!enabled) {
            try { loudnessEnhancer?.release() } catch (_: Exception) {}
            loudnessEnhancer = null
            loudnessSessionId = C.AUDIO_SESSION_ID_UNSET
            return
        }
        val sessionId = if (targetSessionId != C.AUDIO_SESSION_ID_UNSET && targetSessionId > 0) targetSessionId else player.audioSessionId
        if (sessionId == C.AUDIO_SESSION_ID_UNSET || sessionId <= 0) return
        try {
            if (loudnessEnhancer == null || loudnessSessionId != sessionId) {
                try { loudnessEnhancer?.release() } catch (_: Exception) {}
                loudnessEnhancer = LoudnessEnhancer(sessionId)
                loudnessSessionId = sessionId
            }
            loudnessEnhancer?.apply {
                setTargetGain(600) // Safe +6 dB hardware DSP boost (+150% volume without clipping)
                this.enabled = true
            }
        } catch (e: Exception) {
            Log.w("MusicViewModel", "LoudnessEnhancer error: ${e.message}")
        }
    }

    // Volume Booster toggle
    fun toggleVolumeBooster() {
        val newState = !_isVolumeBoosterEnabled.value
        _isVolumeBoosterEnabled.value = newState
        player.volume = 1.0f
        applyVolumeBooster()
        streamPlayerManager.setBoost(newState)
        _networkStatusMessage.value = if (newState) "Volume Booster: +6 dB Active (Safe Boost)" else "Volume Booster: Normal"
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
        _networkStatusMessage.value = "🌙 Sleep Timer set for $minutes mins (Smooth volume fade-out)"
        sleepJob = viewModelScope.launch {
            val totalSeconds = minutes * 60
            for (sec in totalSeconds downTo 0) {
                _sleepMinutesLeft.value = (sec + 59) / 60
                // In last 20 seconds, fade out volume smoothly for ExoPlayer AND WebView
                if (sec in 1..20) {
                    val factor = sec / 20.0f
                    player.volume = factor
                    streamPlayerManager.setVolume((factor * 100).toInt())
                }
                kotlinx.coroutines.delay(1_000)
            }
            pausePlayback()
            player.volume = 1.0f
            streamPlayerManager.setVolume(100)
            _sleepMinutesLeft.value = 0
            _networkStatusMessage.value = "🌙 Sleep Timer finished - Goodnight!"
        }
    }

    fun cancelSleepTimer() {
        sleepJob?.cancel()
        player.volume = 1.0f
        streamPlayerManager.setVolume(100)
        _sleepMinutesLeft.value = 0
        _networkStatusMessage.value = "Sleep Timer cancelled"
    }

    fun toggle8DAudio() {
        val next = !_is8DAudioEnabled.value
        _is8DAudioEnabled.value = next
        eightDAudioProcessor.is8DEnabled = next
        streamPlayerManager.set8DAudio(next)
        if (next) {
            _networkStatusMessage.value = "🎧 8D Spatial Audio: ON (360° Binaural Orbit Panning)"
        } else {
            _networkStatusMessage.value = "8D Audio: Normal"
        }
    }

    fun toggleCrossfade() {
        _isCrossfadeEnabled.value = !_isCrossfadeEnabled.value
        _networkStatusMessage.value = if (_isCrossfadeEnabled.value) "🎛️ Smooth DJ Fade Transition: ON (Fade-out / Fade-in)" else "Fade Transition: OFF"
    }

    private fun triggerCrossfade() {
        if (isCrossfading || (sleepJob?.isActive == true && _sleepMinutesLeft.value <= 1)) {
            playNextSong()
            return
        }
        isCrossfading = true
        viewModelScope.launch {
            try {
                // Smoothly fade out ending song on both ExoPlayer and stream
                for (step in 3 downTo 1) {
                    if (sleepJob?.isActive == true && _sleepMinutesLeft.value <= 1) break
                    val vol = step / 4f
                    player.volume = vol
                    streamPlayerManager.setVolume((vol * 100).toInt())
                    kotlinx.coroutines.delay(600)
                }
                playNextSong()
                // Smoothly fade in beginning of next song
                val fadeSteps = listOf(0.35f, 0.65f, 0.85f, 1.0f)
                for (vol in fadeSteps) {
                    if (sleepJob?.isActive == true && _sleepMinutesLeft.value <= 1) break
                    player.volume = vol
                    streamPlayerManager.setVolume((vol * 100).toInt())
                    kotlinx.coroutines.delay(350)
                }
            } finally {
                if (sleepJob?.isActive != true || _sleepMinutesLeft.value > 1) {
                    player.volume = 1.0f
                    streamPlayerManager.setVolume(100)
                }
                isCrossfading = false
            }
        }
    }

    fun setEqualizerPreset(preset: String) {
        _equalizerPreset.value = preset
        applyEqualizer()
        _networkStatusMessage.value = if (preset == "Off") "Equalizer: OFF" else "🎛️ Equalizer profile: $preset applied"
    }

    private fun applyEqualizer(targetSessionId: Int = player.audioSessionId) {
        val preset = _equalizerPreset.value
        if (preset == "Off") {
            appEqualizer.apply("Off")
            return
        }
        val song = _currentSong.value
        if (song == null || isUsingStreamPlayer(song)) {
            appEqualizer.apply("Off")
            return
        }
        val session = if (targetSessionId != C.AUDIO_SESSION_ID_UNSET && targetSessionId > 0) targetSessionId else player.audioSessionId
        if (session == C.AUDIO_SESSION_ID_UNSET || session <= 0) return
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
                if (_currentSong.value == null && local.isNotEmpty()) {
                    restoreLastPlaybackOrInitial(local)
                }
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
        _currentSong.value?.let { song ->
            playerPrefs.edit()
                .putString("last_played_song_id", song.id)
                .putLong("last_position_ms", _currentPositionMs.value)
                .apply()
        }
        try { loudnessEnhancer?.release() } catch (_: Exception) {}
        loudnessEnhancer = null
        beatDetectionAudioProcessor.isEnabled = false
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

    private var autoplayJob: Job? = null

    private fun ensureAutoplayRadioBuffer(currentSong: SongEntity) {
        autoplayJob?.cancel()
        autoplayJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentQueueList = playbackQueue.value.deduplicate()
                val existingIds = currentQueueList.map { it.id }.toSet()
                val existingKeys = currentQueueList.map { SongDeduplicator.canonicalKey(it) }.toSet()
                val currentKey = SongDeduplicator.canonicalKey(currentSong)
                val currentSongIndex = currentQueueList.indexOfFirst { SongDeduplicator.isSameOrDuplicate(it, currentSong) }

                val remainingCount = if (currentSongIndex >= 0) currentQueueList.size - currentSongIndex - 1 else 0
                if (remainingCount >= 6) return@launch

                val newTracks = mutableListOf<SongEntity>()

                fun isEligible(track: SongEntity): Boolean {
                    if (SongDeduplicator.isSameOrDuplicate(track, currentSong)) return false
                    val key = SongDeduplicator.canonicalKey(track)
                    if (key == currentKey || key in existingKeys || key in playedHistoryKeys) return false
                    if (track.id in existingIds || track.id == currentSong.id || track.id in playedHistoryIds) return false
                    if (newTracks.any { SongDeduplicator.isSameOrDuplicate(it, track) }) return false
                    return true
                }

                // 1. Up to 2 songs from the current artist (never flood with the same artist)
                val cleanArtist = currentSong.artist
                    .replace(Regex("(?i)ft\\.?|feat\\.?|&|,|official|audio|video|vevo|remix|mix"), " ")
                    .trim()
                if (cleanArtist.isNotBlank() && cleanArtist.length >= 3 && !cleanArtist.contains("unknown", ignoreCase = true)) {
                    val artistTracks = repository.searchSongsOnline("$cleanArtist hits").deduplicate()
                    for (track in artistTracks) {
                        if (isEligible(track)) {
                            newTracks.add(track)
                            if (newTracks.size >= 2) break
                        }
                    }
                }

                // 2. Varied radio seeds for diverse genres and artists
                val radioSeeds = listOf(
                    "Trending Hindi Hits",
                    "Top Punjabi Hits",
                    "Bollywood Romantic Hits",
                    "Arijit Singh Hits",
                    "Superhit Bollywood Songs",
                    "Latest Bollywood Songs",
                    "Party Dance Hindi"
                ).shuffled()

                for (seed in radioSeeds) {
                    if (newTracks.size >= 6) break
                    val seedTracks = repository.searchSongsOnline(seed).deduplicate()
                    for (track in seedTracks) {
                        if (isEligible(track)) {
                            newTracks.add(track)
                            if (newTracks.size >= 6) break
                        }
                    }
                }

                // 3. Trending fallback
                if (newTracks.size < 4) {
                    val trending = if (_trendingSongs.value.isNotEmpty()) _trendingSongs.value.deduplicate() else repository.getTrendingSongs().deduplicate()
                    for (track in trending) {
                        if (isEligible(track)) {
                            newTracks.add(track)
                            if (newTracks.size >= 6) break
                        }
                    }
                }

                if (newTracks.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        val currentQueue = playbackQueue.value
                        val updated = (currentQueue + newTracks).deduplicate()
                        playbackQueue.value = updated
                    }
                }
            } catch (e: Exception) {
                Log.w("MusicViewModel", "ensureAutoplayRadioBuffer: ${e.message}")
            }
        }
    }

    private fun ensureAutoplayQueue(currentSong: SongEntity) = ensureAutoplayRadioBuffer(currentSong)
}
