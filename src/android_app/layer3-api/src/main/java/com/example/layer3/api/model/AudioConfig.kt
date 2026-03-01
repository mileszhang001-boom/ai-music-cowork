package com.example.layer3.api.model

data class EqualizerBand(
    val frequency_hz: Int,
    val gain_db: Double = 0.0
)

data class EqualizerConfig(
    val enabled: Boolean = true,
    val bands: List<EqualizerBand> = emptyList(),
    val preset_name: String? = null
)

data class SpatialAudioConfig(
    val enabled: Boolean = false,
    val mode: String = "stereo",
    val room_size: String = "medium",
    val listener_position: List<Double> = listOf(0.5, 0.5)
)

data class VolumeConfig(
    val level: Double = 0.5,
    val muted: Boolean = false,
    val max_limit: Double = 1.0,
    val auto_adjust: Boolean = false
)

data class AudioEnhancements(
    val bass_boost: Double = 0.0,
    val treble_boost: Double = 0.0,
    val surround_enabled: Boolean = false,
    val loudness_enabled: Boolean = true,
    val dynamic_range_compression: Boolean = false
)

data class AudioConfig(
    val config_id: String,
    val equalizer: EqualizerConfig? = null,
    val spatial: SpatialAudioConfig? = null,
    val volume: VolumeConfig = VolumeConfig(),
    val enhancements: AudioEnhancements = AudioEnhancements(),
    val active: Boolean = true,
    val created_at: Long = System.currentTimeMillis(),
    val metadata: Map<String, Any> = emptyMap()
)

object AudioPresets {
    const val FLAT = "flat"
    const val BASS_BOOST = "bass_boost"
    const val TREBLE_BOOST = "treble_boost"
    const val VOCAL = "vocal"
    const val ROCK = "rock"
    const val POP = "pop"
    const val CLASSICAL = "classical"
    const val ELECTRONIC = "electronic"
    const val JAZZ = "jazz"
    const val PODCAST = "podcast"
}

object SpatialModes {
    const val STEREO = "stereo"
    const val SURROUND_5_1 = "surround_5_1"
    const val SURROUND_7_1 = "surround_7_1"
    const val DOLBY_ATMOS = "dolby_atmos"
    const val BINAURAL = "binaural"
}
