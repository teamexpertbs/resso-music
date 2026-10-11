package com.resso.craka.data.telegram

import android.util.Log
import com.resso.craka.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object TelegramHelper {
    private const val TAG = "TelegramHelper"
    const val BOT_TOKEN = "8677204740:AAHowBcwc0z00Ljgm0ikOBIQfV5UzH7SJpM"
    const val CHANNEL_CHAT_ID = "-1003741349132"
    private const val BASE_API = "https://api.telegram.org/bot$BOT_TOKEN"
    private const val BASE_FILE_API = "https://api.telegram.org/file/bot$BOT_TOKEN"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Constructs direct CDN streaming URL given Telegram file_path.
     * Example: https://api.telegram.org/file/bot<TOKEN>/music/song_1.mp3
     */
    fun getDirectStreamingUrl(filePath: String): String {
        val cleanPath = filePath.trimStart('/')
        return "$BASE_FILE_API/$cleanPath"
    }

    /**
     * Resolves a file_id to a direct streaming URL using the Telegram getFile API.
     */
    suspend fun resolveFileIdToStreamUrl(fileId: String): String? = withContext(Dispatchers.IO) {
        if (fileId.isBlank()) return@withContext null
        try {
            val url = "$BASE_API/getFile?file_id=$fileId"
            val request = Request.Builder().url(url).build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val json = JSONObject(body)
                if (json.optBoolean("ok")) {
                    val result = json.optJSONObject("result")
                    val filePath = result?.optString("file_path")
                    if (!filePath.isNullOrBlank()) {
                        return@withContext getDirectStreamingUrl(filePath)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error resolving telegram file id: ${e.message}")
        }
        return@withContext null
    }

    /**
     * Uploads/forwards audio stream URL to the private Telegram channel and resolves the
     * direct Telegram CDN file stream URL.
     */
    suspend fun uploadAndCacheSong(song: Song, audioUrl: String): String? = withContext(Dispatchers.IO) {
        if (audioUrl.isBlank()) return@withContext null
        try {
            val formBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("chat_id", CHANNEL_CHAT_ID)
                .addFormDataPart("audio", audioUrl)
                .addFormDataPart("title", song.title.ifBlank { "Unknown Title" })
                .addFormDataPart("performer", song.getDisplayArtist())
                .addFormDataPart("caption", "🎵 Streamed via Resso Music Cloud CDN\nYouTube ID: ${song.id}")
                .build()

            val request = Request.Builder()
                .url("$BASE_API/sendAudio")
                .post(formBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: return@withContext null
                val json = JSONObject(body)
                if (json.optBoolean("ok")) {
                    val result = json.optJSONObject("result")
                    val audioObj = result?.optJSONObject("audio")
                    val fileId = audioObj?.optString("file_id")
                    if (!fileId.isNullOrBlank()) {
                        Log.i(TAG, "Successfully cached song ${song.id} to Telegram CDN. FileId: $fileId")
                        return@withContext resolveFileIdToStreamUrl(fileId)
                    }
                } else {
                    Log.w(TAG, "Telegram sendAudio returned false: ${json.optString("description")}")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Exception caching song to Telegram: ${e.message}")
        }
        return@withContext null
    }

    /**
     * Helper to verify if a candidate string is an accessible direct Telegram stream URL.
     */
    fun isTelegramUrl(url: String?): Boolean {
        if (url == null) return false
        return url.contains("api.telegram.org/file") || url.contains("telegram.org")
    }
}
