package com.example.layer3.api

sealed class Layer3Error : Exception() {
    data class InitializationError(override val message: String) : Layer3Error()
    data class ConfigurationError(override val message: String) : Layer3Error()
    data class RuntimeError(override val message: String) : Layer3Error()
    data class ConnectionError(override val message: String) : Layer3Error()
    data class ContentError(override val message: String, val code: Int = 0) : Layer3Error()
    data class LightingError(override val message: String, val zoneId: String? = null) : Layer3Error()
    data class AudioError(override val message: String) : Layer3Error()
    data class GenerationError(override val message: String, val sceneId: String? = null) : Layer3Error()
    data class ValidationError(override val message: String, val field: String? = null) : Layer3Error()
    data class TimeoutError(override val message: String, val operation: String? = null) : Layer3Error()
}
