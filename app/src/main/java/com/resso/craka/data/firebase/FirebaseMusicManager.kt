package com.resso.craka.data.firebase

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.resso.craka.data.model.CommentEntity
import com.resso.craka.data.model.PlaylistEntity
import com.resso.craka.data.model.SongEntity
import com.resso.craka.data.model.VibeEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

class FirebaseMusicManager(private val context: Context) {
    companion object {
        private const val TAG = "FirebaseMusicManager"
        private const val PREFS_NAME = "firebase_device_prefs"
        private const val KEY_DEVICE_ID = "firebase_device_uuid"
    }

    private val deviceId: String by lazy {
        resolveDeviceId()
    }

    private val firestore: FirebaseFirestore? by lazy {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                FirebaseFirestore.getInstance()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Firebase not initialized: ${e.message}")
            null
        }
    }

    @SuppressLint("HardwareIds")
    private fun resolveDeviceId(): String {
        return try {
            val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            if (!androidId.isNullOrBlank() && androidId != "9774d56d682e549c") {
                androidId
            } else {
                getOrCreateFallbackUuid()
            }
        } catch (_: Exception) {
            getOrCreateFallbackUuid()
        }
    }

    private fun getOrCreateFallbackUuid(): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY_DEVICE_ID, null)
        if (!existing.isNullOrBlank()) return existing
        val newId = UUID.randomUUID().toString().replace("-", "").take(16)
        prefs.edit().putString(KEY_DEVICE_ID, newId).apply()
        return newId
    }

    init {
        registerDeviceInFirestore()
    }

    fun getActiveDeviceId(): String = deviceId

    /**
     * Register this device in Firestore for the resso-music project
     */
    fun registerDeviceInFirestore() {
        val model = android.os.Build.MODEL ?: "Android Device"
        val manufacturer = android.os.Build.MANUFACTURER ?: "Google"
        val androidVer = android.os.Build.VERSION.RELEASE ?: "14"
        val devId = deviceId

        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            val db = firestore
            if (db != null) {
                try {
                    val deviceData = hashMapOf(
                        "deviceId" to devId,
                        "deviceModel" to "$manufacturer $model",
                        "brand" to manufacturer,
                        "model" to model,
                        "androidVersion" to androidVer,
                        "appName" to "Resso Music",
                        "status" to "Active Online",
                        "lastActive" to System.currentTimeMillis()
                    )
                    db.collection("devices").document(devId).set(deviceData, SetOptions.merge()).await()
                    Log.i(TAG, "Device $devId registered in Firestore")
                } catch (e: Exception) {
                    Log.w(TAG, "Device registration error: ${e.message}")
                }
            } else {
                Log.w(TAG, "Firestore not available for device registration")
            }
        }
    }

    /**
     * Sync liked song to Firestore
     */
    suspend fun syncLikedSong(song: SongEntity, isLiked: Boolean) = withContext(Dispatchers.IO) {
        val db = firestore
        if (db != null) {
            try {
                val docRef = db.collection("users")
                    .document(deviceId)
                    .collection("liked_songs")
                    .document(song.id)

                if (isLiked) {
                    val data = hashMapOf(
                        "id" to song.id,
                        "title" to song.title,
                        "artist" to song.artist,
                        "album" to song.album,
                        "durationMs" to song.durationMs,
                        "audioUrl" to song.audioUrl,
                        "albumArtUrl" to song.albumArtUrl,
                        "lyricsLrc" to song.lyricsLrc,
                        "genre" to song.genre,
                        "mood" to song.mood,
                        "isLiked" to true,
                        "syncedAt" to System.currentTimeMillis()
                    )
                    docRef.set(data, SetOptions.merge()).await()
                } else {
                    docRef.delete().await()
                }
            } catch (e: Exception) {
                Log.w(TAG, "syncLikedSong error: ${e.message}")
            }
        }
    }

    /**
     * Fetch cloud-synced liked songs for this device
     */
    suspend fun fetchCloudLikedSongs(): List<SongEntity> = withContext(Dispatchers.IO) {
        val results = mutableListOf<SongEntity>()
        val db = firestore
        if (db != null) {
            try {
                val snapshot = db.collection("users")
                    .document(deviceId)
                    .collection("liked_songs")
                    .get()
                    .await()

                val fromDb = snapshot.documents.mapNotNull { doc ->
                    val id = doc.getString("id") ?: doc.id
                    val title = doc.getString("title") ?: return@mapNotNull null
                    val audioUrl = doc.getString("audioUrl") ?: ""
                    if (audioUrl.isBlank()) return@mapNotNull null

                    SongEntity(
                        id = id,
                        title = title,
                        artist = doc.getString("artist") ?: "Unknown",
                        album = doc.getString("album") ?: "Single",
                        durationMs = doc.getLong("durationMs") ?: 210000L,
                        audioUrl = audioUrl,
                        albumArtUrl = doc.getString("albumArtUrl") ?: "",
                        lyricsLrc = doc.getString("lyricsLrc") ?: "",
                        genre = doc.getString("genre") ?: "Pop",
                        mood = doc.getString("mood") ?: "Chill",
                        isLiked = true
                    )
                }
                results.addAll(fromDb)
            } catch (e: Exception) {
                Log.w(TAG, "fetchCloudLikedSongs error: ${e.message}")
            }
        }
        results
    }

    /**
     * Sync playlists to Firestore
     */
    suspend fun syncPlaylist(playlist: PlaylistEntity) = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext
        try {
            val docRef = db.collection("users")
                .document(deviceId)
                .collection("playlists")
                .document(playlist.id.toString())

            val data = hashMapOf(
                "id" to playlist.id,
                "name" to playlist.name,
                "description" to playlist.description,
                "songIdsCsv" to playlist.songIdsCsv,
                "updatedAt" to System.currentTimeMillis()
            )
            docRef.set(data, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.w(TAG, "syncPlaylist error: ${e.message}")
        }
    }

    /**
     * Sync vibes to Firestore
     */
    suspend fun syncVibe(vibe: VibeEntity) = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext
        try {
            val docRef = db.collection("users")
                .document(deviceId)
                .collection("vibes")
                .document(vibe.id.toString())

            val data = hashMapOf(
                "songId" to vibe.songId,
                "title" to vibe.title,
                "videoUri" to vibe.videoUri,
                "filterType" to vibe.filterType,
                "creatorName" to vibe.creatorName,
                "createdAt" to vibe.createdAt
            )
            docRef.set(data, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.w(TAG, "syncVibe error: ${e.message}")
        }
    }

    /**
     * Sync comments to Firestore
     */
    suspend fun syncComment(comment: CommentEntity) = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext
        try {
            val docRef = db.collection("comments")
                .document(comment.songId)
                .collection("song_comments")
                .document()

            val data = hashMapOf(
                "songId" to comment.songId,
                "userName" to comment.userName,
                "userAvatarColor" to comment.userAvatarColor,
                "content" to comment.content,
                "songTimestampMs" to comment.songTimestampMs,
                "likesCount" to comment.likesCount,
                "createdAt" to System.currentTimeMillis(),
                "deviceId" to deviceId
            )
            docRef.set(data).await()
        } catch (e: Exception) {
            Log.w(TAG, "syncComment error: ${e.message}")
        }
    }

    /**
     * Sync playback history to Firestore
     */
    suspend fun syncHistory(song: SongEntity) = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext
        try {
            val docRef = db.collection("users")
                .document(deviceId)
                .collection("history")
                .document(song.id)

            val data = hashMapOf(
                "songId" to song.id,
                "title" to song.title,
                "artist" to song.artist,
                "playedAt" to System.currentTimeMillis()
            )
            docRef.set(data, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.w(TAG, "syncHistory error: ${e.message}")
        }
    }

    /**
     * Index song metadata into Firestore 'songs' collection for app-wide search
     */
    suspend fun indexSongMetadata(song: SongEntity) = withContext(Dispatchers.IO) {
        if (!song.audioUrl.startsWith("http")) return@withContext
        val db = firestore
        if (db != null) {
            try {
                val titleLower = song.title.lowercase().trim()
                val artistLower = song.artist.lowercase().trim()
                val tokens = (titleLower.split(Regex("\\s+")) + artistLower.split(Regex("\\s+")))
                    .filter { it.length >= 2 }
                    .distinct()

                val data = hashMapOf(
                    "id" to song.id,
                    "title" to song.title,
                    "title_lower" to titleLower,
                    "artist" to song.artist,
                    "artist_lower" to artistLower,
                    "album" to song.album,
                    "durationMs" to song.durationMs,
                    "audioUrl" to song.audioUrl,
                    "albumArtUrl" to song.albumArtUrl,
                    "lyricsLrc" to song.lyricsLrc,
                    "genre" to song.genre,
                    "mood" to song.mood,
                    "keywords" to tokens,
                    "updatedAt" to System.currentTimeMillis()
                )
                db.collection("songs").document(song.id).set(data, SetOptions.merge()).await()
            } catch (e: Exception) {
                Log.w(TAG, "indexSongMetadata error: ${e.message}")
            }
        }
    }

    suspend fun indexSongsMetadata(songs: List<SongEntity>) = withContext(Dispatchers.IO) {
        songs.forEach { indexSongMetadata(it) }
    }

    /**
     * Real-time search query on Firestore 'songs' collection
     */
    fun searchSongsInFirestoreRealtime(query: String): Flow<List<SongEntity>> = callbackFlow {
        val cleanQuery = query.trim().lowercase()
        if (cleanQuery.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val db = firestore
        var listener: com.google.firebase.firestore.ListenerRegistration? = null
        if (db != null) {
            listener = db.collection("songs")
                .limit(50)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Firestore search listener error: ${error.message}")
                        return@addSnapshotListener
                    }

                    if (snapshot == null || snapshot.isEmpty) {
                        return@addSnapshotListener
                    }

                    val songs = snapshot.documents.mapNotNull { doc ->
                        val id = doc.getString("id") ?: doc.id
                        val title = doc.getString("title") ?: ""
                        val titleLower = doc.getString("title_lower") ?: title.lowercase()
                        val artist = doc.getString("artist") ?: "Various Artists"
                        val artistLower = doc.getString("artist_lower") ?: artist.lowercase()
                        val album = doc.getString("album") ?: "Single"
                        val genre = doc.getString("genre") ?: "Bollywood"
                        val audioUrl = doc.getString("audioUrl") ?: ""
                        val keywords = (doc.get("keywords") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()

                        val matches = titleLower.contains(cleanQuery) ||
                            artistLower.contains(cleanQuery) ||
                            genre.lowercase().contains(cleanQuery) ||
                            album.lowercase().contains(cleanQuery) ||
                            keywords.any { it.contains(cleanQuery) || cleanQuery.contains(it) }

                        if (matches && audioUrl.isNotBlank()) {
                            SongEntity(
                                id = id,
                                title = title,
                                artist = artist,
                                album = album,
                                durationMs = doc.getLong("durationMs") ?: 210000L,
                                audioUrl = audioUrl,
                                albumArtUrl = doc.getString("albumArtUrl") ?: "",
                                lyricsLrc = doc.getString("lyricsLrc") ?: "",
                                genre = genre,
                                mood = doc.getString("mood") ?: "Chill"
                            )
                        } else null
                    }
                    if (songs.isNotEmpty()) {
                        trySend(songs)
                    }
                }
        }

        awaitClose {
            listener?.remove()
        }
    }
}
