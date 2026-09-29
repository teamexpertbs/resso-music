package com.resso.craka.data.network

import android.content.Context
import com.resso.craka.data.model.SongEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class CatalogCache(context: Context) {
    private val dir = File(context.applicationContext.filesDir, "catalog").apply { mkdirs() }

    fun read(key: String, maxAgeMs: Long): List<SongEntity>? {
        val file = fileFor(key)
        if (!file.exists()) return null
        if (System.currentTimeMillis() - file.lastModified() > maxAgeMs) return null
        return decode(file.readText())
    }

    fun write(key: String, songs: List<SongEntity>) {
        if (songs.isEmpty()) return
        fileFor(key).writeText(encode(songs.take(30)))
    }

    private fun fileFor(key: String): File {
        val safe = key.lowercase().replace(Regex("[^a-z0-9]+"), "_").take(48)
        return File(dir, "$safe.json")
    }

    private fun encode(songs: List<SongEntity>): String {
        val array = JSONArray()
        songs.forEach { song ->
            array.put(
                JSONObject()
                    .put("id", song.id)
                    .put("title", song.title)
                    .put("artist", song.artist)
                    .put("album", song.album)
                    .put("durationMs", song.durationMs)
                    .put("audioUrl", song.audioUrl)
                    .put("albumArtUrl", song.albumArtUrl)
                    .put("genre", song.genre)
                    .put("mood", song.mood)
            )
        }
        return array.toString()
    }

    private fun decode(raw: String): List<SongEntity>? {
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    val id = item.optString("id")
                    val title = item.optString("title")
                    if (id.isBlank() || title.isBlank()) continue
                    add(
                        SongEntity(
                            id = id,
                            title = title,
                            artist = item.optString("artist", "Unknown"),
                            album = item.optString("album", ""),
                            durationMs = item.optLong("durationMs", 0L),
                            audioUrl = item.optString("audioUrl", ""),
                            albumArtUrl = item.optString("albumArtUrl", ""),
                            genre = item.optString("genre", "Music"),
                            mood = item.optString("mood", "Chill")
                        )
                    )
                }
            }
        } catch (_: Exception) {
            null
        }
    }
}
