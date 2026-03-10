package com.example.layer3.sdk.algorithm

import com.example.layer3.sdk.util.Logger
import kotlin.math.max
import kotlin.math.min

class ColorAdjuster {
    private val themeColors = mutableMapOf<String, List<String>>()

    fun adjustColor(
        baseColor: String,
        intensity: Double,
        mood: String? = null
    ): String {
        val rgb = hexToRgb(baseColor)
        val adjustedRgb = adjustIntensity(rgb, intensity)
        val moodAdjustedRgb = mood?.let { applyMoodAdjustment(adjustedRgb, it) } ?: adjustedRgb
        
        return rgbToHex(moodAdjustedRgb)
    }

    fun generateColorPalette(
        baseColors: List<String>,
        count: Int
    ): List<String> {
        if (baseColors.isEmpty()) {
            return generateDefaultPalette(count)
        }

        val palette = mutableListOf<String>()
        val step = 1.0 / (count + 1)
        
        for (i in 0 until count) {
            val baseIndex = i % baseColors.size
            val intensity = 0.5 + (i * step)
            palette.add(adjustColor(baseColors[baseIndex], intensity))
        }
        
        return palette
    }

    fun getColorsForTheme(theme: String): List<String> {
        return themeColors[theme.lowercase()] ?: getDefaultThemeColors(theme)
    }

    fun setThemeColors(theme: String, colors: List<String>) {
        themeColors[theme.lowercase()] = colors
        Logger.d("ColorAdjuster: Set $colors for theme '$theme'")
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

    private fun rgbToHex(rgb: List<Int>): String {
        val r = rgb[0].coerceIn(0, 255)
        val g = rgb[1].coerceIn(0, 255)
        val b = rgb[2].coerceIn(0, 255)
        return "#${r.toString(16).padStart(2, '0')}${g.toString(16).padStart(2, '0')}${b.toString(16).padStart(2, '0')}"
    }

    private fun adjustIntensity(rgb: List<Int>, intensity: Double): List<Int> {
        val factor = intensity.coerceIn(0.0, 1.0)
        return rgb.map { channel ->
            val adjusted = (channel * factor).toInt()
            adjusted.coerceIn(0, 255)
        }
    }

    private fun applyMoodAdjustment(rgb: List<Int>, mood: String): List<Int> {
        val adjustment = MOOD_ADJUSTMENTS[mood.lowercase()] ?: return rgb
        
        return listOf(
            (rgb[0] * adjustment[0]).toInt().coerceIn(0, 255),
            (rgb[1] * adjustment[1]).toInt().coerceIn(0, 255),
            (rgb[2] * adjustment[2]).toInt().coerceIn(0, 255)
        )
    }

    private fun getColorName(rgb: List<Int>): String {
        val hue = rgbToHue(rgb)
        return when {
            hue < 15 || hue >= 345 -> "Red"
            hue < 45 -> "Orange"
            hue < 75 -> "Yellow"
            hue < 150 -> "Green"
            hue < 210 -> "Cyan"
            hue < 270 -> "Blue"
            hue < 315 -> "Purple"
            else -> "Pink"
        }
    }

    private fun rgbToHue(rgb: List<Int>): Double {
        val r = rgb[0] / 255.0
        val g = rgb[1] / 255.0
        val b = rgb[2] / 255.0
        
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val delta = max - min
        
        if (delta == 0.0) return 0.0
        
        val hue = when (max) {
            r -> 60 * (((g - b) / delta) % 6)
            g -> 60 * (((b - r) / delta) + 2)
            else -> 60 * (((r - g) / delta) + 4)
        }
        
        return if (hue < 0) hue + 360 else hue
    }

    private fun generateDefaultPalette(count: Int): List<String> {
        val defaultColors = listOf("#FFFFFF", "#F0F8FF", "#E6E6FA")
        return generateColorPalette(defaultColors, count)
    }

    private fun getDefaultThemeColors(theme: String): List<String> {
        return when (theme.lowercase()) {
            "happy", "joyful" -> listOf("#FFD700", "#FFA500", "#FF6347")
            "calm", "relaxed" -> listOf("#4169E1", "#87CEEB", "#98FB98")
            "energetic", "excited" -> listOf("#FF4500", "#FF1493", "#9400D3")
            "focused", "concentrated" -> listOf("#FFFFFF", "#F0F8FF", "#E6E6FA")
            "romantic" -> listOf("#FF69B4", "#FFB6C1", "#DDA0DD")
            "melancholic", "sad" -> listOf("#4682B4", "#708090", "#778899")
            else -> listOf("#FFFFFF", "#CCCCCC", "#999999")
        }
    }

    companion object {
        private val MOOD_ADJUSTMENTS = mapOf(
            "happy" to listOf(1.1, 1.05, 0.95),
            "calm" to listOf(0.95, 1.0, 1.1),
            "energetic" to listOf(1.15, 0.9, 1.1),
            "focused" to listOf(1.0, 1.0, 1.0),
            "romantic" to listOf(1.05, 0.85, 1.0),
            "melancholic" to listOf(0.9, 0.95, 1.1)
        )
    }
}
