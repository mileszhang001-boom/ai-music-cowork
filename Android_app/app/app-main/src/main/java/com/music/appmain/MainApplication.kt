package com.music.appmain

import android.app.Application
import com.example.layer3.api.Layer3Config
import com.example.layer3.sdk.Layer3SDK
import com.music.appmain.controller.DisabledAmbientLightController
import com.music.perception.api.PerceptionConfig

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        val layer3Config = Layer3Config.Builder()
            .setContentProvider(
                Layer3Config.ContentProviderConfig(
                    providerType = "local",
                    enableOfflineMode = true
                )
            )
            .setLightingController(
                Layer3Config.LightingControllerConfig(
                    controllerType = "disabled",
                    autoReconnect = false
                )
            )
            .setAudioEngine(
                Layer3Config.AudioEngineConfig(
                    defaultPreset = "flat",
                    enableSpatialAudio = false
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
            dashScopeApiKey: String = "",
            refreshIntervalMs: Long = 3000L
        ): PerceptionConfig {
            return PerceptionConfig.Builder()
                .setIpCameraUrl(ipCameraUrl)
                .setIpCameraAuth(ipCameraUsername, ipCameraPassword)
                .setDashScopeApiKey(dashScopeApiKey)
                .setRefreshInterval(refreshIntervalMs)
                .build()
        }

        fun getLayer3Config(
            llmApiKey: String = "",
            llmBaseUrl: String = "https://dashscope.aliyuncs.com/compatible-mode/v1",
            llmModel: String = "qwen-plus"
        ): Layer3Config {
            return Layer3Config.Builder()
                .setContentProvider(
                    Layer3Config.ContentProviderConfig(
                        providerType = "local",
                        enableOfflineMode = true
                    )
                )
                .setLightingController(
                    Layer3Config.LightingControllerConfig(
                        controllerType = "disabled",
                        autoReconnect = false
                    )
                )
                .setGenerationEngine(
                    Layer3Config.GenerationEngineConfig(
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
