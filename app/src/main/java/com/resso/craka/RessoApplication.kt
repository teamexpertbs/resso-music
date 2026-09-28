package com.resso.craka

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.resso.craka.data.repository.MusicRepository
import okhttp3.OkHttpClient

class RessoApplication : Application(), ImageLoaderFactory {
    lateinit var repository: MusicRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = MusicRepository(this)
    }

    override fun newImageLoader(): ImageLoader {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                val host = request.url.host
                val builder = request.newBuilder()
                    .header(
                        "User-Agent",
                        "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 " +
                            "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                    )
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
            .build()
    }
}
