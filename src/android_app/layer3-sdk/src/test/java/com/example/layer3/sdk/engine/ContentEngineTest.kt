package com.example.layer3.sdk.engine

import android.content.Context
import com.example.layer3.api.ContentProviderConfig
import com.example.layer3.api.model.*
import com.example.layer3.sdk.algorithm.ArtistDiversityFilter
import com.example.layer3.sdk.algorithm.MusicScorer
import com.example.layer3.sdk.data.CacheManager
import com.example.layer3.sdk.data.MusicLibraryLoader
import com.example.layer3.sdk.data.TrackData
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ContentEngineTest {

    private lateinit var contentEngine: ContentEngine
    private lateinit var mockContext: Context
    private lateinit var mockMusicLoader: MusicLibraryLoader
    private lateinit var config: ContentProviderConfig

    @Before
    fun setUp() {
        mockContext = mockk(relaxed = true)
        mockMusicLoader = mockk(relaxed = true)
        config = ContentProviderConfig()
        
        contentEngine = ContentEngine(mockContext, config)
    }

    @After
    fun tearDown() {
        contentEngine.destroy()
    }

    @Test
    fun testGeneratePlaylist_HappyScene_ReturnsUpbeatSongs() = runTest {
        val happyScene = SceneDescriptor(
            scene_id = "happy_scene_1",
            scene_name = "Happy Morning",
            intent = Intent(
                mood = Mood(valence = 0.8, arousal = 0.7),
                energy_level = 0.8
            ),
            hints = Hints(
                music = MusicHints(
                    genres = listOf("pop", "dance"),
                    tempo = "fast"
                )
            )
        )

        val mockTracks = listOf(
            TrackData(
                id = "track_1",
                title = "Happy Song",
                artist = "Artist A",
                genre = "pop",
                energy = 0.8,
                valence = 0.9,
                bpm = 130
            ),
            TrackData(
                id = "track_2",
                title = "Upbeat Tune",
                artist = "Artist B",
                genre = "dance",
                energy = 0.85,
                valence = 0.75,
                bpm = 140
            )
        )

        mockkConstructor(MusicLibraryLoader::class)
        every { anyConstructed<MusicLibraryLoader>().getAllTracks() } returns mockTracks
        every { anyConstructed<MusicLibraryLoader>().toTrack(any()) } answers {
            val trackData = firstArg<TrackData>()
            Track(
                id = trackData.id,
                title = trackData.title,
                artist = trackData.artist,
                genre = trackData.genre,
                energy = trackData.energy,
                valence = trackData.valence,
                bpm = trackData.bpm
            )
        }

        val result = contentEngine.generatePlaylist(happyScene)

        assertTrue(result.isSuccess)
        val playlist = result.getOrThrow()
        assertNotNull(playlist)
        assertTrue(playlist.tracks.isNotEmpty())
        
        playlist.tracks.forEach { track ->
            assertTrue("Track should have high energy", (track.energy ?: 0.0) >= 0.5)
        }

        clearAllMocks()
    }

    @Test
    fun testGeneratePlaylist_SadScene_ReturnsMelancholicSongs() = runTest {
        val sadScene = SceneDescriptor(
            scene_id = "sad_scene_1",
            scene_name = "Rainy Day",
            intent = Intent(
                mood = Mood(valence = 0.2, arousal = 0.3),
                energy_level = 0.3
            ),
            hints = Hints(
                music = MusicHints(
                    genres = listOf("ballad", "classical"),
                    tempo = "slow"
                )
            )
        )

        val mockTracks = listOf(
            TrackData(
                id = "track_3",
                title = "Melancholy",
                artist = "Artist C",
                genre = "ballad",
                energy = 0.2,
                valence = 0.15,
                bpm = 70
            ),
            TrackData(
                id = "track_4",
                title = "Sad Piano",
                artist = "Artist D",
                genre = "classical",
                energy = 0.25,
                valence = 0.2,
                bpm = 65
            )
        )

        mockkConstructor(MusicLibraryLoader::class)
        every { anyConstructed<MusicLibraryLoader>().getAllTracks() } returns mockTracks
        every { anyConstructed<MusicLibraryLoader>().toTrack(any()) } answers {
            val trackData = firstArg<TrackData>()
            Track(
                id = trackData.id,
                title = trackData.title,
                artist = trackData.artist,
                genre = trackData.genre,
                energy = trackData.energy,
                valence = trackData.valence,
                bpm = trackData.bpm
            )
        }

        val result = contentEngine.generatePlaylist(sadScene)

        assertTrue(result.isSuccess)
        val playlist = result.getOrThrow()
        assertNotNull(playlist)
        assertTrue(playlist.tracks.isNotEmpty())

        playlist.tracks.forEach { track ->
            assertTrue("Track should have low valence", (track.valence ?: 0.5) < 0.6)
        }

        clearAllMocks()
    }

    @Test
    fun testGeneratePlaylist_CacheHit_ReturnsCachedPlaylist() = runTest {
        val scene = SceneDescriptor(
            scene_id = "cached_scene",
            scene_name = "Cached Scene",
            intent = Intent(
                mood = Mood(valence = 0.5, arousal = 0.5),
                energy_level = 0.5
            )
        )

        val mockTracks = listOf(
            TrackData(
                id = "track_cached",
                title = "Cached Track",
                artist = "Cached Artist",
                energy = 0.5,
                valence = 0.5
            )
        )

        mockkConstructor(MusicLibraryLoader::class)
        every { anyConstructed<MusicLibraryLoader>().getAllTracks() } returns mockTracks
        every { anyConstructed<MusicLibraryLoader>().toTrack(any()) } answers {
            val trackData = firstArg<TrackData>()
            Track(
                id = trackData.id,
                title = trackData.title,
                artist = trackData.artist,
                energy = trackData.energy,
                valence = trackData.valence
            )
        }

        val firstResult = contentEngine.generatePlaylist(scene)
        assertTrue(firstResult.isSuccess)

        val secondResult = contentEngine.generatePlaylist(scene)
        assertTrue(secondResult.isSuccess)

        val firstPlaylist = firstResult.getOrThrow()
        val secondPlaylist = secondResult.getOrThrow()
        
        assertEquals(firstPlaylist.id, secondPlaylist.id)
        assertEquals(firstPlaylist.tracks.size, secondPlaylist.tracks.size)

        clearAllMocks()
    }

    @Test
    fun testGeneratePlaylist_ArtistDiversity_LimitsPerArtist() = runTest {
        val scene = SceneDescriptor(
            scene_id = "diversity_test",
            scene_name = "Diversity Test",
            intent = Intent(
                mood = Mood(valence = 0.5, arousal = 0.5),
                energy_level = 0.5
            )
        )

        val mockTracks = listOf(
            TrackData(id = "t1", title = "Song 1", artist = "Artist X", energy = 0.5, valence = 0.5),
            TrackData(id = "t2", title = "Song 2", artist = "Artist X", energy = 0.5, valence = 0.5),
            TrackData(id = "t3", title = "Song 3", artist = "Artist X", energy = 0.5, valence = 0.5),
            TrackData(id = "t4", title = "Song 4", artist = "Artist X", energy = 0.5, valence = 0.5),
            TrackData(id = "t5", title = "Song 5", artist = "Artist Y", energy = 0.5, valence = 0.5),
            TrackData(id = "t6", title = "Song 6", artist = "Artist Y", energy = 0.5, valence = 0.5)
        )

        mockkConstructor(MusicLibraryLoader::class)
        every { anyConstructed<MusicLibraryLoader>().getAllTracks() } returns mockTracks
        every { anyConstructed<MusicLibraryLoader>().toTrack(any()) } answers {
            val trackData = firstArg<TrackData>()
            Track(
                id = trackData.id,
                title = trackData.title,
                artist = trackData.artist,
                energy = trackData.energy,
                valence = trackData.valence
            )
        }

        val result = contentEngine.generatePlaylist(scene)
        assertTrue(result.isSuccess)
        
        val playlist = result.getOrThrow()
        val artistCounts = playlist.tracks.groupingBy { it.artist }.eachCount()
        
        artistCounts.forEach { (_, count) ->
            assertTrue("Each artist should have at most 2 tracks", count <= 2)
        }

        clearAllMocks()
    }
}
