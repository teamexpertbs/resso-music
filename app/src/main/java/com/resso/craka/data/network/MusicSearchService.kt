package com.resso.craka.data.network

import android.util.Base64
import android.util.Log
import com.resso.craka.BuildConfig
import com.resso.craka.data.model.SongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class MusicSearchService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "MusicSearchService"
    }

    private suspend fun fetchText(request: Request): Pair<Int, String?> {
        return suspendCancellableCoroutine { cont ->
            val call = client.newCall(request)
            cont.invokeOnCancellation { call.cancel() }
            try {
                call.execute().use { response ->
                    val text = response.body?.string()
                    if (cont.isActive) cont.resume(response.code to text)
                }
            } catch (e: Exception) {
                if (cont.isActive) cont.resumeWithException(e)
            }
        }
    }

    private val youtubeApiKey = BuildConfig.YOUTUBE_API_KEY.trim()
    private val spotifyClientId = BuildConfig.SPOTIFY_CLIENT_ID.trim()
    private val spotifyClientSecret = BuildConfig.SPOTIFY_CLIENT_SECRET.trim()
    private var spotifyToken: String? = null
    private var spotifyTokenExpiryMs: Long = 0L

    suspend fun searchSongs(query: String, limit: Int = 30): List<SongEntity> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return@withContext emptyList()

        val spotifySongs = searchSpotifySongs(trimmed, 8)
        val playableSpotify = attachYouTubePlayback(spotifySongs.take(3))
        if (playableSpotify.size >= 3) return@withContext playableSpotify
        val youtubeSongs = searchYouTubeSongs(trimmed, 6)
        val merged = (playableSpotify + youtubeSongs).distinctBy { it.id }
        merged
    }

    suspend fun getTrendingSongs(limit: Int = 6): List<SongEntity> = withContext(Dispatchers.IO) {
        val spotifyHits = searchSpotifySongs("Bollywood Hits", limit)
        val playable = attachYouTubePlayback(spotifyHits.take(4))
        if (playable.isNotEmpty()) playable else searchYouTubeSongs("Top Bollywood Hindi Songs", limit)
    }

    private suspend fun attachYouTubePlayback(tracks: List<SongEntity>): List<SongEntity> {
        if (tracks.isEmpty()) return emptyList()
        if (youtubeApiKey.isBlank()) return tracks.filter { it.audioUrl.isNotBlank() }
        return coroutineScope {
            tracks.map { track ->
                async {
                    val videos = searchYouTubeSongs("${track.title} ${track.artist}", 3)
                    val match = videos.firstOrNull { titlesMatch(track.title, it.title) } ?: videos.firstOrNull()
                    if (match == null || !titlesMatch(track.title, match.title)) return@async null
                    match.copy(
                        title = track.title,
                        artist = track.artist,
                        album = track.album,
                        albumArtUrl = track.albumArtUrl.ifBlank { match.albumArtUrl },
                        mood = track.mood
                    )
                }
            }.awaitAll().filterNotNull()
        }
    }

    suspend fun searchYouTubeSongs(query: String, limit: Int = 25): List<SongEntity> = withContext(Dispatchers.IO) {
        if (youtubeApiKey.isBlank()) {
            Log.i(TAG, "YOUTUBE_API_KEY is not configured.")
            return@withContext emptyList()
        }

        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val searchUrl = "https://www.googleapis.com/youtube/v3/search" +
                    "?part=snippet" +
                    "&maxResults=$limit" +
                    "&q=$encoded" +
                    "&type=video" +
                    "&videoEmbeddable=true" +
                    "&videoSyndicated=true" +
                    "&key=$youtubeApiKey"

            val request = Request.Builder()
                .url(searchUrl)
                .header("User-Agent", "RessoMusicApp/1.0 (Android)")
                .build()

            val (code, bodyString) = fetchText(request)
            if (code !in 200..299 || bodyString == null) {
                Log.w(TAG, "YouTube search returned code: $code")
                return@withContext emptyList()
            }
            val json = JSONObject(bodyString)
            val items = json.optJSONArray("items") ?: return@withContext emptyList()

            val rawSongs = mutableListOf<RawYouTubeSong>()
            val videoIds = mutableListOf<String>()

            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                val idObj = item.optJSONObject("id") ?: continue
                val videoId = idObj.optString("videoId", "")
                if (videoId.isEmpty()) continue

                val snippet = item.optJSONObject("snippet") ?: continue
                val rawTitle = snippet.optString("title", "")
                val channelTitle = snippet.optString("channelTitle", "Official Artist")

                val thumbnails = snippet.optJSONObject("thumbnails")
                val thumbUrl = thumbnails?.optJSONObject("high")?.optString("url")
                    ?: thumbnails?.optJSONObject("medium")?.optString("url")
                    ?: thumbnails?.optJSONObject("default")?.optString("url")
                    ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

                videoIds.add(videoId)
                rawSongs.add(RawYouTubeSong(videoId, rawTitle, channelTitle, thumbUrl))
            }

            if (videoIds.isEmpty()) return@withContext emptyList()

            // Fetch video durations in single batch query
            val durationsMap = fetchVideoDurations(videoIds)

            val songs = rawSongs.map { raw ->
                val durationMs = durationsMap[raw.videoId] ?: 210000L
                val cleanTitle = cleanYouTubeTitle(decodeHtmlEntities(raw.title))
                val cleanArtist = decodeHtmlEntities(raw.channelTitle)
                val mood = mapTextToMood("$cleanTitle $cleanArtist")

                SongEntity(
                    id = "yt_${raw.videoId}",
                    title = cleanTitle,
                    artist = cleanArtist,
                    album = "Official Stream",
                    durationMs = durationMs,
                    audioUrl = "https://www.youtube.com/watch?v=${raw.videoId}",
                    albumArtUrl = raw.thumbnailUrl,
                    lyricsLrc = "",
                    genre = "Music",
                    mood = mood,
                    isLiked = false,
                    isCustomUpload = false
                )
            }
            val fullSongs = songs.filter { it.durationMs in 75_000L..900_000L }
            if (fullSongs.isNotEmpty()) fullSongs else songs
        } catch (e: Exception) {
            Log.e(TAG, "YouTube API error: ${e.message}", e)
            emptyList()
        }
    }

    private suspend fun fetchVideoDurations(videoIds: List<String>): Map<String, Long> {
        val durations = mutableMapOf<String, Long>()
        try {
            val idsChunk = videoIds.joinToString(",")
            val url = "https://www.googleapis.com/youtube/v3/videos" +
                    "?part=contentDetails" +
                    "&id=$idsChunk" +
                    "&key=$youtubeApiKey"

            val request = Request.Builder().url(url).build()
            val (code, bodyString) = fetchText(request)
            if (code !in 200..299 || bodyString == null) return durations
            val json = JSONObject(bodyString)
            val items = json.optJSONArray("items") ?: return durations

            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                val id = item.optString("id", "")
                val contentDetails = item.optJSONObject("contentDetails")
                val durationIso = contentDetails?.optString("duration", "") ?: ""
                val durationMs = parseIsoDurationMs(durationIso)
                durations[id] = durationMs
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching video durations: ${e.message}")
        }
        return durations
    }

    private fun parseIsoDurationMs(isoDuration: String): Long {
        try {
            var temp = isoDuration.removePrefix("PT")
            var hours = 0L
            var minutes = 0L
            var seconds = 0L

            if (temp.contains("H")) {
                val parts = temp.split("H")
                hours = parts[0].toLongOrNull() ?: 0L
                temp = if (parts.size > 1) parts[1] else ""
            }
            if (temp.contains("M")) {
                val parts = temp.split("M")
                minutes = parts[0].toLongOrNull() ?: 0L
                temp = if (parts.size > 1) parts[1] else ""
            }
            if (temp.contains("S")) {
                val parts = temp.split("S")
                seconds = parts[0].toLongOrNull() ?: 0L
            }
            val totalMs = (hours * 3600 + minutes * 60 + seconds) * 1000L
            return if (totalMs > 1000L) totalMs else 210000L
        } catch (_: Exception) {
            return 210000L
        }
    }

    suspend fun searchSpotifySongs(query: String, limit: Int = 25): List<SongEntity> = withContext(Dispatchers.IO) {
        val token = spotifyAccessToken() ?: return@withContext emptyList()
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://api.spotify.com/v1/search?type=track&market=IN&limit=$limit&q=$encoded"
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $token")
                .header("User-Agent", "RessoMusicApp/1.0 (Android)")
                .build()
            val (code, body) = fetchText(request)
            if (code !in 200..299 || body == null) {
                Log.w(TAG, "Spotify search returned code: $code")
                return@withContext emptyList()
            }
            val tracks = JSONObject(body).optJSONObject("tracks")?.optJSONArray("items")
                ?: return@withContext emptyList()
            val songs = mutableListOf<SongEntity>()
            for (i in 0 until tracks.length()) {
                val track = tracks.getJSONObject(i)
                val trackId = track.optString("id", "")
                val title = track.optString("name", "")
                if (trackId.isBlank() || title.isBlank()) continue
                val artists = track.optJSONArray("artists")
                val artist = buildString {
                    if (artists != null) {
                        for (a in 0 until artists.length()) {
                            if (isNotEmpty()) append(", ")
                            append(artists.getJSONObject(a).optString("name", ""))
                        }
                    }
                }.ifBlank { "Spotify" }
                val albumObj = track.optJSONObject("album")
                val album = albumObj?.optString("name", "Single") ?: "Single"
                val images = albumObj?.optJSONArray("images")
                val art = images?.optJSONObject(0)?.optString("url").orEmpty()
                val preview = track.optString("preview_url", "")
                val durationMs = track.optLong("duration_ms", 180000L)
                songs.add(
                    SongEntity(
                        id = "sp_$trackId",
                        title = title,
                        artist = artist,
                        album = album,
                        durationMs = durationMs,
                        audioUrl = preview,
                        albumArtUrl = art.ifBlank {
                            "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80"
                        },
                        lyricsLrc = "",
                        genre = "Music",
                        mood = mapTextToMood("$title $artist"),
                        isLiked = false,
                        isCustomUpload = false
                    )
                )
            }
            songs
        } catch (e: Exception) {
            Log.e(TAG, "Spotify search error: ${e.message}", e)
            emptyList()
        }
    }

    private suspend fun spotifyAccessToken(): String? {
        if (spotifyClientId.isBlank() || spotifyClientSecret.isBlank()) return null
        val now = System.currentTimeMillis()
        spotifyToken?.let { cached ->
            if (now < spotifyTokenExpiryMs) return cached
        }
        return try {
            val basic = Base64.encodeToString(
                "$spotifyClientId:$spotifyClientSecret".toByteArray(),
                Base64.NO_WRAP
            )
            val form = FormBody.Builder().add("grant_type", "client_credentials").build()
            val request = Request.Builder()
                .url("https://accounts.spotify.com/api/token")
                .header("Authorization", "Basic $basic")
                .post(form)
                .build()
            val (code, body) = fetchText(request)
            if (code !in 200..299 || body == null) {
                Log.w(TAG, "Spotify token request failed: $code")
                return null
            }
            val json = JSONObject(body)
            val token = json.optString("access_token", "")
            val expiresIn = json.optLong("expires_in", 3600L)
            if (token.isBlank()) return null
            spotifyToken = token
            spotifyTokenExpiryMs = now + (expiresIn - 30) * 1000L
            token
        } catch (e: Exception) {
            Log.w(TAG, "Spotify auth error: ${e.message}")
            null
        }
    }

    private fun mergeCatalog(youtube: List<SongEntity>, spotify: List<SongEntity>): List<SongEntity> {
        if (spotify.isEmpty()) return youtube
        if (youtube.isEmpty()) return spotify.filter { it.audioUrl.isNotBlank() }
        val usedYoutube = mutableSetOf<String>()
        val merged = mutableListOf<SongEntity>()
        for (track in spotify) {
            val match = youtube.firstOrNull { video ->
                video.id !in usedYoutube && titlesMatch(track.title, video.title)
            }
            if (match != null) {
                usedYoutube.add(match.id)
                merged.add(
                    match.copy(
                        title = track.title,
                        artist = track.artist,
                        album = track.album,
                        albumArtUrl = track.albumArtUrl.ifBlank { match.albumArtUrl },
                        durationMs = match.durationMs,
                        mood = track.mood
                    )
                )
            }
        }
        merged.addAll(youtube.filter { it.id !in usedYoutube })
        return if (merged.isNotEmpty()) merged else youtube
    }

    private fun titlesMatch(left: String, right: String): Boolean {
        val a = normalizeTitle(left)
        val b = normalizeTitle(right)
        if (a.length < 3 || b.length < 3) return false
        if (a == b || a in b || b in a) return true
        val aTokens = a.split(" ").filter { it.length > 2 }
        val bTokens = b.split(" ").filter { it.length > 2 }
        if (aTokens.isEmpty() || bTokens.isEmpty()) return false
        val shared = aTokens.count { it in bTokens }
        return shared >= minOf(2, aTokens.size)
    }

    private fun normalizeTitle(value: String): String {
        return value.lowercase()
            .replace(Regex("""(?i)\b(official|video|audio|lyric|lyrics|full|song|hd|4k)\b"""), " ")
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()
    }

    suspend fun fetchSyncedLyrics(artist: String, title: String): String? = withContext(Dispatchers.IO) {
        val cleanTitle = cleanSongTitleForLyrics(title)
        val cleanArtist = artist.split(",", "&", "feat.", "ft.", "/", "-").first().trim()
        if (cleanTitle.isBlank()) return@withContext null
        searchLrcLib(cleanArtist, cleanTitle)
            ?: searchLrcLib("", cleanTitle)
            ?: fetchPlainLyrics(cleanArtist, cleanTitle)
    }

    private suspend fun searchLrcLib(artist: String, title: String): String? {
        return try {
            val url = buildString {
                append("https://lrclib.net/api/search?track_name=")
                append(URLEncoder.encode(title, "UTF-8"))
                if (artist.isNotBlank()) {
                    append("&artist_name=")
                    append(URLEncoder.encode(artist, "UTF-8"))
                }
            }
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "RessoMusicApp/1.0 (Android)")
                .build()
            val (code, body) = fetchText(request)
            if (code !in 200..299 || body == null) return null
            val items = JSONArray(body)
            var bestSynced = ""
            var bestPlain = ""
            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                val synced = item.optString("syncedLyrics", "")
                val plain = item.optString("plainLyrics", "")
                if (synced.length > bestSynced.length) bestSynced = synced
                if (plain.length > bestPlain.length) bestPlain = plain
            }
            bestSynced.ifBlank { bestPlain.ifBlank { null } }
        } catch (e: Exception) {
            Log.w(TAG, "Lyrics search failed: ${e.message}")
            null
        }
    }

    private suspend fun fetchPlainLyrics(artist: String, title: String): String? {
        if (artist.isBlank()) return null
        return try {
            val url = "https://api.lyrics.ovh/v1/" +
                URLEncoder.encode(artist, "UTF-8") + "/" +
                URLEncoder.encode(title, "UTF-8")
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "RessoMusicApp/1.0 (Android)")
                .build()
            val (code, body) = fetchText(request)
            if (code !in 200..299 || body == null) return null
            val lyrics = JSONObject(body).optString("lyrics", "").trim()
            lyrics.ifBlank { null }
        } catch (_: Exception) {
            null
        }
    }

    private fun decodeHtmlEntities(text: String): String {
        return text
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
    }

    private fun cleanYouTubeTitle(title: String): String {
        return title
            .replace(Regex("""(?i)\(official\s*(music)?\s*(video|audio|lyric|track)?\)"""), "")
            .replace(Regex("""(?i)\[official\s*(music)?\s*(video|audio|lyric|track)?\]"""), "")
            .replace(Regex("""(?i)\(full\s*song\)"""), "")
            .replace(Regex("""(?i)\[full\s*song\]"""), "")
            .replace(Regex("""(?i)\(lyrical\s*(video)?\)"""), "")
            .replace(Regex("""(?i)\[lyrical\s*(video)?\]"""), "")
            .replace(Regex("""(?i)\(visualizer\)"""), "")
            .replace(Regex("""(?i)\|.*"""), "")
            .trim()
    }

    private fun cleanSongTitleForLyrics(title: String): String {
        return title
            .replace(Regex("""\(.*?\)|\[.*?\]"""), "")
            .replace(Regex("""(?i)full\s+song|official\s+video|lyric.*"""), "")
            .trim()
    }

    private fun mapTextToMood(text: String): String {
        val t = text.lowercase()
        return when {
            t.contains("rock") || t.contains("metal") || t.contains("workout") || t.contains("gym") || t.contains("bass") -> "Workout"
            t.contains("dance") || t.contains("party") || t.contains("remix") || t.contains("dj") || t.contains("punjabi") || t.contains("bhangra") -> "Party"
            t.contains("chill") || t.contains("lo-fi") || t.contains("lofi") || t.contains("acoustic") || t.contains("ambient") || t.contains("piano") -> "Focus"
            t.contains("love") || t.contains("romance") || t.contains("sad") || t.contains("heart") || t.contains("romantic") || t.contains("arijit") -> "Romance"
            else -> "Chill"
        }
    }

    private data class RawYouTubeSong(
        val videoId: String,
        val title: String,
        val channelTitle: String,
        val thumbnailUrl: String
    )
}
