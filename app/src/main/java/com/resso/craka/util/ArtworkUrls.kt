package com.resso.craka.util

object ArtworkUrls {
    fun best(songId: String?, albumArtUrl: String?): String {
        return candidates(songId, albumArtUrl).firstOrNull().orEmpty()
    }

    fun candidates(songId: String?, albumArtUrl: String?): List<String> {
        val urls = linkedSetOf<String>()
        val norm = normalize(albumArtUrl)
        if (norm != null) {
            urls.add(norm)
            // JioSaavn image dimension fallbacks:
            // Often 500x500.jpg doesn't exist on CDN or returns 404, but 150x150.jpg is guaranteed
            if (norm.contains("500x500")) {
                urls.add(norm.replace("500x500", "150x150"))
                urls.add(norm.replace("500x500", "50x50"))
            } else if (norm.contains("150x150")) {
                urls.add(norm.replace("150x150", "500x500"))
                urls.add(norm.replace("150x150", "50x50"))
            }
            if (norm.startsWith("http://")) {
                urls.add(norm.replace("http://", "https://"))
            }
        }
        saavnFallbacks(songId).forEach { urls.add(it) }
        youtubeFallbacks(songId).forEach { urls.add(it) }
        return urls.toList()
    }

    private fun saavnFallbacks(songId: String?): List<String> {
        return when (songId) {
            "saavn_payal" -> listOf(
                "https://c.saavncdn.com/173/GLORY-Hindi-2024-20240826144815-500x500.jpg",
                "https://c.saavncdn.com/173/GLORY-Hindi-2024-20240826144815-150x150.jpg"
            )
            "saavn_tauba_tauba" -> listOf(
                "https://c.saavncdn.com/992/Bad-Newz-Hindi-2024-20240709080001-500x500.jpg",
                "https://c.saavncdn.com/992/Bad-Newz-Hindi-2024-20240709080001-150x150.jpg"
            )
            "saavn_aaj_ki_raat" -> listOf(
                "https://c.saavncdn.com/373/Stree-2-Hindi-2024-20240827150130-500x500.jpg",
                "https://c.saavncdn.com/373/Stree-2-Hindi-2024-20240827150130-150x150.jpg"
            )
            "saavn_kesariya" -> listOf(
                "https://c.saavncdn.com/871/Brahmastra-Hindi-2022-20220717092820-500x500.jpg",
                "https://c.saavncdn.com/871/Brahmastra-Hindi-2022-20220717092820-150x150.jpg"
            )
            "saavn_millionaire" -> listOf(
                "https://c.saavncdn.com/173/GLORY-Hindi-2024-20240826144815-500x500.jpg",
                "https://c.saavncdn.com/173/GLORY-Hindi-2024-20240826144815-150x150.jpg"
            )
            else -> emptyList()
        }
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
