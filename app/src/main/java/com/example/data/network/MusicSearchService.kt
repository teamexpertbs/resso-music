package com.example.data.network

import android.util.Log
import com.example.data.model.SongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
        const val YOUTUBE_API_KEY = "AIzaSyAjVymdjvlkvZkP6gdkTjByGMiw-CMzjhY"
    }

    suspend fun searchSongs(query: String, limit: Int = 30): List<SongEntity> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return@withContext emptyList()

        // 1. Try YouTube Data API first for full-length songs
        val ytSongs = searchYouTubeSongs(trimmed, limit)
        if (ytSongs.isNotEmpty()) {
            return@withContext ytSongs
        }

        // 2. Fallback to iTunes catalog if YouTube yields no results
        searchItunesSongs(trimmed, limit)
    }

    suspend fun getTrendingSongs(limit: Int = 25): List<SongEntity> = withContext(Dispatchers.IO) {
        val ytTrending = searchYouTubeSongs("Top Bollywood Hindi Songs", limit)
        if (ytTrending.isNotEmpty()) {
            ytTrending
        } else {
            searchItunesSongs("Bollywood Hits", limit)
        }
    }

    suspend fun searchYouTubeSongs(query: String, limit: Int = 25): List<SongEntity> = withContext(Dispatchers.IO) {
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val searchUrl = "https://www.googleapis.com/youtube/v3/search" +
                    "?part=snippet" +
                    "&maxResults=$limit" +
                    "&q=$encoded" +
                    "&type=video" +
                    "&key=$YOUTUBE_API_KEY"

            val request = Request.Builder()
                .url(searchUrl)
                .header("User-Agent", "RessoMusicApp/1.0 (Android)")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w("MusicSearchService", "YouTube search returned code: ${response.code}")
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
                val channelTitle = snippet.optString("channelTitle", "YouTube Music")

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

            rawSongs.map { raw ->
                val durationMs = durationsMap[raw.videoId] ?: 210000L
                val cleanTitle = cleanYouTubeTitle(decodeHtmlEntities(raw.title))
                val cleanArtist = decodeHtmlEntities(raw.channelTitle)
                val mood = mapTextToMood("$cleanTitle $cleanArtist")

                SongEntity(
                    id = "yt_${raw.videoId}",
                    title = cleanTitle,
                    artist = cleanArtist,
                    album = "YouTube Music",
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
        } catch (e: Exception) {
            Log.e("MusicSearchService", "YouTube API error: ${e.message}", e)
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
                    "&key=$YOUTUBE_API_KEY"

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
            Log.w("MusicSearchService", "Error fetching video durations: ${e.message}")
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
