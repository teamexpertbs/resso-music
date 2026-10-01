package com.resso.craka.player

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.util.Log

class AppEqualizer {
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var attachedSession: Int = Int.MIN_VALUE

    fun attach(sessionId: Int = 0) {
        if (equalizer != null && sessionId == attachedSession) return
        release()
        try {
            equalizer = Equalizer(0, sessionId).apply { enabled = true }
        } catch (e: Exception) {
            Log.w(TAG, "Equalizer unavailable: ${e.message}")
        }
        try {
            bassBoost = BassBoost(0, sessionId).apply {
                if (strengthSupported) {
                    setStrength(600.toShort())
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "BassBoost unavailable: ${e.message}")
        }
        try {
            virtualizer = Virtualizer(0, sessionId).apply {
                if (strengthSupported) {
                    setStrength(1000.toShort())
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Virtualizer unavailable: ${e.message}")
        }
        attachedSession = sessionId
    }

    fun set8DAudio(enabled: Boolean) {
        try {
            if (virtualizer == null && enabled) {
                attach(0)
            }
            virtualizer?.enabled = enabled
            bassBoost?.enabled = enabled
            if (enabled) {
                try { virtualizer?.setStrength(1000.toShort()) } catch (_: Exception) {}
                try { bassBoost?.setStrength(700.toShort()) } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            Log.w(TAG, "8D Spatial Audio toggle error: ${e.message}")
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
                "8D Spatial" -> (max * 0.4f).toInt()
                else -> 0
            }
            try {
                eq.setBandLevel(band.toShort(), level.coerceIn(min, max).toShort())
            } catch (e: Exception) {
                Log.w(TAG, "Band $band skipped: ${e.message}")
            }
        }
        try {
            eq.enabled = preset != "Off"
        } catch (e: Exception) {
            Log.w(TAG, "Equalizer enable failed: ${e.message}")
        }
    }

    fun release() {
        try { equalizer?.release() } catch (_: Exception) {}
        try { bassBoost?.release() } catch (_: Exception) {}
        try { virtualizer?.release() } catch (_: Exception) {}
        equalizer = null
        bassBoost = null
        virtualizer = null
        attachedSession = Int.MIN_VALUE
    }

    companion object {
        private const val TAG = "AppEqualizer"
        val presets = listOf("Off", "Normal", "Bass", "Vocal", "Treble", "8D Spatial")
    }
}
