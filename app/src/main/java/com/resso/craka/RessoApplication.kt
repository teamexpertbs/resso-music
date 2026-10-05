package com.resso.craka

import android.app.Application
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.google.firebase.Firebase
import com.google.firebase.initialize
import com.google.firebase.auth.FirebaseAuth
import com.resso.craka.data.firebase.FirebaseInitializer
import com.resso.craka.data.repository.MusicRepository
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class RessoApplication : Application(), ImageLoaderFactory {
    private val tag = "RessoApplication"
    
    lateinit var repository: MusicRepository
        private set

    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase (should auto-initialize from google-services.json)
        try {
            Firebase.initialize(this)
            Log.i(tag, "Firebase initialized from google-services.json")
        } catch (e: Exception) {
            Log.w(tag, "Firebase initialization attempt: ${e.message}")
        }
        
        // Verify Firebase is ready and log configuration
        FirebaseInitializer.initialize()
        FirebaseInitializer.logConfiguration()
        
        // Authenticate anonymously before starting the repository.
        // Firestore rules can then use request.auth.uid safely.
        val auth = FirebaseAuth.getInstance()

        if (auth.currentUser != null) {
            Log.i(tag, "Firebase Auth already signed in: ${auth.currentUser?.uid}")
            repository = MusicRepository(this)
            Log.i(tag, "RessoApplication onCreate complete. Firebase ready: ${FirebaseInitializer.isReady()}")
        } else {
            auth.signInAnonymously()
                .addOnSuccessListener { result ->
                    Log.i(tag, "Anonymous Firebase Auth successful: ${result.user?.uid}")

                    // Start repository only after authentication succeeds.
                    repository = MusicRepository(this)

                    Log.i(
                        tag,
                        "RessoApplication onCreate complete. Firebase ready: ${FirebaseInitializer.isReady()}"
                    )
                }
                .addOnFailureListener { error ->
                    Log.e(tag, "Anonymous Firebase Auth failed: ${error.message}", error)

                    // Keep the app usable even if Auth is temporarily unavailable.
                    repository = MusicRepository(this)

                    Log.i(
                        tag,
                        "RessoApplication started without authenticated Firestore access"
                    )
                }
        }
    }

    override fun newImageLoader(): ImageLoader {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request()
                val host = request.url.host
                val builder = request.newBuilder()
                    .header(
                        "User-Agent",
                        "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 " +
                            "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                    )
                if (host.contains("saavncdn.com") || host.contains("jiosaavn.com")) {
                    builder.header("Referer", "https://www.jiosaavn.com/")
                }
                if (host.contains("ytimg") || host.contains("ggpht") || host.contains("googleusercontent")) {
                    builder.header("Referer", "https://www.youtube.com/")
                }
                chain.proceed(builder.build())
            }
            .build()

        return ImageLoader.Builder(this)
            .okHttpClient(client)
            .allowHardware(false)
            .crossfade(true)
            .respectCacheHeaders(false)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(200L * 1024 * 1024) // 200MB dedicated disk cache
                    .build()
            }
            .build()
    }
}
