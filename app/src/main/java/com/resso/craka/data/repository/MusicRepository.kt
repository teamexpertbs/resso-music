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
    private val appContext = context.applicationContext
    private val db = Room.databaseBuilder(
        appContext,
        AppDatabase::class.java,
        "resso_music.db"
    ).build()

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
            updateDefaultStreamsIfEmpty()
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

    fun peekSearch(query: String): List<SongEntity> {
        val key = "search_${query.trim().lowercase()}"
        return catalogCache.read(key, WEEK_MS).orEmpty()
    }

    fun peekSearches(queries: List<String>): List<SongEntity> {
        val merged = LinkedHashMap<String, SongEntity>()
        queries.forEach { query ->
            peekSearch(query).forEach { song -> merged.putIfAbsent(song.id, song) }
        }
        return merged.values.toList()
    }
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

    private suspend fun updateDefaultStreamsIfEmpty() {
        val streamMap = mapOf(
            "saavn_payal" to "https://aac.saavncdn.com/173/ad5df053bfb2a4755cbb6c74e6183406_320.mp4",
            "saavn_tauba_tauba" to "https://aac.saavncdn.com/992/5d44da8bc1d78fb72d18b701d758fd1f_320.mp4",
            "saavn_aaj_ki_raat" to "https://aac.saavncdn.com/373/36b1b3637cdeedfaa9a9012453948aa6_320.mp4",
            "saavn_kesariya" to "https://aac.saavncdn.com/871/c2febd353f3a076a406fa37510f31f9f_320.mp4",
            "saavn_millionaire" to "https://aac.saavncdn.com/173/4528cbe9b2ceba863a8e2e92c2da2882_320.mp4",
            "yt_IJq0yyWug1k" to "https://aac.saavncdn.com/264/3df8a213e4b7858cfa9900c430fa728c_320.mp4",
            "yt_5Eqb_-j3FDA" to "https://aac.saavncdn.com/392/689a74aa9bc3b624b45ce7508cf31c77_320.mp4",
            "yt_ElZfdU54Cp8" to "https://aac.saavncdn.com/308/879685a21eb234c98e169527ecb82b6b_320.mp4",
            "yt_Wv2rLZmbPMA" to "https://aac.saavncdn.com/475/8863f6087b32d326dae004a600d9a690_320.mp4",
            "yt_VNs_cCtdbPc" to "https://aac.saavncdn.com/973/7710b144cb742f1cf59f5b610c144a6d_320.mp4",
            "yt_BddP6PYo2gs" to "https://aac.saavncdn.com/871/c2febd353f3a076a406fa37510f31f9f_320.mp4",
            "yt_n_FCrCQ6-9U" to "https://aac.saavncdn.com/431/e4f20532454a85fa0a94460f4eb78996_320.mp4",
            "yt_mH_LFkWxpI0" to "https://aac.saavncdn.com/745/e4db870813e3bc3101d2ae0a09e0722c_320.mp4"
        )
        for ((id, url) in streamMap) {
            val song = songDao.getSongById(id)
            if (song != null && song.audioUrl.isBlank()) {
                songDao.insertSong(song.copy(audioUrl = url))
            }
        }
    }

    private suspend fun seedInitialDataIfEmpty() {
        val count = songDao.getSongCount()
        if (count >= 5) return

        val sampleSongs = listOf(
            SongEntity(
                id = "saavn_payal",
                title = "Payal",
                artist = "Yo Yo Honey Singh, Paradox",
                album = "GLORY",
                durationMs = 221000L,
                audioUrl = "https://aac.saavncdn.com/173/ad5df053bfb2a4755cbb6c74e6183406_320.mp4",
                albumArtUrl = "https://c.saavncdn.com/173/GLORY-Hindi-2024-20240826144815-500x500.jpg",
                lyricsLrc = """
                    [00:00.00] (Yo Yo Honey Singh! Paradox!)
                    [00:05.00] Chhan chhan chhan chhan baaje payal
                    [00:10.00] Teri chhan chhan ne kar daala ghayal
                    [00:15.00] Aankhon mein surma, kaano mein baali
                    [00:20.00] Lagti hai tu toh qaatil niraali
                    [00:25.00] Chhan chhan baaje payal teri
                    [00:30.00] Loot gayi duniya saari meri
                    [00:35.00] Baby chal mere sang thoda jhoom le
                    [00:40.00] Aaj ki raat saare gham bhool le
                """.trimIndent(),
                genre = "Desi Hip-Hop",
                mood = "Party",
                isLiked = true
            ),
            SongEntity(
                id = "saavn_tauba_tauba",
                title = "Tauba Tauba",
                artist = "Karan Aujla",
                album = "Bad Newz",
                durationMs = 208000L,
                audioUrl = "https://aac.saavncdn.com/992/5d44da8bc1d78fb72d18b701d758fd1f_320.mp4",
                albumArtUrl = "https://c.saavncdn.com/992/Bad-Newz-Hindi-2024-20240709080001-500x500.jpg",
                lyricsLrc = """
                    [00:00.00] (Yeah, Karan Aujla!)
                    [00:06.00] Husan tera tauba tauba
                    [00:12.00] Chadhdi jawani tauba tauba
                    [00:18.00] Akh da nishana tauba tauba
                    [00:24.00] Dil kare bechain tauba tauba
                    [00:30.00] Tauba tauba, tauba tauba!
                """.trimIndent(),
                genre = "Punjabi Pop",
                mood = "Groovy",
                isLiked = true
            ),
            SongEntity(
                id = "saavn_aaj_ki_raat",
                title = "Aaj Ki Raat",
                artist = "Sachin-Jigar, Madhubanti Bagchi, Divya Kumar",
                album = "Stree 2",
                durationMs = 228000L,
                audioUrl = "https://aac.saavncdn.com/373/36b1b3637cdeedfaa9a9012453948aa6_320.mp4",
                albumArtUrl = "https://c.saavncdn.com/373/Stree-2-Hindi-2024-20240827150130-500x500.jpg",
                lyricsLrc = """
                    [00:00.00] (Thumping Dance Beats)
                    [00:06.00] Aaj ki raat maza husn ka aankhon se lijiye
                    [00:14.00] Dil mein bitha ke rakhiye
                    [00:20.00] Thoda sa hosh khoyi, thoda behak lijiye
                    [00:28.00] Aaj ki raat maza husn ka aankhon se lijiye
                """.trimIndent(),
                genre = "Bollywood Item",
                mood = "Dance",
                isLiked = true
            ),
            SongEntity(
                id = "saavn_kesariya",
                title = "Kesariya",
                artist = "Arijit Singh, Pritam",
                album = "Brahmastra",
                durationMs = 268000L,
                audioUrl = "https://aac.saavncdn.com/871/c2febd353f3a076a406fa37510f31f9f_320.mp4",
                albumArtUrl = "https://c.saavncdn.com/871/Brahmastra-Hindi-2022-20220717092820-500x500.jpg",
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
                """.trimIndent(),
                genre = "Bollywood",
                mood = "Romance",
                isLiked = true
            ),
            SongEntity(
                id = "saavn_millionaire",
                title = "Millionaire",
                artist = "Yo Yo Honey Singh",
                album = "GLORY",
                durationMs = 200000L,
                audioUrl = "https://aac.saavncdn.com/173/4528cbe9b2ceba863a8e2e92c2da2882_320.mp4",
                albumArtUrl = "https://c.saavncdn.com/173/GLORY-Hindi-2024-20240826144815-500x500.jpg",
                lyricsLrc = """
                    [00:00.00] (Yo Yo Honey Singh!)
                    [00:05.00] Main ban gaya millionaire
                    [00:10.00] Jeb mein dollar, aankhon mein flair
                    [00:15.00] Duniya dekhe meri raftaar
                    [00:20.00] Desi hip hop ka superstar!
                """.trimIndent(),
                genre = "Hip-Hop",
                mood = "Hype",
                isLiked = true
            ),
            SongEntity(
                id = "yt_IJq0yyWug1k",
                title = "Tum Hi Ho",
                artist = "Arijit Singh",
                album = "Aashiqui 2",
                durationMs = 262000L,
                audioUrl = "https://aac.saavncdn.com/264/3df8a213e4b7858cfa9900c430fa728c_320.mp4",
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
                    [01:07.00] Meri aashiqui ab tum hi ho
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
                audioUrl = "https://aac.saavncdn.com/392/689a74aa9bc3b624b45ce7508cf31c77_320.mp4",
                albumArtUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/f3/f9/06/f3f906c3-79d5-ac9a-5fdd-262048f955f9/cover.jpg/600x600bb.jpg",
                lyricsLrc = """
                    [00:00.00] (Agg laavan majboori nu)
                    [00:05.00] Aan jaan di pasoori nu
                    [00:13.00] Zahar bane haan teri pee jaavan
                    [00:21.00] Marjaavan ya jee jaavan
                    [00:29.00] Dil boliyan te aave
                    [00:37.00] Aavan te dil lag jaave
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
                audioUrl = "https://aac.saavncdn.com/308/879685a21eb234c98e169527ecb82b6b_320.mp4",
                albumArtUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music122/v4/2e/0b/c0/2e0bc070-112f-a827-6ad8-6bc64f7caaff/840214460180.png/600x600bb.jpg",
                lyricsLrc = """
                    [00:00.00] (Soft guitar strumming)
                    [00:06.00] Tu mera koi na hoke bhi kuch laage
                    [00:14.00] Kiya re jo bhi tune kaise kiya re
                    [00:22.00] Jiya ko mere baandh aise liya re
                    [00:30.00] Samajh ke bhi na samajh main saku
                    [00:38.00] Apna bana le piya, apna bana le piya
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
                audioUrl = "https://aac.saavncdn.com/475/8863f6087b32d326dae004a600d9a690_320.mp4",
                albumArtUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music126/v4/bb/f4/f5/bbf4f511-3c12-c25e-a475-b6d06faa8c13/8902894362047_cover.jpg/600x600bb.jpg",
                lyricsLrc = """
                    [00:00.00] (Anirudh Beat Drop)
                    [00:05.00] Ishq mein dil bana hai, ishq mein dil fanaa hai
                    [00:13.00] Jitna bhi roko dil ko, utna hi dil bada hai
                    [00:21.00] Chaleya teri ore chaleya
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
                audioUrl = "https://aac.saavncdn.com/973/7710b144cb742f1cf59f5b610c144a6d_320.mp4",
                albumArtUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/26/a3/ac/26a3ac64-69e4-95ec-80ab-1f5a477537d2/859742042973_cover.jpg/600x600bb.jpg",
                lyricsLrc = """
                    [00:00.00] (Trap Punjabi Beats)
                    [00:06.00] Desi jehe geet aa trappan jehi beat aa
                    [00:13.00] Sir kadd gajde speakeran ch wajde
                    [00:20.00] Brown munde, brown munde
                """.trimIndent(),
                genre = "Punjabi Hip-Hop",
                mood = "Hype",
                isLiked = true
            ),
            SongEntity(
                id = "yt_n_FCrCQ6-9U",
                title = "295",
                artist = "Sidhu Moose Wala",
                album = "Moosetape",
                durationMs = 270000L,
                audioUrl = "https://aac.saavncdn.com/431/e4f20532454a85fa0a94460f4eb78996_320.mp4",
                albumArtUrl = "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=600&auto=format&fit=crop&q=80",
                lyricsLrc = """
                    [00:00.00] (Heavy 808 Punjabi Beat Drops)
                    [00:06.00] Sidhu Moose Wala
                    [00:10.00] The Kidd!
                    [00:14.00] Dass kihda aithey sach bolda
                    [00:19.00] Kihda rab de naal match karda
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
                audioUrl = "https://aac.saavncdn.com/745/e4db870813e3bc3101d2ae0a09e0722c_320.mp4",
                albumArtUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&auto=format&fit=crop&q=80",
                lyricsLrc = """
                    [00:00.00] (Synthesizer & Upbeat Pop Rhythm)
                    [00:05.00] Diljit Dosanjh!
                    [00:08.00] Intense music
                    [00:12.00] Tera ni lover, tera ni lover
                """.trimIndent(),
                genre = "Punjabi Pop",
                mood = "Party",
                isLiked = false
            )
        )

        sampleSongs.forEach { songDao.insertSong(it) }

        // Seed some sample comments
        val sampleComments = listOf(
            CommentEntity(
                songId = "saavn_payal",
                userName = "Aarav Sharma",
                userAvatarColor = 0xFFFF2A6DL,
                content = "Honey Singh & Paradox colab is fire! Repeat mode on 🔥",
                songTimestampMs = 25000L,
                likesCount = 142,
                isLikedByMe = true
            ),
            CommentEntity(
                songId = "saavn_payal",
                userName = "Sneha Patel",
                userAvatarColor = 0xFF05D9E8L,
                content = "Pure nostalgia vibe with modern 320kbps beats!",
                songTimestampMs = 60000L,
                likesCount = 89,
                isLikedByMe = false
            ),
            CommentEntity(
                songId = "yt_IJq0yyWug1k",
                userName = "Rohan Verma",
                userAvatarColor = 0xFFFFE600L,
                content = "The synced lyrics and flash beat on Resso are unbeatable! Love this vibe.",
                songTimestampMs = 15000L,
                likesCount = 37,
                isLikedByMe = false
            ),
            CommentEntity(
                songId = "saavn_kesariya",
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
                songIdsCsv = "saavn_payal,saavn_tauba_tauba,saavn_aaj_ki_raat,saavn_kesariya,saavn_millionaire"
            )
        )
    }
}
