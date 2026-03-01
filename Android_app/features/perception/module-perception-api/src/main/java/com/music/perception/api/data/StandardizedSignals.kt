package com.music.perception.api.data

data class StandardizedSignals(
    val version: String = "1.0",
    val timestamp: String,
    val signals: Signals,
    val confidence: Confidence
)

data class Signals(
    val vehicle: VehicleSignal,
    val environment: EnvironmentSignal,
    val external_camera: ExternalCameraSignal,
    val internal_camera: InternalCameraSignal,
    val internal_mic: InternalMicSignal,
    val user_query: UserQuerySignal? = null
)

data class VehicleSignal(
    val speed_kmh: Double = 0.0,
    val passenger_count: Int = 1,
    val gear: String = "P"
)

data class EnvironmentSignal(
    val time_of_day: Double = 0.5,
    val weather: String = "clear",
    val temperature: Double = 20.0,
    val date_type: String = "weekday"
)

data class ExternalCameraSignal(
    val primary_color: String = "#000000",
    val secondary_color: String = "#FFFFFF",
    val brightness: Double = 0.5,
    val scene_description: String = "unknown"
)

data class InternalCameraSignal(
    val mood: String = "neutral",
    val confidence: Double = 0.8,
    val passengers: PassengersDetail
)

data class PassengersDetail(
    val children: Int = 0,
    val adults: Int = 1,
    val seniors: Int = 0
)

data class InternalMicSignal(
    val volume_level: Double = 0.0,
    val has_voice: Boolean = false,
    val voice_count: Int = 0,
    val noise_level: Double = 0.0
)

data class UserQuerySignal(
    val text: String,
    val intent: String = "creative",
    val confidence: Double = 0.9
)

data class Confidence(
    val overall: Double = 1.0,
    val by_source: Map<String, Double> = emptyMap()
)
