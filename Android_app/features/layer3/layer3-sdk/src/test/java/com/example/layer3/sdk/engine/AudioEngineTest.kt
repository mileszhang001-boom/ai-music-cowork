package com.example.layer3.sdk.engine

import android.content.Context
import com.example.layer3.api.AudioEngineConfig
import com.example.layer3.api.model.*
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AudioEngineTest {

    private lateinit var audioEngine: AudioEngine
    private lateinit var mockContext: Context
    private lateinit var config: AudioEngineConfig

    @Before
    fun setUp() {
        mockContext = mockk(relaxed = true)
        config = AudioEngineConfig(
            defaultPreset = AudioPresets.FLAT,
            enableSpatialAudio = true,
            defaultSpatialMode = SpatialModes.STEREO
        )
        audioEngine = AudioEngine(mockContext, config)
    }

    @After
    fun tearDown() {
        audioEngine.destroy()
    }

    @Test
    fun testGenerateAudioConfig_HighEnergy_ReturnsElectronicPreset() = runTest {
        val highEnergyScene = SceneDescriptor(
            scene_id = "high_energy_audio",
            scene_name = "High Energy",
            intent = Intent(
                mood = Mood(valence = 0.8, arousal = 0.9),
                energy_level = 0.9
            )
        )

        val result = audioEngine.generateAudioConfig(highEnergyScene)

        assertTrue(result.isSuccess)
        val audioConfig = result.getOrThrow()
        assertNotNull(audioConfig)
        assertEquals(AudioPresets.ELECTRONIC, audioConfig.equalizer?.preset_name)
    }

    @Test
    fun testGenerateAudioConfig_LowArousal_ReturnsJazzPreset() = runTest {
        val calmScene = SceneDescriptor(
            scene_id = "calm_audio",
            scene_name = "Calm Scene",
            intent = Intent(
                mood = Mood(valence = 0.6, arousal = 0.2),
                energy_level = 0.3
            )
        )

        val result = audioEngine.generateAudioConfig(calmScene)

        assertTrue(result.isSuccess)
        val audioConfig = result.getOrThrow()
        assertNotNull(audioConfig)
        assertEquals(AudioPresets.JAZZ, audioConfig.equalizer?.preset_name)
    }

    @Test
    fun testGenerateAudioConfig_CustomHints_AppliesCorrectly() = runTest {
        val customScene = SceneDescriptor(
            scene_id = "custom_audio",
            scene_name = "Custom Audio",
            intent = Intent(
                mood = Mood(valence = 0.5, arousal = 0.5),
                energy_level = 0.5
            ),
            hints = Hints(
                audio = AudioHints(
                    preset = AudioPresets.ROCK,
                    spatial_mode = SpatialModes.SURROUND_5_1,
                    bass_boost = 0.3,
                    treble_boost = 0.2
                )
            )
        )

        val result = audioEngine.generateAudioConfig(customScene)

        assertTrue(result.isSuccess)
        val audioConfig = result.getOrThrow()
        assertNotNull(audioConfig)
        assertEquals(AudioPresets.ROCK, audioConfig.equalizer?.preset_name)
        assertEquals(0.3, audioConfig.enhancements.bass_boost, 0.01)
        assertEquals(0.2, audioConfig.enhancements.treble_boost, 0.01)
    }

    @Test
    fun testSetBassBoost_UpdatesState() = runTest {
        val result = audioEngine.setBassBoost(0.4)

        assertTrue(result.isSuccess)
        assertEquals(0.4, audioEngine.getAudioState().bassBoost, 0.01)
    }

    @Test
    fun testSetBassBoost_ClampsToMax() = runTest {
        val result = audioEngine.setBassBoost(1.0)

        assertTrue(result.isSuccess)
        assertTrue(audioEngine.getAudioState().bassBoost <= config.maxBassBoost)
    }

    @Test
    fun testSetTrebleBoost_UpdatesState() = runTest {
        val result = audioEngine.setTrebleBoost(0.3)

        assertTrue(result.isSuccess)
        assertEquals(0.3, audioEngine.getAudioState().trebleBoost, 0.01)
    }

    @Test
    fun testEnableSpatialAudio_UpdatesState() = runTest {
        val result = audioEngine.enableSpatialAudio(true)

        assertTrue(result.isSuccess)
        assertTrue(audioEngine.getAudioState().spatialEnabled)
    }

    @Test
    fun testSetSpatialMode_UpdatesState() = runTest {
        val result = audioEngine.setSpatialMode(SpatialModes.SURROUND_7_1)

        assertTrue(result.isSuccess)
        assertEquals(SpatialModes.SURROUND_7_1, audioEngine.getAudioState().spatialMode)
    }

    @Test
    fun testResetToDefault_ResetsState() = runTest {
        audioEngine.setBassBoost(0.5)
        audioEngine.setTrebleBoost(0.4)
        audioEngine.setSpatialMode(SpatialModes.DOLBY_ATMOS)

        val result = audioEngine.resetToDefault()

        assertTrue(result.isSuccess)
        val state = audioEngine.getAudioState()
        assertEquals(0.0, state.bassBoost, 0.01)
        assertEquals(0.0, state.trebleBoost, 0.01)
        assertEquals(config.defaultSpatialMode, state.spatialMode)
    }

    @Test
    fun testGetAvailablePresets_ReturnsAllPresets() {
        val presets = audioEngine.getAvailablePresets()

        assertTrue(presets.contains(AudioPresets.FLAT))
        assertTrue(presets.contains(AudioPresets.BASS_BOOST))
        assertTrue(presets.contains(AudioPresets.TREBLE_BOOST))
        assertTrue(presets.contains(AudioPresets.ROCK))
        assertTrue(presets.contains(AudioPresets.POP))
        assertTrue(presets.contains(AudioPresets.CLASSICAL))
        assertTrue(presets.contains(AudioPresets.ELECTRONIC))
        assertTrue(presets.contains(AudioPresets.JAZZ))
    }
}
