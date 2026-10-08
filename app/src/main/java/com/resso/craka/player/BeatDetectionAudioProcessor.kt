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

class BeatDetectionAudioProcessor(
    private val onBeatDetected: () -> Unit
) : BaseAudioProcessor() {

    @Volatile
    var isEnabled: Boolean = false

    private var b0 = 0f; private var b1 = 0f; private var b2 = 0f
    private var a1 = 0f; private var a2 = 0f
    private var x1 = 0f; private var x2 = 0f
    private var y1 = 0f; private var y2 = 0f

    // Fixed ~20ms analysis window
    private var windowSize = 882
    private var windowCount = 0
    private var windowSum = 0.0

    // ~1 second of history (50 windows of 20ms)
    private val history = FloatArray(50)
    private var histIdx = 0
    private var histFilled = 0
    private var prevEnergy = 0f
    private var lastBeatTimestamp = -1000L

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        windowSize = (inputAudioFormat.sampleRate * 0.020f).toInt().coerceAtLeast(160)
        computeBiquad(inputAudioFormat.sampleRate.toFloat())
        return inputAudioFormat
    }

    private fun computeBiquad(sampleRate: Float) {
        val sr = sampleRate.coerceAtLeast(8000f)
        val cutoffHz = 120.0
        val q = 0.7071
        val omega = 2.0 * PI * cutoffHz / sr
        val alpha = sin(omega) / (2.0 * q)
        val cosO = cos(omega)
        val a0 = 1.0 + alpha
        b0 = (((1.0 - cosO) / 2.0) / a0).toFloat()
        b1 = ((1.0 - cosO) / a0).toFloat()
        b2 = b0
        a1 = ((-2.0 * cosO) / a0).toFloat()
        a2 = ((1.0 - alpha) / a0).toFloat()
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        val readOnly = inputBuffer.asReadOnlyBuffer().order(inputBuffer.order())
        val out = replaceOutputBuffer(remaining)
        out.put(inputBuffer)
        out.flip()

        if (!isEnabled) return

        val ch = inputAudioFormat.channelCount.coerceAtLeast(1)
        while (readOnly.remaining() >= 2 * ch) {
            var sum = 0
            for (c in 0 until ch) sum += readOnly.short
            val x0 = (sum / ch).toFloat()

            val y0 = b0 * x0 + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
            x2 = x1; x1 = x0
            y2 = y1; y1 = y0

            windowSum += (y0 * y0).toDouble()
            windowCount++

            if (windowCount >= windowSize) {
                val energy = sqrt(windowSum / windowCount).toFloat()
                windowSum = 0.0
                windowCount = 0
                analyzeWindow(energy)
            }
        }
    }

    private fun analyzeWindow(energy: Float) {
        var avg = 0f
        if (histFilled > 0) {
            for (i in 0 until histFilled) avg += history[i]
            avg /= histFilled
        }
        history[histIdx] = energy
        histIdx = (histIdx + 1) % history.size
        if (histFilled < history.size) histFilled++

        val rise = energy - prevEnergy
        prevEnergy = energy

        if (histFilled < 1) return           // warm-up: need at least 1 previous baseline window
        val effectiveAvg = avg.coerceAtLeast(30f)
        if (energy < 40f) return             // silence guard

        val isBeat = energy > effectiveAvg * 1.35f && rise > effectiveAvg * 0.15f
        val now = SystemClock.elapsedRealtime()
        if (isBeat && now - lastBeatTimestamp >= 180L) {
            lastBeatTimestamp = now
            onBeatDetected()
        }
    }

    override fun onReset() {
        x1 = 0f; x2 = 0f; y1 = 0f; y2 = 0f
        windowCount = 0
        windowSum = 0.0
        histIdx = 0
        histFilled = 0
        prevEnergy = 0f
        lastBeatTimestamp = -1000L
    }
}
