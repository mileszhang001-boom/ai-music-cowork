package com.example.layer3.sdk.engine

import android.content.Context
import com.example.layer3.api.ILightingEngine
import com.music.core.api.models.SceneDescriptor
import com.music.core.api.models.LightingCommand
import com.example.layer3.sdk.util.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class LightingEngine(
    private val context: Context
) : ILightingEngine {
    
    private val _lightingStateFlow = MutableStateFlow(LightingCommand(action = "set", theme = "default", intensity = 1.0))
    override val lightingStateFlow: Flow<LightingCommand> = _lightingStateFlow.asStateFlow()

    override suspend fun generateLighting(scene: SceneDescriptor): Result<LightingCommand> {
        return try {
            val colorTheme = scene.hints.lighting?.color_theme
            val sceneType = scene.scene_type
            val pattern = scene.hints.lighting?.pattern ?: "breathing"
            val intensity = scene.hints.lighting?.intensity
                ?: scene.intent.energy_level.coerceIn(0.0, 1.0)

            val theme = resolveTheme(colorTheme, sceneType)
            val baseColors = COLOR_THEMES[theme] ?: COLOR_THEMES["calm"]!!
            var primary = baseColors.first
            var secondary = baseColors.second

            val energy = scene.intent.energy_level
            val valence = scene.intent.mood.valence
            primary = adjustColorByEnergy(primary, energy)
            secondary = adjustColorByEnergy(secondary, energy)
            primary = adjustColorByValence(primary, valence)
            secondary = adjustColorByValence(secondary, valence)

            val command = LightingCommand(
                action = "set",
                theme = theme,
                colors = listOf(primary, secondary),
                pattern = pattern,
                intensity = intensity.coerceIn(0.0, 1.0)
            )
            Logger.i("LightingEngine: theme=$theme, colors=[$primary, $secondary], pattern=$pattern")
            Result.success(command)
        } catch (e: Exception) {
            Logger.e("LightingEngine: Failed to generate lighting", e)
            Result.failure(e)
        }
    }

    private fun resolveTheme(colorTheme: String?, sceneType: String): String {
        if (!colorTheme.isNullOrBlank() && COLOR_THEMES.containsKey(colorTheme)) {
            return colorTheme
        }
        return SCENE_THEME_MAP[sceneType] ?: colorTheme ?: "calm"
    }

    private fun adjustColorByEnergy(hex: String, energy: Double): String {
        val hsl = hexToHSL(hex)
        val adjustedS = when {
            energy > 0.7 -> min(100.0, hsl.second * 1.2)
            energy < 0.3 -> hsl.second * 0.7
            else -> hsl.second
        }
        return hslToHex(hsl.first, adjustedS, hsl.third)
    }

    private fun adjustColorByValence(hex: String, valence: Double): String {
        val hsl = hexToHSL(hex)
        val adjustedH = when {
            valence > 0.7 -> (hsl.first + 10.0) % 360.0
            valence < 0.3 -> (hsl.first - 10.0 + 360.0) % 360.0
            else -> hsl.first
        }
        return hslToHex(adjustedH, hsl.second, hsl.third)
    }

    private fun hexToHSL(hex: String): Triple<Double, Double, Double> {
        val clean = hex.removePrefix("#")
        val color = clean.toLong(16)
        val r = ((color shr 16) and 0xFF) / 255.0
        val g = ((color shr 8) and 0xFF) / 255.0
        val b = (color and 0xFF) / 255.0

        val cMax = maxOf(r, g, b)
        val cMin = minOf(r, g, b)
        val delta = cMax - cMin
        val l = (cMax + cMin) / 2.0

        if (delta == 0.0) return Triple(0.0, 0.0, l * 100.0)

        val s = if (l > 0.5) delta / (2.0 - cMax - cMin) else delta / (cMax + cMin)
        val h = when (cMax) {
            r -> ((g - b) / delta + (if (g < b) 6.0 else 0.0)) / 6.0
            g -> ((b - r) / delta + 2.0) / 6.0
            else -> ((r - g) / delta + 4.0) / 6.0
        }
        return Triple((h * 360.0), s * 100.0, l * 100.0)
    }

    private fun hslToHex(h: Double, s: Double, l: Double): String {
        val hN = h / 360.0
        val sN = s / 100.0
        val lN = l / 100.0

        val r: Double; val g: Double; val b: Double
        if (sN == 0.0) {
            r = lN; g = lN; b = lN
        } else {
            val q = if (lN < 0.5) lN * (1.0 + sN) else lN + sN - lN * sN
            val p = 2.0 * lN - q
            r = hue2rgb(p, q, hN + 1.0 / 3.0)
            g = hue2rgb(p, q, hN)
            b = hue2rgb(p, q, hN - 1.0 / 3.0)
        }
        val ri = (r * 255).roundToInt().coerceIn(0, 255)
        val gi = (g * 255).roundToInt().coerceIn(0, 255)
        val bi = (b * 255).roundToInt().coerceIn(0, 255)
        return "#${ri.toString(16).padStart(2, '0')}${gi.toString(16).padStart(2, '0')}${bi.toString(16).padStart(2, '0')}"
    }

    private fun hue2rgb(p: Double, q: Double, tIn: Double): Double {
        var t = tIn
        if (t < 0) t += 1.0
        if (t > 1) t -= 1.0
        return when {
            t < 1.0 / 6.0 -> p + (q - p) * 6.0 * t
            t < 1.0 / 2.0 -> q
            t < 2.0 / 3.0 -> p + (q - p) * (2.0 / 3.0 - t) * 6.0
            else -> p
        }
    }

    override fun applyLighting(command: LightingCommand) {
        _lightingStateFlow.value = command
        Logger.i("LightingEngine: Applied lighting command: $command")
    }

    override fun setTheme(theme: String) {
        val current = _lightingStateFlow.value
        _lightingStateFlow.value = current.copy(theme = theme)
    }

    override fun setIntensity(intensity: Double) {
        val current = _lightingStateFlow.value
        _lightingStateFlow.value = current.copy(intensity = intensity.coerceIn(0.0, 1.0))
    }

    fun destroy() {
        Logger.i("LightingEngine: Destroyed")
    }

    companion object {
        private val COLOR_THEMES = mapOf(
            "calm" to Pair("#1A237E", "#4A148C"),
            "warm" to Pair("#FF5722", "#FF9800"),
            "vibrant" to Pair("#E91E63", "#9C27B0"),
            "alert" to Pair("#F44336", "#FFEB3B"),
            "romantic" to Pair("#FF69B4", "#FFB6C1"),
            "cool" to Pair("#00BCD4", "#3F51B5"),
            "night" to Pair("#0D1B2A", "#1B263B"),
            "focus" to Pair("#2196F3", "#009688"),
            "energetic" to Pair("#FF6B00", "#FFD600"),
            "party" to Pair("#FF1493", "#00FF7F"),
            "ocean" to Pair("#00CED1", "#20B2AA"),
            "sunset" to Pair("#FF6B35", "#FFD700"),
            "rainy" to Pair("#4682B4", "#708090"),
            "forest" to Pair("#228B22", "#2E8B57"),
            "citynight" to Pair("#191970", "#FFD700"),
            "spring" to Pair("#90EE90", "#FFB6C1"),
            "summer" to Pair("#00BFFF", "#FFDAB9"),
            "autumn" to Pair("#D2691E", "#FF8C00"),
            "winter" to Pair("#B0E0E6", "#FFFFFF"),
            "meditation" to Pair("#9370DB", "#E6E6FA"),
            "gloomy" to Pair("#4A5568", "#696969")
        )

        private val SCENE_THEME_MAP = mapOf(
            "family_outing" to "ocean",
            "sunset_drive" to "sunset",
            "rainy_day" to "rainy",
            "night_drive" to "citynight",
            "road_trip" to "forest",
            "morning_commute" to "spring",
            "beach_vacation" to "summer",
            "autumn_drive" to "autumn",
            "winter_drive" to "winter",
            "romantic_date" to "romantic",
            "party" to "party",
            "meditation" to "meditation",
            "cloudy_day" to "gloomy",
            "kids_mode" to "vibrant",
            "focus_work" to "focus",
            "late_night" to "night",
            "fatigue_alert" to "alert",
            "relaxed_cruise" to "calm",
            "city_night_cruise" to "citynight",
            "highway_cruise" to "cool",
            "weekend_outing" to "warm",
            "rainy_commute" to "rainy",
            "snow_drive" to "winter"
        )
    }
}
