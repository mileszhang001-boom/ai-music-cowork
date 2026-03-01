package com.example.layer3.sdk.algorithm

import com.example.layer3.api.model.*
import com.example.layer3.sdk.data.TrackData
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MusicScorerTest {

    private lateinit var musicScorer: MusicScorer

    @Before
    fun setUp() {
        musicScorer = MusicScorer()
    }

    @Test
    fun testCalculateScore_PerfectMatch_ReturnsHighScore() {
        val scene = SceneDescriptor(
            scene_id = "perfect_match",
            scene_name = "Perfect Match Scene",
            intent = Intent(
                mood = Mood(valence = 0.8, arousal = 0.7),
                energy_level = 0.8
            ),
            hints = Hints(
                music = MusicHints(
                    genres = listOf("pop"),
                    artists = listOf("Test Artist"),
                    tempo = "fast"
                )
            )
        )

        val tracks = listOf(
            TrackData(
                id = "track_1",
                title = "Perfect Song",
                artist = "Test Artist",
                genre = "pop",
                energy = 0.8,
                valence = 0.8,
                bpm = 140,
                popularity = 100
            )
        )

        val result = musicScorer.scoreTracks(tracks, scene)

        assertEquals(1, result.size)
        val (_, score) = result.first()
        assertTrue("Score should be high for perfect match", score > 0.7)
    }

    @Test
    fun testCalculateScore_NoMatch_ReturnsLowScore() {
        val scene = SceneDescriptor(
            scene_id = "no_match",
            scene_name = "No Match Scene",
            intent = Intent(
                mood = Mood(valence = 0.9, arousal = 0.9),
                energy_level = 0.9
            ),
            hints = Hints(
                music = MusicHints(
                    genres = listOf("metal"),
                    artists = listOf("Metal Band"),
                    tempo = "fast"
                )
            )
        )

        val tracks = listOf(
            TrackData(
                id = "track_2",
                title = "Slow Ballad",
                artist = "Slow Artist",
                genre = "ballad",
                energy = 0.1,
                valence = 0.1,
                bpm = 60,
                popularity = 10
            )
        )

        val result = musicScorer.scoreTracks(tracks, scene)

        assertEquals(1, result.size)
        val (_, score) = result.first()
        assertTrue("Score should be low for no match", score < 0.5)
    }

    @Test
    fun testCalculateScore_PartialMatch_ReturnsMediumScore() {
        val scene = SceneDescriptor(
            scene_id = "partial_match",
            scene_name = "Partial Match Scene",
            intent = Intent(
                mood = Mood(valence = 0.7, arousal = 0.6),
                energy_level = 0.7
            ),
            hints = Hints(
                music = MusicHints(
                    genres = listOf("rock"),
                    tempo = "medium"
                )
            )
        )

        val tracks = listOf(
            TrackData(
                id = "track_3",
                title = "Partial Match Song",
                artist = "Some Artist",
                genre = "rock",
                energy = 0.6,
                valence = 0.5,
                bpm = 110,
                popularity = 50
            )
        )

        val result = musicScorer.scoreTracks(tracks, scene)

        assertEquals(1, result.size)
        val (_, score) = result.first()
        assertTrue("Score should be medium for partial match", score in 0.4..0.7)
    }

    @Test
    fun testScoreTracks_ReturnsSortedByScore() {
        val scene = SceneDescriptor(
            scene_id = "sorting_test",
            scene_name = "Sorting Test",
            intent = Intent(
                mood = Mood(valence = 0.8, arousal = 0.8),
                energy_level = 0.8
            )
        )

        val tracks = listOf(
            TrackData(id = "low", title = "Low Score", artist = "A", energy = 0.2, valence = 0.2),
            TrackData(id = "high", title = "High Score", artist = "B", energy = 0.9, valence = 0.9),
            TrackData(id = "medium", title = "Medium Score", artist = "C", energy = 0.5, valence = 0.5)
        )

        val result = musicScorer.scoreTracks(tracks, scene)

        assertEquals(3, result.size)
        for (i in 0 until result.size - 1) {
            assertTrue("Tracks should be sorted by score descending",
                result[i].second >= result[i + 1].second)
        }
    }

    @Test
    fun testScoreTracks_WithGenreHint_PrefersMatchingGenre() {
        val scene = SceneDescriptor(
            scene_id = "genre_test",
            scene_name = "Genre Test",
            intent = Intent(
                mood = Mood(valence = 0.5, arousal = 0.5),
                energy_level = 0.5
            ),
            hints = Hints(
                music = MusicHints(
                    genres = listOf("jazz")
                )
            )
        )

        val tracks = listOf(
            TrackData(id = "jazz_track", title = "Jazz", artist = "A", genre = "jazz", energy = 0.5, valence = 0.5),
            TrackData(id = "rock_track", title = "Rock", artist = "B", genre = "rock", energy = 0.5, valence = 0.5)
        )

        val result = musicScorer.scoreTracks(tracks, scene)

        val jazzScore = result.find { it.first.id == "jazz_track" }?.second ?: 0.0
        val rockScore = result.find { it.first.id == "rock_track" }?.second ?: 0.0
        
        assertTrue("Jazz track should score higher", jazzScore > rockScore)
    }

    @Test
    fun testScoreTracks_WithArtistHint_PrefersMatchingArtist() {
        val scene = SceneDescriptor(
            scene_id = "artist_test",
            scene_name = "Artist Test",
            intent = Intent(
                mood = Mood(valence = 0.5, arousal = 0.5),
                energy_level = 0.5
            ),
            hints = Hints(
                music = MusicHints(
                    artists = listOf("Favorite Artist")
                )
            )
        )

        val tracks = listOf(
            TrackData(id = "fav_track", title = "Song", artist = "Favorite Artist", energy = 0.5, valence = 0.5),
            TrackData(id = "other_track", title = "Other Song", artist = "Other Artist", energy = 0.5, valence = 0.5)
        )

        val result = musicScorer.scoreTracks(tracks, scene)

        val favScore = result.find { it.first.id == "fav_track" }?.second ?: 0.0
        val otherScore = result.find { it.first.id == "other_track" }?.second ?: 0.0
        
        assertTrue("Favorite artist track should score higher", favScore > otherScore)
    }

    @Test
    fun testScoreTracks_WithTempoHint_PrefersMatchingTempo() {
        val scene = SceneDescriptor(
            scene_id = "tempo_test",
            scene_name = "Tempo Test",
            intent = Intent(
                mood = Mood(valence = 0.5, arousal = 0.5),
                energy_level = 0.5
            ),
            hints = Hints(
                music = MusicHints(
                    tempo = "fast"
                )
            )
        )

        val tracks = listOf(
            TrackData(id = "fast_track", title = "Fast", artist = "A", bpm = 150, energy = 0.5, valence = 0.5),
            TrackData(id = "slow_track", title = "Slow", artist = "B", bpm = 70, energy = 0.5, valence = 0.5)
        )

        val result = musicScorer.scoreTracks(tracks, scene)

        val fastScore = result.find { it.first.id == "fast_track" }?.second ?: 0.0
        val slowScore = result.find { it.first.id == "slow_track" }?.second ?: 0.0
        
        assertTrue("Fast track should score higher for fast tempo hint", fastScore > slowScore)
    }

    @Test
    fun testScoreTracks_EmptyTracks_ReturnsEmptyList() {
        val scene = SceneDescriptor(
            scene_id = "empty_test",
            scene_name = "Empty Test",
            intent = Intent(
                mood = Mood(valence = 0.5, arousal = 0.5),
                energy_level = 0.5
            )
        )

        val result = musicScorer.scoreTracks(emptyList(), scene)

        assertTrue("Empty input should return empty list", result.isEmpty())
    }
}
