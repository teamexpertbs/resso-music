package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.RessoApplication
import com.example.data.model.CommentEntity
import com.example.data.model.LyricLine
import com.example.data.model.PlaylistEntity
import com.example.data.model.SongEntity
import com.example.data.model.VibeEntity
import com.example.flash.FlashSyncManager
import com.example.util.LyricsParser
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MusicViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as RessoApplication).repository
    val flashSyncManager = FlashSyncManager(application)

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

    private var searchJob: Job? = null
    private var progressJob: Job? = null
    private var commentsJob: Job? = null

    init {
        setupPlayerListener()
        startProgressTracking()
        loadTrendingSongs()

        viewModelScope.launch {
            allSongs.collect { songs ->
                if (songs.isNotEmpty() && _currentSong.value == null) {
                    selectSong(songs[0], 0, autoPlay = false)
                }
            }
        }
    }

    private fun loadTrendingSongs() {
        viewModelScope.launch {
            try {
                val trending = repository.searchSongsOnline("Bollywood hits")
                if (trending.isNotEmpty()) {
                    _trendingSongs.value = trending
                }
            } catch (_: Exception) {
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

                // 2. Search online songs from iTunes catalog
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

    fun playSongFromAnywhere(song: SongEntity, autoPlay: Boolean = true) {
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
                _isPlaying.value = playing
                if (playing && _isFlashSyncEnabled.value) {
                    flashSyncManager.startSync(128)
                } else {
                    flashSyncManager.stopSync()
                }
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    _durationMs.value = player.duration.coerceAtLeast(1L)
                } else if (state == Player.STATE_ENDED) {
                    when (_repeatMode.value) {
                        2 -> {
                            player.seekTo(0)
                            player.play()
                        }
                        1 -> playNextSong()
                        else -> {
                            if (_currentSongIndex.value < allSongs.value.size - 1) {
                                playNextSong()
                            } else {
                                _isPlaying.value = false
                            }
                        }
                    }
                }
            }
        })
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (isActive) {
                if (player.isPlaying) {
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

    fun selectSong(song: SongEntity, index: Int, autoPlay: Boolean = true) {
        _currentSong.value = song
        _currentSongIndex.value = index
        _lyrics.value = LyricsParser.parse(song.lyricsLrc)
        _currentVibeUri.value = song.vibeVideoUri
        _activeLyricIndex.value = 0

        // Observe comments for this song
        observeCommentsForSong(song.id)

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
        } catch (_: Exception) {
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
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    fun playNextSong() {
        val songs = allSongs.value
        if (songs.isEmpty()) return
        val nextIndex = if (_isShuffle.value) {
            (0 until songs.size).filter { it != _currentSongIndex.value }.randomOrNull() ?: 0
        } else {
            (_currentSongIndex.value + 1) % songs.size
        }
        selectSong(songs[nextIndex], nextIndex, autoPlay = true)
    }

    fun playPreviousSong() {
        val songs = allSongs.value
        if (songs.isEmpty()) return
        val prevIndex = if (_currentPositionMs.value > 3000) {
            _currentSongIndex.value
        } else {
            if (_currentSongIndex.value - 1 < 0) songs.size - 1 else _currentSongIndex.value - 1
        }
        selectSong(songs[prevIndex], prevIndex, autoPlay = true)
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
        _currentPositionMs.value = positionMs
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
        progressJob?.cancel()
        commentsJob?.cancel()
        flashSyncManager.stopSync()
        player.release()
    }
}
