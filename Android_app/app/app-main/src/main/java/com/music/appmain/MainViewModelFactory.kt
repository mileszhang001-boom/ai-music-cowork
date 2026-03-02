package com.music.appmain

import android.app.Application
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.layer3.api.Layer3Config
import com.music.perception.api.PerceptionConfig

class MainViewModelFactory(
    private val application: Application,
    private val lifecycleOwner: LifecycleOwner,
    private val perceptionConfig: PerceptionConfig,
    private val layer3Config: Layer3Config,
    private val llmApiKey: String = "",
    private val llmBaseUrl: String = "https://dashscope.aliyuncs.com/compatible-mode/v1",
    private val llmModel: String = "qwen-plus"
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(
                application = application,
                lifecycleOwner = lifecycleOwner,
                perceptionConfig = perceptionConfig,
                layer3Config = layer3Config,
                llmApiKey = llmApiKey,
                llmBaseUrl = llmBaseUrl,
                llmModel = llmModel
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
