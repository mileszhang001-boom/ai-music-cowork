package com.example.demo

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.layer3.api.*
import com.example.layer3.api.model.*
import com.example.layer3.sdk.Layer3SDK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class Layer3IntegrationTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        
        val config = Layer3Config.Builder()
            .setContentProvider(ContentProviderConfig())
            .setLightingController(LightingControllerConfig())
            .setAudioEngine(AudioEngineConfig())
            .setGenerationEngine(GenerationEngineConfig())
            .build()
        
        if (Layer3SDK.isInitialized()) {
            Layer3SDK.destroy()
        }
        Layer3SDK.init(context, config)
    }

    @After
    fun tearDown() {
        if (Layer3SDK.isInitialized()) {
            Layer3SDK.destroy()
        }
    }

    @Test
    fun testEndToEnd_MorningCommute_GeneratesCorrectEffects() = runTest {
        val morningCommuteScene = SceneDescriptor(
            scene_id = "morning_commute",
            scene_name = "Morning Commute",
            scene_narrative = "A sunny morning drive to work",
            intent = Intent(
                mood = Mood(valence = 0.7, arousal = 0.6),
                energy_level = 0.6,
                atmosphere = "energetic"
            ),
            hints = Hints(
                music = MusicHints(
                    genres = listOf("pop", "indie"),
                    tempo = "medium"
                ),
                lighting = LightingHints(
                    color_theme = "energetic",
                    pattern = LightingPatterns.BREATHING,
                    intensity = 0.7
                ),
                audio = AudioHints(
                    preset = AudioPresets.POP,
                    bass_boost = 0.2
                )
            )
        )

        val contentEngine = Layer3SDK.getContentEngine()
        val lightingEngine = Layer3SDK.getLightingEngine()
        val audioEngine = Layer3SDK.getAudioEngine()

        val playlistResult = contentEngine.generatePlaylist(morningCommuteScene)
        assertTrue("Playlist generation should succeed", playlistResult.isSuccess)
        val playlist = playlistResult.getOrThrow()
        assertNotNull("Playlist should not be null", playlist)
        assertTrue("Playlist should have tracks", playlist.tracks.isNotEmpty())

        val lightingResult = lightingEngine.generateLightingConfig(morningCommuteScene)
        assertTrue("Lighting config generation should succeed", lightingResult.isSuccess)
        val lightingConfig = lightingResult.getOrThrow()
        assertNotNull("Lighting config should not be null", lightingConfig)
        assertTrue("Should have lighting zones", lightingConfig.scene.zones.isNotEmpty())

        val audioResult = audioEngine.generateAudioConfig(morningCommuteScene)
        assertTrue("Audio config generation should succeed", audioResult.isSuccess)
        val audioConfig = audioResult.getOrThrow()
        assertNotNull("Audio config should not be null", audioConfig)
        assertNotNull("Equalizer should be configured", audioConfig.equalizer)
    }

    @Test
    fun testEndToEnd_NightDrive_GeneratesCorrectEffects() = runTest {
        val nightDriveScene = SceneDescriptor(
            scene_id = "night_drive",
            scene_name = "Night Drive",
            scene_narrative = "A calm night drive through the city",
            intent = Intent(
                mood = Mood(valence = 0.5, arousal = 0.4),
                energy_level = 0.4,
                atmosphere = "relaxed"
            ),
            hints = Hints(
                music = MusicHints(
                    genres = listOf("jazz", "lo-fi"),
                    tempo = "slow"
                ),
                lighting = LightingHints(
                    color_theme = "calm",
                    pattern = LightingPatterns.STATIC,
                    intensity = 0.4
                ),
                audio = AudioHints(
                    preset = AudioPresets.JAZZ,
                    bass_boost = 0.1
                )
            )
        )

        val contentEngine = Layer3SDK.getContentEngine()
        val lightingEngine = Layer3SDK.getLightingEngine()
        val audioEngine = Layer3SDK.getAudioEngine()

        val playlistResult = contentEngine.generatePlaylist(nightDriveScene)
        assertTrue("Playlist generation should succeed", playlistResult.isSuccess)
        val playlist = playlistResult.getOrThrow()
        assertNotNull("Playlist should not be null", playlist)

        val lightingResult = lightingEngine.generateLightingConfig(nightDriveScene)
        assertTrue("Lighting config generation should succeed", lightingResult.isSuccess)
        val lightingConfig = lightingResult.getOrThrow()
        
        val avgBrightness = lightingConfig.scene.zones.map { it.brightness }.average()
        assertTrue("Night drive should have lower brightness", avgBrightness < 0.8)

        val audioResult = audioEngine.generateAudioConfig(nightDriveScene)
        assertTrue("Audio config generation should succeed", audioResult.isSuccess)
        val audioConfig = audioResult.getOrThrow()
        
        assertTrue("Bass boost should be low for night drive", 
            audioConfig.enhancements.bass_boost <= 0.3)
    }

    @Test
    fun testEndToEnd_RainyDay_GeneratesCorrectEffects() = runTest {
        val rainyDayScene = SceneDescriptor(
            scene_id = "rainy_day",
            scene_name = "Rainy Day",
            scene_narrative = "A melancholic rainy afternoon",
            intent = Intent(
                mood = Mood(valence = 0.3, arousal = 0.3),
                energy_level = 0.3,
                atmosphere = "melancholic"
            ),
            hints = Hints(
                music = MusicHints(
                    genres = listOf("ballad", "classical"),
                    tempo = "slow"
                ),
                lighting = LightingHints(
                    color_theme = "melancholic",
                    pattern = LightingPatterns.BREATHING,
                    intensity = 0.3
                ),
                audio = AudioHints(
                    preset = AudioPresets.CLASSICAL,
                    bass_boost = 0.0
                )
            )
        )

        val contentEngine = Layer3SDK.getContentEngine()
        val lightingEngine = Layer3SDK.getLightingEngine()
        val audioEngine = Layer3SDK.getAudioEngine()

        val playlistResult = contentEngine.generatePlaylist(rainyDayScene)
        assertTrue("Playlist generation should succeed", playlistResult.isSuccess)
        val playlist = playlistResult.getOrThrow()
        assertNotNull("Playlist should not be null", playlist)

        val lightingResult = lightingEngine.generateLightingConfig(rainyDayScene)
        assertTrue("Lighting config generation should succeed", lightingResult.isSuccess)
        val lightingConfig = lightingResult.getOrThrow()
        
        val zoneColors = lightingConfig.scene.zones.map { it.color.hex }
        val hasCoolColors = zoneColors.any { hex ->
            val rgb = hexToRgb(hex)
            rgb[2] >= rgb[0]
        }
        assertTrue("Rainy day should have cool colors", hasCoolColors)

        val audioResult = audioEngine.generateAudioConfig(rainyDayScene)
        assertTrue("Audio config generation should succeed", audioResult.isSuccess)
        val audioConfig = audioResult.getOrThrow()
        
        assertEquals("Should use classical preset for rainy day",
            AudioPresets.CLASSICAL, audioConfig.equalizer?.preset_name)
    }

    @Test
    fun testSDK_Initialization_Success() {
        assertTrue("SDK should be initialized", Layer3SDK.isInitialized())
        assertNotNull("Config should not be null", Layer3SDK.getConfig())
    }

    @Test
    fun testSDK_GetEngines_ReturnsValidInstances() {
        val contentEngine = Layer3SDK.getContentEngine()
        val lightingEngine = Layer3SDK.getLightingEngine()
        val audioEngine = Layer3SDK.getAudioEngine()

        assertNotNull("Content engine should not be null", contentEngine)
        assertNotNull("Lighting engine should not be null", lightingEngine)
        assertNotNull("Audio engine should not be null", audioEngine)
    }

    @Test
    fun testSDK_Destroy_ClearsState() = runTest {
        Layer3SDK.destroy()

        assertFalse("SDK should not be initialized after destroy", Layer3SDK.isInitialized())
        assertNull("Config should be null after destroy", Layer3SDK.getConfig())
    }

    @Test
    fun testEndToEnd_HighEnergyScene_GeneratesIntenseEffects() = runTest {
        val highEnergyScene = SceneDescriptor(
            scene_id = "high_energy",
            scene_name = "Party Mode",
            intent = Intent(
                mood = Mood(valence = 0.9, arousal = 0.9),
                energy_level = 0.95
            ),
            hints = Hints(
                music = MusicHints(
                    genres = listOf("electronic", "dance"),
                    tempo = "fast"
                ),
                lighting = LightingHints(
                    color_theme = "energetic",
                    pattern = LightingPatterns.MUSIC_SYNC,
                    intensity = 1.0
                ),
                audio = AudioHints(
                    preset = AudioPresets.ELECTRONIC,
                    bass_boost = 0.4
                )
            )
        )

        val contentEngine = Layer3SDK.getContentEngine()
        val lightingEngine = Layer3SDK.getLightingEngine()
        val audioEngine = Layer3SDK.getAudioEngine()

        val playlistResult = contentEngine.generatePlaylist(highEnergyScene)
        assertTrue("Playlist generation should succeed", playlistResult.isSuccess)

        val lightingResult = lightingEngine.generateLightingConfig(highEnergyScene)
        assertTrue("Lighting config generation should succeed", lightingResult.isSuccess)
        val lightingConfig = lightingResult.getOrThrow()
        
        assertTrue("High energy scene should sync with music", 
            lightingConfig.scene.sync_with_music)

        val audioResult = audioEngine.generateAudioConfig(highEnergyScene)
        assertTrue("Audio config generation should succeed", audioResult.isSuccess)
        val audioConfig = audioResult.getOrThrow()
        
        assertEquals("Should use electronic preset",
            AudioPresets.ELECTRONIC, audioConfig.equalizer?.preset_name)
    }

    private fun hexToRgb(hex: String): List<Int> {
        val cleanHex = hex.removePrefix("#")
        val color = cleanHex.toLong(16)
        return listOf(
            ((color shr 16) and 0xFF).toInt(),
            ((color shr 8) and 0xFF).toInt(),
            (color and 0xFF).toInt()
        )
    }
}
