package com.resso.craka.data.model

import com.google.gson.annotations.SerializedName

data class Song(
    @SerializedName("id") val id: String = "",
    @SerializedName("title") val title: String = "",
    @SerializedName("artist") val artist: String = "",
    @SerializedName("channel") val channel: String = "",
    @SerializedName("thumbnail") val thumbnail: String = "",
    @SerializedName("duration") val duration: String = "",
    @SerializedName("duration_seconds") val durationSeconds: Long = 0L,
    @SerializedName("views") val views: String = "",
    @SerializedName("telegram_url") var telegramUrl: String? = null,
    @SerializedName("youtube_url") val youtubeUrl: String = "",
    @SerializedName("stream_url") var streamUrl: String = "",
    @SerializedName("play_url") var playUrl: String = "",
    @SerializedName("download_url") var downloadUrl: String = "",
    @SerializedName("timestamp") val timestamp: Long = System.currentTimeMillis()
) {
    fun getDisplayArtist(): String = artist.ifBlank { channel.ifBlank { "Unknown Artist" } }

    fun getEffectiveThumbnail(): String {
        return if (thumbnail.isNotBlank()) {
            thumbnail
        } else if (id.isNotBlank()) {
            "https://i.ytimg.com/vi/$id/hqdefault.jpg"
        } else {
            ""
        }
    }

    fun toSongEntity(): SongEntity {
        val validAudioUrl = when {
            !telegramUrl.isNullOrBlank() -> telegramUrl!!
            streamUrl.isNotBlank() && !streamUrl.contains("vercel.app") && (streamUrl.contains(".mp4") || streamUrl.contains(".mp3") || streamUrl.contains(".m4a") || streamUrl.contains("telegram.org")) -> streamUrl
            playUrl.isNotBlank() && !playUrl.contains("vercel.app") && (playUrl.contains(".mp4") || playUrl.contains(".mp3") || playUrl.contains(".m4a") || playUrl.contains("telegram.org")) -> playUrl
            else -> "https://youtu.be/$id"
        }
        return SongEntity(
            id = id,
            title = title,
            artist = getDisplayArtist(),
            album = channel.ifBlank { "YouTube Music" },
            durationMs = if (durationSeconds > 0) durationSeconds * 1000L else 210_000L,
            audioUrl = validAudioUrl,
            albumArtUrl = getEffectiveThumbnail(),
            genre = "Trending",
            mood = "Energetic"
        )
    }
}

fun SongEntity.toSong(): Song {
    val durationSec = durationMs / 1000
    val minutes = durationSec / 60
    val seconds = durationSec % 60
    val durStr = String.format("%d:%02d", minutes, seconds)
    return Song(
        id = id,
        title = title,
        artist = artist,
        channel = album,
        thumbnail = albumArtUrl,
        duration = durStr,
        durationSeconds = durationSec,
        youtubeUrl = if (audioUrl.contains("youtu")) audioUrl else "https://youtu.be/$id",
        streamUrl = if (audioUrl.contains("vercel.app")) "" else audioUrl,
        telegramUrl = if (audioUrl.contains("telegram.org")) audioUrl else null
    )
}
