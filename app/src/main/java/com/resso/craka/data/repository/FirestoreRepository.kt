package com.resso.craka.data.repository

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.SetOptions
import com.resso.craka.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FirestoreRepository private constructor(private val context: Context) {

    companion object {
        private const val TAG = "FirestoreRepository"
        const val COLLECTION_CACHED_SONGS = "cached_songs"
        const val COLLECTION_USERS = "users"
        const val SUBCOLLECTION_LIKED = "liked_songs"
        const val SUBCOLLECTION_HISTORY = "history"

        @Volatile
        private var INSTANCE: FirestoreRepository? = null

        fun getInstance(context: Context): FirestoreRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FirestoreRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    val db: FirebaseFirestore by lazy {
        val firestore = FirebaseFirestore.getInstance()
        try {
            val cacheSettings = PersistentCacheSettings.newBuilder()
                .setSizeBytes(100L * 1024L * 1024L) // 100 MB offline persistence
                .build()

            val settings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(cacheSettings)
                .build()

            firestore.firestoreSettings = settings
            Log.d(TAG, "Firestore persistent cache settings successfully enabled")
        } catch (e: Exception) {
            Log.w(TAG, "Note: Firestore settings already initialized or warning: ${e.message}")
        }
        firestore
    }

    /**
     * Check cached song for Telegram CDN URL or existing metadata in cached_songs/{videoId}
     */
    suspend fun getCachedSong(videoId: String): Song? = withContext(Dispatchers.IO) {
        if (videoId.isBlank()) return@withContext null
        try {
            val doc = db.collection(COLLECTION_CACHED_SONGS).document(videoId).get().await()
            if (doc.exists()) {
                val id = doc.getString("id") ?: videoId
                val title = doc.getString("title") ?: ""
                val artist = doc.getString("artist") ?: ""
                val thumbnail = doc.getString("thumbnail") ?: ""
                val duration = doc.getString("duration") ?: ""
                val telegramUrl = doc.getString("telegram_url")
                val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()

                return@withContext Song(
                    id = id,
                    title = title,
                    artist = artist,
                    thumbnail = thumbnail,
                    duration = duration,
                    telegramUrl = telegramUrl,
                    timestamp = timestamp
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching cached song $videoId: ${e.message}")
        }
        return@withContext null
    }

    /**
     * Save/update metadata to cached_songs/{videoId}
     */
    suspend fun saveCachedSong(song: Song, telegramUrl: String? = null) = withContext(Dispatchers.IO) {
        if (song.id.isBlank()) return@withContext
        try {
            val effectiveTelegramUrl = telegramUrl ?: song.telegramUrl
            val data = hashMapOf(
                "id" to song.id,
                "title" to song.title,
                "artist" to song.getDisplayArtist(),
                "thumbnail" to song.getEffectiveThumbnail(),
                "duration" to song.duration,
                "views" to song.views,
                "timestamp" to System.currentTimeMillis()
            )
            if (!effectiveTelegramUrl.isNullOrBlank()) {
                data["telegram_url"] = effectiveTelegramUrl
            }

            db.collection(COLLECTION_CACHED_SONGS).document(song.id)
                .set(data, SetOptions.merge())
                .await()
            Log.d(TAG, "Saved song ${song.id} to cached_songs")
        } catch (e: Exception) {
            Log.w(TAG, "Error saving cached song: ${e.message}")
        }
    }

    /**
     * Liked Songs: users/{userId}/liked_songs/{videoId}
     */
    suspend fun isSongLiked(userId: String, videoId: String): Boolean = withContext(Dispatchers.IO) {
        if (userId.isBlank() || videoId.isBlank()) return@withContext false
        try {
            val doc = db.collection(COLLECTION_USERS)
                .document(userId)
                .collection(SUBCOLLECTION_LIKED)
                .document(videoId)
                .get()
                .await()
            return@withContext doc.exists()
        } catch (e: Exception) {
            Log.w(TAG, "Error checking like state: ${e.message}")
            return@withContext false
        }
    }

    suspend fun toggleLike(userId: String, song: Song): Boolean = withContext(Dispatchers.IO) {
        if (userId.isBlank() || song.id.isBlank()) return@withContext false
        try {
            val ref = db.collection(COLLECTION_USERS)
                .document(userId)
                .collection(SUBCOLLECTION_LIKED)
                .document(song.id)

            val doc = ref.get().await()
            if (doc.exists()) {
                ref.delete().await()
                return@withContext false
            } else {
                val data = hashMapOf(
                    "id" to song.id,
                    "title" to song.title,
                    "artist" to song.getDisplayArtist(),
                    "thumbnail" to song.getEffectiveThumbnail(),
                    "duration" to song.duration,
                    "telegram_url" to (song.telegramUrl ?: ""),
                    "timestamp" to System.currentTimeMillis()
                )
                ref.set(data).await()
                return@withContext true
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error toggling like for song ${song.id}: ${e.message}")
            return@withContext false
        }
    }

    fun getLikedSongsFlow(userId: String): Flow<List<Song>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = db.collection(COLLECTION_USERS)
            .document(userId)
            .collection(SUBCOLLECTION_LIKED)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Listen failed for liked songs: ${error.message}")
                    return@addSnapshotListener
                }
                val songs = snapshot?.documents?.mapNotNull { doc ->
                    Song(
                        id = doc.getString("id") ?: doc.id,
                        title = doc.getString("title") ?: "",
                        artist = doc.getString("artist") ?: "",
                        thumbnail = doc.getString("thumbnail") ?: "",
                        duration = doc.getString("duration") ?: "",
                        telegramUrl = doc.getString("telegram_url")?.takeIf { it.isNotBlank() },
                        timestamp = doc.getLong("timestamp") ?: 0L
                    )
                } ?: emptyList()
                trySend(songs)
            }

        awaitClose { listener.remove() }
    }.flowOn(Dispatchers.IO)

    /**
     * History: users/{userId}/history/{videoId}
     */
    suspend fun addToHistory(userId: String, song: Song) = withContext(Dispatchers.IO) {
        if (userId.isBlank() || song.id.isBlank()) return@withContext
        try {
            val data = hashMapOf(
                "id" to song.id,
                "title" to song.title,
                "artist" to song.getDisplayArtist(),
                "thumbnail" to song.getEffectiveThumbnail(),
                "duration" to song.duration,
                "telegram_url" to (song.telegramUrl ?: ""),
                "timestamp" to System.currentTimeMillis()
            )

            db.collection(COLLECTION_USERS)
                .document(userId)
                .collection(SUBCOLLECTION_HISTORY)
                .document(song.id)
                .set(data, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.w(TAG, "Error recording history: ${e.message}")
        }
    }

    fun getHistoryFlow(userId: String): Flow<List<Song>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = db.collection(COLLECTION_USERS)
            .document(userId)
            .collection(SUBCOLLECTION_HISTORY)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Listen failed for history: ${error.message}")
                    return@addSnapshotListener
                }
                val songs = snapshot?.documents?.mapNotNull { doc ->
                    Song(
                        id = doc.getString("id") ?: doc.id,
                        title = doc.getString("title") ?: "",
                        artist = doc.getString("artist") ?: "",
                        thumbnail = doc.getString("thumbnail") ?: "",
                        duration = doc.getString("duration") ?: "",
                        telegramUrl = doc.getString("telegram_url")?.takeIf { it.isNotBlank() },
                        timestamp = doc.getLong("timestamp") ?: 0L
                    )
                } ?: emptyList()
                trySend(songs)
            }

        awaitClose { listener.remove() }
    }.flowOn(Dispatchers.IO)
}
