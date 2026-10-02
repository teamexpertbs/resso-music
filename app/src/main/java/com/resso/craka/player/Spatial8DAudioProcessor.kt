package com.resso.craka.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
import kotlin.math.cos

class Spatial8DAudioProcessor : BaseAudioProcessor() {
    @Volatile
    var is8DEnabled: Boolean = false
    private var phase: Double = 0.0

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT || inputAudioFormat.channelCount != 2) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        if (!is8DEnabled) {
            val outputBuffer = replaceOutputBuffer(remaining)
            outputBuffer.put(inputBuffer)
            outputBuffer.flip()
            return
        }

        val outputBuffer = replaceOutputBuffer(remaining)
        val sampleRate = inputAudioFormat.sampleRate.toDouble().coerceAtLeast(44100.0)
        // 1 full 360 degree rotation every 8.5 seconds
        val phaseIncrement = (2.0 * Math.PI) / (sampleRate * 8.5)

        while (inputBuffer.remaining() >= 4) {
            val left = inputBuffer.short
            val right = inputBuffer.short

            // Smooth sinusoidal panning: left and right modulate smoothly
            val panLeft = (cos(phase) * 0.45 + 0.55).toFloat()
            val panRight = (-cos(phase) * 0.45 + 0.55).toFloat()

            val outLeft = (left * panLeft).toInt().coerceIn(-32768, 32767).toShort()
            val outRight = (right * panRight).toInt().coerceIn(-32768, 32767).toShort()

            outputBuffer.putShort(outLeft)
            outputBuffer.putShort(outRight)

            phase += phaseIncrement
            if (phase > 2.0 * Math.PI) {
                phase -= 2.0 * Math.PI
            }
        }
        outputBuffer.flip()
    }
}
