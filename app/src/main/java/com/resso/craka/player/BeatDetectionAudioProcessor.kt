package com.resso.craka.player

import android.os.SystemClock
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
import kotlin.math.exp
import kotlin.math.sqrt

class BeatDetectionAudioProcessor(
    private val onBeatDetected: () -> Unit
) : BaseAudioProcessor() {

    @Volatile
    var isEnabled: Boolean = false

    private var lowPassSample: Float = 0f
    private var energyHistory: Float = 300f
    private var dynamicNoiseFloor: Float = 100f
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

        // Forward raw input directly to outputBuffer so downstream processors receive exact PCM
        val outputBuffer = replaceOutputBuffer(remaining)
        // Fix byte order: asReadOnlyBuffer() defaults to BIG_ENDIAN, ensure inputBuffer's order (LITTLE_ENDIAN)
        val readOnly = inputBuffer.asReadOnlyBuffer().order(inputBuffer.order())
        outputBuffer.put(inputBuffer)
        outputBuffer.flip()

        if (!isEnabled) return

        val sampleRate = inputAudioFormat.sampleRate.toFloat().coerceAtLeast(8000f)
        val channelCount = inputAudioFormat.channelCount.coerceAtLeast(1)

        // 1st order IIR low-pass filter focused strictly on sub-bass (<130Hz)
        val dt = 1.0f / sampleRate
        val fc = 130.0f
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
            val now = SystemClock.elapsedRealtime()

            // Time-invariant exponential decay (smoothing independent of buffer size)
            val bufferDurationSec = sampleCount.toFloat() / sampleRate
            val decay = exp(-bufferDurationSec / 0.25f).coerceIn(0.5f, 0.99f)
            energyHistory = energyHistory * decay + currentRmsEnergy * (1.0f - decay)

            // Dynamic noise floor tracking
            dynamicNoiseFloor = dynamicNoiseFloor * 0.98f + (currentRmsEnergy * 0.35f).coerceAtLeast(60f) * 0.02f

            // Adaptive threshold that scales down for quiet songs and up for loud/club tracks
            val dynamicFloor = (energyHistory * 0.35f).coerceAtLeast(dynamicNoiseFloor).coerceAtLeast(80f)
            val beatThreshold = (energyHistory * 1.36f).coerceAtLeast(dynamicFloor * 1.35f)

            if (currentRmsEnergy > beatThreshold && (now - lastBeatTimestamp) > 200L) {
                lastBeatTimestamp = now
                onBeatDetected()
            }
        }
    }

    override fun onReset() {
        lowPassSample = 0f
        energyHistory = 300f
        dynamicNoiseFloor = 100f
        lastBeatTimestamp = 0L
    }
}
