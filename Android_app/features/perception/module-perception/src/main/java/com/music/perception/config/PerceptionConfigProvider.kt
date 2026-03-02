package com.music.perception.config

import com.music.perception.api.PerceptionConfig

object PerceptionConfigProvider {
    
    private const val IP_CAMERA_URL = "http://172.31.2.50:8081/video"
    private const val IP_CAMERA_USERNAME = "admin"
    private const val IP_CAMERA_PASSWORD = "123456"
    private const val DASH_SCOPE_API_KEY = "sk-fb1a1b32bf914059a043ee4ebd1c845a"
    private const val REFRESH_INTERVAL_MS = 3000L
    
    private var customConfig: PerceptionConfig? = null
    
    fun getConfig(): PerceptionConfig {
        return customConfig ?: PerceptionConfig(
            ipCameraUrl = IP_CAMERA_URL,
            ipCameraUsername = IP_CAMERA_USERNAME,
            ipCameraPassword = IP_CAMERA_PASSWORD,
            dashScopeApiKey = DASH_SCOPE_API_KEY,
            refreshIntervalMs = REFRESH_INTERVAL_MS
        )
    }
    
    fun setCustomConfig(config: PerceptionConfig) {
        customConfig = config
    }
    
    fun setIpCameraUrl(url: String) {
        val current = getConfig()
        customConfig = current.copy(ipCameraUrl = url)
    }
    
    fun setIpCameraAuth(username: String, password: String) {
        val current = getConfig()
        customConfig = current.copy(
            ipCameraUsername = username,
            ipCameraPassword = password
        )
    }
    
    fun setDashScopeApiKey(key: String) {
        val current = getConfig()
        customConfig = current.copy(dashScopeApiKey = key)
    }
}
