package com.music.appmain

import android.app.Application
import com.example.layer3.api.Layer3Config
import com.example.layer3.api.ContentProviderConfig
import com.example.layer3.api.LightingControllerConfig
import com.example.layer3.api.AudioEngineConfig
import com.example.layer3.api.GenerationEngineConfig
import com.example.layer3.sdk.Layer3SDK
import com.music.appmain.controller.DisabledAmbientLightController
import com.music.perception.api.PerceptionConfig

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        val layer3Config = Layer3Config.Builder()
            .setContentProvider(
                ContentProviderConfig(
                    providerType = "local",
                    enableOfflineMode = true
                )
            )
            .setLightingController(
                LightingControllerConfig(
                    controllerType = "disabled",
                    autoReconnect = false
                )
            )
            .setAudioEngine(
                AudioEngineConfig(
                    defaultPreset = "flat",
                    enableSpatialAudio = false
                )
            )
            .setGenerationEngine(
                GenerationEngineConfig(
                    llmApiKey = "sk-fb1a1b32bf914059a043ee4ebd1c845a",
                    llmBaseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1",
                    llmModel = "qwen-plus"
                )
            )
            .setEnableAutoTransition(true)
            .setTransitionDuration(500)
            .build()

        Layer3SDK.init(this, layer3Config)

        disabledAmbientLightController = DisabledAmbientLightController()
    }

    override fun onTerminate() {
        super.onTerminate()
        Layer3SDK.destroy()
    }

    companion object {
        lateinit var disabledAmbientLightController: DisabledAmbientLightController
            private set

        fun getPerceptionConfig(
            ipCameraUrl: String = "",
            ipCameraUsername: String = "",
            ipCameraPassword: String = "",
            dashScopeApiKey: String = "sk-fb1a1b32bf914059a043ee4ebd1c845a",
            refreshIntervalMs: Long = 1000L
        ): PerceptionConfig {
            return PerceptionConfig.Builder()
                .setIpCameraUrl(ipCameraUrl)
                .setIpCameraAuth(ipCameraUsername, ipCameraPassword)
                .setDashScopeApiKey(dashScopeApiKey)
                .setRefreshInterval(refreshIntervalMs)
                .build()
        }

        fun getLayer3Config(
            llmApiKey: String = "sk-fb1a1b32bf914059a043ee4ebd1c845a",
            llmBaseUrl: String = "https://dashscope.aliyuncs.com/compatible-mode/v1",
            llmModel: String = "qwen-plus"
        ): Layer3Config {
            return Layer3Config.Builder()
                .setContentProvider(
                    ContentProviderConfig(
                        providerType = "local",
                        enableOfflineMode = true
                    )
                )
                .setLightingController(
                    LightingControllerConfig(
                        controllerType = "disabled",
                        autoReconnect = false
                    )
                )
                .setGenerationEngine(
                    GenerationEngineConfig(
                        llmApiKey = llmApiKey,
                        llmBaseUrl = llmBaseUrl,
                        llmModel = llmModel
                    )
                )
                .setEnableAutoTransition(true)
                .setTransitionDuration(500)
                .build()
        }
    }
}
