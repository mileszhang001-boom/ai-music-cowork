package com.example.layer3.sdk

import android.content.Context
import com.example.layer3.api.*
import com.example.layer3.sdk.engine.AudioEngine
import com.example.layer3.sdk.engine.ContentEngine
import com.example.layer3.sdk.engine.GenerationEngine
import com.example.layer3.sdk.engine.LightingEngine
import com.example.layer3.sdk.util.Logger

object Layer3SDK {
    private var contentEngine: ContentEngine? = null
    private var lightingEngine: LightingEngine? = null
    private var audioEngine: AudioEngine? = null
    private var generationEngine: GenerationEngine? = null
    private var isInitialized = false
    private var config: Layer3Config? = null

    fun init(context: Context, config: Layer3Config) {
        if (isInitialized) {
            Logger.i("Layer3SDK: Already initialized, updating config")
            updateConfig(config)
            return
        }

        this.config = config
        val appContext = context.applicationContext

        contentEngine = ContentEngine(appContext)
        lightingEngine = LightingEngine(appContext)
        audioEngine = AudioEngine(appContext)
        generationEngine = GenerationEngine(
            appContext,
            contentEngine!!,
            lightingEngine!!,
            audioEngine!!
        )

        isInitialized = true
        Logger.i("Layer3SDK: Initialized successfully")
    }

    fun getContentEngine(): IContentEngine {
        return contentEngine ?: throw IllegalStateException(
            "Layer3SDK not initialized. Call init() first."
        )
    }

    fun getLightingEngine(): ILightingEngine {
        return lightingEngine ?: throw IllegalStateException(
            "Layer3SDK not initialized. Call init() first."
        )
    }

    fun getAudioEngine(): IAudioEngine {
        return audioEngine ?: throw IllegalStateException(
            "Layer3SDK not initialized. Call init() first."
        )
    }

    fun getGenerationEngine(): IGenerationEngine {
        return generationEngine ?: throw IllegalStateException(
            "Layer3SDK not initialized. Call init() first."
        )
    }

    fun isInitialized(): Boolean = isInitialized

    fun getConfig(): Layer3Config? = config

    fun updateConfig(newConfig: Layer3Config) {
        this.config = newConfig
        generationEngine?.updateConfig(newConfig)
        Logger.i("Layer3SDK: Config updated")
    }

    fun destroy() {
        contentEngine?.destroy()
        lightingEngine?.destroy()
        audioEngine?.destroy()
        generationEngine?.destroy()

        contentEngine = null
        lightingEngine = null
        audioEngine = null
        generationEngine = null
        isInitialized = false
        config = null

        Logger.i("Layer3SDK: Destroyed")
    }
}
