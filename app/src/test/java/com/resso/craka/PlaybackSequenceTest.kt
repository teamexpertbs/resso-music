package com.resso.craka

import com.resso.craka.data.model.SongEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.ArrayDeque

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PlaybackSequenceTest {

    private fun createSong(id: String, title: String) = SongEntity(
        id = id,
        title = title,
        artist = "Artist $id",
        album = "Album",
        audioUrl = "https://example.com/$id.mp4",
        albumArtUrl = "https://example.com/art.jpg",
        durationMs = 180000L
    )

    @Test
    fun testNextPreviousSynchronizationSequence() {
        val songA = createSong("song_a", "Love Dose")
        val songB = createSong("song_b", "Blue Eyes")
        val songC = createSong("song_c", "Desi Kalakaar")

        val queue = mutableListOf(songA, songB, songC)
        val backStack = ArrayDeque<SongEntity>()
        val forwardStack = ArrayDeque<SongEntity>()
        var currentSong: SongEntity = songA

        fun next() {
            val forward = if (forwardStack.isNotEmpty()) forwardStack.removeLast() else null
            if (forward != null) {
                backStack.addLast(currentSong)
                currentSong = forward
                return
            }
            val currIdx = queue.indexOfFirst { it.id == currentSong.id }
            val nextSong = if (currIdx >= 0 && currIdx + 1 < queue.size) {
                queue[currIdx + 1]
            } else {
                queue[0]
            }
            backStack.addLast(currentSong)
            currentSong = nextSong
        }

        fun prev() {
            val back = if (backStack.isNotEmpty()) backStack.removeLast() else null
            if (back != null) {
                forwardStack.addLast(currentSong)
                currentSong = back
                return
            }
            val currIdx = queue.indexOfFirst { it.id == currentSong.id }
            if (currIdx > 0) {
                forwardStack.addLast(currentSong)
                currentSong = queue[currIdx - 1]
            }
        }

        // Initial: Song A
        assertEquals("Love Dose", currentSong.title)

        // 1. Next -> Song B
        next()
        assertEquals("Blue Eyes", currentSong.title)

        // 2. Previous -> Song A
        prev()
        assertEquals("Love Dose", currentSong.title)

        // 3. Next -> Song B (MUST play Song B again!)
        next()
        assertEquals("Blue Eyes", currentSong.title)

        // 4. Previous -> Song A
        prev()
        assertEquals("Love Dose", currentSong.title)

        // 5. Next -> Song B
        next()
        assertEquals("Blue Eyes", currentSong.title)

        // 6. Next -> Song C
        next()
        assertEquals("Desi Kalakaar", currentSong.title)

        // 7. Previous -> Song B
        prev()
        assertEquals("Blue Eyes", currentSong.title)

        // 8. Previous -> Song A
        prev()
        assertEquals("Love Dose", currentSong.title)
    }

    @Test
    fun testRealMusicSearch() {
        val service = com.resso.craka.data.network.MusicSearchService()
        val results = kotlinx.coroutines.runBlocking {
            service.searchSongs("Love Dose", 10)
        }
        println("SEARCH RESULTS COUNT: ${results.size}")
        for (song in results) {
            println("SONG: ${song.title} - ${song.artist} -> ${song.audioUrl}")
        }
        org.junit.Assert.assertTrue("Should find Love Dose", results.isNotEmpty())
    }
}
