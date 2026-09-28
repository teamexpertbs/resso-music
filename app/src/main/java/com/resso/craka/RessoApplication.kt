package com.resso.craka

import android.app.Application
import com.resso.craka.data.repository.MusicRepository

class RessoApplication : Application() {
    lateinit var repository: MusicRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = MusicRepository(this)
    }
}
