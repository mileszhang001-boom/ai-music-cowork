package com.example.layer3.api

data class Layer3Config(
    val contentProvider: ContentProviderConfig = ContentProviderConfig(),
    val lightingController: LightingControllerConfig = LightingControllerConfig(),
    val audioEngine: AudioEngineConfig = AudioEngineConfig(),
    val generationEngine: GenerationEngineConfig = GenerationEngineConfig(),
    val moodMappingPath: String = "",
    val enableAutoTransition: Boolean = true,
    val transitionDurationMs: Long = 500
) {
    class Builder {
        private var contentProvider = ContentProviderConfig()
        private var lightingController = LightingControllerConfig()
        private var audioEngine = AudioEngineConfig()
        private var generationEngine = GenerationEngineConfig()
        private var moodMappingPath = ""
        private var enableAutoTransition = true
        private var transitionDurationMs = 500L

        fun setContentProvider(config: ContentProviderConfig) = apply { this.contentProvider = config }
        fun setLightingController(config: LightingControllerConfig) = apply { this.lightingController = config }
        fun setAudioEngine(config: AudioEngineConfig) = apply { this.audioEngine = config }
        fun setGenerationEngine(config: GenerationEngineConfig) = apply { this.generationEngine = config }
        fun setMoodMappingPath(path: String) = apply { this.moodMappingPath = path }
        fun setEnableAutoTransition(enabled: Boolean) = apply { this.enableAutoTransition = enabled }
        fun setTransitionDuration(ms: Long) = apply { this.transitionDurationMs = ms }

        fun build() = Layer3Config(
            contentProvider, lightingController, audioEngine, generationEngine,
            moodMappingPath, enableAutoTransition, transitionDurationMs
        )
    }
}

data class ContentProviderConfig(
    val providerType: String = "local",
    val apiKey: String = "",
    val apiSecret: String = "",
    val baseUrl: String = "",
    val cacheSizeMb: Int = 100,
    val enableOfflineMode: Boolean = true
)

data class LightingControllerConfig(
    val controllerType: String = "mock",
    val connectionAddress: String = "",
    val connectionPort: Int = 0,
    val autoReconnect: Boolean = true,
    val reconnectIntervalMs: Long = 3000,
    val timeoutMs: Long = 5000
)

data class AudioEngineConfig(
    val defaultPreset: String = "flat",
    val enableSpatialAudio: Boolean = false,
    val defaultSpatialMode: String = "stereo",
    val maxBassBoost: Double = 0.5,
    val maxTrebleBoost: Double = 0.5
)

data class GenerationEngineConfig(
    val llmApiKey: String = "",
    val llmBaseUrl: String = "",
    val llmModel: String = "qwen-plus",
    val maxTokens: Int = 2000,
    val temperature: Double = 0.7,
    val templatePath: String = ""
)
