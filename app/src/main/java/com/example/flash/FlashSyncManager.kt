package com.example.flash

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FlashSyncManager(private val context: Context) {
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private var cameraId: String? = null
    private var isTorchOn = false
    private var flashJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    init {
        findCameraWithFlash()
    }

    private fun findCameraWithFlash() {
        try {
            cameraManager?.cameraIdList?.forEach { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                if (hasFlash) {
                    cameraId = id
                    return
                }
            }
        } catch (_: Exception) {
            cameraId = null
        }
    }

    fun startSync(bpm: Int = 120) {
        stopSync()
        val intervalMs = (60_000 / bpm).coerceIn(200, 800)
        flashJob = scope.launch {
            while (isActive) {
                setFlash(true)
                delay(80)
                setFlash(false)
                delay(intervalMs - 80L)
            }
        }
    }

    fun pulseOnce() {
        scope.launch {
            setFlash(true)
            delay(100)
            setFlash(false)
        }
    }

    fun stopSync() {
        flashJob?.cancel()
        flashJob = null
        setFlash(false)
    }

    private fun setFlash(on: Boolean) {
        val cid = cameraId ?: return
        try {
            cameraManager?.setTorchMode(cid, on)
            isTorchOn = on
        } catch (_: CameraAccessException) {
        } catch (_: Exception) {
        }
    }
}
