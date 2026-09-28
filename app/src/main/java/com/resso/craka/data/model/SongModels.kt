package com.resso.craka.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val audioUrl: String,
    val albumArtUrl: String,
    val vibeVideoUri: String? = null,
    val lyricsLrc: String = "",
    val genre: String = "Pop",
    val mood: String = "Chill",
    val isLiked: Boolean = false,
    val isCustomUpload: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class LyricLine(
    val timeMs: Long,
    val text: String
)

@Entity(tableName = "vibes")
data class VibeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songId: String,
    val title: String,
    val videoUri: String,
    val startTrimMs: Long = 0L,
    val endTrimMs: Long = 15000L,
    val filterType: String = "Neon",
    val creatorName: String = "Me",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "comments")
data class CommentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songId: String,
    val userName: String,
    val userAvatarColor: Long,
    val content: String,
    val songTimestampMs: Long = 0L,
    val likesCount: Int = 0,
    val isLikedByMe: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val songIdsCsv: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
