package com.example.layer1.api

import com.example.layer1.api.data.StandardizedSignals
import kotlinx.coroutines.flow.Flow

interface IPerceptionEngine {
    val standardizedSignalsFlow: Flow<StandardizedSignals>
    fun start()
    fun stop()
    fun destroy()
    fun updateConfig(config: Layer1Config)
}
