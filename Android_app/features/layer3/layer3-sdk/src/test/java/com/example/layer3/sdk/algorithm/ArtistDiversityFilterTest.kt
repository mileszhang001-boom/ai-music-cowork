package com.example.layer3.sdk.algorithm

import com.example.layer3.sdk.data.TrackData
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ArtistDiversityFilterTest {

    private lateinit var filter: ArtistDiversityFilter

    @Before
    fun setUp() {
        filter = ArtistDiversityFilter(maxTracksPerArtist = 2)
    }

    @Test
    fun testApplyDiversity_LimitsTracksPerArtist() {
        val tracks = listOf(
            TrackData(id = "1", title = "Song 1", artist = "Artist A"),
            TrackData(id = "2", title = "Song 2", artist = "Artist A"),
            TrackData(id = "3", title = "Song 3", artist = "Artist A"),
            TrackData(id = "4", title = "Song 4", artist = "Artist A"),
            TrackData(id = "5", title = "Song 5", artist = "Artist B"),
            TrackData(id = "6", title = "Song 6", artist = "Artist B"),
            TrackData(id = "7", title = "Song 7", artist = "Artist B")
        )

        val result = filter.applyDiversity(tracks)

        val artistCounts = result.groupingBy { it.artist }.eachCount()
        artistCounts.forEach { (_, count) ->
            assertTrue("Each artist should have at most 2 tracks", count <= 2)
        }
    }

    @Test
    fun testApplyDiversity_PreservesOrder() {
        val tracks = listOf(
            TrackData(id = "1", title = "Song 1", artist = "Artist A"),
            TrackData(id = "2", title = "Song 2", artist = "Artist B"),
            TrackData(id = "3", title = "Song 3", artist = "Artist A")
        )

        val result = filter.applyDiversity(tracks)

        assertEquals(3, result.size)
        assertEquals("1", result[0].id)
        assertEquals("2", result[1].id)
        assertEquals("3", result[2].id)
    }

    @Test
    fun testApplyDiversity_EmptyList_ReturnsEmptyList() {
        val result = filter.applyDiversity(emptyList())

        assertTrue("Empty input should return empty list", result.isEmpty())
    }

    @Test
    fun testApplyDiversity_SingleArtist_ReturnsLimitedTracks() {
        val tracks = listOf(
            TrackData(id = "1", title = "Song 1", artist = "Solo Artist"),
            TrackData(id = "2", title = "Song 2", artist = "Solo Artist"),
            TrackData(id = "3", title = "Song 3", artist = "Solo Artist"),
            TrackData(id = "4", title = "Song 4", artist = "Solo Artist"),
            TrackData(id = "5", title = "Song 5", artist = "Solo Artist")
        )

        val result = filter.applyDiversity(tracks)

        assertEquals(2, result.size)
        result.forEach { assertEquals("Solo Artist", it.artist) }
    }

    @Test
    fun testApplyDiversityWithScore_LimitsTracksPerArtist() {
        val scoredTracks = listOf(
            Pair(TrackData(id = "1", title = "Song 1", artist = "Artist A"), 0.9),
            Pair(TrackData(id = "2", title = "Song 2", artist = "Artist A"), 0.85),
            Pair(TrackData(id = "3", title = "Song 3", artist = "Artist A"), 0.8),
            Pair(TrackData(id = "4", title = "Song 4", artist = "Artist B"), 0.75),
            Pair(TrackData(id = "5", title = "Song 5", artist = "Artist B"), 0.7),
            Pair(TrackData(id = "6", title = "Song 6", artist = "Artist B"), 0.65)
        )

        val result = filter.applyDiversityWithScore(scoredTracks)

        val artistCounts = result.map { it.first }.groupingBy { it.artist }.eachCount()
        artistCounts.forEach { (_, count) ->
            assertTrue("Each artist should have at most 2 tracks", count <= 2)
        }
    }

    @Test
    fun testApplyDiversityWithScore_PreservesScores() {
        val scoredTracks = listOf(
            Pair(TrackData(id = "1", title = "Song 1", artist = "Artist A"), 0.9),
            Pair(TrackData(id = "2", title = "Song 2", artist = "Artist A"), 0.85)
        )

        val result = filter.applyDiversityWithScore(scoredTracks)

        assertEquals(2, result.size)
        assertEquals(0.9, result[0].second, 0.001)
        assertEquals(0.85, result[1].second, 0.001)
    }

    @Test
    fun testApplyDiversityWithScore_EmptyList_ReturnsEmptyList() {
        val result = filter.applyDiversityWithScore(emptyList())

        assertTrue("Empty input should return empty list", result.isEmpty())
    }

    @Test
    fun testApplyDiversity_MultipleArtists_AllowsUpToMaxPerArtist() {
        val tracks = listOf(
            TrackData(id = "1", title = "Song 1", artist = "Artist A"),
            TrackData(id = "2", title = "Song 2", artist = "Artist A"),
            TrackData(id = "3", title = "Song 3", artist = "Artist B"),
            TrackData(id = "4", title = "Song 4", artist = "Artist B"),
            TrackData(id = "5", title = "Song 5", artist = "Artist C"),
            TrackData(id = "6", title = "Song 6", artist = "Artist C")
        )

        val result = filter.applyDiversity(tracks)

        assertEquals(6, result.size)
    }

    @Test
    fun testApplyDiversity_DifferentMaxLimit() {
        val customFilter = ArtistDiversityFilter(maxTracksPerArtist = 3)
        
        val tracks = listOf(
            TrackData(id = "1", title = "Song 1", artist = "Artist A"),
            TrackData(id = "2", title = "Song 2", artist = "Artist A"),
            TrackData(id = "3", title = "Song 3", artist = "Artist A"),
            TrackData(id = "4", title = "Song 4", artist = "Artist A"),
            TrackData(id = "5", title = "Song 5", artist = "Artist A")
        )

        val result = customFilter.applyDiversity(tracks)

        assertEquals(3, result.size)
    }
}
