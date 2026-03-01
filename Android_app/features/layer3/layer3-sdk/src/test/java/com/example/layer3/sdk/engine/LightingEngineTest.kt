package com.example.layer3.sdk.engine

import android.content.Context
import com.example.layer3.api.LightingControllerConfig
import com.example.layer3.api.model.*
import com.example.layer3.sdk.algorithm.ColorAdjuster
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LightingEngineTest {

    private lateinit var lightingEngine: LightingEngine
    private lateinit var mockContext: Context
    private lateinit var config: LightingControllerConfig

    @Before
    fun setUp() {
        mockContext = mockk(relaxed = true)
        config = LightingControllerConfig()
        lightingEngine = LightingEngine(mockContext, config)
    }

    @After
    fun tearDown() {
        lightingEngine.destroy()
    }

    @Test
    fun testGenerateLighting_HappyScene_ReturnsWarmColors() = runTest {
        val happyScene = SceneDescriptor(
            scene_id = "happy_lighting",
            scene_name = "Happy Scene",
            intent = Intent(
                mood = Mood(valence = 0.8, arousal = 0.7),
                energy_level = 0.8
            )
        )

        val result = lightingEngine.generateLightingConfig(happyScene)

        assertTrue(result.isSuccess)
        val lightingConfig = result.getOrThrow()
        assertNotNull(lightingConfig)
        assertTrue(lightingConfig.scene.zones.isNotEmpty())

        val zoneColors = lightingConfig.scene.zones.map { it.color.hex }
        val hasWarmColors = zoneColors.any { hex ->
            val rgb = hexToRgb(hex)
            rgb[0] > rgb[2]
        }
        assertTrue("Should have warm colors (more red than blue)", hasWarmColors)
    }

    @Test
    fun testGenerateLighting_SadScene_ReturnsCoolColors() = runTest {
        val sadScene = SceneDescriptor(
            scene_id = "sad_lighting",
            scene_name = "Sad Scene",
            intent = Intent(
                mood = Mood(valence = 0.2, arousal = 0.3),
                energy_level = 0.3
            )
        )

        val result = lightingEngine.generateLightingConfig(sadScene)

        assertTrue(result.isSuccess)
        val lightingConfig = result.getOrThrow()
        assertNotNull(lightingConfig)
        assertTrue(lightingConfig.scene.zones.isNotEmpty())

        val zoneColors = lightingConfig.scene.zones.map { it.color.hex }
        val hasCoolColors = zoneColors.any { hex ->
            val rgb = hexToRgb(hex)
            rgb[2] >= rgb[0]
        }
        assertTrue("Should have cool colors (more blue than red)", hasCoolColors)
    }

    @Test
    fun testGenerateLighting_CustomTheme_AppliesCorrectly() = runTest {
        val customThemeScene = SceneDescriptor(
            scene_id = "custom_theme",
            scene_name = "Custom Theme Scene",
            intent = Intent(
                mood = Mood(valence = 0.5, arousal = 0.5),
                energy_level = 0.5
            ),
            hints = Hints(
                lighting = LightingHints(
                    color_theme = "romantic",
                    pattern = LightingPatterns.PULSE,
                    intensity = 0.8
                )
            )
        )

        val result = lightingEngine.generateLightingConfig(customThemeScene)

        assertTrue(result.isSuccess)
        val lightingConfig = result.getOrThrow()
        assertNotNull(lightingConfig)
        
        assertEquals(LightingPatterns.PULSE, lightingConfig.scene.zones.firstOrNull()?.pattern)
        
        lightingConfig.scene.zones.forEach { zone ->
            assertTrue("Brightness should be close to 0.8", 
                kotlin.math.abs(zone.brightness - 0.8) < 0.3)
        }
    }

    @Test
    fun testApplyConfig_UpdatesCurrentConfig() = runTest {
        val config = LightingConfig(
            config_id = "test_config_1",
            scene = LightingScene(
                scene_id = "test_scene",
                name = "Test Scene",
                zones = listOf(
                    ZoneConfig(
                        zone_id = LightingZones.DASHBOARD,
                        enabled = true,
                        color = ColorConfig(hex = "#FF0000"),
                        brightness = 0.9
                    )
                )
            )
        )

        val result = lightingEngine.applyConfig(config)

        assertTrue(result.isSuccess)
        assertEquals(config, lightingEngine.getCurrentConfig())
    }

    @Test
    fun testSetBrightness_UpdatesState() = runTest {
        val result = lightingEngine.setBrightness(0.5)

        assertTrue(result.isSuccess)
        assertEquals(0.5, lightingEngine.getLightingState().brightness, 0.01)
    }

    @Test
    fun testTurnOff_UpdatesState() = runTest {
        val result = lightingEngine.turnOff()

        assertTrue(result.isSuccess)
        assertFalse(lightingEngine.getLightingState().is_on)
    }

    @Test
    fun testTurnOn_UpdatesState() = runTest {
        lightingEngine.turnOff()
        
        val result = lightingEngine.turnOn()

        assertTrue(result.isSuccess)
        assertTrue(lightingEngine.getLightingState().is_on)
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
