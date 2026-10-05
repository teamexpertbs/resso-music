package com.resso.craka.data.repository

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.resso.craka.data.firebase.FirebaseMusicManager
import com.resso.craka.data.model.SongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger

/**
 * Repository class that fetches song metadata from Firebase Firestore (resso-music-ad0cf project).
 * Uses dynamic queries to ensure variety in results.
 */
class FirestoreMusicRepository(
    private val context: Context,
    private val firebaseManager: FirebaseMusicManager
) {
    companion object {
        private const val TAG = "FirestoreMusicRepo"
        private const val COLLECTION_SONGS = "songs"
    }

    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.w(TAG, "Firestore instance not ready: ${e.message}")
            null
        }
    }

    private val dynamicQueryCounter = AtomicInteger(0)

    /**
     * Dynamically fetches songs from Firestore.
     * Rotates sorting strategies to ensure variety.
     */
    suspend fun getDynamicSongs(limit: Int = 30, excludeIds: Set<String> = emptySet()): List<SongEntity> =
        withContext(Dispatchers.IO) {
            val result = mutableListOf<SongEntity>()

            val db = firestore
            if (db != null) {
                try {
                    val sortFields = listOf("updatedAt", "durationMs", "title", "lastPlayedAt")
                    val selectedField = sortFields[dynamicQueryCounter.getAndIncrement() % sortFields.size]
                    val direction =
                        if (dynamicQueryCounter.get() % 2 == 0) Query.Direction.DESCENDING else Query.Direction.ASCENDING

                    val snapshot = try {
                        db.collection(COLLECTION_SONGS)
                            .orderBy(selectedField, direction)
                            .limit((limit * 2).toLong())
                            .get()
                            .await()
                    } catch (_: Exception) {
                        // Fallback to basic limit if compound index is pending
                        db.collection(COLLECTION_SONGS)
                            .limit((limit * 2).toLong())
                            .get()
                            .await()
                    }

                    for (doc in snapshot.documents) {
                        val id = doc.getString("id") ?: doc.id
                        val audioUrl = doc.getString("audioUrl") ?: ""
                        if (audioUrl.startsWith("http") && id !in excludeIds && result.none { it.id == id }) {
                            result.add(
                                SongEntity(
                                    id = id,
                                    title = doc.getString("title") ?: "Unknown Track",
                                    artist = doc.getString("artist") ?: "Various Artists",
                                    album = doc.getString("album") ?: "Single",
                                    durationMs = doc.getLong("durationMs") ?: 210000L,
                                    audioUrl = audioUrl,
                                    albumArtUrl = doc.getString("albumArtUrl") ?: "",
                                    lyricsLrc = doc.getString("lyricsLrc") ?: "",
                                    genre = doc.getString("genre") ?: "Bollywood",
                                    mood = doc.getString("mood") ?: "Chill"
                                )
                            )
                        }
                        if (result.size >= limit) break
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "getDynamicSongs error: ${e.message}")
                }
            }

            result.shuffled()
        }

    /**
     * Real-time dynamic Flow of songs from Firestore.
     */
    fun observeDynamicSongsRealtime(limit: Int = 30): Flow<List<SongEntity>> = callbackFlow {
        val db = firestore
        if (db == null) {
            val fallback = getDynamicSongs(limit)
            trySend(fallback)
            close()
            return@callbackFlow
        }

        val listener = db.collection(COLLECTION_SONGS)
            .limit((limit * 2).toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Dynamic snapshot listener error: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        val id = doc.getString("id") ?: doc.id
                        val audioUrl = doc.getString("audioUrl") ?: ""
                        val title = doc.getString("title") ?: ""
                        if (audioUrl.isNotBlank() && title.isNotBlank()) {
                            SongEntity(
                                id = id,
                                title = title,
                                artist = doc.getString("artist") ?: "Various Artists",
                                album = doc.getString("album") ?: "Single",
                                durationMs = doc.getLong("durationMs") ?: 210000L,
                                audioUrl = audioUrl,
                                albumArtUrl = doc.getString("albumArtUrl") ?: "",
                                lyricsLrc = doc.getString("lyricsLrc") ?: "",
                                genre = doc.getString("genre") ?: "Bollywood",
                                mood = doc.getString("mood") ?: "Chill"
                            )
                        } else null
                    }.shuffled().take(limit)

                    if (list.isNotEmpty()) {
                        trySend(list)
                    }
                }
            }

        awaitClose {
            listener.remove()
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Real-time search query directly on Firestore metadata
     */
    fun searchDynamicSongs(query: String): Flow<List<SongEntity>> =
        firebaseManager.searchSongsInFirestoreRealtime(query)

    /**
     * Persist song metadata to Firestore
     */
    suspend fun saveSongMetadata(song: SongEntity) = withContext(Dispatchers.IO) {
        firebaseManager.indexSongMetadata(song)
    }

    /**
     * Batch sync songs metadata into Firestore
     */
    suspend fun saveSongsBatch(songs: List<SongEntity>) = withContext(Dispatchers.IO) {
        firebaseManager.indexSongsMetadata(songs)
    }

    /**
     * Updates playback statistics in Firestore
     */
    suspend fun recordSongActivity(song: SongEntity) = withContext(Dispatchers.IO) {
        val db = firestore
        if (db != null) {
            try {
                db.collection(COLLECTION_SONGS).document(song.id).update(
                    mapOf(
                        "lastPlayedAt" to System.currentTimeMillis(),
                        "updatedAt" to System.currentTimeMillis()
                    )
                ).await()
            } catch (_: Exception) {}
        }
        firebaseManager.syncHistory(song)
    }
}
