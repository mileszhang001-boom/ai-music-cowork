package com.music.perception.api

import com.music.perception.api.data.StandardizedSignals
import kotlinx.coroutines.flow.Flow

interface IPerceptionEngine {
    val standardizedSignalsFlow: Flow<StandardizedSignals>
    fun start()
    fun stop()
    fun destroy()
    fun updateConfig(config: PerceptionConfig)
}
