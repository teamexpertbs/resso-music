package com.resso.craka.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.resso.craka.RessoApplication
import com.resso.craka.data.model.CommentEntity
import com.resso.craka.data.model.LyricLine
import com.resso.craka.data.model.PlaylistEntity
import com.resso.craka.data.model.SongEntity
import com.resso.craka.data.model.VibeEntity
import com.resso.craka.flash.FlashSyncManager
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

    private val player: ExoPlayer = ExoPlayer.Builder(application).build()

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

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(1L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _lyrics = MutableStateFlow<List<LyricLine>>(emptyList())
    val lyrics: StateFlow<List<LyricLine>> = _lyrics.asStateFlow()

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
    }

    fun setVideoMode(enabled: Boolean) {
        _isVideoMode.value = enabled
    }

    private val _isLyricsVisible = MutableStateFlow(true)
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
        setupPlayerListener()
        setupStreamListener()
        startProgressTracking()
        loadTrendingSongs()
        setupNetworkConnectivityListener()

        viewModelScope.launch {
            allSongs.collect { songs ->
                if (songs.isNotEmpty() && _currentSong.value == null) {
                    selectSong(songs[0], 0, autoPlay = false)
                }
            }
        }
    }

    private fun setupNetworkConnectivityListener() {
        try {
            val cm = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            connectivityManager = cm
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onLost(network: Network) {
                    if (_isPlaying.value) {
                        wasPlayingBeforeNetworkLost = true
                        savedPositionBeforeLost = _currentPositionMs.value
                        _networkStatusMessage.value = "⚠️ Internet कट गया! कनेक्शन आते ही गाना वहीं से चालू होगा 📡"
                    }
                }

                override fun onAvailable(network: Network) {
                    if (wasPlayingBeforeNetworkLost) {
                        wasPlayingBeforeNetworkLost = false
                        viewModelScope.launch {
                            delay(600) // Brief delay for stable network buffer
                            _networkStatusMessage.value = "⚡ Internet वापस आ गया! गाना वहीं से जारी हो रहा है..."
                            val song = _currentSong.value
                            if (song != null) {
                                if (song.id.startsWith("yt_")) {
                                    streamPlayerManager.seekTo(savedPositionBeforeLost)
                                    streamPlayerManager.play()
                                    _isPlaying.value = true
                                } else {
                                    player.seekTo(savedPositionBeforeLost)
                                    player.play()
                                    _isPlaying.value = true
                                }
                            }
                        }
                    }
                }
            }
            networkCallback = callback
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            cm?.registerNetworkCallback(request, callback)
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
                    _isPlaying.value = streamPlaying
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
                        val idx = currentLyrics.indexOfLast { it.timeMs <= streamPos }
                        _activeLyricIndex.value = if (idx >= 0) idx else 0
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
        viewModelScope.launch {
            try {
                val trending = repository.getTrendingSongs()
                if (trending.isNotEmpty()) {
                    _trendingSongs.value = trending
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

            // Fetch synced lyrics if not already present
            if (song.lyricsLrc.isBlank()) {
                fetchAndApplyLyrics(song)
            }
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
                }
            } else {
                val fallbackLrc = buildFallbackLyrics(song)
                repository.updateSongLyrics(song.id, fallbackLrc)
                if (_currentSong.value?.id == song.id) {
                    _currentSong.value = _currentSong.value?.copy(lyricsLrc = fallbackLrc)
                    _lyrics.value = LyricsParser.parse(fallbackLrc)
                }
            }
        }
    }

    private fun buildFallbackLyrics(song: SongEntity): String {
        return """
            [00:00.00] ♪ Now playing: ${song.title} ♪
            [00:05.00] 🎤 ${song.artist}
            [00:10.00] Feel the rhythm & bass drop...
            [00:18.00] ✨ High Fidelity Audio Streaming ✨
            [00:26.00] Enjoying the vibe on Resso Music
            [00:35.00] Tap to share lyric poster or create vibe video
            [00:45.00] ♪ Let the rhythm flow ♪
            [00:55.00] ${song.album}
        """.trimIndent()
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
                    if (currentLyrics.isNotEmpty()) {
                        val idx = currentLyrics.indexOfLast { it.timeMs <= pos }
                        _activeLyricIndex.value = if (idx >= 0) idx else 0
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
        if (!queue.isNullOrEmpty()) {
            playbackQueue.value = queue
        } else if (playbackQueue.value.none { it.id == song.id }) {
            playbackQueue.value = allSongs.value.ifEmpty { listOf(song) }
        }
        _currentSong.value = song
        _currentSongIndex.value = index
        _lyrics.value = LyricsParser.parse(song.lyricsLrc)
        _currentVibeUri.value = song.vibeVideoUri
        _activeLyricIndex.value = 0
        _currentPositionMs.value = 0L
        _durationMs.value = song.durationMs.coerceAtLeast(1000L)

        // Observe comments for this song
        observeCommentsForSong(song.id)

        if (song.id.startsWith("yt_")) {
            // Online Stream Song: pause ExoPlayer, play via Stream Manager in full length
            if (player.isPlaying) {
                player.stop()
            }
            val videoId = song.id.removePrefix("yt_")
            streamPlayerManager.loadAndPlay(videoId, autoPlay)
            _isPlaying.value = autoPlay
        } else {
            // Standard media: pause Stream, play via ExoPlayer
            streamPlayerManager.pause()
            _isVideoMode.value = false
            try {
                val mediaItem = MediaItem.fromUri(Uri.parse(song.audioUrl))
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

        if (song.lyricsLrc.isBlank()) {
            fetchAndApplyLyrics(song)
        }
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
            streamPlayerManager.togglePlayPause()
        } else {
            if (player.isPlaying) {
                player.pause()
            } else {
                player.play()
            }
        }
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
    }

    fun toggleRepeat() {
        _repeatMode.value = (_repeatMode.value + 1) % 3
    }

    fun toggleLikeCurrentSong() {
        val song = _currentSong.value ?: return
        viewModelScope.launch {
            repository.toggleLike(song.id, song.isLiked)
            _currentSong.value = song.copy(isLiked = !song.isLiked)
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
        val vol = if (newState) 1.5f else 1.0f
        player.volume = vol
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

    // Custom song upload
    fun addLocalSong(uri: Uri, title: String, artist: String) {
        viewModelScope.launch {
            val newSong = SongEntity(
                id = "custom_${System.currentTimeMillis()}",
                title = title.ifBlank { "Local Track" },
                artist = artist.ifBlank { "Unknown Artist" },
                album = "My Uploads",
                durationMs = 180000L,
                audioUrl = uri.toString(),
                albumArtUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80",
                lyricsLrc = "[00:00.00] Enjoy your uploaded track\n[00:10.00] Add a custom Vibe background from the side menu!\n[00:25.00] Tap lyrics to seek anytime",
                genre = "Custom",
                mood = "Chill",
                isLiked = true,
                isCustomUpload = true
            )
            repository.insertCustomSong(newSong)
            selectSong(newSong, allSongs.value.size, autoPlay = true)
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            networkCallback?.let { connectivityManager?.unregisterNetworkCallback(it) }
        } catch (_: Exception) {}
        progressJob?.cancel()
        commentsJob?.cancel()
        flashSyncManager.stopSync()
        player.release()
        streamPlayerManager.release()
    }
}
