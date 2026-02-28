package com.music.core.api.models

import kotlinx.serialization.Serializable

@Serializable
data class EffectCommands(
    val version: String = "1.0",
    val scene_id: String,
    val commands: Commands,
    val execution_report: ExecutionReport? = null
)

@Serializable
data class Commands(
    val content: ContentCommand? = null,
    val lighting: LightingCommand? = null,
    val audio: AudioCommand? = null
)

@Serializable
data class ContentCommand(
    val action: String,
    val playlist: List<Track>? = null,
    val play_mode: String? = null
)

@Serializable
data class Track(
    val track_id: String,
    val title: String,
    val artist: String,
    val duration_sec: Int,
    val energy: Double? = null
)

@Serializable
data class LightingCommand(
    val action: String,
    val theme: String? = null,
    val colors: List<String>? = null,
    val pattern: String? = null,
    val intensity: Double? = null
)

@Serializable
data class AudioCommand(
    val action: String,
    val preset: String? = null,
    val settings: AudioSettings? = null
)

@Serializable
data class AudioSettings(
    val eq: EqSettings? = null,
    val volume_db: Int? = null
)

@Serializable
data class EqSettings(
    val bass: Int? = 0,
    val mid: Int? = 0,
    val treble: Int? = 0
)

@Serializable
data class ExecutionReport(
    val status: String,
    val details: Map<String, String>? = null
)
