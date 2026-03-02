package com.music.appmain

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.example.layer3.api.Layer3Config
import com.example.layer3.api.model.EffectCommands
import com.example.layer3.sdk.Layer3SDK
import com.music.core.api.models.SceneDescriptor
import com.music.core.api.models.StandardizedSignals
import com.music.perception.api.PerceptionConfig
import com.music.perception.sdk.PerceptionEngine
import com.music.semantic.EngineState
import com.music.semantic.SemanticEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    application: Application,
    private val lifecycleOwner: LifecycleOwner,
    private val perceptionConfig: PerceptionConfig,
    private val layer3Config: Layer3Config,
    private val llmApiKey: String = "",
    private val llmBaseUrl: String = "https://dashscope.aliyuncs.com/compatible-mode/v1",
    private val llmModel: String = "qwen-plus"
) : AndroidViewModel(application), TtsCallback {

    companion object {
        private const val TAG = "MainViewModel"
    }

    private val context = application.applicationContext

    private var perceptionEngine: PerceptionEngine? = null
    private var semanticEngine: SemanticEngine? = null

    private val ttsService: TtsService = TtsService(context)
    private val audioDuckManager: AudioDuckManager = AudioDuckManager(context)

    private val _standardizedSignalsFlow = MutableStateFlow<StandardizedSignals?>(null)
    val standardizedSignalsFlow: StateFlow<StandardizedSignals?> = _standardizedSignalsFlow.asStateFlow()

    private val _sceneDescriptorFlow = MutableStateFlow<SceneDescriptor?>(null)
    val sceneDescriptorFlow: StateFlow<SceneDescriptor?> = _sceneDescriptorFlow.asStateFlow()

    private val _effectCommandsFlow = MutableStateFlow<EffectCommands?>(null)
    val effectCommandsFlow: StateFlow<EffectCommands?> = _effectCommandsFlow.asStateFlow()

    private val _engineStateFlow = MutableStateFlow<EngineState>(EngineState.Idle)
    val engineStateFlow: StateFlow<EngineState> = _engineStateFlow.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunningFlow: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _isInitialized = MutableStateFlow(false)
    val isInitializedFlow: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private var perceptionJob: Job? = null
    private var semanticJob: Job? = null
    private var effectJob: Job? = null

    init {
        initializeEngines()
        initializeTts()
    }

    private fun initializeTts() {
        ttsService.setCallback(this)
        ttsService.init()
    }

    override fun onInitSuccess() {
        Log.i(TAG, "TTS 初始化成功")
    }

    override fun onInitFailed(message: String) {
        Log.e(TAG, "TTS 初始化失败: $message")
    }

    override fun onSpeakStart(text: String) {
        Log.i(TAG, "TTS 开始播报: $text")
    }

    override fun onSpeakDone() {
        Log.i(TAG, "TTS 播报完成")
        audioDuckManager.unduck()
    }

    override fun onError(message: String) {
        Log.e(TAG, "TTS 错误: $message")
        audioDuckManager.unduck()
    }

    private fun initializeEngines() {
        perceptionEngine = PerceptionEngine(
            context = context,
            config = perceptionConfig,
            lifecycleOwner = lifecycleOwner
        )

        semanticEngine = SemanticEngine(
            context = context,
            llmApiKey = llmApiKey,
            llmBaseUrl = llmBaseUrl,
            llmModel = llmModel
        )

        val semanticInitialized = semanticEngine?.initialize() ?: false

        if (!Layer3SDK.isInitialized()) {
            Layer3SDK.init(context, layer3Config)
        }

        _isInitialized.value = semanticInitialized && Layer3SDK.isInitialized()

        setupFlowObservers()
    }

    private fun setupFlowObservers() {
        perceptionJob = viewModelScope.launch {
            perceptionEngine?.standardizedSignalsFlow?.collect { signals ->
                _standardizedSignalsFlow.value = signals
                semanticEngine?.processSignals(signals)
            }
        }

        semanticJob = viewModelScope.launch {
            semanticEngine?.sceneDescriptorFlow?.collect { descriptor ->
                descriptor?.let {
                    _sceneDescriptorFlow.value = it
                    _engineStateFlow.value = EngineState.Ready(it)
                    generateEffects(it)
                    handleAnnouncement(it)
                }
            }
        }

        effectJob = viewModelScope.launch {
            Layer3SDK.getGenerationEngine().effectCommandsFlow.collect { commands ->
                _effectCommandsFlow.value = commands
            }
        }

        viewModelScope.launch {
            semanticEngine?.engineStateFlow?.collect { state ->
                _engineStateFlow.value = state
            }
        }
    }

    private suspend fun generateEffects(scene: SceneDescriptor) {
        Layer3SDK.getGenerationEngine().generateEffects(scene)
    }

    private fun handleAnnouncement(scene: SceneDescriptor) {
        val announcement = scene.announcement
        if (!announcement.isNullOrBlank()) {
            Log.i(TAG, "准备播报 announcement: $announcement")
            if (ttsService.isInitialized.value) {
                audioDuckManager.duck()
                val success = ttsService.speak(announcement)
                if (!success) {
                    Log.e(TAG, "TTS 播报失败")
                    audioDuckManager.unduck()
                }
            } else {
                Log.w(TAG, "TTS 尚未初始化，无法播报")
            }
        }
    }

    fun start() {
        if (_isRunning.value) return

        perceptionEngine?.start()
        Layer3SDK.getGenerationEngine().start()
        _isRunning.value = true
    }

    fun stop() {
        perceptionEngine?.stop()
        Layer3SDK.getGenerationEngine().stop()
        _isRunning.value = false
    }

    fun updatePerceptionConfig(config: PerceptionConfig) {
        perceptionEngine?.updateConfig(config)
    }

    fun updateLayer3Config(config: Layer3Config) {
        Layer3SDK.updateConfig(config)
    }

    fun getCurrentScene(): SceneDescriptor? {
        return semanticEngine?.getCurrentScene()
    }

    fun forceUpdateScene(descriptor: SceneDescriptor) {
        semanticEngine?.forceUpdate(descriptor)
    }

    fun resetScene() {
        semanticEngine?.reset()
        _sceneDescriptorFlow.value = null
        _effectCommandsFlow.value = null
    }

    override fun onCleared() {
        super.onCleared()
        destroy()
    }

    fun destroy() {
        perceptionJob?.cancel()
        semanticJob?.cancel()
        effectJob?.cancel()

        perceptionEngine?.destroy()
        semanticEngine?.reset()
        Layer3SDK.destroy()

        ttsService.shutdown()
        audioDuckManager.release()

        perceptionEngine = null
        semanticEngine = null

        _isRunning.value = false
        _isInitialized.value = false
    }
}
