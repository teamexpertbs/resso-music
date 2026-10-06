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

    @Test
    fun testSongDeduplicationPreventsReplayLoops() {
        val song1 = createSong("saavn_tauba_tauba", "Tauba Tauba")
        val song2 = createSong("saavn_998811", "Tauba Tauba (From \"Bad Newz\")")
        val song3 = createSong("saavn_payal", "Payal")
        val song4 = createSong("saavn_445566", "Payal (Glory)")
        val song5 = createSong("saavn_kesariya", "Kesariya")
        val song6 = createSong("saavn_112233", "Kesariya - Film Version")
        val song7 = createSong("saavn_millionaire", "Millionaire")
        val song8 = createSong("saavn_778899", "Millionaire (Glory)")

        // Verify duplicates detection
        org.junit.Assert.assertTrue(com.resso.craka.util.SongDeduplicator.isSameOrDuplicate(song1, song2))
        org.junit.Assert.assertTrue(com.resso.craka.util.SongDeduplicator.isSameOrDuplicate(song3, song4))
        org.junit.Assert.assertTrue(com.resso.craka.util.SongDeduplicator.isSameOrDuplicate(song5, song6))
        org.junit.Assert.assertTrue(com.resso.craka.util.SongDeduplicator.isSameOrDuplicate(song7, song8))

        // Verify that 8 songs with duplicates are correctly deduplicated into exactly 4 unique songs
        val duplicateList = listOf(song1, song2, song3, song4, song5, song6, song7, song8)
        val deduplicated = com.resso.craka.util.SongDeduplicator.deduplicateList(duplicateList)

        assertEquals(4, deduplicated.size)
        assertEquals("Tauba Tauba", deduplicated[0].title)
        assertEquals("Payal", deduplicated[1].title)
        assertEquals("Kesariya", deduplicated[2].title)
        assertEquals("Millionaire", deduplicated[3].title)
    }

    @Test
    fun testBeatDetectionKickTransient() {
        var beatCount = 0
        val processor = com.resso.craka.player.BeatDetectionAudioProcessor {
            beatCount++
        }
        processor.isEnabled = true

        val format = androidx.media3.common.audio.AudioProcessor.AudioFormat(
            44100,
            2,
            androidx.media3.common.C.ENCODING_PCM_16BIT
        )
        processor.configure(format)
        processor.flush()

        // 1. Initial baseline buffer (silence)
        val silence = java.nio.ByteBuffer.allocateDirect(1024 * 2 * 2).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until 1024) {
            silence.putShort(0)
            silence.putShort(0)
        }
        silence.flip()
        processor.queueInput(silence)

        // 2. High-energy 60Hz kick transient
        val kick = java.nio.ByteBuffer.allocateDirect(2048 * 2 * 2).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until 2048) {
            val sample = (kotlin.math.sin(2.0 * Math.PI * 60.0 * i / 44100.0) * 28000.0).toInt().toShort()
            kick.putShort(sample)
            kick.putShort(sample)
        }
        kick.flip()
        processor.queueInput(kick)

        org.junit.Assert.assertTrue("Beat detector should identify kick drum transient", beatCount >= 1)
    }
}
