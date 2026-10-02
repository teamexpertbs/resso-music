package com.resso.craka.flash

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.audiofx.Visualizer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.hypot

class FlashSyncManager(private val context: Context) {
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private var cameraId: String? = null
    var isTorchOn = false
        private set
    var isBeatSyncRunning = false
        private set
    private var flashJob: Job? = null
    private var visualizer: Visualizer? = null
    private var lastBeatTimestamp = 0L
    private var smoothedBassEnergy = 25.0
    private val scope = CoroutineScope(Dispatchers.Default)

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
        } catch (e: Exception) {
            cameraId = null
        }
    }

    fun startSync(audioSessionId: Int = 0) {
        stopSync()
        isBeatSyncRunning = true

        // True dynamic beat sync using real-time audio FFT
        if (audioSessionId > 0) {
            try {
                val viz = Visualizer(audioSessionId).apply {
                    captureSize = Visualizer.getCaptureSizeRange()[0].coerceAtLeast(128)
                    setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(v: Visualizer?, waveform: ByteArray?, sr: Int) {}

                        override fun onFftDataCapture(v: Visualizer?, fft: ByteArray?, sr: Int) {
                            if (fft == null || !isBeatSyncRunning) return
                            // Low-frequency bins (1 to 4 correspond to ~30Hz-180Hz kick/bass)
                            var bassEnergy = 0.0
                            val maxBins = minOf(4, fft.size / 2)
                            for (k in 1 until maxBins) {
                                val re = fft[2 * k].toDouble()
                                val im = fft[2 * k + 1].toDouble()
                                bassEnergy += hypot(re, im)
                            }

                            // Dynamic adaptive beat detection
                            val now = System.currentTimeMillis()
                            if (bassEnergy > smoothedBassEnergy * 1.45 && (now - lastBeatTimestamp) > 220) {
                                lastBeatTimestamp = now
                                pulseOnce()
                            }
                            smoothedBassEnergy = smoothedBassEnergy * 0.92 + bassEnergy * 0.08
                        }
                    }, Visualizer.getMaxCaptureRate() / 2, false, true)
                    enabled = true
                }
                visualizer = viz
                return
            } catch (e: Exception) {
                Log.w("FlashSyncManager", "Visualizer beat detection fallback: ${e.message}")
            }
        }

        // Adaptive rhythm fallback if session ID unset or visualizer denied
        flashJob = scope.launch {
            while (isActive) {
                pulseOnce()
                delay(450)
            }
        }
    }

    fun pulseOnce() {
        scope.launch {
            setFlash(true)
            delay(55)
            setFlash(false)
        }
    }

    fun stopSync() {
        isBeatSyncRunning = false
        try {
            visualizer?.enabled = false
            visualizer?.release()
        } catch (_: Exception) {}
        visualizer = null
        flashJob?.cancel()
        flashJob = null
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
        } catch (e: CameraAccessException) {
            isTorchOn = false
        } catch (e: Exception) {
            isTorchOn = false
        }
    }
}
