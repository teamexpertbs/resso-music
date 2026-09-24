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
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun searchSongs(query: String, limit: Int = 30): List<SongEntity> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return@withContext emptyList()

        try {
            val encoded = URLEncoder.encode(trimmed, "UTF-8")
            val url = "https://saavn.dev/api/search/songs?query=$encoded"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "RessoMusicApp/1.0 (Android)")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w("MusicSearchService", "Saavn search returned HTTP ${response.code}")
                    return@withContext emptyList()
                }

                val bodyString = response.body?.string() ?: return@withContext emptyList()
                val data = JSONObject(bodyString).optJSONObject("data")
                    ?: return@withContext emptyList()
                val resultsArray = data.optJSONArray("results")
                    ?: return@withContext emptyList()

                val songs = mutableListOf<SongEntity>()
                val resultLimit = minOf(resultsArray.length(), limit)
                for (i in 0 until resultLimit) {
                    val item = resultsArray.optJSONObject(i) ?: continue
                    val id = item.optString("id", "")
                    val title = item.optString("name", "").trim()
                    val album = item.optJSONObject("album")?.optString("name", "Single") ?: "Single"
                    val durationSeconds = item.optLong("duration", 0L)
                    val artist = item.optJSONObject("artists")
                        ?.optJSONArray("primary")
                        ?.let { artists ->
                            (0 until artists.length())
                                .mapNotNull { artists.optJSONObject(it)?.optString("name")?.takeIf(String::isNotBlank) }
                                .joinToString(", ")
                        }
                        ?.ifBlank { null }
                        ?: item.optString("primaryArtists", "Unknown Artist")

                    val imageArray = item.optJSONArray("image")
                    val artworkUrl = imageArray?.let { images ->
                        (0 until images.length())
                            .mapNotNull { images.optJSONObject(it)?.optString("url")?.takeIf(String::isNotBlank) }
                            .lastOrNull()
                    } ?: "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80"

                    // Prefer Saavn's 320kbps stream, then fall back to the highest available quality.
                    val downloadUrl = item.optJSONArray("downloadUrl")?.let { downloads ->
                        val candidates = (0 until downloads.length()).mapNotNull { index ->
                            downloads.optJSONObject(index)?.let { download ->
                                val quality = download.optString("quality")
                                val streamUrl = download.optString("url")
                                if (streamUrl.isNotBlank()) quality to streamUrl else null
                            }
                        }
                        candidates.firstOrNull { it.first == "320kbps" }?.second
                            ?: candidates.lastOrNull()?.second
                    }.orEmpty()

                    if (id.isNotBlank() && title.isNotBlank() && downloadUrl.isNotBlank()) {
                        val genre = item.optString("language", "Hindi")
                        songs.add(
                            SongEntity(
                                id = "saavn_$id",
                                title = title,
                                artist = artist,
                                album = album,
                                durationMs = if (durationSeconds > 0) durationSeconds * 1000L else 180000L,
                                audioUrl = downloadUrl,
                                albumArtUrl = artworkUrl,
                                lyricsLrc = "",
                                genre = genre,
                                mood = mapGenreToMood(genre),
                                isLiked = false,
                                isCustomUpload = false
                            )
                        )
                    }
                }
                songs
            }
        } catch (e: Exception) {
            Log.e("MusicSearchService", "Saavn online search error: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun fetchSyncedLyrics(artist: String, title: String): String? = withContext(Dispatchers.IO) {
        try {
            val cleanTitle = title.replace(Regex("""\(.*?\)|\[.*?\]"""), "").trim()
            val cleanArtist = artist.split(",", "&", "feat.", "ft.", "/").first().trim()
            val encodedArtist = URLEncoder.encode(cleanArtist, "UTF-8")
            val encodedTitle = URLEncoder.encode(cleanTitle, "UTF-8")
            val url = "https://lrclib.net/api/get?artist_name=$encodedArtist&track_name=$encodedTitle"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "RessoMusicApp/1.0 (Android)")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val json = JSONObject(body)
                json.optString("syncedLyrics", "").takeIf { it.isNotBlank() }
                    ?: json.optString("plainLyrics", "").takeIf { it.isNotBlank() }
            }
        } catch (_: Exception) {
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
