package com.resso.craka.data.network

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.resso.craka.data.model.Song
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface MusicApiService {
    @GET("search")
    suspend fun searchSongsRaw(
        @Query("q") query: String
    ): Response<JsonElement>
}

object MusicApiHelper {
    private const val TAG = "MusicApiHelper"
    private val gson = Gson()

    suspend fun searchSongs(query: String): List<Song> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()

        return try {
            val response = RetrofitClient.apiService.searchSongsRaw(trimmed)
            if (!response.isSuccessful || response.body() == null) {
                Log.w(TAG, "Search API HTTP failed: ${response.code()}")
                return emptyList()
            }
            val element = response.body()!!
            val songsList = mutableListOf<Song>()

            if (element.isJsonObject) {
                val obj = element.asJsonObject
                val songsArray = when {
                    obj.has("songs") && obj.get("songs").isJsonArray -> obj.getAsJsonArray("songs")
                    obj.has("results") && obj.get("results").isJsonArray -> obj.getAsJsonArray("results")
                    obj.has("data") && obj.get("data").isJsonArray -> obj.getAsJsonArray("data")
                    else -> null
                }
                if (songsArray != null) {
                    for (item in songsArray) {
                        try {
                            val song = gson.fromJson(item, Song::class.java)
                            if (song != null && song.id.isNotBlank()) {
                                songsList.add(song)
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to parse song item: ${e.message}")
                        }
                    }
                }
            } else if (element.isJsonArray) {
                val array = element.asJsonArray
                for (item in array) {
                    try {
                        val song = gson.fromJson(item, Song::class.java)
                        if (song != null && song.id.isNotBlank()) {
                            songsList.add(song)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to parse song item: ${e.message}")
                    }
                }
            }
            songsList
        } catch (e: Exception) {
            Log.w(TAG, "Exception searching songs via Vercel: ${e.message}")
            emptyList()
        }
    }
}
