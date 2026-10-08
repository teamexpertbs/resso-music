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

        // 1. Direct songId known verified high-res art
        saavnFallbacks(songId).forEach { urls.add(it) }

        // 2. Normalized URL from metadata
        val norm = normalize(albumArtUrl)
        if (norm != null) {
            // Check known expired Saavn timestamps and repair them
            val repaired = repairOutdatedSaavnUrl(norm)
            if (repaired != norm) {
                urls.add(repaired)
            }
            urls.add(norm)

            // JioSaavn image dimension fallbacks:
            // 500x500 is preferred for crisp UI, but 150x150 is guaranteed to exist on CDN
            if (norm.contains("500x500")) {
                urls.add(norm.replace("500x500", "150x150"))
                urls.add(norm.replace("500x500", "50x50"))
            } else if (norm.contains("150x150")) {
                urls.add(norm.replace("150x150", "500x500"))
                urls.add(norm.replace("150x150", "50x50"))
            }
        }

        // 3. YouTube thumbnail fallbacks
        youtubeFallbacks(songId).forEach { urls.add(it) }

        // 4. Guaranteed curated music artwork fallback for zero-cover edge cases
        val coverHash = abs((songId ?: albumArtUrl ?: "resso").hashCode())
        urls.add(CURATED_COVERS[coverHash % CURATED_COVERS.size])

        return urls.toList()
    }

    private fun repairOutdatedSaavnUrl(url: String): String {
        return url
            .replace("20240826144815", "20250117161048")
            .replace("20240709080001", "20250730113701")
            .replace("20240827150130", "20240828083834")
            .replace(
                "Brahmastra-Hindi-2022-20220717092820",
                "Brahmastra-Original-Motion-Picture-Soundtrack-Hindi-2022-20221006155213"
            )
    }

    private fun saavnFallbacks(songId: String?): List<String> {
        return when (songId) {
            "saavn_payal" -> listOf(
                "https://c.saavncdn.com/173/GLORY-Hindi-2024-20250117161048-500x500.jpg",
                "https://c.saavncdn.com/173/GLORY-Hindi-2024-20250117161048-150x150.jpg"
            )
            "saavn_tauba_tauba" -> listOf(
                "https://c.saavncdn.com/992/Bad-Newz-Hindi-2024-20250730113701-500x500.jpg",
                "https://c.saavncdn.com/992/Bad-Newz-Hindi-2024-20250730113701-150x150.jpg"
            )
            "saavn_aaj_ki_raat" -> listOf(
                "https://c.saavncdn.com/373/Stree-2-Hindi-2024-20240828083834-500x500.jpg",
                "https://c.saavncdn.com/373/Stree-2-Hindi-2024-20240828083834-150x150.jpg"
            )
            "saavn_kesariya" -> listOf(
                "https://c.saavncdn.com/871/Brahmastra-Original-Motion-Picture-Soundtrack-Hindi-2022-20221006155213-500x500.jpg",
                "https://c.saavncdn.com/871/Brahmastra-Original-Motion-Picture-Soundtrack-Hindi-2022-20221006155213-150x150.jpg"
            )
            "saavn_millionaire" -> listOf(
                "https://c.saavncdn.com/173/GLORY-Hindi-2024-20250117161048-500x500.jpg",
                "https://c.saavncdn.com/173/GLORY-Hindi-2024-20250117161048-150x150.jpg"
            )
            else -> emptyList()
        }
    }

    private fun youtubeFallbacks(songId: String?): List<String> {
        val id = songId.orEmpty()
        val videoId = when {
            id.startsWith("yt_") -> id.removePrefix("yt_")
            id.length in 10..15 && !id.startsWith("saavn") -> id
            else -> return emptyList()
        }
        return listOf(
            "https://i.ytimg.com/vi/$videoId/maxresdefault.jpg",
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
