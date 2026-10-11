package com.resso.craka.data.network

import android.util.Log
import com.resso.craka.data.model.SongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MusicSearchService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "MusicSearchService"
        var customApiBaseUrl: String = "https://music-api-lime-nine.vercel.app"

        private fun logW(tag: String, msg: String) {
            try {
                Log.w(tag, msg)
            } catch (_: Throwable) {
                println("[$tag] WARN: $msg")
            }
        }
    }

    private suspend fun fetchText(request: Request): Pair<Int, String?> {
        return suspendCancellableCoroutine { cont ->
            val call = client.newCall(request)
            cont.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    try {
                        val body = response.body?.string()
                        if (cont.isActive) cont.resume(response.code to body)
                    } catch (e: Exception) {
                        if (cont.isActive) cont.resumeWithException(e)
                    } finally {
                        response.close()
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    if (cont.isActive) cont.resumeWithException(e)
                }
            })
        }
    }

    /**
     * Search songs via the high-performance YouTube Music API on Vercel.
     * Guaranteed 0 JioSaavn / 0 WebView dependencies.
     */
    suspend fun searchSongs(query: String, limit: Int = 25): List<SongEntity> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return@withContext emptyList()

        try {
            val songs = MusicApiHelper.searchSongs(trimmed)
            if (songs.isNotEmpty()) {
                return@withContext songs.take(limit).map { song ->
                    val entity = song.toSongEntity()
                    entity.copy(
                        mood = mapTextToMood("${entity.title} ${entity.artist}"),
                        genre = "Trending"
                    )
                }
            }
        } catch (e: Exception) {
            logW(TAG, "Search via MusicApiHelper error: ${e.message}")
        }
        emptyList()
    }

    /**
     * Fetches trending songs using diverse queries on YouTube Music API.
     */
    suspend fun getTrendingSongs(limit: Int = 30): List<SongEntity> = withContext(Dispatchers.IO) {
        val trendingQueries = listOf(
            "Trending Hindi Hits",
            "Top Bollywood Songs",
            "Latest Hindi Songs",
            "Arijit Singh Hits",
            "Top Punjabi Hits",
            "Badshah Party Hits",
            "Romantic Bollywood Hits",
            "Sidhu Moose Wala",
            "Diljit Dosanjh Hits",
            "Lo-Fi Hindi Songs"
        ).shuffled()

        val results = mutableListOf<SongEntity>()
        for (query in trendingQueries.take(3)) {
            val batch = searchSongs(query, limit = 15)
            for (s in batch) {
                if (results.none { it.id == s.id }) {
                    results.add(s)
                }
            }
            if (results.size >= limit) break
        }

        if (results.isNotEmpty()) results.shuffled() else searchSongs("Hindi Songs", limit)
    }

    /**
     * Fetches synced lyrics via LRCLIB and fallback to plain lyrics.
     */
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
            logW(TAG, "Lyrics search failed: ${e.message}")
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
}
