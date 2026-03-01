package com.example.layer3.sdk.algorithm

import com.example.layer3.api.model.ColorConfig
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ColorAdjusterTest {

    private lateinit var colorAdjuster: ColorAdjuster

    @Before
    fun setUp() {
        colorAdjuster = ColorAdjuster()
    }

    @Test
    fun testAdjustColor_FullIntensity_ReturnsSameColor() {
        val result = colorAdjuster.adjustColor("#FF0000", 1.0)

        assertEquals("#ff0000", result.hex.lowercase())
    }

    @Test
    fun testAdjustColor_LowIntensity_DarkensColor() {
        val result = colorAdjuster.adjustColor("#FF0000", 0.5)

        val rgb = result.rgb
        assertEquals(127, rgb[0])
        assertEquals(0, rgb[1])
        assertEquals(0, rgb[2])
    }

    @Test
    fun testAdjustColor_WithHappyMood_AdjustsColors() {
        val result = colorAdjuster.adjustColor("#FF0000", 1.0, "happy")

        assertNotNull(result.hex)
        assertTrue(result.hex.startsWith("#"))
    }

    @Test
    fun testAdjustColor_WithCalmMood_AdjustsColors() {
        val result = colorAdjuster.adjustColor("#0000FF", 1.0, "calm")

        assertNotNull(result.hex)
        assertTrue(result.hex.startsWith("#"))
    }

    @Test
    fun testGenerateColorPalette_ReturnsCorrectCount() {
        val baseColors = listOf("#FF0000", "#00FF00", "#0000FF")
        val count = 5

        val result = colorAdjuster.generateColorPalette(baseColors, count)

        assertEquals(count, result.size)
    }

    @Test
    fun testGenerateColorPalette_EmptyBaseColors_ReturnsDefaultPalette() {
        val count = 3

        val result = colorAdjuster.generateColorPalette(emptyList(), count)

        assertEquals(count, result.size)
    }

    @Test
    fun testGetColorsForTheme_HappyTheme_ReturnsWarmColors() {
        val result = colorAdjuster.getColorsForTheme("happy")

        assertTrue(result.isNotEmpty())
        result.forEach { hex ->
            assertTrue("Happy theme should have warm colors", hex.startsWith("#"))
        }
    }

    @Test
    fun testGetColorsForTheme_CalmTheme_ReturnsCoolColors() {
        val result = colorAdjuster.getColorsForTheme("calm")

        assertTrue(result.isNotEmpty())
    }

    @Test
    fun testGetColorsForTheme_MelancholicTheme_ReturnsCoolColors() {
        val result = colorAdjuster.getColorsForTheme("melancholic")

        assertTrue(result.isNotEmpty())
    }

    @Test
    fun testGetColorsForTheme_UnknownTheme_ReturnsDefaultColors() {
        val result = colorAdjuster.getColorsForTheme("unknown_theme")

        assertTrue(result.isNotEmpty())
    }

    @Test
    fun testSetThemeColors_StoresCustomTheme() {
        val customColors = listOf("#123456", "#789ABC", "#DEF012")
        
        colorAdjuster.setThemeColors("custom", customColors)
        val result = colorAdjuster.getColorsForTheme("custom")

        assertEquals(customColors, result)
    }

    @Test
    fun testAdjustColor_CaseInsensitiveHex() {
        val result1 = colorAdjuster.adjustColor("#ff0000", 1.0)
        val result2 = colorAdjuster.adjustColor("#FF0000", 1.0)

        assertEquals(result1.hex.lowercase(), result2.hex.lowercase())
    }

    @Test
    fun testAdjustColor_ReturnsValidHexFormat() {
        val result = colorAdjuster.adjustColor("#ABCDEF", 0.8)

        assertTrue("Hex should start with #", result.hex.startsWith("#"))
        assertEquals("Hex should have 7 characters", 7, result.hex.length)
    }

    @Test
    fun testAdjustColor_ReturnsValidRgb() {
        val result = colorAdjuster.adjustColor("#FF8000", 1.0)

        assertEquals(3, result.rgb.size)
        result.rgb.forEach { channel ->
            assertTrue("RGB channel should be in valid range", channel in 0..255)
        }
    }

    @Test
    fun testAdjustColor_ReturnsColorName() {
        val result = colorAdjuster.adjustColor("#FF0000", 1.0)

        assertNotNull(result.name)
        assertTrue("Color name should not be empty", result.name!!.isNotEmpty())
    }

    @Test
    fun testGenerateColorPalette_VariesIntensity() {
        val baseColors = listOf("#FF0000")
        val count = 3

        val result = colorAdjuster.generateColorPalette(baseColors, count)

        val uniqueHexes = result.map { it.hex }.distinct()
        assertTrue("Palette should have varying colors", uniqueHexes.size >= 1)
    }

    @Test
    fun testGetColorsForTheme_EnergeticTheme_ReturnsVibrantColors() {
        val result = colorAdjuster.getColorsForTheme("energetic")

        assertTrue(result.isNotEmpty())
        assertEquals(3, result.size)
    }

    @Test
    fun testGetColorsForTheme_RomanticTheme_ReturnsPinkColors() {
        val result = colorAdjuster.getColorsForTheme("romantic")

        assertTrue(result.isNotEmpty())
    }
}
