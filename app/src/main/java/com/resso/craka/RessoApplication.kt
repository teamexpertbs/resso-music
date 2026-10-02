package com.resso.craka

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.resso.craka.data.repository.MusicRepository
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class RessoApplication : Application(), ImageLoaderFactory {
    lateinit var repository: MusicRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = MusicRepository(this)
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
