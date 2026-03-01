package com.example.layer3.api.model

data class ColorConfig(
    val hex: String = "#FFFFFF",
    val rgb: List<Int> = listOf(255, 255, 255),
    val name: String? = null
)

data class ZoneConfig(
    val zone_id: String,
    val enabled: Boolean = true,
    val color: ColorConfig = ColorConfig(),
    val brightness: Double = 1.0,
    val pattern: String = "static",
    val speed: Double = 1.0
)

data class AnimationConfig(
    val type: String = "none",
    val duration_ms: Long = 1000,
    val easing: String = "linear",
    val loop: Boolean = false
)

data class LightingScene(
    val scene_id: String,
    val name: String,
    val zones: List<ZoneConfig> = emptyList(),
    val animation: AnimationConfig? = null,
    val sync_with_music: Boolean = false,
    val sync_intensity: Double = 0.5
)

data class LightingConfig(
    val config_id: String,
    val scene: LightingScene,
    val transition_duration_ms: Long = 500,
    val active: Boolean = true,
    val created_at: Long = System.currentTimeMillis(),
    val metadata: Map<String, Any> = emptyMap()
)

object LightingPatterns {
    const val STATIC = "static"
    const val BREATHING = "breathing"
    const val PULSE = "pulse"
    const val WAVE = "wave"
    const val MUSIC_SYNC = "music_sync"
    const val RAINBOW = "rainbow"
    const val FADE = "fade"
}

object LightingZones {
    const val DASHBOARD = "dashboard"
    const val DOOR_LEFT = "door_left"
    const val DOOR_RIGHT = "door_right"
    const val FOOTWELL = "footwell"
    const val CEILING = "ceiling"
    const val TRUNK = "trunk"
    const val EXTERIOR = "exterior"
}
