package com.resso.craka.util

import com.resso.craka.data.model.SongEntity

/**
 * Intelligent Song Deduplication Utility.
 * Normalizes song titles, artists, and metadata to eliminate duplicate replays,
 * remakes, and redundant catalog variations (e.g., "(From 'Bad Newz')", "(From 'GLORY')",
 * "(Remix)", "(Official Video)", different JioSaavn IDs for the same track).
 */
object SongDeduplicator {

    /**
     * Normalizes a song title by removing album tags, film tags, OST markers,
     * bracketed text, parentheses, and noise terms.
     */
    fun normalizeTitle(title: String): String {
        if (title.isBlank()) return ""
        return title.lowercase()
            // Remove text in parentheses: (From "Bad Newz"), (feat. X), (Remix), etc.
            .replace(Regex("\\((.*?)\\)"), " ")
            // Remove text in brackets: [From Stree 2], [Official Audio], etc.
            .replace(Regex("\\[(.*?)\\]"), " ")
            // Remove text after hyphen if it indicates film/artist/mix
            .replace(Regex("-\\s*(from|film|remix|lofi|slowed|ost|soundtrack).*$", RegexOption.IGNORE_CASE), " ")
            // Remove common music descriptors
            .replace(
                Regex("(?i)\\b(official|audio|video|lyric|lyrics|full song|original|remix|lofi|lo-fi|slowed|reverb|version|film version|soundtrack|ost|deluxe|mix|hd|hq|320kbps)\\b"),
                " "
            )
            // Keep only alphanumeric characters
            .replace(Regex("[^a-z0-9]"), "")
            .trim()
    }

    /**
     * Normalizes artist string to primary artist keyword.
     */
    fun normalizeArtist(artist: String): String {
        if (artist.isBlank()) return ""
        val primary = artist.lowercase()
            .split(Regex("[,&/x+]|\\b(feat\\.?|ft\\.?|with)\\b"))
            .firstOrNull()?.trim() ?: artist.lowercase()
        return primary.replace(Regex("[^a-z0-9]"), "").trim()
    }

    /**
     * Generates a unique canonical fingerprint for a song.
     * Songs with identical fingerprints represent the exact same track,
     * even if they have different IDs or minor metadata differences across albums.
     */
    fun canonicalKey(song: SongEntity?): String {
        if (song == null) return ""
        val cleanT = normalizeTitle(song.title)
        val cleanA = normalizeArtist(song.artist)

        // For distinctive long titles (>= 5 chars like "taubatauba", "kesariya", "millionaire", "pasoori", "aajkiraat"),
        // the normalized title itself is sufficiently unique to catch cross-artist attribution differences.
        return if (cleanT.length >= 5) {
            cleanT
        } else {
            "$cleanT::$cleanA"
        }
    }

    /**
     * Checks if two songs are identical or duplicate variations of each other.
     */
    fun isSameOrDuplicate(s1: SongEntity?, s2: SongEntity?): Boolean {
        if (s1 == null || s2 == null) return false
        if (s1.id == s2.id) return true

        val key1 = canonicalKey(s1)
        val key2 = canonicalKey(s2)
        if (key1.isNotEmpty() && key1 == key2) return true

        val t1 = normalizeTitle(s1.title)
        val t2 = normalizeTitle(s2.title)
        if (t1.isNotEmpty() && t2.isNotEmpty()) {
            if (t1 == t2) return true
            // Contained title match for long titles
            if (t1.length >= 6 && t2.length >= 6 && (t1.contains(t2) || t2.contains(t1))) {
                val a1Tokens = s1.artist.lowercase().split(Regex("[^a-z0-9]+")).filter { it.length >= 3 }.toSet()
                val a2Tokens = s2.artist.lowercase().split(Regex("[^a-z0-9]+")).filter { it.length >= 3 }.toSet()
                if (a1Tokens.isEmpty() || a2Tokens.isEmpty() || a1Tokens.intersect(a2Tokens).isNotEmpty()) {
                    return true
                }
            }
        }

        return false
    }

    /**
     * Filters a list of songs, keeping only unique canonical tracks in order.
     */
    fun deduplicateList(songs: List<SongEntity>): List<SongEntity> {
        val seenKeys = mutableSetOf<String>()
        val seenIds = mutableSetOf<String>()
        val result = mutableListOf<SongEntity>()

        for (song in songs) {
            val key = canonicalKey(song)
            if (song.id !in seenIds && (key.isEmpty() || key !in seenKeys)) {
                seenIds.add(song.id)
                if (key.isNotEmpty()) seenKeys.add(key)
                result.add(song)
            }
        }
        return result
    }
}

/**
 * Extension helper to deduplicate a song collection.
 */
fun List<SongEntity>.deduplicate(): List<SongEntity> = SongDeduplicator.deduplicateList(this)
