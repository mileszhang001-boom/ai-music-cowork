package com.example.layer1.sdk

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import com.example.layer1.api.IPerceptionEngine
import com.example.layer1.api.Layer1Config

object Layer1SDK {
    private var engine: IPerceptionEngine? = null

    fun init(context: Context, config: Layer1Config, lifecycleOwner: LifecycleOwner) {
        if (engine == null) {
            engine = PerceptionEngine(context, config, lifecycleOwner)
        } else {
            engine?.updateConfig(config)
        }
    }

    fun getEngine(): IPerceptionEngine {
        return engine ?: throw IllegalStateException("Layer1SDK not initialized. Call init() first.")
    }

    fun destroy() {
        engine?.destroy()
        engine = null
    }
}
