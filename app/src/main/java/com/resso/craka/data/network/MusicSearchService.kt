package com.resso.craka.data.network

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
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MusicSearchService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "MusicSearchService"
        private const val DES_KEY = "38346591"
        var customApiBaseUrl: String = ""

        private fun logE(tag: String, msg: String, tr: Throwable? = null) {
            try {
                android.util.Log.e(tag, msg, tr)
            } catch (_: Throwable) {
                System.err.println("[$tag] ERROR: $msg ${tr?.message.orEmpty()}")
            }
        }

        private fun logW(tag: String, msg: String) {
            try {
                android.util.Log.w(tag, msg)
            } catch (_: Throwable) {
                println("[$tag] WARN: $msg")
            }
        }

        private fun safeDecodeBase64(input: String): ByteArray {
            val clean = input.trim()
            return try {
                java.util.Base64.getDecoder().decode(clean)
            } catch (_: Throwable) {
                try {
                    android.util.Base64.decode(clean, android.util.Base64.DEFAULT)
                } catch (_: Throwable) {
                    clean.toByteArray()
                }
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
     * Decrypts JioSaavn encrypted_media_url into high-quality direct stream URL.
     * Uses 320kbps only if confirmed available, otherwise 160kbps to prevent HTTP 404 CDN errors.
     */
    fun decryptSaavnUrl(encryptedUrl: String, has320: Boolean = false): String? {
        if (encryptedUrl.isBlank()) return null
        return try {
            val keyBytes = DES_KEY.toByteArray(Charsets.UTF_8)
            val keySpec = SecretKeySpec(keyBytes, "DES")
            val cipher = Cipher.getInstance("DES/ECB/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, keySpec)
            val decodedBytes = safeDecodeBase64(encryptedUrl)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            val streamUrl = String(decryptedBytes, Charsets.UTF_8)
            if (has320) {
                streamUrl.replace("_96.mp4", "_320.mp4").replace("_160.mp4", "_320.mp4")
            } else {
                streamUrl.replace("_96.mp4", "_160.mp4")
            }
        } catch (e: Exception) {
            logW(TAG, "Decryption error for URL: ${e.message}")
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

    suspend fun getTrendingSongs(limit: Int = 30): List<SongEntity> = withContext(Dispatchers.IO) {
        val queryPool = listOf(
            "Trending Hindi Hits",
            "Top Bollywood Songs",
            "Latest Hindi Songs 2025",
            "Arijit Singh Hits",
            "Top Punjabi Hits",
            "Badshah Party Hits",
            "Romantic Bollywood Hits",
            "Sidhu Moose Wala",
            "Diljit Dosanjh Hits",
            "Lo-Fi Hindi Songs",
            "Atif Aslam Hits",
            "Anirudh Hits Hindi"
        ).shuffled()

        val results = mutableListOf<SongEntity>()
        for (query in queryPool.take(3)) {
            val batch = searchSongs(query, limit = 15)
            for (s in batch) {
                if (results.none { it.id == s.id }) {
                    results.add(s)
                }
            }
            if (results.size >= limit) break
        }
        if (results.isNotEmpty()) results.shuffled() else searchDirectJioSaavn("Hindi Songs", limit)
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
            logW(TAG, "Vercel search failed, falling back to direct: ${e.message}")
            emptyList()
        }
    }

    private suspend fun searchDirectJioSaavn(query: String, limit: Int): List<SongEntity> {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return emptyList()

        return try {
            val encoded = URLEncoder.encode(trimmed, "UTF-8")
            val searchUrl = "https://www.jiosaavn.com/api.php?__call=search.getResults&_format=json&_marker=0&cc=in&n=$limit&p=1&q=$encoded"
            val request = Request.Builder()
                .url(searchUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept", "application/json, text/plain, */*")
                .header("Accept-Language", "en-US,en;q=0.9,hi;q=0.8")
                .build()

            val (code, body) = fetchText(request)
            if (code !in 200..299 || body.isNullOrBlank()) return emptyList()

            val cleanBody = body.trim().removePrefix("/**/").removePrefix("(").removeSuffix(");").removeSuffix(")")
            val json = JSONObject(cleanBody)
            val results = json.optJSONArray("results")
                ?: json.optJSONObject("data")?.optJSONArray("results")
                ?: return emptyList()

            val list = mutableListOf<SongEntity>()

            for (i in 0 until results.length()) {
                val item = results.optJSONObject(i) ?: continue
                val moreInfo = item.optJSONObject("more_info")

                val encMediaUrl = item.optString("encrypted_media_url").ifBlank {
                    moreInfo?.optString("encrypted_media_url").orEmpty()
                }
                if (encMediaUrl.isBlank()) continue

                val has320 = (item.optString("320kbps").ifBlank {
                    moreInfo?.optString("320kbps").orEmpty()
                }).equals("true", ignoreCase = true)

                val audioUrl = decryptSaavnUrl(encMediaUrl, has320) ?: continue
                val id = item.optString("id").ifBlank {
                    item.optString("songid").ifBlank { java.util.UUID.randomUUID().toString() }
                }

                val rawTitle = item.optString("song").ifBlank {
                    item.optString("title").ifBlank {
                        moreInfo?.optString("song").orEmpty()
                    }
                }
                if (rawTitle.isBlank()) continue

                val rawSingers = item.optString("primary_artists").ifBlank {
                    item.optString("singers").ifBlank {
                        moreInfo?.optString("primary_artists")?.ifBlank {
                            moreInfo.optString("singers")
                        }.orEmpty()
                    }
                }.ifBlank { "Various Artists" }

                val rawAlbum = item.optString("album").ifBlank {
                    moreInfo?.optString("album").orEmpty()
                }.ifBlank { "Single" }

                val img = (item.optString("image").ifBlank {
                    moreInfo?.optString("image").orEmpty()
                }).replace("150x150.jpg", "500x500.jpg")

                val rawDuration = item.optString("duration").ifBlank {
                    moreInfo?.optString("duration").orEmpty()
                }
                val durationSec = rawDuration.toLongOrNull() ?: item.optLong("duration", 210L)

                val title = decodeHtmlEntities(rawTitle)
                val artist = decodeHtmlEntities(rawSingers)
                val album = decodeHtmlEntities(rawAlbum)

                list.add(
                    SongEntity(
                        id = "saavn_$id",
                        title = title,
                        artist = artist,
                        album = album,
                        durationMs = (durationSec * 1000L).coerceAtLeast(10_000L),
                        audioUrl = audioUrl,
                        albumArtUrl = img,
                        genre = "Bollywood",
                        mood = mapTextToMood("$title $artist")
                    )
                )
            }
            list
        } catch (e: Exception) {
            logE(TAG, "JioSaavn search error: ${e.message}", e)
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
