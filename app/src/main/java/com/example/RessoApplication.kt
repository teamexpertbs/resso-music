package com.example

import android.app.Application
import com.example.data.repository.MusicRepository

class RessoApplication : Application() {
    lateinit var repository: MusicRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = MusicRepository(this)
    }
}
