package com.resso.craka.data.firebase

import android.util.Base64
import android.util.Log
import com.resso.craka.data.model.CommentEntity
import com.resso.craka.data.model.PlaylistEntity
import com.resso.craka.data.model.SongEntity
import com.resso.craka.data.model.VibeEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.util.concurrent.TimeUnit

class SleepokFirestoreClient {
    companion object {
        private const val TAG = "SleepokFirestore"
        const val PROJECT_ID = "sleepok"
        private const val CLIENT_EMAIL = "firebase-adminsdk-fbsvc@sleepok.iam.gserviceaccount.com"
        private const val TOKEN_URI = "https://oauth2.googleapis.com/token"
        private const val FIRESTORE_BASE = "https://firestore.googleapis.com/v1/projects/$PROJECT_ID/databases/(default)/documents"

        private val PRIVATE_KEY_PEM = """
            -----BEGIN PRIVATE KEY-----
            MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQD29bKjIhb+HJY9
            +ZEU7fSVgT9+NNMdwgJuNiNVxdq1ki4XTilf0g11Krcoy75PudtNaJERu3dCmHt7
            GHv5fZkrj0BLdO4GaDiFsSwhjRhnOW3IbirasIEwbybu5Q+QxJng2cOtFKaQ9BZC
            pOJn70FpSDR3MqZHYRi5ZTlOgOh5vEpL8Ypo3yeSHLCG4Hwc1byXLL/Ud+hqoJts
            9KjH7bsXA+KwBKgeJyToTtVb8odobEy72DgloFHkfvVgC8oBOGJHswRhMU/pOboa
            REE0xw8Dw8uQciuEAiIieVpzumMN3OxuH6prXzzosWBsEwMRoekQWoll7Bw16kyf
            C/Ak9vMtAgMBAAECggEAWDzBbvgnLvQX2lzpMYkz8pcTkIxfQiqno6lERslCs81f
            GoSa09nBZTO17aWege3uJWJPMv7ARdMli33cDfCKYb31QwCsay2hxGLhWbuIFJm4
            V/zE4w5dmmiciI7fWOJl+KjV+Wl/ehs8OOOg4XHi9TMPS+eU8bwHkXZRW3NDnPqu
            A0qX97/pIJFoVG0kzz2Rt0/XysiBqfzRvckutQEzkgy7rcn+DKg98TYa53WOi+eS
            yUOUtsrRh7Rvf2yJwh8o/xvUnY6csQzxA7qAnIzL8bDlDzMCa+twPyA8+NFeCRAq
            OQ0N8mekkYXgks3jtWdfy6IyXm18c1YgwtCbjz+SIQKBgQD8Jh91pBA+wfpuIvel
            7U+XfZYybVzTlPIwMmu9x56fmqltRNXaJEi+cZOCLv+0ZeGA685/0Pqw/gUM5JU1
            ziJ+5twU1V/85fMaMEJ9id6Sq+zx/kG66LLN+1piFLMgHH580HgbP1yxO3AS51VQ
            SA4D2jGrdwznD/95huV2CyaF6wKBgQD6u0krC72rf0qoIbqOVO554mH/bIZvgUwo
            pFTUR/DnrEdquIY5ZMP1XC/HWeYToMfbzxf/lfxjCZIOZf2xsFIHFN7EPM355KV/
            dWHWTqoexpmj9MH1TC45Av56JxI07CQQLpYT8clxxoE228g1xNIy2DRCPqNwnOyd
            0edY9FitRwKBgEZxTBasiBFQVetQ/4iufK7g2gYqgWVm1iZa37i8PPdv2Od69Jel
            zWDPEButj6hRUieXOTCCLJcn0Ddi4MCQXxi+3DtBxTg88aaeVuUFkFW/jEmq86gI
            /HMJRp3iFIzjCP9LqlJKaFXnOg8965qrFqdC3N9/Oe6PjJrh8dcBlHiTAoGAbVhi
            F3C+Xd1cjKhw+IOXoPYWcNM+acCEmzDXgeCB8jcSyjsA5mTIhOeYOqM9EJBMuNK/
            D4q0j+hrlvXpxqUzkL0MTZ6K4ZFSj5x8d37E+Dj6ZiNmtvhrLBffLRx+9y+Iprn6
            X2ZRyNmeHnOJA9H7LYtWdxFkK8dz9XvGHegqCB8CgYEA1a2aF9Hy0wS0lwMGTKwa
            FNqdNu+xCO0Od8mzV16YcNfwQjD5saiLd+ljBOjMrOeyWR8ef+vEcfRFbBuZGtP4
            f0d9aHBqHaWj54APt9fgBsDrEVrb1TxGtq/17iWOBi2p/w0I8DdtlWxCO9L5xi5c
            KxlZGRSFsKCO81+jhzRd4vM=
            -----END PRIVATE KEY-----
        """.trimIndent()
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private var cachedToken: String? = null
    private var tokenExpiryTimeMs: Long = 0L

    private val privateKey: PrivateKey? by lazy {
        try {
            val cleanKey = PRIVATE_KEY_PEM
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("\\s+".toRegex(), "")
            val decoded = Base64.decode(cleanKey, Base64.DEFAULT)
            val spec = PKCS8EncodedKeySpec(decoded)
            KeyFactory.getInstance("RSA").generatePrivate(spec)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load private key: ${e.message}", e)
            null
        }
    }

    private suspend fun getAccessToken(): String? = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (cachedToken != null && now < tokenExpiryTimeMs - 60_000L) {
            return@withContext cachedToken
        }

        val key = privateKey ?: return@withContext null

        try {
            val nowSec = now / 1000L
            val expSec = nowSec + 3600L

            val header = JSONObject().apply {
                put("alg", "RS256")
                put("typ", "JWT")
            }.toString()

            val claims = JSONObject().apply {
                put("iss", CLIENT_EMAIL)
                put("scope", "https://www.googleapis.com/auth/datastore https://www.googleapis.com/auth/firebase.database")
                put("aud", TOKEN_URI)
                put("exp", expSec)
                put("iat", nowSec)
            }.toString()

            val encHeader = Base64.encodeToString(header.toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
            val encClaims = Base64.encodeToString(claims.toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
            val input = "$encHeader.$encClaims"

            val signer = Signature.getInstance("SHA256withRSA")
            signer.initSign(key)
            signer.update(input.toByteArray(Charsets.UTF_8))
            val signature = signer.sign()
            val encSignature = Base64.encodeToString(signature, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)

            val assertion = "$input.$encSignature"

            val formBody = "grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer&assertion=$assertion"
            val request = Request.Builder()
                .url(TOKEN_URI)
                .post(formBody.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val bodyStr = response.body?.string() ?: return@withContext null
                val json = JSONObject(bodyStr)
                val token = json.optString("access_token", null)
                val expiresIn = json.optLong("expires_in", 3600L)
                if (!token.isNullOrBlank()) {
                    cachedToken = token
                    tokenExpiryTimeMs = now + (expiresIn * 1000L)
                    Log.i(TAG, "Successfully authenticated with Firebase project sleepok!")
                    return@withContext token
                }
            } else {
                Log.w(TAG, "OAuth token fetch failed: ${response.code} ${response.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "OAuth token exception: ${e.message}", e)
        }
        return@withContext null
    }

    /**
     * Search songs metadata in Firestore 'songs' collection for sleepok project
     */
    suspend fun searchSongs(query: String): List<SongEntity> = withContext(Dispatchers.IO) {
        val token = getAccessToken() ?: return@withContext emptyList()
        val cleanQuery = query.trim().lowercase()

        try {
            val url = "$FIRESTORE_BASE/songs?pageSize=50"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext emptyList()

            val body = response.body?.string() ?: return@withContext emptyList()
            val json = JSONObject(body)
            val documents = json.optJSONArray("documents") ?: return@withContext emptyList()
            val results = mutableListOf<SongEntity>()

            for (i in 0 until documents.length()) {
                val doc = documents.getJSONObject(i)
                val fields = doc.optJSONObject("fields") ?: continue

                val id = fields.optJSONObject("id")?.optString("stringValue")
                    ?: doc.optString("name").substringAfterLast("/")
                val title = fields.optJSONObject("title")?.optString("stringValue") ?: ""
                val artist = fields.optJSONObject("artist")?.optString("stringValue") ?: "Various Artists"
                val album = fields.optJSONObject("album")?.optString("stringValue") ?: "Single"
                val genre = fields.optJSONObject("genre")?.optString("stringValue") ?: "Bollywood"
                val audioUrl = fields.optJSONObject("audioUrl")?.optString("stringValue") ?: ""
                val albumArtUrl = fields.optJSONObject("albumArtUrl")?.optString("stringValue") ?: ""
                val lyricsLrc = fields.optJSONObject("lyricsLrc")?.optString("stringValue") ?: ""
                val durationMs = fields.optJSONObject("durationMs")?.optString("integerValue")?.toLongOrNull() ?: 210000L

                val matches = cleanQuery.isBlank() ||
                    title.lowercase().contains(cleanQuery) ||
                    artist.lowercase().contains(cleanQuery) ||
                    genre.lowercase().contains(cleanQuery) ||
                    album.lowercase().contains(cleanQuery)

                if (matches && audioUrl.isNotBlank()) {
                    results.add(
                        SongEntity(
                            id = id,
                            title = title,
                            artist = artist,
                            album = album,
                            durationMs = durationMs,
                            audioUrl = audioUrl,
                            albumArtUrl = albumArtUrl,
                            lyricsLrc = lyricsLrc,
                            genre = genre,
                            mood = "Chill"
                        )
                    )
                }
            }
            results
        } catch (e: Exception) {
            Log.w(TAG, "searchSongs error: ${e.message}")
            emptyList()
        }
    }

    /**
     * Index song metadata directly into sleepok Firestore
     */
    suspend fun indexSong(song: SongEntity) = withContext(Dispatchers.IO) {
        val token = getAccessToken() ?: return@withContext
        if (!song.audioUrl.startsWith("http")) return@withContext

        try {
            val url = "$FIRESTORE_BASE/songs/${song.id}"
            val fieldsJson = JSONObject().apply {
                put("id", JSONObject().put("stringValue", song.id))
                put("title", JSONObject().put("stringValue", song.title))
                put("artist", JSONObject().put("stringValue", song.artist))
                put("album", JSONObject().put("stringValue", song.album))
                put("genre", JSONObject().put("stringValue", song.genre))
                put("audioUrl", JSONObject().put("stringValue", song.audioUrl))
                put("albumArtUrl", JSONObject().put("stringValue", song.albumArtUrl))
                put("lyricsLrc", JSONObject().put("stringValue", song.lyricsLrc))
                put("durationMs", JSONObject().put("integerValue", song.durationMs.toString()))
                put("updatedAt", JSONObject().put("integerValue", System.currentTimeMillis().toString()))
            }

            val docJson = JSONObject().apply {
                put("fields", fieldsJson)
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .patch(docJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            httpClient.newCall(request).execute().close()
        } catch (e: Exception) {
            Log.w(TAG, "indexSong error: ${e.message}")
        }
    }

    suspend fun indexSongs(songs: List<SongEntity>) = withContext(Dispatchers.IO) {
        songs.forEach { indexSong(it) }
    }

    /**
     * Sync user liked song to sleepok
     */
    suspend fun syncLikedSong(deviceId: String, song: SongEntity, isLiked: Boolean) = withContext(Dispatchers.IO) {
        val token = getAccessToken() ?: return@withContext
        try {
            val url = "$FIRESTORE_BASE/users/$deviceId/liked_songs/${song.id}"
            if (isLiked) {
                val fields = JSONObject().apply {
                    put("id", JSONObject().put("stringValue", song.id))
                    put("title", JSONObject().put("stringValue", song.title))
                    put("artist", JSONObject().put("stringValue", song.artist))
                    put("album", JSONObject().put("stringValue", song.album))
                    put("audioUrl", JSONObject().put("stringValue", song.audioUrl))
                    put("albumArtUrl", JSONObject().put("stringValue", song.albumArtUrl))
                    put("durationMs", JSONObject().put("integerValue", song.durationMs.toString()))
                    put("isLiked", JSONObject().put("booleanValue", true))
                    put("syncedAt", JSONObject().put("integerValue", System.currentTimeMillis().toString()))
                }
                val body = JSONObject().put("fields", fields).toString()
                val request = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer $token")
                    .patch(body.toRequestBody("application/json".toMediaType()))
                    .build()
                httpClient.newCall(request).execute().close()
            } else {
                val request = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer $token")
                    .delete()
                    .build()
                httpClient.newCall(request).execute().close()
            }
        } catch (e: Exception) {
            Log.w(TAG, "syncLikedSong error: ${e.message}")
        }
    }

    /**
     * Restore cloud liked songs from sleepok
     */
    suspend fun fetchLikedSongs(deviceId: String): List<SongEntity> = withContext(Dispatchers.IO) {
        val token = getAccessToken() ?: return@withContext emptyList()
        try {
            val url = "$FIRESTORE_BASE/users/$deviceId/liked_songs"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext emptyList()

            val body = response.body?.string() ?: return@withContext emptyList()
            val json = JSONObject(body)
            val documents = json.optJSONArray("documents") ?: return@withContext emptyList()
            val list = mutableListOf<SongEntity>()

            for (i in 0 until documents.length()) {
                val doc = documents.getJSONObject(i)
                val fields = doc.optJSONObject("fields") ?: continue
                val id = fields.optJSONObject("id")?.optString("stringValue") ?: continue
                val title = fields.optJSONObject("title")?.optString("stringValue") ?: continue
                val audioUrl = fields.optJSONObject("audioUrl")?.optString("stringValue") ?: continue

                list.add(
                    SongEntity(
                        id = id,
                        title = title,
                        artist = fields.optJSONObject("artist")?.optString("stringValue") ?: "Various Artists",
                        album = fields.optJSONObject("album")?.optString("stringValue") ?: "Single",
                        durationMs = fields.optJSONObject("durationMs")?.optString("integerValue")?.toLongOrNull() ?: 210000L,
                        audioUrl = audioUrl,
                        albumArtUrl = fields.optJSONObject("albumArtUrl")?.optString("stringValue") ?: "",
                        lyricsLrc = "",
                        genre = "Bollywood",
                        mood = "Chill",
                        isLiked = true
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.w(TAG, "fetchLikedSongs error: ${e.message}")
            emptyList()
        }
    }

    /**
     * Register device in Firestore sleepok project immediately on app launch
     * Creates documents in both 'devices' and 'users' collections!
     */
    suspend fun registerDevice(
        deviceId: String,
        model: String,
        manufacturer: String,
        androidVer: String
    ) = withContext(Dispatchers.IO) {
        val token = getAccessToken() ?: return@withContext
        try {
            // 1. Write to 'devices' collection
            val deviceUrl = "$FIRESTORE_BASE/devices/$deviceId"
            val deviceFields = JSONObject().apply {
                put("deviceId", JSONObject().put("stringValue", deviceId))
                put("deviceModel", JSONObject().put("stringValue", "$manufacturer $model"))
                put("brand", JSONObject().put("stringValue", manufacturer))
                put("model", JSONObject().put("stringValue", model))
                put("androidVersion", JSONObject().put("stringValue", androidVer))
                put("appName", JSONObject().put("stringValue", "Resso Music"))
                put("status", JSONObject().put("stringValue", "Active Online"))
                put("registeredAt", JSONObject().put("integerValue", System.currentTimeMillis().toString()))
                put("lastActive", JSONObject().put("integerValue", System.currentTimeMillis().toString()))
            }
            val deviceBody = JSONObject().put("fields", deviceFields).toString()
            httpClient.newCall(
                Request.Builder()
                    .url(deviceUrl)
                    .addHeader("Authorization", "Bearer $token")
                    .patch(deviceBody.toRequestBody("application/json".toMediaType()))
                    .build()
            ).execute().close()

            // 2. Write to 'users' collection
            val userUrl = "$FIRESTORE_BASE/users/$deviceId"
            val userFields = JSONObject().apply {
                put("deviceId", JSONObject().put("stringValue", deviceId))
                put("displayName", JSONObject().put("stringValue", "$model User"))
                put("device", JSONObject().put("stringValue", "$manufacturer $model (Android $androidVer)"))
                put("createdAt", JSONObject().put("integerValue", System.currentTimeMillis().toString()))
                put("lastSeen", JSONObject().put("integerValue", System.currentTimeMillis().toString()))
            }
            val userBody = JSONObject().put("fields", userFields).toString()
            httpClient.newCall(
                Request.Builder()
                    .url(userUrl)
                    .addHeader("Authorization", "Bearer $token")
                    .patch(userBody.toRequestBody("application/json".toMediaType()))
                    .build()
            ).execute().close()

            Log.i(TAG, "Device $deviceId ($manufacturer $model) successfully registered in sleepok Firestore!")
        } catch (e: Exception) {
            Log.w(TAG, "registerDevice error: ${e.message}")
        }
    }
}
