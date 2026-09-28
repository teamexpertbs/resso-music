package com.resso.craka.data.network

import android.util.Base64
import android.util.Log
import com.resso.craka.BuildConfig
import com.resso.craka.data.model.SongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
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

    private val youtubeApiKey = BuildConfig.YOUTUBE_API_KEY.trim()
    private val spotifyClientId = BuildConfig.SPOTIFY_CLIENT_ID.trim()
    private val spotifyClientSecret = BuildConfig.SPOTIFY_CLIENT_SECRET.trim()
    private var spotifyToken: String? = null
    private var spotifyTokenExpiryMs: Long = 0L

    suspend fun searchSongs(query: String, limit: Int = 30): List<SongEntity> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return@withContext emptyList()

        val youtubeSongs = searchYouTubeSongs(trimmed, limit)
        val spotifySongs = searchSpotifySongs(trimmed, limit)
        val merged = mergeCatalog(youtubeSongs, spotifySongs)
        if (merged.isNotEmpty()) merged else searchItunesSongs(trimmed, limit)
    }

    suspend fun getTrendingSongs(limit: Int = 25): List<SongEntity> {
        return searchSongs("Top Bollywood Hindi Songs", limit)
    }

    suspend fun searchYouTubeSongs(query: String, limit: Int = 25): List<SongEntity> = withContext(Dispatchers.IO) {
        if (youtubeApiKey.isBlank()) {
            Log.i(TAG, "YOUTUBE_API_KEY is not configured; using the iTunes fallback.")
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

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "YouTube search returned code: ${response.code}")
                return@withContext emptyList()
            }

            val bodyString = response.body?.string() ?: return@withContext emptyList()
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

    private fun fetchVideoDurations(videoIds: List<String>): Map<String, Long> {
        val durations = mutableMapOf<String, Long>()
        try {
            val idsChunk = videoIds.joinToString(",")
            val url = "https://www.googleapis.com/youtube/v3/videos" +
                    "?part=contentDetails" +
                    "&id=$idsChunk" +
                    "&key=$youtubeApiKey"

            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return durations

            val bodyString = response.body?.string() ?: return durations
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
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "Spotify search returned code: ${response.code}")
                return@withContext emptyList()
            }
            val body = response.body?.string() ?: return@withContext emptyList()
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

    private fun spotifyAccessToken(): String? {
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
            val body = FormBody.Builder().add("grant_type", "client_credentials").build()
            val request = Request.Builder()
                .url("https://accounts.spotify.com/api/token")
                .header("Authorization", "Basic $basic")
                .post(body)
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "Spotify token request failed: ${response.code}")
                return null
            }
            val json = JSONObject(response.body?.string().orEmpty())
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
        val a = left.lowercase().replace(Regex("[^a-z0-9]"), "")
        val b = right.lowercase().replace(Regex("[^a-z0-9]"), "")
        if (a.length < 3 || b.length < 3) return false
        return a == b || a in b || b in a
    }

    suspend fun searchItunesSongs(query: String, limit: Int = 30): List<SongEntity> = withContext(Dispatchers.IO) {
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://itunes.apple.com/search?term=$encoded&entity=song&media=music&limit=$limit"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "RessoMusicApp/1.0 (Android)")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext emptyList()

            val bodyString = response.body?.string() ?: return@withContext emptyList()
            val json = JSONObject(bodyString)
            val resultsArray = json.optJSONArray("results") ?: return@withContext emptyList()

            val songs = mutableListOf<SongEntity>()
            for (i in 0 until resultsArray.length()) {
                val item = resultsArray.getJSONObject(i)
                val trackId = item.optLong("trackId", 0L)
                val trackName = item.optString("trackName", "")
                val artistName = item.optString("artistName", "Unknown Artist")
                val collectionName = item.optString("collectionName", "Single")
                val previewUrl = item.optString("previewUrl", "")
                val artwork100 = item.optString("artworkUrl100", "")
                val artworkHighRes = if (artwork100.isNotEmpty()) {
                    artwork100.replace("100x100bb", "600x600bb")
                } else {
                    "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80"
                }
                val durationMs = item.optLong("trackTimeMillis", 180000L)
                val primaryGenre = item.optString("primaryGenreName", "Pop")

                if (trackName.isNotEmpty() && previewUrl.isNotEmpty()) {
                    val mood = mapTextToMood("$primaryGenre $trackName")
                    songs.add(
                        SongEntity(
                            id = "online_$trackId",
                            title = trackName,
                            artist = artistName,
                            album = collectionName,
                            durationMs = durationMs,
                            audioUrl = previewUrl,
                            albumArtUrl = artworkHighRes,
                            lyricsLrc = "",
                            genre = primaryGenre,
                            mood = mood,
                            isLiked = false,
                            isCustomUpload = false
                        )
                    )
                }
            }
            songs
        } catch (e: Exception) {
            Log.e("MusicSearchService", "iTunes search error: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun fetchSyncedLyrics(artist: String, title: String): String? = withContext(Dispatchers.IO) {
        try {
            val cleanTitle = cleanSongTitleForLyrics(title)
            val cleanArtist = artist.split(",", "&", "feat.", "ft.", "/", "-").first().trim()

            val encodedArtist = URLEncoder.encode(cleanArtist, "UTF-8")
            val encodedTitle = URLEncoder.encode(cleanTitle, "UTF-8")
            val url = "https://lrclib.net/api/get?artist_name=$encodedArtist&track_name=$encodedTitle"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "RessoMusicApp/1.0 (Android)")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val body = response.body?.string() ?: return@withContext null
            val json = JSONObject(body)

            val synced = json.optString("syncedLyrics", "")
            if (synced.isNotBlank()) return@withContext synced

            val plain = json.optString("plainLyrics", "")
            if (plain.isNotBlank()) return@withContext plain

            null
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
