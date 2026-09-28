package com.example.data.repository

import android.content.Context
import androidx.room.Room
import com.example.data.db.AppDatabase
import com.example.data.model.CommentEntity
import com.example.data.model.PlaylistEntity
import com.example.data.model.SongEntity
import com.example.data.model.VibeEntity
import com.example.data.network.MusicSearchService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class MusicRepository(context: Context) {
    private val db = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "resso_music.db"
    ).build()

    val songDao = db.songDao()
    val vibeDao = db.vibeDao()
    val commentDao = db.commentDao()
    val playlistDao = db.playlistDao()
    val searchService = MusicSearchService()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            seedInitialDataIfEmpty()
        }
    }

    fun getAllSongs(): Flow<List<SongEntity>> = songDao.getAllSongs()
    fun getLikedSongs(): Flow<List<SongEntity>> = songDao.getLikedSongs()
    suspend fun getSongById(id: String): SongEntity? = songDao.getSongById(id)
    suspend fun toggleLike(id: String, currentLiked: Boolean) = songDao.setLiked(id, !currentLiked)
    suspend fun insertCustomSong(song: SongEntity) = songDao.insertSong(song)
    suspend fun updateSongVibe(songId: String, vibeUri: String) = songDao.updateSongVibe(songId, vibeUri)

    // Online & Local Search
    suspend fun searchSongsOnline(query: String): List<SongEntity> = searchService.searchSongs(query)
    suspend fun getTrendingSongs(): List<SongEntity> = searchService.getTrendingSongs()
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

    private suspend fun seedInitialDataIfEmpty() {
        val sampleSongs = listOf(
            SongEntity(
                id = "song_1",
                title = "Starfall Echoes",
                artist = "Luna Eclipse",
                album = "Cosmic Dreams",
                durationMs = 210000L,
                audioUrl = "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Kangaroo_MusiQue_-_The_Neverwritten_Role_Playing_Game.mp3",
                albumArtUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=600&auto=format&fit=crop&q=80",
                lyricsLrc = """
                    [00:00.00] (Instrumental intro with cosmic reverb)
                    [00:08.50] Floating high above the city neon lights
                    [00:15.20] Walking through the static of these velvet nights
                    [00:23.00] You said the universe was made for two
                    [00:30.40] Every starfall echoes back to you
                    [00:38.00] (Feel the bass drop)
                    [00:44.20] Catch the wavelength, riding on the frequency
                    [00:52.00] Drifting closer, gravity surrounding me
                    [00:59.80] Hold my hand before the skyline disappears
                    [01:07.50] We've been dreaming here for thousand years
                    [01:15.00] Every beat is a pulse in the midnight air
                    [01:22.50] Look around, no one else is there
                    [01:30.00] Just you and the cosmic sound
                    [01:38.00] Spinning till the morning comes around
                """.trimIndent(),
                genre = "Synthwave",
                mood = "Chill",
                isLiked = true
            ),
            SongEntity(
                id = "song_2",
                title = "Neon Velocity",
                artist = "CyberVibe",
                album = "Tokyo Overdrive",
                durationMs = 185000L,
                audioUrl = "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Sevish_-__nbsp_.mp3",
                albumArtUrl = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=600&auto=format&fit=crop&q=80",
                lyricsLrc = """
                    [00:00.00] (High tempo beat starts pumping)
                    [00:06.00] 120 on the dash, racing down Shinjuku
                    [00:12.50] Neon reflections in the rearview mirror
                    [00:18.00] Can't slow down now, the beat is getting clearer
                    [00:24.00] Speed of sound, electricity in the vein
                    [00:30.00] Dancing in the cybernetic summer rain
                    [00:37.00] Jump into the pulse!
                    [00:43.00] Turn the volume up, let the speakers shake
                    [00:50.00] This is the rhythm we were born to make
                    [00:57.00] Tokyo skyline glowing in pink and blue
                    [01:04.00] I'm staying up all night with you
                """.trimIndent(),
                genre = "EDM",
                mood = "Party",
                isLiked = false
            ),
            SongEntity(
                id = "song_3",
                title = "Rainy Café Melancholy",
                artist = "Coffee & Tape",
                album = "Study Chill Beats",
                durationMs = 160000L,
                audioUrl = "https://commondatastorage.googleapis.com/codeskulptor-assets/Epoq-Lepidoptera.ogg",
                albumArtUrl = "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=600&auto=format&fit=crop&q=80",
                lyricsLrc = """
                    [00:00.00] (Rain sounds and soft vinyl crackle)
                    [00:07.00] Steam rises from the porcelain cup
                    [00:14.00] Window pane watching drops trickle up
                    [00:22.00] Turn the page of an old paper book
                    [00:30.00] Finding memories in every quiet nook
                    [00:39.00] Soft Rhodes piano playing in the afternoon
                    [00:48.00] Wishing the storm won't finish too soon
                    [00:58.00] Warm sweater, thoughts drifting away
                    [01:08.00] Just another cozy rainy day
                """.trimIndent(),
                genre = "Lo-Fi",
                mood = "Focus",
                isLiked = true
            ),
            SongEntity(
                id = "song_4",
                title = "Golden Hour Romance",
                artist = "Sunkissed Avenue",
                album = "Coastline",
                durationMs = 200000L,
                audioUrl = "https://commondatastorage.googleapis.com/codeskulptor-demos/pyman_assets/ateapill.ogg",
                albumArtUrl = "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=600&auto=format&fit=crop&q=80",
                lyricsLrc = """
                    [00:00.00] (Acoustic guitar and gentle seaside breeze)
                    [00:09.00] Sunlight tangling in your hair
                    [00:16.00] Ocean breeze with warm salt air
                    [00:24.00] You looked at me and laughed out loud
                    [00:32.00] Two of us far away from the crowd
                    [00:40.00] If time could freeze right at sunset
                    [00:49.00] This is a memory I'll never forget
                    [00:58.00] Barefoot footprints along the shore
                    [01:06.00] I could love you forevermore
                """.trimIndent(),
                genre = "Indie",
                mood = "Romance",
                isLiked = false
            ),
            SongEntity(
                id = "song_5",
                title = "Hyperdrive Overload",
                artist = "BassForge",
                album = "Workout Fuel",
                durationMs = 175000L,
                audioUrl = "https://commondatastorage.googleapis.com/codeskulptor-assets/jump.ogg",
                albumArtUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=600&auto=format&fit=crop&q=80",
                lyricsLrc = """
                    [00:00.00] (Heavy countdown synth builds)
                    [00:05.00] 3, 2, 1 - Unleash the power!
                    [00:10.00] Feel the adrenaline rise in this hour
                    [00:16.00] Heart rate pumping, muscles tight
                    [00:22.00] We're conquering the mountain tonight
                    [00:29.00] Push through the limit, break through the wall
                    [00:36.00] Stand up tall, never gonna fall
                    [00:44.00] Maximum power!
                    [00:52.00] One more rep, one more mile
                    [01:00.00] Crossing the finish line with a smile
                """.trimIndent(),
                genre = "Rock / Workout",
                mood = "Workout",
                isLiked = false
            ),
            SongEntity(
                id = "song_kesariya",
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
                id = "song_295",
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
                id = "song_lover",
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
                songId = "song_1",
                userName = "Aarav Sharma",
                userAvatarColor = 0xFFFF2A6DL,
                content = "This beat transition at 00:38 is literally goosebumps! 🔥",
                songTimestampMs = 38000L,
                likesCount = 142,
                isLikedByMe = true
            ),
            CommentEntity(
                songId = "song_1",
                userName = "Sneha Patel",
                userAvatarColor = 0xFF05D9E8L,
                content = "Listening to this while late night driving hits completely different 🌌",
                songTimestampMs = 60000L,
                likesCount = 89,
                isLikedByMe = false
            ),
            CommentEntity(
                songId = "song_1",
                userName = "Rohan Verma",
                userAvatarColor = 0xFFFFE600L,
                content = "The synced lyrics feature on Resso is unbeatable! Love this vibe.",
                songTimestampMs = 15000L,
                likesCount = 37,
                isLikedByMe = false
            ),
            CommentEntity(
                songId = "song_2",
                userName = "Priya Roy",
                userAvatarColor = 0xFF00F5D4L,
                content = "Added this straight to my Tokyo Night drive playlist! 🏎️💨",
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
                description = "Synthwave, lo-fi and aesthetic tunes for 2 AM thoughts",
                songIdsCsv = "song_1,song_3,song_4"
            )
        )
    }
}
