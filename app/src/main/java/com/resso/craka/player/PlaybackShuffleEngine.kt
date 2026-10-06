package com.resso.craka.player

import com.resso.craka.data.model.SongEntity
import com.resso.craka.util.SongDeduplicator
import com.resso.craka.util.deduplicate
import kotlin.random.Random

/**
 * Dedicated Shuffle Algorithm for music playback.
 * Implements Fisher-Yates permutation and dynamic index randomization
 * whenever a new song is fetched or when the user clicks 'next'.
 * Enforces canonical deduplication so songs never repeat or play duplicate variations.
 */
class PlaybackShuffleEngine {

    private val random = Random(System.currentTimeMillis())

    /**
     * Fisher-Yates shuffle algorithm that provides uniform O(N) random permutations.
     */
    fun <T> shuffleList(list: List<T>): List<T> {
        val result = list.toMutableList()
        for (i in result.size - 1 downTo 1) {
            val j = random.nextInt(i + 1)
            val temp = result[i]
            result[i] = result[j]
            result[j] = temp
        }
        return result
    }

    /**
     * Randomizes the queue and calculates a new random index
     * whenever new songs are fetched or added to the queue,
     * ensuring no duplicate tracks exist in the resulting queue.
     */
    fun onNewSongsFetched(
        currentQueue: List<SongEntity>,
        newSongs: List<SongEntity>,
        currentSongId: String?
    ): Pair<List<SongEntity>, Int> {
        val combined = (currentQueue + newSongs).deduplicate()
        if (combined.isEmpty()) return emptyList<SongEntity>() to 0

        val currentSong = combined.firstOrNull { it.id == currentSongId }
        val otherSongs = combined.filter {
            if (currentSong != null) !SongDeduplicator.isSameOrDuplicate(it, currentSong) else it.id != currentSongId
        }

        // Fisher-Yates shuffle the rest of the queue
        val shuffledOthers = shuffleList(otherSongs)

        val newQueue = if (currentSong != null) {
            listOf(currentSong) + shuffledOthers
        } else {
            shuffledOthers
        }

        val targetIndex = if (currentSong != null) {
            0
        } else if (newQueue.isNotEmpty()) {
            random.nextInt(newQueue.size)
        } else {
            0
        }

        return newQueue to targetIndex
    }

    /**
     * Computes the next randomized index when the user clicks 'next',
     * prioritizing unplayed songs and strictly filtering out any duplicate variations of played tracks.
     */
    fun selectNextRandomIndex(
        queue: List<SongEntity>,
        currentSongId: String?,
        playedHistoryIds: Set<String>,
        playedHistoryKeys: Set<String> = emptySet()
    ): Pair<SongEntity?, Int> {
        if (queue.isEmpty()) return null to -1

        val currentSong = queue.firstOrNull { it.id == currentSongId }

        // Filter unplayed candidates: exclude current song and any already played IDs or canonical keys
        val unplayedCandidates = queue.filter { song ->
            !SongDeduplicator.isSameOrDuplicate(song, currentSong) &&
                song.id !in playedHistoryIds &&
                SongDeduplicator.canonicalKey(song) !in playedHistoryKeys
        }

        val selectedSong = if (unplayedCandidates.isNotEmpty()) {
            val randomIndex = random.nextInt(unplayedCandidates.size)
            unplayedCandidates[randomIndex]
        } else {
            val remaining = queue.filter { !SongDeduplicator.isSameOrDuplicate(it, currentSong) }
            if (remaining.isNotEmpty()) {
                val randomIndex = random.nextInt(remaining.size)
                remaining[randomIndex]
            } else {
                queue.firstOrNull()
            }
        }

        if (selectedSong == null) return null to -1

        val finalIndex = queue.indexOfFirst { it.id == selectedSong.id }.coerceAtLeast(0)
        return selectedSong to finalIndex
    }
}

