package com.resso.craka.util

import com.resso.craka.data.model.LyricLine

object LyricsParser {
    private val LRC_REGEX = Regex("""\[(\d{2}):(\d{2})(?:\.(\d{2,3}))?](.*)""")

    fun parse(lrcText: String): List<LyricLine> {
        val lines = mutableListOf<LyricLine>()
        lrcText.lines().forEach { line ->
            val match = LRC_REGEX.find(line.trim())
            if (match != null) {
                val min = match.groupValues[1].toLongOrNull() ?: 0L
                val sec = match.groupValues[2].toLongOrNull() ?: 0L
                val msPart = match.groupValues[3]
                val millis = when {
                    msPart.isEmpty() -> 0L
                    msPart.length == 2 -> msPart.toLong() * 10
                    else -> msPart.toLong()
                }
                val totalMs = (min * 60 + sec) * 1000 + millis
                val content = match.groupValues[4].trim()
                if (content.isNotEmpty()) {
                    lines.add(LyricLine(timeMs = totalMs, text = content))
                }
            } else {
                val trimmed = line.trim()
                if (trimmed.isNotEmpty() && !trimmed.startsWith("[")) {
                    lines.add(LyricLine(timeMs = (lines.size * 3500).toLong(), text = trimmed))
                }
            }
        }
        return lines.sortedBy { it.timeMs }
    }

    fun formatTime(ms: Long): String {
        val totalSec = ms / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        return "%02d:%02d".format(min, sec)
    }
}
