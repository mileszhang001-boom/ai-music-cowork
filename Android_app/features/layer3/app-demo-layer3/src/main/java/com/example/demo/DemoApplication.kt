package com.example.demo

import android.app.Application
import com.example.layer3.api.Layer3Config
import com.example.layer3.sdk.Layer3SDK
import com.example.demo.controller.MockAmbientLightController

class DemoApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        val config = Layer3Config(
            contentProvider = Layer3Config.ContentProviderConfig(
                providerType = "local",
                enableOfflineMode = true
            ),
            lightingController = Layer3Config.LightingControllerConfig(
                controllerType = "mock",
                autoReconnect = true
            )
        )
        
        Layer3SDK.init(this, config)
        
        mockAmbientLightController = MockAmbientLightController()
    }
    
    override fun onTerminate() {
        super.onTerminate()
        Layer3SDK.destroy()
    }
    
    companion object {
        lateinit var mockAmbientLightController: MockAmbientLightController
            private set
    }
}
