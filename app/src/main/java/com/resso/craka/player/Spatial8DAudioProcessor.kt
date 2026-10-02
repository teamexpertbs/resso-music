package com.resso.craka.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
import kotlin.math.cos
import kotlin.math.sin

class Spatial8DAudioProcessor : BaseAudioProcessor() {
    @Volatile
    var is8DEnabled: Boolean = false
    private var phase: Double = 0.0

    // ITD (Interaural Time Difference) circular delay buffers (32 samples capacity, max delay ~16 samples)
    private val delayRingLeft = ShortArray(32)
    private val delayRingRight = ShortArray(32)
    private var ringIndex: Int = 0

    // Pinna rear shadow low-pass state
    private var rearLpLeft: Float = 0f
    private var rearLpRight: Float = 0f

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        if (inputAudioFormat.channelCount != 1 && inputAudioFormat.channelCount != 2) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        // Always produce stereo output (2 channels) so mono & stereo can orbit in 3D binaural space
        return AudioProcessor.AudioFormat(inputAudioFormat.sampleRate, 2, C.ENCODING_PCM_16BIT)
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        val inputChannels = inputAudioFormat.channelCount
        val sampleRate = inputAudioFormat.sampleRate.toDouble().coerceAtLeast(8000.0)

        // 360-degree orbit with constant angular velocity (9.5 seconds per revolution)
        val phaseIncrement = (2.0 * Math.PI) / (sampleRate * 9.5)

        // ITD max delay ~0.35ms (corresponds to human ear distance)
        val maxDelaySamples = (0.00035 * sampleRate).toInt().coerceIn(1, 15)

        // Pinna rear low-pass filter coefficient (~4.5 kHz cutoff for behind-the-head acoustic shadow)
        val rearAlpha = (1.0f / (1.0f + (sampleRate.toFloat() / (2.0f * Math.PI.toFloat() * 4500f)))).coerceIn(0.1f, 0.5f)

        val outputBytes = if (inputChannels == 1) remaining * 2 else remaining
        val outputBuffer = replaceOutputBuffer(outputBytes)

        if (!is8DEnabled) {
            if (inputChannels == 1) {
                while (inputBuffer.remaining() >= 2) {
                    val sample = inputBuffer.short
                    outputBuffer.putShort(sample)
                    outputBuffer.putShort(sample)
                }
            } else {
                outputBuffer.put(inputBuffer)
            }
            outputBuffer.flip()
            return
        }

        while (inputBuffer.remaining() >= inputChannels * 2) {
            val leftIn: Short
            val rightIn: Short
            if (inputChannels == 1) {
                val s = inputBuffer.short
                leftIn = s
                rightIn = s
            } else {
                leftIn = inputBuffer.short
                rightIn = inputBuffer.short
            }

            // Blend into core mono signal + retain subtle stereo ambient width
            val mono = (leftIn.toFloat() + rightIn.toFloat()) * 0.5f
            val stereoDiff = (leftIn.toFloat() - rightIn.toFloat()) * 0.25f

            // Orbit coordinates:
            // x: Left (-1.0) to Right (+1.0)
            // y: Behind (-1.0) to In-Front (+1.0)
            val x = cos(phase).toFloat()
            val y = sin(phase).toFloat()

            // 1. Equal-power binaural level panning (ILD)
            val panLeft = 0.58f - 0.38f * x
            val panRight = 0.58f + 0.38f * x

            var procLeft = (mono * panLeft + stereoDiff).toInt().coerceIn(-32768, 32767).toShort()
            var procRight = (mono * panRight - stereoDiff).toInt().coerceIn(-32768, 32767).toShort()

            // 2. Rear Pinna Acoustic Shadow (low-pass filtering when sound is behind the head)
            rearLpLeft += rearAlpha * (procLeft.toFloat() - rearLpLeft)
            rearLpRight += rearAlpha * (procRight.toFloat() - rearLpRight)
            if (y < 0f) {
                val rearBlend = (-y).coerceIn(0f, 1f) * 0.45f
                val shadowedL = procLeft * (1f - rearBlend) + rearLpLeft * rearBlend
                val shadowedR = procRight * (1f - rearBlend) + rearLpRight * rearBlend
                procLeft = shadowedL.toInt().coerceIn(-32768, 32767).toShort()
                procRight = shadowedR.toInt().coerceIn(-32768, 32767).toShort()
            }

            // 3. Interaural Time Difference (ITD ~0.35ms head delay)
            delayRingLeft[ringIndex] = procLeft
            delayRingRight[ringIndex] = procRight

            val outLeft: Short
            val outRight: Short

            if (x > 0f) {
                // Sound on right side -> Left ear hears delayed arrival
                val delaySamples = (maxDelaySamples * x).toInt().coerceIn(0, 15)
                val readIndex = (ringIndex - delaySamples + 32) and 31
                outLeft = delayRingLeft[readIndex]
                outRight = procRight
            } else {
                // Sound on left side -> Right ear hears delayed arrival
                val delaySamples = (maxDelaySamples * (-x)).toInt().coerceIn(0, 15)
                val readIndex = (ringIndex - delaySamples + 32) and 31
                outLeft = procLeft
                outRight = delayRingRight[readIndex]
            }

            ringIndex = (ringIndex + 1) and 31

            outputBuffer.putShort(outLeft)
            outputBuffer.putShort(outRight)

            phase += phaseIncrement
            if (phase > 2.0 * Math.PI) phase -= 2.0 * Math.PI
        }

        outputBuffer.flip()
    }

    override fun onReset() {
        phase = 0.0
        ringIndex = 0
        rearLpLeft = 0f
        rearLpRight = 0f
        delayRingLeft.fill(0)
        delayRingRight.fill(0)
    }
}
