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
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    suspend fun searchSongs(query: String, limit: Int = 30): List<SongEntity> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return@withContext emptyList()

        try {
            val encoded = URLEncoder.encode(trimmed, "UTF-8")
            val url = "https://itunes.apple.com/search?term=$encoded&entity=song&media=music&limit=$limit"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "RessoMusicApp/1.0 (Android)")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w("MusicSearchService", "iTunes search returned HTTP ${response.code}")
                return@withContext emptyList()
            }

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
                // High-resolution artwork replacement (600x600)
                val artworkHighRes = if (artwork100.isNotEmpty()) {
                    artwork100.replace("100x100bb", "600x600bb")
                } else {
                    "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80"
                }
                val durationMs = item.optLong("trackTimeMillis", 180000L)
                val primaryGenre = item.optString("primaryGenreName", "Pop")

                if (trackName.isNotEmpty() && previewUrl.isNotEmpty()) {
                    val mood = mapGenreToMood(primaryGenre)
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
            Log.e("MusicSearchService", "Online search error: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun fetchSyncedLyrics(artist: String, title: String): String? = withContext(Dispatchers.IO) {
        try {
            // Clean up title and artist to increase match probability
            val cleanTitle = title.replace(Regex("""\(.*?\)|\[.*?\]"""), "").trim()
            val cleanArtist = artist.split(",", "&", "feat.", "ft.", "/").first().trim()

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
        } catch (e: Exception) {
            null
        }
    }

    private fun mapGenreToMood(genre: String): String {
        val g = genre.lowercase()
        return when {
            g.contains("rock") || g.contains("metal") || g.contains("workout") || g.contains("fitness") || g.contains("hard") -> "Workout"
            g.contains("dance") || g.contains("electronic") || g.contains("edm") || g.contains("club") || g.contains("party") || g.contains("punjabi") -> "Party"
            g.contains("chill") || g.contains("lo-fi") || g.contains("ambient") || g.contains("acoustic") || g.contains("classical") -> "Focus"
            g.contains("love") || g.contains("romance") || g.contains("ballad") || g.contains("r&b") || g.contains("soul") || g.contains("bollywood") -> "Romance"
            else -> "Chill"
        }
    }
}
