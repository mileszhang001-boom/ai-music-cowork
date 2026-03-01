package com.example.layer1.api

sealed class Layer1Error : Exception() {
    data class InitializationError(override val message: String) : Layer1Error()
    data class ConfigurationError(override val message: String) : Layer1Error()
    data class RuntimeError(override val message: String) : Layer1Error()
}
