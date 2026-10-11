package com.resso.craka.data.firebase

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.resso.craka.BuildConfig

/**
 * Firebase initialization and configuration singleton.
 * Centralizes Firebase App setup, programmatic fallback, and error handling.
 */
object FirebaseInitializer {
    private const val TAG = "FirebaseInitializer"
    @Volatile
    private var isInitialized = false

    const val PROJECT_ID = "resso-music-ad0cf"
    const val APPLICATION_ID = "1:564959689026:android:faa4c6af1c2ad24830f0db"
    const val API_KEY = "AIzaSyBjUTi9GJ6L1XH8qVFeU8dSbj4Nb6huyAU"
    const val STORAGE_BUCKET = "resso-music-ad0cf.firebasestorage.app"

    /**
     * Initialize Firebase with context and guaranteed fallback options.
     */
    fun initialize(context: Context? = null): Boolean {
        try {
            val app = if (context != null) {
                if (FirebaseApp.getApps(context).isEmpty()) {
                    val options = FirebaseOptions.Builder()
                        .setApplicationId(APPLICATION_ID)
                        .setApiKey(API_KEY)
                        .setProjectId(PROJECT_ID)
                        .setStorageBucket(STORAGE_BUCKET)
                        .build()
                    FirebaseApp.initializeApp(context.applicationContext, options)
                    Log.i(TAG, "Firebase initialized with programmatic FirebaseOptions")
                } else {
                    FirebaseApp.getInstance()
                }
            } else {
                try {
                    FirebaseApp.getInstance()
                } catch (_: Exception) {
                    null
                }
            }

            if (app != null) {
                isInitialized = true
                Log.i(TAG, "FirebaseApp ready: ${app.options.projectId}")

                try {
                    val firestore = FirebaseFirestore.getInstance()
                    val cacheSettings = PersistentCacheSettings.newBuilder()
                        .setSizeBytes(100L * 1024L * 1024L) // 100 MB offline cache
                        .build()

                    val settings = FirebaseFirestoreSettings.Builder()
                        .setLocalCacheSettings(cacheSettings)
                        .build()

                    firestore.firestoreSettings = settings
                    Log.i(TAG, "Firestore cache settings configured")
                } catch (e: Exception) {
                    Log.w(TAG, "Firestore settings note: ${e.message}")
                }

                return true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Firebase initialization error: ${e.message}")
        }
        return false
    }

    fun isReady(context: Context? = null): Boolean {
        if (!isInitialized) {
            initialize(context)
        }
        return try {
            FirebaseApp.getInstance() != null
        } catch (_: Exception) {
            false
        }
    }

    fun getFirestore(context: Context? = null): FirebaseFirestore? {
        if (!isReady(context)) {
            initialize(context)
        }
        return try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.w(TAG, "Firestore access error: ${e.message}")
            null
        }
    }

    fun logConfiguration() {
        try {
            val app = FirebaseApp.getInstance()
            Log.d(TAG, """
                Firebase Configuration:
                - Project ID: ${app.options.projectId}
                - API Key: ${app.options.apiKey?.take(10)}...
                - Storage Bucket: ${app.options.storageBucket}
                - Build Config: ${BuildConfig.APPLICATION_ID}
            """.trimIndent())
        } catch (e: Exception) {
            Log.w(TAG, "Could not log Firebase config: ${e.message}")
        }
    }
}
