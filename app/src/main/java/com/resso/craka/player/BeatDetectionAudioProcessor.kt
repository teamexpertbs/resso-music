package com.resso.craka.player

import android.os.SystemClock
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Ultra-Accurate Beat & Transient Detection Audio Processor.
 * Filters raw PCM audio through a 2nd-order Biquad Butterworth Low-Pass Filter (cutoff 115Hz),
 * isolating physical kick drums and sub-bass transients from vocals and mid frequencies.
 * Applies differential onset detection (attack transient rise-rate) with adaptive dynamic thresholding.
 */
class BeatDetectionAudioProcessor(
    private val onBeatDetected: () -> Unit
) : BaseAudioProcessor() {

    @Volatile
    var isEnabled: Boolean = false

    // 2nd-order Biquad Filter coefficients
    private var b0 = 0.0f
    private var b1 = 0.0f
    private var b2 = 0.0f
    private var a1 = 0.0f
    private var a2 = 0.0f

    // Filter memory registers
    private var x1 = 0.0f
    private var x2 = 0.0f
    private var y1 = 0.0f
    private var y2 = 0.0f

    // Dynamic energy tracking & onset detector
    private var energyHistory = 200.0f
    private var dynamicNoiseFloor = 60.0f
    private var previousRmsEnergy = 0.0f
    private var lastBeatTimestamp = -1000L

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        computeBiquadCoefficients(inputAudioFormat.sampleRate.toFloat())
        return inputAudioFormat
    }

    private fun computeBiquadCoefficients(sampleRate: Float) {
        val sr = sampleRate.coerceAtLeast(8000f)
        val cutoffHz = 115.0f // Pure kick drum & 808 sub-bass range (below vocal frequencies)
        val q = 0.7071f // Butterworth Q for optimal transient response without ringing

        val omega = (2.0 * PI * cutoffHz / sr).toFloat()
        val alpha = (sin(omega.toDouble()) / (2.0 * q)).toFloat()
        val cosOmega = cos(omega.toDouble()).toFloat()

        val a0 = 1.0f + alpha
        b0 = ((1.0f - cosOmega) / 2.0f) / a0
        b1 = (1.0f - cosOmega) / a0
        b2 = ((1.0f - cosOmega) / 2.0f) / a0
        a1 = (-2.0f * cosOmega) / a0
        a2 = (1.0f - alpha) / a0
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        // Forward raw audio intact to downstream audio sinks (ExoPlayer AudioTrack)
        val outputBuffer = replaceOutputBuffer(remaining)
        val readOnly = inputBuffer.asReadOnlyBuffer().order(inputBuffer.order())
        outputBuffer.put(inputBuffer)
        outputBuffer.flip()

        if (!isEnabled) return

        val channelCount = inputAudioFormat.channelCount.coerceAtLeast(1)
        var bassEnergySum = 0.0
        var sampleCount = 0

        while (readOnly.remaining() >= 2 * channelCount) {
            var sumSample = 0
            for (c in 0 until channelCount) {
                sumSample += readOnly.short
            }
            val x0 = (sumSample / channelCount).toFloat()

            // Apply 2nd-order Biquad Butterworth difference equation:
            // y[n] = b0*x[n] + b1*x[n-1] + b2*x[n-2] - a1*y[n-1] - a2*y[n-2]
            val y0 = b0 * x0 + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2

            // Shift filter registers
            x2 = x1
            x1 = x0
            y2 = y1
            y1 = y0

            bassEnergySum += (y0 * y0)
            sampleCount++
        }

        if (sampleCount > 0) {
            val currentRmsEnergy = sqrt(bassEnergySum / sampleCount).toFloat()
            val now = SystemClock.elapsedRealtime()

            // Adaptive moving baseline for track dynamic range
            energyHistory = energyHistory * 0.88f + currentRmsEnergy * 0.12f
            dynamicNoiseFloor = dynamicNoiseFloor * 0.96f + (currentRmsEnergy * 0.30f).coerceAtLeast(50f) * 0.04f

            // Differential onset: detect the rapid attack slope of the kick drum hit
            val energyRise = currentRmsEnergy - previousRmsEnergy
            previousRmsEnergy = currentRmsEnergy

            // Beat Threshold calculation:
            // 1. Must exceed dynamic noise floor
            // 2. Must exceed current running average by at least 1.25x
            // 3. Must exhibit a positive energy attack spike (transient onset)
            val minFloor = (energyHistory * 0.40f).coerceAtLeast(dynamicNoiseFloor).coerceAtLeast(75f)
            val beatThreshold = (energyHistory * 1.25f).coerceAtLeast(minFloor * 1.30f)
            val hasOnsetTransient = energyRise > (beatThreshold * 0.18f).coerceAtLeast(18f)

            val isBeat = (currentRmsEnergy > beatThreshold && hasOnsetTransient) ||
                (currentRmsEnergy > beatThreshold * 1.45f) // Strong accented bass drop

            // Refractory period: 215ms (supports up to 279 BPM without duplicate false triggers on decay tails)
            if (isBeat && (now - lastBeatTimestamp) >= 215L) {
                lastBeatTimestamp = now
                onBeatDetected()
            }
        }
    }

    override fun onReset() {
        x1 = 0.0f
        x2 = 0.0f
        y1 = 0.0f
        y2 = 0.0f
        energyHistory = 200.0f
        dynamicNoiseFloor = 60.0f
        previousRmsEnergy = 0.0f
        lastBeatTimestamp = -1000L
    }
}
