package com.music.perception.api

import com.music.core.api.models.StandardizedSignals
import kotlinx.coroutines.flow.Flow

interface IPerceptionEngine {
    val standardizedSignalsFlow: Flow<StandardizedSignals>
    fun warmup()
    fun start()
    fun stop()
    fun destroy()
    fun updateConfig(config: PerceptionConfig)
}
