package com.music.core.api.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class RawSignal(
    val source: String,
    val type: String,
    val value: JsonObject,
    val timestamp: Long? = null,
    val confidence: Double? = 1.0,
    val metadata: JsonObject? = null
)

@Serializable
data class StandardizedSignals(
    val version: String = "1.0",
    val timestamp: String,
    val signals: Signals,
    val confidence: Confidence
)

@Serializable
data class Signals(
    val vehicle: Vehicle? = null,
    val environment: Environment? = null,
    val external_camera: ExternalCamera? = null,
    val internal_camera: InternalCamera? = null,
    val internal_mic: InternalMic? = null,
    val user_query: JsonObject? = null
)

@Serializable
data class Vehicle(
    val speed_kmh: Double? = null,
    val passenger_count: Int? = null,
    val gear: String? = null
)

@Serializable
data class Environment(
    val time_of_day: Double? = null,
    val weather: String? = null,
    val temperature: Double? = null,
    val date_type: String? = null
)

@Serializable
data class ExternalCamera(
    val primary_color: String? = null,
    val secondary_color: String? = null,
    val brightness: Double? = null,
    val scene_description: String? = null
)

@Serializable
data class InternalCamera(
    val mood: String? = null,
    val confidence: Double? = null,
    val passengers: Passengers? = null
)

@Serializable
data class Passengers(
    val children: Int? = 0,
    val adults: Int? = 0,
    val seniors: Int? = 0
)

@Serializable
data class InternalMic(
    val volume_level: Double? = null,
    val has_voice: Boolean? = null,
    val voice_count: Int? = null,
    val noise_level: Double? = null
)

@Serializable
data class Confidence(
    val overall: Double,
    val by_source: Map<String, Double>? = null
)
