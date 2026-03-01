package com.music.perception.api

data class PerceptionConfig(
    val ipCameraUrl: String,
    val ipCameraUsername: String,
    val ipCameraPassword: String,
    val dashScopeApiKey: String,
    val refreshIntervalMs: Long = 3000L
) {
    class Builder {
        private var ipCameraUrl = ""
        private var ipCameraUsername = ""
        private var ipCameraPassword = ""
        private var dashScopeApiKey = ""
        private var refreshIntervalMs = 3000L

        fun setIpCameraUrl(url: String) = apply { this.ipCameraUrl = url }
        fun setIpCameraAuth(username: String, password: String) = apply {
            this.ipCameraUsername = username
            this.ipCameraPassword = password
        }
        fun setDashScopeApiKey(key: String) = apply { this.dashScopeApiKey = key }
        fun setRefreshInterval(ms: Long) = apply { this.refreshIntervalMs = ms }

        fun build() = PerceptionConfig(
            ipCameraUrl, ipCameraUsername, ipCameraPassword, dashScopeApiKey, refreshIntervalMs
        )
    }
}
