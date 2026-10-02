package com.resso.craka.flash

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FlashSyncManager(private val context: Context) {
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private var cameraId: String? = null
    var isTorchOn = false
        private set
    var isBeatSyncRunning = false
        private set
    private var rhythmJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    // Latency compensation offset (AudioTrack buffer decode-to-speaker delay, default 160ms)
    var latencyOffsetMs: Long = 160L

    init {
        findCameraWithFlash()
    }

    private fun findCameraWithFlash() {
        try {
            cameraManager?.cameraIdList?.forEach { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (hasFlash && (facing == CameraCharacteristics.LENS_FACING_BACK || cameraId == null)) {
                    cameraId = id
                    if (facing == CameraCharacteristics.LENS_FACING_BACK) return
                }
            }
        } catch (_: Exception) {
            cameraId = null
        }
    }

    fun pulseOnce(delayMs: Long = latencyOffsetMs) {
        if (!isBeatSyncRunning) return
        scope.launch {
            if (delayMs > 0) {
                delay(delayMs)
            }
            if (!isBeatSyncRunning) return@launch
            setFlash(true)
            delay(50)
            setFlash(false)
        }
    }

    fun startDirectSync() {
        stopSync()
        isBeatSyncRunning = true
    }

    fun startFallbackRhythm() {
        stopSync()
        isBeatSyncRunning = true
        rhythmJob = scope.launch {
            while (isActive) {
                setFlash(true)
                delay(50)
                setFlash(false)
                delay(450)
            }
        }
    }

    fun stopSync() {
        isBeatSyncRunning = false
        rhythmJob?.cancel()
        rhythmJob = null
        setFlash(false)
    }

    fun toggleSteadyTorch(): Boolean {
        stopSync()
        val newState = !isTorchOn
        setFlash(newState)
        return newState
    }

    private fun setFlash(on: Boolean) {
        val cid = cameraId ?: return
        try {
            cameraManager?.setTorchMode(cid, on)
            isTorchOn = on
        } catch (_: CameraAccessException) {
            isTorchOn = false
        } catch (_: Exception) {
            isTorchOn = false
        }
    }
}
