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
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        if (inputAudioFormat.channelCount != 1 && inputAudioFormat.channelCount != 2) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        // Always produce stereo output (2 channels) so mono tracks can be panned in 3D orbit
        return AudioProcessor.AudioFormat(inputAudioFormat.sampleRate, 2, C.ENCODING_PCM_16BIT)
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        val inputChannels = inputAudioFormat.channelCount
        val sampleRate = inputAudioFormat.sampleRate.toDouble().coerceAtLeast(8000.0)

        // Smooth 9.5-second 360-degree orbit (consistent across 22.05kHz, 32kHz, 44.1kHz, 48kHz, 96kHz)
        val phaseIncrement = (2.0 * Math.PI) / (sampleRate * 9.5)

        // Allocate buffer: mono (1 ch) doubles size to stereo (2 ch); stereo (2 ch) keeps same size
        val outputBytes = if (inputChannels == 1) remaining * 2 else remaining
        val outputBuffer = replaceOutputBuffer(outputBytes)

        if (!is8DEnabled) {
            if (inputChannels == 1) {
                // Expand mono to centered stereo
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

        // Balanced 360-degree binaural panning: ranges smoothly from 0.38 to 0.98
        // One side NEVER completely drops out (no uncomfortable 10% dead zone)
        if (inputChannels == 1) {
            while (inputBuffer.remaining() >= 2) {
                val sample = inputBuffer.short
                val panLeft = (cos(phase) * 0.30 + 0.68).toFloat()
                val panRight = (-cos(phase) * 0.30 + 0.68).toFloat()

                val outLeft = (sample * panLeft).toInt().coerceIn(-32768, 32767).toShort()
                val outRight = (sample * panRight).toInt().coerceIn(-32768, 32767).toShort()

                outputBuffer.putShort(outLeft)
                outputBuffer.putShort(outRight)

                phase += phaseIncrement
                if (phase > 2.0 * Math.PI) phase -= 2.0 * Math.PI
            }
        } else {
            while (inputBuffer.remaining() >= 4) {
                val left = inputBuffer.short
                val right = inputBuffer.short
                val panLeft = (cos(phase) * 0.30 + 0.68).toFloat()
                val panRight = (-cos(phase) * 0.30 + 0.68).toFloat()

                val outLeft = (left * panLeft).toInt().coerceIn(-32768, 32767).toShort()
                val outRight = (right * panRight).toInt().coerceIn(-32768, 32767).toShort()

                outputBuffer.putShort(outLeft)
                outputBuffer.putShort(outRight)

                phase += phaseIncrement
                if (phase > 2.0 * Math.PI) phase -= 2.0 * Math.PI
            }
        }
        outputBuffer.flip()
    }

    override fun onReset() {
        phase = 0.0
    }
}
