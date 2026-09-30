package com.resso.craka.data.repository

import android.content.Context
import androidx.room.Room
import com.resso.craka.data.db.AppDatabase
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
    private val db = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "resso_music.db"
    ).fallbackToDestructiveMigration(true).build()

    val songDao = db.songDao()
    val vibeDao = db.vibeDao()
    val commentDao = db.commentDao()
    val playlistDao = db.playlistDao()
    val searchService = MusicSearchService()
    private val catalogCache = CatalogCache(context)

    init {
        CoroutineScope(Dispatchers.IO).launch {
            seedInitialDataIfEmpty()
            songDao.clearPlaceholderLyrics()
        }
    }

    fun getAllSongs(): Flow<List<SongEntity>> = songDao.getAllSongs()
    fun getLikedSongs(): Flow<List<SongEntity>> = songDao.getLikedSongs()
    suspend fun getSongById(id: String): SongEntity? = songDao.getSongById(id)
    suspend fun toggleLike(id: String, currentLiked: Boolean) = songDao.setLiked(id, !currentLiked)
    suspend fun insertCustomSong(song: SongEntity) = songDao.insertSong(song)
    suspend fun updateSongVibe(songId: String, vibeUri: String) = songDao.updateSongVibe(songId, vibeUri)

    // Online & Local Search
    suspend fun searchSongsOnline(query: String): List<SongEntity> {
        val key = "search_${query.trim().lowercase()}"
        catalogCache.read(key, SEARCH_CACHE_MS)?.let { return it }
        val fresh = searchService.searchSongs(query)
        if (fresh.isNotEmpty()) catalogCache.write(key, fresh)
        return fresh.ifEmpty { catalogCache.read(key, WEEK_MS).orEmpty() }
    }

    suspend fun getTrendingSongs(): List<SongEntity> {
        catalogCache.read(TRENDING_KEY, TRENDING_CACHE_MS)?.let { return it }
        val fresh = searchService.getTrendingSongs()
        if (fresh.isNotEmpty()) catalogCache.write(TRENDING_KEY, fresh)
        return fresh.ifEmpty { catalogCache.read(TRENDING_KEY, WEEK_MS).orEmpty() }
    }

    fun peekTrending(): List<SongEntity> = catalogCache.read(TRENDING_KEY, WEEK_MS).orEmpty()
    suspend fun searchLocalSongs(query: String): List<SongEntity> = songDao.searchLocalSongs(query)
    suspend fun fetchLyrics(artist: String, title: String): String? = searchService.fetchSyncedLyrics(artist, title)
    suspend fun updateSongLyrics(songId: String, lyrics: String) = songDao.updateSongLyrics(songId, lyrics)

    // Vibes
    fun getVibesForSong(songId: String): Flow<List<VibeEntity>> = vibeDao.getVibesForSong(songId)
    fun getAllVibes(): Flow<List<VibeEntity>> = vibeDao.getAllVibes()
    suspend fun saveVibe(vibe: VibeEntity): Long = vibeDao.insertVibe(vibe)
    suspend fun deleteVibe(id: Long) = vibeDao.deleteVibe(id)

    // Comments
    fun getCommentsForSong(songId: String): Flow<List<CommentEntity>> = commentDao.getCommentsForSong(songId)
    suspend fun addComment(comment: CommentEntity): Long = commentDao.insertComment(comment)
    suspend fun toggleCommentLike(commentId: Long) = commentDao.toggleCommentLike(commentId)

    // Playlists
    fun getAllPlaylists(): Flow<List<PlaylistEntity>> = playlistDao.getAllPlaylists()
    suspend fun createPlaylist(name: String, description: String = ""): Long {
        return playlistDao.insertPlaylist(
            PlaylistEntity(name = name, description = description, songIdsCsv = "")
        )
    }

    suspend fun songsInPlaylist(playlist: PlaylistEntity): List<SongEntity> {
        val ids = playlist.songIdsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (ids.isEmpty()) return emptyList()
        val found = songDao.getSongsByIds(ids).associateBy { it.id }
        return ids.mapNotNull { found[it] }
    }

    fun recentSongs(): List<SongEntity> = catalogCache.read("recent_played", WEEK_MS).orEmpty()

    fun rememberRecent(song: SongEntity) {
        val next = listOf(song) + recentSongs().filter { it.id != song.id }
        catalogCache.write("recent_played", next.take(30))
    }

    suspend fun movePlaylistSong(playlistId: Long, from: Int, to: Int) {
        val playlist = playlistDao.getPlaylist(playlistId) ?: return
        val ids = playlist.songIdsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
        if (from !in ids.indices || to !in ids.indices) return
        val item = ids.removeAt(from)
        ids.add(to, item)
        playlistDao.updatePlaylist(playlist.copy(songIdsCsv = ids.joinToString(",")))
    }

    suspend fun getPlaylist(id: Long) = playlistDao.getPlaylist(id)

    suspend fun saveForOffline(song: SongEntity): String {
        if (song.id.startsWith("yt_") || song.audioUrl.contains("youtube.com") || song.audioUrl.contains("youtu.be")) {
            return "YouTube songs can't be saved offline"
        }
        if (song.audioUrl.startsWith("file://") || song.audioUrl.startsWith("/")) {
            return "Already saved on this phone"
        }
        if (!song.audioUrl.startsWith("http")) {
            if (songDao.getSongById(song.id) == null) songDao.insertSong(song.copy(isCustomUpload = true))
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
            val saved = song.copy(audioUrl = target.absolutePath, isCustomUpload = true)
            songDao.insertSong(saved)
            "Saved for offline play"
        } catch (e: Exception) {
            "Couldn't save this song"
        }
    }

    private val appContext = context.applicationContext

    suspend fun addSongToPlaylist(playlistId: Long, song: SongEntity) {
        if (songDao.getSongById(song.id) == null) {
            songDao.insertSong(song)
        }
        val playlist = playlistDao.getPlaylist(playlistId) ?: return
        val ids = playlist.songIdsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
        if (song.id !in ids) ids.add(song.id)
        playlistDao.updatePlaylist(playlist.copy(songIdsCsv = ids.joinToString(",")))
    }

    companion object {
        private const val TRENDING_KEY = "trending"
        private const val TRENDING_CACHE_MS = 12 * 60 * 60 * 1000L
        private const val SEARCH_CACHE_MS = 12 * 60 * 60 * 1000L
        private const val WEEK_MS = 7 * 24 * 60 * 60 * 1000L
    }

    private suspend fun seedInitialDataIfEmpty() {
        val count = songDao.getSongCount()
        if (count >= 8) return

        val sampleSongs = listOf(
            SongEntity(
                id = "yt_IJq0yyWug1k",
                title = "Tum Hi Ho",
                artist = "Arijit Singh",
                album = "Aashiqui 2",
                durationMs = 262000L,
                audioUrl = "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview211/v4/77/59/c5/7759c5b2-b044-0e68-fea5-1fc4b84f312a/mzaf_3185920023072222766.plus.aac.p.m4a",
                albumArtUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music124/v4/2d/11/b9/2d11b994-b4fa-19eb-953d-70b472165e95/8903431566911_cover.jpg/600x600bb.jpg",
                lyricsLrc = """
                    [00:00.00] (Gentle Piano & Acoustic Melody)
                    [00:04.00] Hum tere bin ab reh nahi sakte
                    [00:13.00] Tere bina kya wajood mera
                    [00:22.00] Tujhse juda agar ho jayenge
                    [00:31.00] Toh khud se hi ho jayenge juda
                    [00:40.00] Kyunki tum hi ho, ab tum hi ho
                    [00:49.00] Zindagi ab tum hi ho
                    [00:58.00] Chain bhi, mera dard bhi
                    [00:67.00] Meri aashiqui ab tum hi ho
                    [00:78.00] (Feel the acoustic rhythm & bass)
                    [00:95.00] Tera mera rishta hai kaisa
                    [01:04.00] Ek pal door gawaara nahi
                """.trimIndent(),
                genre = "Bollywood Romance",
                mood = "Soulful",
                isLiked = true
            ),
            SongEntity(
                id = "yt_5Eqb_-j3FDA",
                title = "Pasoori",
                artist = "Ali Sethi x Shae Gill",
                album = "Coke Studio Season 14",
                durationMs = 224000L,
                audioUrl = "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview126/v4/62/33/1e/62331ea8-d1df-027d-fe75-ac16a519323d/mzaf_14381883946572745360.plus.aac.p.m4a",
                albumArtUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/f3/f9/06/f3f906c3-79d5-ac9a-5fdd-262048f955f9/cover.jpg/600x600bb.jpg",
                lyricsLrc = """
                    [00:00.00] (Agg laavan majboori nu)
                    [00:05.00] Aan jaan di pasoori nu
                    [00:13.00] Zahar bane haan teri pee jaavan
                    [00:21.00] Marjaavan ya jee jaavan
                    [00:29.00] Dil boliyan te aave
                    [00:37.00] Aavan te dil lag jaave
                    [00:45.00] Chad gaya mainu tera nasha
                    [00:53.00] Raawaan ch baithaan main tere
                """.trimIndent(),
                genre = "Indie Fusion",
                mood = "Groovy",
                isLiked = false
            ),
            SongEntity(
                id = "yt_ElZfdU54Cp8",
                title = "Apna Bana Le",
                artist = "Arijit Singh & Sachin-Jigar",
                album = "Bhediya",
                durationMs = 261000L,
                audioUrl = "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview211/v4/eb/27/61/eb2761c7-d606-0912-dff0-2dc6b69974bd/mzaf_2023722930851223219.plus.aac.p.m4a",
                albumArtUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music122/v4/2e/0b/c0/2e0bc070-112f-a827-6ad8-6bc64f7caaff/840214460180.png/600x600bb.jpg",
                lyricsLrc = """
                    [00:00.00] (Soft guitar strumming)
                    [00:06.00] Tu mera koi na hoke bhi kuch laage
                    [00:14.00] Kiya re jo bhi tune kaise kiya re
                    [00:22.00] Jiya ko mere baandh aise liya re
                    [00:30.00] Samajh ke bhi na samajh main saku
                    [00:38.00] Apna bana le piya, apna bana le piya
                    [00:46.00] Dil ke nagar mein shehar tu basa le piya
                """.trimIndent(),
                genre = "Bollywood",
                mood = "Heartfelt",
                isLiked = true
            ),
            SongEntity(
                id = "yt_Wv2rLZmbPMA",
                title = "Chaleya",
                artist = "Arijit Singh & Shilpa Rao",
                album = "Jawan",
                durationMs = 200000L,
                audioUrl = "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview211/v4/76/05/d9/7605d905-f631-517d-df7f-e162affcd414/mzaf_9976541859961700749.plus.aac.p.m4a",
                albumArtUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music126/v4/bb/f4/f5/bbf4f511-3c12-c25e-a475-b6d06faa8c13/8902894362047_cover.jpg/600x600bb.jpg",
                lyricsLrc = """
                    [00:00.00] (Anirudh Beat Drop)
                    [00:05.00] Ishq mein dil bana hai, ishq mein dil fanaa hai
                    [00:13.00] Jitna bhi roko dil ko, utna hi dil bada hai
                    [00:21.00] Chaleya teri ore chaleya
                    [00:28.00] Mera dil ab toh tera ho chukeya
                    [00:36.00] Dhadkan ne teri dhun pakad li
                    [00:44.00] Ishq tera ab mera hoke chaleya
                """.trimIndent(),
                genre = "Bollywood Dance",
                mood = "Energetic",
                isLiked = false
            ),
            SongEntity(
                id = "yt_VNs_cCtdbPc",
                title = "Brown Munde",
                artist = "AP Dhillon, Gurinder Gill",
                album = "Brown Munde",
                durationMs = 267000L,
                audioUrl = "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview211/v4/97/74/69/977469be-a9d5-35a7-80ad-ebe12a799ccc/mzaf_804867738726203367.plus.aac.p.m4a",
                albumArtUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/26/a3/ac/26a3ac64-69e4-95ec-80ab-1f5a477537d2/859742042973_cover.jpg/600x600bb.jpg",
                lyricsLrc = """
                    [00:00.00] (Trap Punjabi Beats)
                    [00:06.00] Desi jehe geet aa trappan jehi beat aa
                    [00:13.00] Sir kadd gajde speakeran ch wajde
                    [00:20.00] Brown munde, brown munde
                    [00:27.00] Dope shope maarde na, game vi vigaarde na
                    [00:34.00] Akhaan ch khumaari ae, yaari hi pyari ae
                    [00:41.00] Kamm saare end ne, yaaran naal trend ne
                    [00:48.00] Sun dhyan naal brown munde!
                """.trimIndent(),
                genre = "Punjabi Hip-Hop",
                mood = "Hype",
                isLiked = true
            ),
            SongEntity(
                id = "yt_BddP6PYo2gs",
                title = "Kesariya",
                artist = "Arijit Singh, Pritam",
                album = "Brahmastra",
                durationMs = 268000L,
                audioUrl = "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview221/v4/44/26/50/442650b7-256e-034a-380a-4bbf16e59e53/mzaf_272051127111758324.plus.aac.p.m4a",
                albumArtUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&auto=format&fit=crop&q=80",
                lyricsLrc = """
                    [00:00.00] (Romantic Flute & Acoustic Intro)
                    [00:06.00] Mujhko itna bataye koi
                    [00:12.00] Kaise tujhse dil na lagaye koi
                    [00:18.00] Rabba ne tujhko banane mein
                    [00:24.00] Kar di hai husn ki khaali tijoriyan
                    [00:30.00] Kajal ki siyahi se likhi hai tune
                    [00:36.00] Jaane kitno ki love storiyan
                    [00:43.00] Kesariya tera ishq hai piya
                    [00:49.00] Rang jaaun jo main haath lagaun
                    [00:55.00] Din beete saara teri fikr mein
                    [01:01.00] Rain saari teri khair manaun
                """.trimIndent(),
                genre = "Bollywood",
                mood = "Romance",
                isLiked = true
            ),
            SongEntity(
                id = "yt_n_FCrCQ6-9U",
                title = "295",
                artist = "Sidhu Moose Wala",
                album = "Moosetape",
                durationMs = 270000L,
                audioUrl = "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview221/v4/6c/2b/b5/6c2bb54c-cbb6-558e-87e6-cc750aa39c54/mzaf_11043876483606963092.plus.aac.p.m4a",
                albumArtUrl = "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=600&auto=format&fit=crop&q=80",
                lyricsLrc = """
                    [00:00.00] (Heavy 808 Punjabi Beat Drops)
                    [00:06.00] Sidhu Moose Wala
                    [00:10.00] The Kidd!
                    [00:14.00] Dass kihda aithey sach bolda
                    [00:19.00] Kihda rab de naal match karda
                    [00:24.00] Nitt nawa koyi vivaad khada
                    [00:29.00] Jeda sach bole ohi baad khada
                    [00:35.00] Dhara 295 je lagdi ae
                    [00:41.00] Kise sach bolan te khed chaldi ae
                    [00:48.00] Aithe sachian te pabandi ae
                    [00:54.00] Jithe jhooth di chadhdi chandi ae
                """.trimIndent(),
                genre = "Punjabi",
                mood = "Party",
                isLiked = true
            ),
            SongEntity(
                id = "yt_mH_LFkWxpI0",
                title = "Lover",
                artist = "Diljit Dosanjh",
                album = "MoonChild Era",
                durationMs = 210000L,
                audioUrl = "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview211/v4/a9/82/78/a9827837-5ca7-1fe3-fc24-3dedeffb86e4/mzaf_4678729972431007397.plus.aac.p.m4a",
                albumArtUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&auto=format&fit=crop&q=80",
                lyricsLrc = """
                    [00:00.00] (Synthesizer & Upbeat Pop Rhythm)
                    [00:05.00] Diljit Dosanjh!
                    [00:08.00] Intense music
                    [00:12.00] Tera ni lover, tera ni lover
                    [00:17.00] Karda pyaar tenu kina sara
                    [00:22.00] Vekh le akhiyan vich tu yaara
                    [00:27.00] Tere bina lagda nahi dil mera
                    [00:32.00] Tu hi ban gayi ae sahara
                    [00:38.00] Tera ni lover, tera ni lover!
                """.trimIndent(),
                genre = "Punjabi Pop",
                mood = "Party",
                isLiked = false
            )
        )

        songDao.insertSongs(sampleSongs)

        // Seed some sample comments
        val sampleComments = listOf(
            CommentEntity(
                songId = "yt_IJq0yyWug1k",
                userName = "Aarav Sharma",
                userAvatarColor = 0xFFFF2A6DL,
                content = "This beat transition at 00:38 is literally goosebumps! 🔥",
                songTimestampMs = 38000L,
                likesCount = 142,
                isLikedByMe = true
            ),
            CommentEntity(
                songId = "yt_IJq0yyWug1k",
                userName = "Sneha Patel",
                userAvatarColor = 0xFF05D9E8L,
                content = "Listening to this while late night driving hits completely different 🌌",
                songTimestampMs = 60000L,
                likesCount = 89,
                isLikedByMe = false
            ),
            CommentEntity(
                songId = "yt_5Eqb_-j3FDA",
                userName = "Rohan Verma",
                userAvatarColor = 0xFFFFE600L,
                content = "The synced lyrics and flash beat on Resso are unbeatable! Love this vibe.",
                songTimestampMs = 15000L,
                likesCount = 37,
                isLikedByMe = false
            ),
            CommentEntity(
                songId = "yt_BddP6PYo2gs",
                userName = "Priya Roy",
                userAvatarColor = 0xFF00F5D4L,
                content = "Arijit Singh voice + Resso lyrics poster studio = perfection ❤️",
                songTimestampMs = 24000L,
                likesCount = 56,
                isLikedByMe = false
            )
        )
        sampleComments.forEach { commentDao.insertComment(it) }

        // Seed sample playlist
        playlistDao.insertPlaylist(
            PlaylistEntity(
                name = "Late Night Vibes ✨",
                description = "Romantic, soulful and high-energy hits for late night thoughts",
                songIdsCsv = "yt_IJq0yyWug1k,yt_5Eqb_-j3FDA,yt_BddP6PYo2gs,yt_ElZfdU54Cp8"
            )
        )
    }
}
