package com.resso.craka.data.repository

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.resso.craka.data.db.AppDatabase
import com.resso.craka.data.firebase.FirebaseMusicManager
import com.resso.craka.data.model.CommentEntity
import com.resso.craka.data.model.PlaylistEntity
import com.resso.craka.data.model.SongEntity
import com.resso.craka.data.model.VibeEntity
import com.resso.craka.data.network.CatalogCache
import com.resso.craka.data.network.MusicSearchService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class MusicRepository(context: Context) {
    private val appContext = context.applicationContext
    private val db = Room.databaseBuilder(
        appContext,
        AppDatabase::class.java,
        "resso_music.db"
    ).fallbackToDestructiveMigration().build()

    val songDao = db.songDao()
    val vibeDao = db.vibeDao()
    val commentDao = db.commentDao()
    val playlistDao = db.playlistDao()
    val searchService = MusicSearchService()
    val firebaseManager = FirebaseMusicManager(appContext)
    val firestoreMusicRepo = FirestoreMusicRepository(appContext, firebaseManager)
    private val catalogCache = CatalogCache(context)

    init {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                songDao.purgeLocalSongs()
                songDao.purgeDummySongs()
                fetchAndSeedDynamicSongsFromDatabase()
                songDao.clearPlaceholderLyrics()
                syncFromCloud()
            } catch (t: Throwable) {
                Log.e("MusicRepository", "Background init error: ${t.message}", t)
            }
        }
    }

    private suspend fun syncFromCloud() {
        try {
            val cloudLikes = firebaseManager.fetchCloudLikedSongs()
            if (cloudLikes.isNotEmpty()) {
                songDao.insertSongs(cloudLikes)
            }
            // Seed sample songs metadata to Firestore for instant live search
            val sampleSongs = songDao.getInitialSongsSync()
            if (sampleSongs.isNotEmpty()) {
                firestoreMusicRepo.saveSongsBatch(sampleSongs)
            }
        } catch (_: Exception) {}
    }

    suspend fun getDynamicFirestoreSongs(limit: Int = 30, excludeIds: Set<String> = emptySet()): List<SongEntity> =
        firestoreMusicRepo.getDynamicSongs(limit, excludeIds)

    fun observeDynamicFirestoreSongs(limit: Int = 30): Flow<List<SongEntity>> =
        firestoreMusicRepo.observeDynamicSongsRealtime(limit)

    suspend fun recordSongActivityInFirestore(song: SongEntity) =
        firestoreMusicRepo.recordSongActivity(song)

    fun searchSongsInFirestoreRealtime(query: String): Flow<List<SongEntity>> =
        firestoreMusicRepo.searchDynamicSongs(query)

    suspend fun indexSongsInFirestore(songs: List<SongEntity>) {
        firestoreMusicRepo.saveSongsBatch(songs)
    }

    fun getAllSongs(): Flow<List<SongEntity>> = songDao.getAllSongs()
    fun getLikedSongs(): Flow<List<SongEntity>> = songDao.getLikedSongs()
    suspend fun getSongById(id: String): SongEntity? = songDao.getSongById(id)

    suspend fun toggleLike(id: String, currentLiked: Boolean) {
        val newLiked = !currentLiked
        songDao.setLiked(id, newLiked)
        val song = songDao.getSongById(id)
        if (song != null) {
            CoroutineScope(Dispatchers.IO).launch {
                firebaseManager.syncLikedSong(song, newLiked)
            }
        }
    }

    suspend fun insertCustomSong(song: SongEntity) {
        if (!song.audioUrl.startsWith("http")) return
        songDao.insertSong(song)
        if (song.isLiked) {
            CoroutineScope(Dispatchers.IO).launch {
                firebaseManager.syncLikedSong(song, true)
            }
        }
    }

    suspend fun updateSongVibe(songId: String, vibeUri: String) = songDao.updateSongVibe(songId, vibeUri)

    // Online & Local Search
    suspend fun searchSongsOnline(query: String): List<SongEntity> {
        val key = "search_${query.trim().lowercase()}"
        catalogCache.read(key, SEARCH_CACHE_MS)?.let { return com.resso.craka.util.SongDeduplicator.deduplicateList(it) }
        val fresh = com.resso.craka.util.SongDeduplicator.deduplicateList(searchService.searchSongs(query))
        if (fresh.isNotEmpty()) catalogCache.write(key, fresh)
        return fresh.ifEmpty { com.resso.craka.util.SongDeduplicator.deduplicateList(catalogCache.read(key, WEEK_MS).orEmpty()) }
    }

    suspend fun getTrendingSongs(): List<SongEntity> {
        // 1. Try dynamic songs from Firestore database first
        try {
            val dynamicFirestore = com.resso.craka.util.SongDeduplicator.deduplicateList(firestoreMusicRepo.getDynamicSongs(35))
            if (dynamicFirestore.isNotEmpty()) {
                songDao.insertSongs(dynamicFirestore)
                return dynamicFirestore.shuffled()
            }
        } catch (_: Exception) {}

        // 2. Try Firestore cached_songs
        try {
            val firestoreRepo = FirestoreRepository.getInstance(appContext)
            val cached = firestoreRepo.getAllCachedSongs(35)
            if (cached.isNotEmpty()) {
                val entities = com.resso.craka.util.SongDeduplicator.deduplicateList(cached.map { it.toSongEntity() })
                songDao.insertSongs(entities)
                return entities.shuffled()
            }
        } catch (_: Exception) {}

        // 3. Query live YouTube Music API via Vercel for fresh hits
        try {
            val fresh = com.resso.craka.util.SongDeduplicator.deduplicateList(searchService.getTrendingSongs(35))
            if (fresh.isNotEmpty()) {
                songDao.insertSongs(fresh)
                firestoreMusicRepo.saveSongsBatch(fresh)
                return fresh.shuffled()
            }
        } catch (_: Exception) {}

        // 4. Return any existing non-dummy songs from local DB
        val local = songDao.getInitialSongsSync().filter { !it.id.startsWith("saavn_") && !it.id.startsWith("yt_sample_") }
        return com.resso.craka.util.SongDeduplicator.deduplicateList(local).shuffled()
    }

    fun peekTrending(): List<SongEntity> = emptyList()

    fun peekSearch(query: String): List<SongEntity> {
        val key = "search_${query.trim().lowercase()}"
        return com.resso.craka.util.SongDeduplicator.deduplicateList(catalogCache.read(key, WEEK_MS).orEmpty())
    }

    fun peekSearches(queries: List<String>): List<SongEntity> {
        val allSongs = mutableListOf<SongEntity>()
        queries.forEach { query ->
            allSongs.addAll(peekSearch(query))
        }
        return com.resso.craka.util.SongDeduplicator.deduplicateList(allSongs)
    }
    suspend fun searchLocalSongs(query: String): List<SongEntity> = songDao.searchLocalSongs(query)
    suspend fun fetchLyrics(artist: String, title: String): String? = searchService.fetchSyncedLyrics(artist, title)
    suspend fun updateSongLyrics(songId: String, lyrics: String) = songDao.updateSongLyrics(songId, lyrics)

    // Vibes
    fun getVibesForSong(songId: String): Flow<List<VibeEntity>> = vibeDao.getVibesForSong(songId)
    fun getAllVibes(): Flow<List<VibeEntity>> = vibeDao.getAllVibes()
    suspend fun saveVibe(vibe: VibeEntity): Long {
        val id = vibeDao.insertVibe(vibe)
        val withId = vibe.copy(id = id)
        CoroutineScope(Dispatchers.IO).launch {
            firebaseManager.syncVibe(withId)
        }
        return id
    }
    suspend fun deleteVibe(id: Long) = vibeDao.deleteVibe(id)

    // Comments
    fun getCommentsForSong(songId: String): Flow<List<CommentEntity>> = commentDao.getCommentsForSong(songId)
    suspend fun addComment(comment: CommentEntity): Long {
        val id = commentDao.insertComment(comment)
        val withId = comment.copy(id = id)
        CoroutineScope(Dispatchers.IO).launch {
            firebaseManager.syncComment(withId)
        }
        return id
    }
    suspend fun toggleCommentLike(commentId: Long) = commentDao.toggleCommentLike(commentId)

    // Playlists
    fun getAllPlaylists(): Flow<List<PlaylistEntity>> = playlistDao.getAllPlaylists()
    suspend fun createPlaylist(name: String, description: String = ""): Long {
        val entity = PlaylistEntity(name = name, description = description, songIdsCsv = "")
        val id = playlistDao.insertPlaylist(entity)
        val withId = entity.copy(id = id)
        CoroutineScope(Dispatchers.IO).launch {
            firebaseManager.syncPlaylist(withId)
        }
        return id
    }

    suspend fun addSongToPlaylist(playlistId: Long, song: SongEntity) {
        val playlist = playlistDao.getPlaylist(playlistId) ?: return
        val currentIds = playlist.songIdsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
        if (song.id !in currentIds) {
            currentIds.add(song.id)
            val updated = playlist.copy(songIdsCsv = currentIds.joinToString(","))
            playlistDao.updatePlaylist(updated)
            CoroutineScope(Dispatchers.IO).launch {
                firebaseManager.syncPlaylist(updated)
            }
        }
    }

    suspend fun songsInPlaylist(playlist: PlaylistEntity): List<SongEntity> {
        val ids = playlist.songIdsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (ids.isEmpty()) return emptyList()
        val found = songDao.getSongsByIds(ids).associateBy { it.id }
        return ids.mapNotNull { found[it] }
    }

    fun recentSongs(): List<SongEntity> = com.resso.craka.util.SongDeduplicator.deduplicateList(
        catalogCache.read("recent_played", WEEK_MS).orEmpty()
    )

    fun rememberRecent(song: SongEntity) {
        if (!song.audioUrl.startsWith("http")) return
        val next = com.resso.craka.util.SongDeduplicator.deduplicateList(
            listOf(song) + recentSongs().filter { !com.resso.craka.util.SongDeduplicator.isSameOrDuplicate(it, song) }
        )
        catalogCache.write("recent_played", next.take(30))
        CoroutineScope(Dispatchers.IO).launch {
            firebaseManager.syncHistory(song)
        }
    }

    suspend fun movePlaylistSong(playlistId: Long, from: Int, to: Int) {
        val playlist = playlistDao.getPlaylist(playlistId) ?: return
        val ids = playlist.songIdsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
        if (from !in ids.indices || to !in ids.indices) return
        val item = ids.removeAt(from)
        ids.add(to, item)
        val updated = playlist.copy(songIdsCsv = ids.joinToString(","))
        playlistDao.updatePlaylist(updated)
        CoroutineScope(Dispatchers.IO).launch {
            firebaseManager.syncPlaylist(updated)
        }
    }

    suspend fun getPlaylist(id: Long) = playlistDao.getPlaylist(id)

    suspend fun saveForOffline(song: SongEntity): String {
        if (song.audioUrl.startsWith("file://") || song.audioUrl.startsWith("/")) {
            return "Already saved on this phone"
        }
        if (!song.audioUrl.startsWith("http")) {
            if (songDao.getSongById(song.id) == null) songDao.insertSong(song)
            return "Saved in your library"
        }
        return try {
            val dir = java.io.File(appContext.filesDir, "offline").apply { mkdirs() }
            val target = java.io.File(dir, song.id.replace(Regex("[^a-zA-Z0-9_]"), "_") + ".audio")
            val request = okhttp3.Request.Builder().url(song.audioUrl).build()
            okhttp3.OkHttpClient().newCall(request).execute().use { response ->
                if (!response.isSuccessful) return "Couldn't save this song"
                target.outputStream().use { out -> response.body?.byteStream()?.copyTo(out) }
            }
            val saved = song.copy(audioUrl = android.net.Uri.fromFile(target).toString())
            songDao.insertSong(saved)
            "Saved for offline play"
        } catch (e: Exception) {
            "Couldn't save this song"
        }
    }

    companion object {
        private const val TRENDING_KEY = "trending"
        private const val TRENDING_CACHE_MS = 12 * 60 * 60 * 1000L
        private const val SEARCH_CACHE_MS = 12 * 60 * 60 * 1000L
        private const val WEEK_MS = 7 * 24 * 60 * 60 * 1000L
    }

    suspend fun scanDeviceAudio() = kotlinx.coroutines.withContext(Dispatchers.IO) {
        // Enforce 100% online streaming songs: purge local audio files
        try {
            songDao.purgeLocalSongs()
        } catch (_: Exception) {}
    }

    private suspend fun updateDefaultStreamsIfEmpty() {
        val streamMap = mapOf(
            "a-PAcmi5Kas" to "https://music-api-lime-nine.vercel.app/play/a-PAcmi5Kas",
            "IJq0yyWug1k" to "https://music-api-lime-nine.vercel.app/play/IJq0yyWug1k",
            "5Eqb_-j3FDA" to "https://music-api-lime-nine.vercel.app/play/5Eqb_-j3FDA",
            "ElZfdU54Cp8" to "https://music-api-lime-nine.vercel.app/play/ElZfdU54Cp8",
            "Wv2rLZmbPMA" to "https://music-api-lime-nine.vercel.app/play/Wv2rLZmbPMA",
            "BddP6PYo2gs" to "https://music-api-lime-nine.vercel.app/play/BddP6PYo2gs",
            "n_FCrCQ6-9U" to "https://music-api-lime-nine.vercel.app/play/n_FCrCQ6-9U",
            "mH_LFkWxpI0" to "https://music-api-lime-nine.vercel.app/play/mH_LFkWxpI0"
        )
        for ((id, url) in streamMap) {
            val song = songDao.getSongById(id)
            if (song != null && song.audioUrl.isBlank()) {
                songDao.insertSong(song.copy(audioUrl = url))
            }
        }
    }

        private suspend fun fetchAndSeedDynamicSongsFromDatabase() {
        try {
            songDao.purgeDummySongs()
        } catch (_: Exception) {}

        // 1. Fetch dynamic songs directly from Firestore database
        try {
            val dynamicFirestore = firestoreMusicRepo.getDynamicSongs(35)
            if (dynamicFirestore.isNotEmpty()) {
                val cleaned = com.resso.craka.util.SongDeduplicator.deduplicateList(dynamicFirestore)
                songDao.insertSongs(cleaned)
                Log.i("MusicRepository", "Successfully loaded ${cleaned.size} dynamic songs from Firestore database")
                return
            }
        } catch (e: Exception) {
            Log.w("MusicRepository", "Firestore dynamic fetch note: ${e.message}")
        }

        // 2. Fetch from Firestore cached_songs
        try {
            val firestoreRepo = FirestoreRepository.getInstance(appContext)
            val cached = firestoreRepo.getAllCachedSongs(35)
            if (cached.isNotEmpty()) {
                val entities = com.resso.craka.util.SongDeduplicator.deduplicateList(cached.map { it.toSongEntity() })
                songDao.insertSongs(entities)
                Log.i("MusicRepository", "Loaded ${entities.size} songs from Firestore cached_songs")
                return
            }
        } catch (e: Exception) {
            Log.w("MusicRepository", "Firestore cached_songs fetch note: ${e.message}")
        }

        // 3. Dynamic online fetch from YouTube Music API via Vercel (real songs, zero dummy)
        try {
            val freshTrending = searchService.getTrendingSongs(35)
            if (freshTrending.isNotEmpty()) {
                val cleaned = com.resso.craka.util.SongDeduplicator.deduplicateList(freshTrending)
                songDao.insertSongs(cleaned)
                firestoreMusicRepo.saveSongsBatch(cleaned)
                Log.i("MusicRepository", "Fetched & cached ${cleaned.size} real trending songs from online API to Firestore")
                return
            }
        } catch (e: Exception) {
            Log.w("MusicRepository", "Online trending fetch note: ${e.message}")
        }
    }
}
