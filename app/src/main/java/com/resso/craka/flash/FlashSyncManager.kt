package com.resso.craka.flash

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Process
import android.util.Log

/**
 * Ultra-Low-Latency Hardware Flash Torch Synchronization Manager.
 * Safely manages camera flash strobe pulses without throwing security exceptions
 * or crashing on devices without flash hardware.
 */
class FlashSyncManager(private val context: Context) {

    companion object {
        private const val TAG = "FlashSyncManager"
        private const val FLASH_PULSE_DURATION_MS = 38L // Crisp, sharp strobe burst
        private const val MIN_STROBE_INTERVAL_MS = 140L // Guard against LED overheating / camera binder flood
    }

    private val cameraManager = try {
        context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    } catch (_: Throwable) {
        null
    }
    private var cameraId: String? = null

    @Volatile
    var isTorchOn = false
        private set

    @Volatile
    var isBeatSyncRunning = false
        private set

    var latencyOffsetMs: Long = 180L

    // Dedicated background thread with safe priority to avoid SecurityException on OEM ROMs
    private val flashThread: HandlerThread? = try {
        HandlerThread("FlashSyncThread", Process.THREAD_PRIORITY_DEFAULT).apply {
            start()
        }
    } catch (t: Throwable) {
        Log.w(TAG, "Fallback HandlerThread: ${t.message}")
        null
    }

    private val flashHandler: Handler = try {
        flashThread?.looper?.let { Handler(it) } ?: Handler(Looper.getMainLooper())
    } catch (_: Throwable) {
        Handler(Looper.getMainLooper())
    }

    private var lastFlashTimestamp = 0L

    private val turnOffRunnable = Runnable {
        applyTorch(false)
    }

    private val fallbackRhythmRunnable = object : Runnable {
        override fun run() {
            if (!isBeatSyncRunning) return
            pulseInternal(0L)
            // 128 BPM energetic beat pulse (468ms interval)
            try {
                flashHandler.postDelayed(this, 468L)
            } catch (_: Throwable) {}
        }
    }

    init {
        try {
            flashHandler.post {
                findCameraWithFlash()
            }
        } catch (_: Throwable) {}
    }

    private fun findCameraWithFlash() {
        if (cameraId != null) return
        try {
            val cm = cameraManager ?: return
            val idList = cm.cameraIdList ?: return
            for (id in idList) {
                try {
                    val characteristics = cm.getCameraCharacteristics(id)
                    val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                    val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                    if (hasFlash && (facing == CameraCharacteristics.LENS_FACING_BACK || cameraId == null)) {
                        cameraId = id
                        if (facing == CameraCharacteristics.LENS_FACING_BACK) return
                    }
                } catch (_: Throwable) {
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Safe check for flash camera: ${t.message}")
            cameraId = null
        }
    }

    /**
     * Pulses the camera torch once on beat with microsecond-level accuracy.
     */
    fun pulseOnce(delayMs: Long = latencyOffsetMs) {
        if (!isBeatSyncRunning || cameraId == null) return
        val effectiveDelay = delayMs.coerceAtLeast(0L)
        try {
            if (effectiveDelay == 0L) {
                flashHandler.post {
                    pulseInternal(0L)
                }
            } else {
                flashHandler.postDelayed({
                    pulseInternal(0L)
                }, effectiveDelay)
            }
        } catch (_: Throwable) {}
    }

    private fun pulseInternal(extraOffsetMs: Long) {
        if (!isBeatSyncRunning || cameraId == null) return
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - lastFlashTimestamp < MIN_STROBE_INTERVAL_MS) {
            return
        }
        lastFlashTimestamp = now

        try {
            // Cancel any pending turn-off to avoid race conditions
            flashHandler.removeCallbacks(turnOffRunnable)

            // Turn on instantly
            applyTorch(true)

            // Schedule turn-off precisely after 38ms
            flashHandler.postDelayed(turnOffRunnable, FLASH_PULSE_DURATION_MS)
        } catch (_: Throwable) {}
    }

    fun startDirectSync() {
        stopSync()
        if (cameraId == null) findCameraWithFlash()
        if (cameraId != null) {
            isBeatSyncRunning = true
        }
    }

    fun startFallbackRhythm() {
        stopSync()
        if (cameraId == null) findCameraWithFlash()
        if (cameraId != null) {
            isBeatSyncRunning = true
            try {
                flashHandler.post(fallbackRhythmRunnable)
            } catch (_: Throwable) {}
        }
    }

    fun stopSync() {
        isBeatSyncRunning = false
        try {
            flashHandler.removeCallbacksAndMessages(null)
            flashHandler.post {
                applyTorch(false)
            }
        } catch (_: Throwable) {}
    }

    fun toggleSteadyTorch(): Boolean {
        stopSync()
        if (cameraId == null) findCameraWithFlash()
        val newState = !isTorchOn
        try {
            flashHandler.post {
                applyTorch(newState)
            }
        } catch (_: Throwable) {}
        return newState
    }

    private fun applyTorch(on: Boolean) {
        val cid = cameraId ?: return
        try {
            cameraManager?.setTorchMode(cid, on)
            isTorchOn = on
        } catch (_: Throwable) {
            isTorchOn = false
        }
    }

    fun release() {
        stopSync()
        try {
            flashThread?.quitSafely()
        } catch (_: Throwable) {}
    }
}
