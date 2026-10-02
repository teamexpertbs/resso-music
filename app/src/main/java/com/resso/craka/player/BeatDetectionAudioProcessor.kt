package com.resso.craka.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
import kotlin.math.sqrt

/**
 * Real-time direct PCM Beat & Bass Kick Detector.
 * Runs directly inside ExoPlayer's AudioSink pipeline.
 * Requires ZERO Android permissions (no RECORD_AUDIO needed), zero latency,
 * and zero reliance on Android's fragile Visualizer API.
 */
class BeatDetectionAudioProcessor(
    private val onBeatDetected: () -> Unit
) : BaseAudioProcessor() {

    @Volatile
    var isEnabled: Boolean = false

    private var lowPassSample: Float = 0f
    private var energyHistory: Float = 300f
    private var lastBeatTimestamp: Long = 0L

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        val outputBuffer = replaceOutputBuffer(remaining)

        if (!isEnabled) {
            outputBuffer.put(inputBuffer)
            outputBuffer.flip()
            return
        }

        // Duplicate read buffer for zero-overhead PCM analysis
        val readOnly = inputBuffer.asReadOnlyBuffer()
        outputBuffer.put(inputBuffer)
        outputBuffer.flip()

        val sampleRate = inputAudioFormat.sampleRate.toFloat().coerceAtLeast(8000f)
        val channelCount = inputAudioFormat.channelCount.coerceAtLeast(1)

        // 1st order low-pass filter for kick & sub-bass (< 130 Hz)
        val fc = 130.0f
        val dt = 1.0f / sampleRate
        val rc = 1.0f / (2.0f * Math.PI.toFloat() * fc)
        val alpha = (dt / (rc + dt)).coerceIn(0.01f, 0.4f)

        var bassEnergySum = 0.0
        var sampleCount = 0

        while (readOnly.remaining() >= 2 * channelCount) {
            var sumSample = 0
            for (c in 0 until channelCount) {
                sumSample += readOnly.short
            }
            val avg = (sumSample / channelCount).toFloat()

            lowPassSample += alpha * (avg - lowPassSample)
            bassEnergySum += (lowPassSample * lowPassSample)
            sampleCount++
        }

        if (sampleCount > 0) {
            val currentRmsEnergy = sqrt(bassEnergySum / sampleCount).toFloat()
            val now = System.currentTimeMillis()

            if (currentRmsEnergy > energyHistory * 1.48f && currentRmsEnergy > 350f && (now - lastBeatTimestamp) > 220L) {
                lastBeatTimestamp = now
                onBeatDetected()
            }

            energyHistory = energyHistory * 0.94f + currentRmsEnergy * 0.06f
        }
    }

    override fun onReset() {
        lowPassSample = 0f
        energyHistory = 300f
        lastBeatTimestamp = 0L
    }
}
