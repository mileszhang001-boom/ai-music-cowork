package com.music.perception.api

sealed class PerceptionError(
    val code: Int,
    open val message: String
) {
    data class CameraError(override val message: String) : PerceptionError(1001, message)
    data class LocationError(override val message: String) : PerceptionError(1002, message)
    data class AudioError(override val message: String) : PerceptionError(1003, message)
    data class AIError(override val message: String) : PerceptionError(2001, message)
    data class NetworkError(override val message: String) : PerceptionError(3001, message)
    data class ConfigError(override val message: String) : PerceptionError(4001, message)
}
