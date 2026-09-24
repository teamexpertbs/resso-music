package com.example.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import com.example.data.model.CommentEntity
import com.example.data.model.PlaylistEntity
import com.example.data.model.SongEntity
import com.example.data.model.VibeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM songs ORDER BY createdAt ASC")
    fun getAllSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE isLiked = 1 ORDER BY createdAt DESC")
    fun getLikedSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE id = :id LIMIT 1")
    suspend fun getSongById(id: String): SongEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSongs(songs: List<SongEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: SongEntity)

    @Update
    suspend fun updateSong(song: SongEntity)

    @Query("UPDATE songs SET isLiked = :isLiked WHERE id = :id")
    suspend fun setLiked(id: String, isLiked: Boolean)

    @Query("UPDATE songs SET vibeVideoUri = :vibeUri WHERE id = :id")
    suspend fun updateSongVibe(id: String, vibeUri: String)

    @Query("SELECT COUNT(*) FROM songs")
    suspend fun getSongCount(): Int
}

@Dao
interface VibeDao {
    @Query("SELECT * FROM vibes WHERE songId = :songId ORDER BY createdAt DESC")
    fun getVibesForSong(songId: String): Flow<List<VibeEntity>>

    @Query("SELECT * FROM vibes ORDER BY createdAt DESC")
    fun getAllVibes(): Flow<List<VibeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVibe(vibe: VibeEntity): Long

    @Query("DELETE FROM vibes WHERE id = :id")
    suspend fun deleteVibe(id: Long)
}

@Dao
interface CommentDao {
    @Query("SELECT * FROM comments WHERE songId = :songId ORDER BY createdAt DESC")
    fun getCommentsForSong(songId: String): Flow<List<CommentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: CommentEntity): Long

    @Query("UPDATE comments SET likesCount = likesCount + (CASE WHEN isLikedByMe = 1 THEN -1 ELSE 1 END), isLikedByMe = (CASE WHEN isLikedByMe = 1 THEN 0 ELSE 1 END) WHERE id = :commentId")
    suspend fun toggleCommentLike(commentId: Long)
}

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)
}

@Database(
    entities = [SongEntity::class, VibeEntity::class, CommentEntity::class, PlaylistEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun vibeDao(): VibeDao
    abstract fun commentDao(): CommentDao
    abstract fun playlistDao(): PlaylistDao
}
