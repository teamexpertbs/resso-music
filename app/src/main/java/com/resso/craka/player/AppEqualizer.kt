package com.resso.craka.player

import android.media.audiofx.Equalizer
import android.util.Log

class AppEqualizer {
    private var equalizer: Equalizer? = null
    private var attachedSession: Int = Int.MIN_VALUE

    fun attach(sessionId: Int) {
        if (equalizer != null && sessionId == attachedSession) return
        release()
        try {
            equalizer = Equalizer(0, sessionId).apply { enabled = true }
            attachedSession = sessionId
        } catch (e: Exception) {
            Log.w(TAG, "Equalizer unavailable: ${e.message}")
            equalizer = null
            attachedSession = Int.MIN_VALUE
        }
    }

    fun apply(preset: String) {
        val eq = equalizer ?: return
        val bands = eq.numberOfBands.toInt()
        if (bands <= 0) return
        val range = eq.bandLevelRange
        val min = range[0].toInt()
        val max = range[1].toInt()
        for (band in 0 until bands) {
            val level = when (preset) {
                "Bass" -> if (band <= bands / 3) max / 2 else min / 5
                "Vocal" -> if (band in (bands / 3) until (bands * 2 / 3)) max / 2 else 0
                "Treble" -> if (band >= bands * 2 / 3) max / 2 else min / 5
                else -> 0
            }
            eq.setBandLevel(band.toShort(), level.coerceIn(min, max).toShort())
        }
        eq.enabled = preset != "Off"
    }

    fun release() {
        try {
            equalizer?.release()
        } catch (_: Exception) {
        }
        equalizer = null
        attachedSession = Int.MIN_VALUE
    }

    companion object {
        private const val TAG = "AppEqualizer"
        val presets = listOf("Off", "Normal", "Bass", "Vocal", "Treble")
    }
}
