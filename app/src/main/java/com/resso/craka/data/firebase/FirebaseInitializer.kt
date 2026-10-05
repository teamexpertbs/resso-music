package com.resso.craka.data.firebase

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.firestore
import com.resso.craka.BuildConfig

/**
 * Firebase initialization and configuration singleton.
 * Centralizes Firebase App setup, error handling, and readiness checks.
 */
object FirebaseInitializer {
    private const val TAG = "FirebaseInitializer"
    private var isInitialized = false

    /**
     * Initialize Firebase if not already done.
     * Returns true if Firebase is ready, false otherwise.
     */
    fun initialize(): Boolean {
        if (isInitialized) return true

        return try {
            // FirebaseApp.initializeApp() is called automatically by Google Services plugin
            // when google-services.json is present in app/
            val firebaseApp = FirebaseApp.getInstance()
            isInitialized = true
            
            Log.i(TAG, "Firebase initialized successfully")
            Log.i(TAG, "Project ID: ${firebaseApp.options.projectId}")
            
            // Enable Firestore offline persistence
            try {
                Firebase.firestore.firestoreSettings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)
                    .build()
                Log.i(TAG, "Firestore offline persistence enabled")
            } catch (e: Exception) {
                Log.w(TAG, "Firestore offline persistence warning: ${e.message}")
            }
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Firebase initialization failed: ${e.message}")
            isInitialized = false
            false
        }
    }

    /**
     * Check if Firebase is ready for use.
     */
    fun isReady(): Boolean {
        return try {
            isInitialized && FirebaseApp.getInstance() != null
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get Firestore instance if ready, null otherwise.
     */
    fun getFirestore() = if (isReady()) {
        try {
            Firebase.firestore
        } catch (e: Exception) {
            Log.w(TAG, "Firestore access failed: ${e.message}")
            null
        }
    } else {
        null
    }

    /**
     * Log Firebase configuration for debugging.
     */
    fun logConfiguration() {
        try {
            val app = FirebaseApp.getInstance()
            Log.d(TAG, """
                Firebase Configuration:
                - Project ID: ${app.options.projectId}
                - API Key: ${app.options.apiKey?.take(10)}...
                - Storage Bucket: ${app.options.storageBucket}
                - Database URL: ${app.options.databaseUrl}
                - Build Config: ${BuildConfig.APPLICATION_ID}
            """.trimIndent())
        } catch (e: Exception) {
            Log.w(TAG, "Could not log Firebase config: ${e.message}")
        }
    }
}
