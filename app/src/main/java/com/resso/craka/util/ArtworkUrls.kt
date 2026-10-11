package com.resso.craka.util

import kotlin.math.abs

object ArtworkUrls {
    private val CURATED_COVERS = listOf(
        "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=600&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1465847899084-d164df4dedc6?w=600&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=600&auto=format&fit=crop&q=80"
    )

    fun best(songId: String?, albumArtUrl: String?): String {
        return candidates(songId, albumArtUrl).firstOrNull().orEmpty()
    }

    fun candidates(songId: String?, albumArtUrl: String?): List<String> {
        val urls = linkedSetOf<String>()

        // 1. Normalized URL from metadata
        val norm = normalize(albumArtUrl)
        if (norm != null) {
            urls.add(norm)
        }

        // 2. YouTube thumbnail fallbacks
        youtubeFallbacks(songId).forEach { urls.add(it) }

        // 3. Guaranteed curated music artwork fallback for zero-cover edge cases
        val coverHash = abs((songId ?: albumArtUrl ?: "resso").hashCode())
        urls.add(CURATED_COVERS[coverHash % CURATED_COVERS.size])

        return urls.toList()
    }

    private fun youtubeFallbacks(songId: String?): List<String> {
        val id = songId.orEmpty()
        val videoId = when {
            id.startsWith("yt_") -> id.removePrefix("yt_")
            id.length in 10..15 -> id
            else -> return emptyList()
        }
        return listOf(
            "https://i.ytimg.com/vi/$videoId/maxresdefault.jpg",
            "https://i.ytimg.com/vi/$videoId/hqdefault.jpg",
            "https://i.ytimg.com/vi/$videoId/mqdefault.jpg"
        )
    }

    private fun normalize(raw: String?): String {
        val value = raw?.trim().orEmpty()
        if (value.isBlank()) return ""
        val https = when {
            value.startsWith("https://") -> value
            value.startsWith("http://") -> "https://" + value.removePrefix("http://")
            value.startsWith("//") -> "https:$value"
            else -> value
        }
        return https
    }
}
