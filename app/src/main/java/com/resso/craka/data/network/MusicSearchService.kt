package com.resso.craka.data.network

import android.util.Base64
import android.util.Log
import com.resso.craka.data.model.SongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MusicSearchService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "MusicSearchService"
        private const val DES_KEY = "38346591"
        // Optional custom Vercel API endpoint. If empty, uses direct JioSaavn with native decryption.
        var customApiBaseUrl: String = "https://crakaresso-music-api.vercel.app"
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

    /**
     * Decrypts JioSaavn encrypted_media_url into 320kbps high-quality direct stream URL
     */
    private fun decryptSaavnUrl(encryptedUrl: String): String? {
        return try {
            val keyBytes = DES_KEY.toByteArray(Charsets.UTF_8)
            val keySpec = SecretKeySpec(keyBytes, "DES")
            val cipher = Cipher.getInstance("DES/ECB/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, keySpec)
            val decodedBytes = Base64.decode(encryptedUrl, Base64.DEFAULT)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            val streamUrl = String(decryptedBytes, Charsets.UTF_8)
            // Upgrade stream to 320kbps HD quality
            streamUrl.replace("_96.mp4", "_320.mp4").replace("_160.mp4", "_320.mp4")
        } catch (e: Exception) {
            Log.w(TAG, "Decryption error: ${e.message}")
            null
        }
    }

    /**
     * Search songs in real-time (100% full song 320kbps, zero YouTube/Spotify dependency)
     */
    suspend fun searchSongs(query: String, limit: Int = 25): List<SongEntity> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return@withContext emptyList()

        // 1. If custom Vercel API is configured, use it for edge-cached sub-millisecond response
        if (customApiBaseUrl.isNotBlank()) {
            val fromVercel = searchViaVercelApi(trimmed, limit)
            if (fromVercel.isNotEmpty()) return@withContext fromVercel
        }

        // 2. Direct high-speed JioSaavn search + local decryption
        return@withContext searchDirectJioSaavn(trimmed, limit)
    }

    suspend fun getTrendingSongs(limit: Int = 15): List<SongEntity> = withContext(Dispatchers.IO) {
        val songs = searchSongs("Trending Hindi Hits", limit)
        if (songs.isNotEmpty()) songs else searchSongs("Top Bollywood", limit)
    }

    private suspend fun searchViaVercelApi(query: String, limit: Int): List<SongEntity> {
        return try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "${customApiBaseUrl.trimEnd('/')}/api/search?q=$encoded"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "RessoMusicApp/1.0 (Android)")
                .build()

            val (code, body) = fetchText(request)
            if (code !in 200..299 || body == null) return emptyList()

            val json = JSONObject(body)
            val resultsArray = json.optJSONArray("results") ?: return emptyList()
            val list = mutableListOf<SongEntity>()

            for (i in 0 until minOf(resultsArray.length(), limit)) {
                val item = resultsArray.getJSONObject(i)
                val id = item.optString("id", "")
                val title = item.optString("title", "Unknown")
                val artist = item.optString("artist", "Various Artists")
                val album = item.optString("album", "Single")
                val durationSec = item.optLong("duration", 210L)
                val audioUrl = item.optString("audioUrl", "")
                val albumArt = item.optString("albumArt", "")

                if (audioUrl.isNotBlank()) {
                    list.add(
                        SongEntity(
                            id = "saavn_$id",
                            title = title,
                            artist = artist,
                            album = album,
                            durationMs = durationSec * 1000L,
                            audioUrl = audioUrl,
                            albumArtUrl = albumArt,
                            genre = "Bollywood",
                            mood = mapTextToMood("$title $artist")
                        )
                    )
                }
            }
            list
        } catch (e: Exception) {
            Log.w(TAG, "Vercel search failed, falling back to direct: ${e.message}")
            emptyList()
        }
    }

    private suspend fun searchDirectJioSaavn(query: String, limit: Int): List<SongEntity> {
        return try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val searchUrl = "https://www.jiosaavn.com/api.php?__call=search.getResults&_format=json&_marker=0&cc=in&n=$limit&p=1&q=$encoded"
            val request = Request.Builder()
                .url(searchUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            val (code, body) = fetchText(request)
            if (code !in 200..299 || body == null) return emptyList()

            val json = JSONObject(body)
            val results = json.optJSONArray("results") ?: return emptyList()
            val list = mutableListOf<SongEntity>()

            for (i in 0 until results.length()) {
                val item = results.getJSONObject(i)
                val encMediaUrl = item.optString("encrypted_media_url", "")
                if (encMediaUrl.isBlank()) continue

                val audioUrl = decryptSaavnUrl(encMediaUrl) ?: continue
                val id = item.optString("id", "")
                val rawTitle = item.optString("song", "")
                val rawSingers = item.optString("primary_artists", "").ifBlank {
                    item.optString("singers", "Various Artists")
                }
                val rawAlbum = item.optString("album", "Single")
                val img = item.optString("image", "").replace("150x150.jpg", "500x500.jpg")
                val durationSec = item.optLong("duration", 210L)

                list.add(
                    SongEntity(
                        id = "saavn_$id",
                        title = decodeHtmlEntities(rawTitle),
                        artist = decodeHtmlEntities(rawSingers),
                        album = decodeHtmlEntities(rawAlbum),
                        durationMs = durationSec * 1000L,
                        audioUrl = audioUrl,
                        albumArtUrl = img,
                        genre = "Bollywood",
                        mood = mapTextToMood("$rawTitle $rawSingers")
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "JioSaavn search error: ${e.message}", e)
            emptyList()
        }
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
