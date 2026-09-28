package com.resso.craka.util

object ArtworkUrls {
    fun best(songId: String?, albumArtUrl: String?): String {
        return candidates(songId, albumArtUrl).firstOrNull().orEmpty()
    }

    fun candidates(songId: String?, albumArtUrl: String?): List<String> {
        val urls = linkedSetOf<String>()
        normalize(albumArtUrl)?.let { urls.add(it) }
        youtubeFallbacks(songId).forEach { urls.add(it) }
        return urls.toList()
    }

    private fun youtubeFallbacks(songId: String?): List<String> {
        val id = songId.orEmpty()
        if (!id.startsWith("yt_") || id.length <= 3) return emptyList()
        val videoId = id.removePrefix("yt_")
        return listOf(
            "https://i.ytimg.com/vi/$videoId/hqdefault.jpg",
            "https://i.ytimg.com/vi/$videoId/mqdefault.jpg"
        )
    }

    private fun normalize(raw: String?): String? {
        val value = raw?.trim().orEmpty()
        if (value.isBlank()) return null
        val https = when {
            value.startsWith("https://") -> value
            value.startsWith("http://") -> "https://" + value.removePrefix("http://")
            value.startsWith("//") -> "https:$value"
            else -> return null
        }
        return https
    }
}
