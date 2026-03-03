package com.music.appmain

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import com.example.layer3.api.Layer3Config
import com.music.core.api.models.EffectCommands
import com.example.layer3.sdk.Layer3SDK
import com.music.core.api.models.SceneDescriptor
import com.music.core.api.models.StandardizedSignals
import com.music.localmusic.LocalMusicConfig
import com.music.localmusic.LocalMusicIndex
import com.music.localmusic.player.MusicPlayer
import com.music.localmusic.player.PlayerState
import com.music.localmusic.player.PlaybackInfo
import com.music.localmusic.player.RepeatMode
import com.music.localmusic.player.ShuffleMode
import com.music.localmusic.util.CoverGenerator
import com.music.localmusic.models.Track
import com.music.appmain.voice.VoiceInputState
import com.music.perception.api.PerceptionConfig
import com.music.perception.sdk.PerceptionEngine
import com.music.semantic.EngineState
import com.music.semantic.SemanticEngine
import com.music.appmain.voice.VoiceInputCallback
import com.music.appmain.voice.VoiceInputService
import com.music.appmain.TtsCallback
import com.music.appmain.TtsService
import com.music.appmain.audio.AudioDuckManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(
    application: Application,
    private val lifecycleOwner: LifecycleOwner,
    private val perceptionConfig: PerceptionConfig,
    private val layer3Config: Layer3Config,
    private val llmApiKey: String = "",
    private val llmBaseUrl: String = "https://dashscope.aliyuncs.com/compatible-mode/v1",
    private val llmModel: String = "qwen-plus"
) : AndroidViewModel(application), TtsCallback, VoiceInputCallback {

    companion object {
        private const val TAG = "MainViewModel"
    }

    private val context = application.applicationContext

    private var perceptionEngine: PerceptionEngine? = null
    private var semanticEngine: SemanticEngine? = null

    private val ttsService: TtsService = TtsService(context)
    private val audioDuckManager: AudioDuckManager = AudioDuckManager(context)
    private val voiceInputService: VoiceInputService = VoiceInputService(context)
    
    private var musicPlayer: MusicPlayer? = null
    private var localMusicIndex: LocalMusicIndex? = null

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
    
    private val _voiceInputState = MutableStateFlow<VoiceInputState>(VoiceInputState.Idle)
    val voiceInputStateFlow: StateFlow<VoiceInputState> = _voiceInputState.asStateFlow()
    
    private val _voiceRecognizedText = MutableStateFlow("")
    val voiceRecognizedTextFlow: StateFlow<String> = _voiceRecognizedText.asStateFlow()
    
    private val _voiceAmplitude = MutableStateFlow(0f)
    val voiceAmplitudeFlow: StateFlow<Float> = _voiceAmplitude.asStateFlow()
    
    private val _playerState = MutableStateFlow<PlayerState>(PlayerState.Idle)
    val playerStateFlow: StateFlow<PlayerState> = _playerState.asStateFlow()
    
    private val _playbackInfo = MutableStateFlow(PlaybackInfo())
    val playbackInfoFlow: StateFlow<PlaybackInfo> = _playbackInfo.asStateFlow()
    
    private val _localMusicReady = MutableStateFlow(false)
    val localMusicReadyFlow: StateFlow<Boolean> = _localMusicReady.asStateFlow()
    
    private val _trackCount = MutableStateFlow(0)
    val trackCountFlow: StateFlow<Int> = _trackCount.asStateFlow()
    
    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatModeFlow: StateFlow<RepeatMode> = _repeatMode.asStateFlow()
    
    private val _shuffleMode = MutableStateFlow(false)
    val shuffleModeFlow: StateFlow<Boolean> = _shuffleMode.asStateFlow()
    
    private val _currentAlbumArt = MutableStateFlow<Bitmap?>(null)
    val currentAlbumArtFlow: StateFlow<Bitmap?> = _currentAlbumArt.asStateFlow()
    
    private val _playlistSize = MutableStateFlow(0)
    val playlistSizeFlow: StateFlow<Int> = _playlistSize.asStateFlow()
    
    private val _playlistIndex = MutableStateFlow(0)
    val playlistIndexFlow: StateFlow<Int> = _playlistIndex.asStateFlow()
    
    private val _playlist = MutableStateFlow<List<Track>>(emptyList())
    val playlistFlow: StateFlow<List<Track>> = _playlist.asStateFlow()

    private val _audioEnhanceEnabled = MutableStateFlow(true)
    val audioEnhanceEnabledFlow: StateFlow<Boolean> = _audioEnhanceEnabled.asStateFlow()

    private var lastAudioCommand: com.music.core.api.models.AudioCommand? = null

    private var perceptionJob: Job? = null
    private var semanticJob: Job? = null
    private var effectJob: Job? = null

    init {
        initializeEngines()
        initializeTts()
        initializeVoiceInput()
        initializeMusicPlayer()
    }
    
    private fun initializeVoiceInput() {
        voiceInputService.setCallback(this)
        voiceInputService.createSpeechRecognizer()
        Log.i(TAG, "语音输入服务初始化完成")
    }
    
    private fun initializeMusicPlayer() {
        musicPlayer = MusicPlayer(context)
        
        audioDuckManager.setVolumeCallback(object : AudioDuckManager.VolumeCallback {
            override fun onDuckVolume(ratio: Float) {
                viewModelScope.launch(Dispatchers.Main) {
                    val baseVol = getCurrentVolume()
                    musicPlayer?.setVolume(baseVol * ratio)
                }
            }
            override fun onRestoreVolume() {
                viewModelScope.launch(Dispatchers.Main) {
                    musicPlayer?.setVolume(getCurrentVolume())
                }
            }
        })
        
        musicPlayer?.setListener(object : MusicPlayer.Listener {
            override fun onStateChanged(state: PlayerState) {
                _playerState.value = state
                updatePlaylistIndex()
            }
            override fun onTrackChanged(track: Track) {
                _currentAlbumArt.value = musicPlayer?.getCurrentAlbumArt()
                updatePlaylistIndex()
            }
            override fun onPositionChanged(position: Long, duration: Long) {}
            override fun onBuffering(isBuffering: Boolean) {}
            override fun onError(error: String) {}
            override fun onPlaybackCompleted() {
                updatePlaylistIndex()
            }
        })
        
        viewModelScope.launch {
            musicPlayer?.playerState?.collect { state ->
                _playerState.value = state
            }
        }
        
        viewModelScope.launch {
            musicPlayer?.playbackInfo?.collect { info ->
                _playbackInfo.value = info
            }
        }
        
        // 使用 assets 模式加载 index.json，音乐文件存储在 /data/local/tmp/aimusic/
        val publicMusicDir = "/data/local/tmp/aimusic"
        val config = LocalMusicConfig(
            storagePath = publicMusicDir,
            indexJsonPath = "index.json"  // assets 中的文件名
        )
        LocalMusicIndex.setConfig(config)
        localMusicIndex = LocalMusicIndex.getInstance(context)
        val indexInitialized = localMusicIndex?.initialize() ?: false
        _localMusicReady.value = indexInitialized
        _trackCount.value = localMusicIndex?.getTrackCount() ?: 0
        
        if (indexInitialized) {
            Log.i(TAG, "本地音乐索引初始化成功（从 assets 加载），共 ${_trackCount.value} 首曲目")
        } else {
            Log.w(TAG, "本地音乐索引初始化失败")
        }
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

    override fun onReadyForSpeech() {
        Log.i(TAG, "语音输入: 准备就绪")
        _voiceInputState.value = VoiceInputState.Listening()
    }

    override fun onBeginningOfSpeech() {
        Log.i(TAG, "语音输入: 开始说话")
    }

    override fun onRmsChanged(rmsdB: Float) {
        val normalizedAmplitude = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
        _voiceAmplitude.value = normalizedAmplitude
        _voiceInputState.value = VoiceInputState.Processing((normalizedAmplitude * 100).toInt())
    }

    override fun onEndOfSpeech() {
        Log.i(TAG, "语音输入: 说话结束")
        _voiceInputState.value = VoiceInputState.Processing()
    }

    override fun onResult(text: String) {
        Log.i(TAG, "语音输入识别结果: $text")
        _voiceRecognizedText.value = text
        _voiceInputState.value = VoiceInputState.Result(text)
        
        if (text.isNotBlank()) {
            processVoiceQuery(text)
        }
    }

    override fun onVoiceError(message: String) {
        Log.e(TAG, "语音输入错误: $message")
        _voiceInputState.value = VoiceInputState.Error(message)
    }

    override fun onError(error: Int) {
        val errorMessage = when (error) {
            android.speech.SpeechRecognizer.ERROR_AUDIO -> "音频错误"
            android.speech.SpeechRecognizer.ERROR_CLIENT -> "客户端错误"
            android.speech.SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "权限不足"
            android.speech.SpeechRecognizer.ERROR_NETWORK -> "网络错误"
            android.speech.SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "网络超时"
            android.speech.SpeechRecognizer.ERROR_NO_MATCH -> "无法识别"
            android.speech.SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "识别器忙"
            android.speech.SpeechRecognizer.ERROR_SERVER -> "服务器错误"
            android.speech.SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "语音超时"
            else -> "未知错误: $error"
        }
        onVoiceError(errorMessage)
    }

    fun startVoiceInput() {
        if (_isRunning.value) {
            stop()
        }
        
        audioDuckManager.duck()
        voiceInputService.startListening()
        _voiceInputState.value = VoiceInputState.Listening()
        _voiceRecognizedText.value = ""
        Log.i(TAG, "开始语音输入")
    }

    fun stopVoiceInput() {
        voiceInputService.stopListening()
        audioDuckManager.unduck()
        Log.i(TAG, "停止语音输入")
    }

    fun cancelVoiceInput() {
        voiceInputService.cancel()
        audioDuckManager.unduck()
        _voiceInputState.value = VoiceInputState.Idle
        _voiceRecognizedText.value = ""
        Log.i(TAG, "取消语音输入")
    }

    private fun processVoiceQuery(query: String) {
        Log.i(TAG, "处理语音查询: $query")
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val signals = createSignalsFromVoiceQuery(query)
                _standardizedSignalsFlow.value = signals
                
                val externalCamera = signals.signals.external_camera
                if (externalCamera != null) {
                    val primaryColor = externalCamera.primary_color
                    val secondaryColor = externalCamera.secondary_color
                    if (!primaryColor.isNullOrBlank()) {
                        CoverGenerator.setSceneColors(primaryColor, secondaryColor ?: primaryColor)
                    }
                }
                
                semanticEngine?.processSignals(signals)
            } catch (e: Exception) {
                Log.e(TAG, "处理语音查询失败: ${e.message}")
            }
        }
    }

    private fun createSignalsFromVoiceQuery(query: String): StandardizedSignals {
        val lowerQuery = query.lowercase()
        
        val (mood, sceneDescription, primaryColor, secondaryColor) = when {
            lowerQuery.contains("伤心") || lowerQuery.contains("难过") || lowerQuery.contains("emo") -> {
                Tuple4("sad", "rainy_night_city", "#1A237E", "#4A148C")
            }
            lowerQuery.contains("开心") || lowerQuery.contains("高兴") || lowerQuery.contains("快乐") -> {
                Tuple4("happy", "sunny_day", "#FFCA28", "#FF6F00")
            }
            lowerQuery.contains("浪漫") || lowerQuery.contains("约会") || lowerQuery.contains("情侣") -> {
                Tuple4("romantic", "city_night_lights", "#AD1457", "#880E4F")
            }
            lowerQuery.contains("累") || lowerQuery.contains("疲劳") || lowerQuery.contains("困") -> {
                Tuple4("tired", "highway_monotonous", "#FF6D00", "#FFAB00")
            }
            lowerQuery.contains("放松") || lowerQuery.contains("轻松") || lowerQuery.contains("休闲") -> {
                Tuple4("relaxed", "coastal_highway", "#00B8D4", "#80DEEA")
            }
            lowerQuery.contains("兴奋") || lowerQuery.contains("激动") || lowerQuery.contains("嗨") -> {
                Tuple4("excited", "city_evening", "#D500F9", "#EA80FC")
            }
            lowerQuery.contains("儿童") || lowerQuery.contains("小孩") || lowerQuery.contains("孩子") -> {
                Tuple4("happy", "suburban_street", "#FF6F00", "#FFCA28")
            }
            lowerQuery.contains("流行") || lowerQuery.contains("pop") -> {
                Tuple4("neutral", "highway_drive", "#00E5FF", "#18FFFF")
            }
            else -> {
                Tuple4("neutral", "city_drive", "#607D8B", "#455A64")
            }
        }
        
        val passengerCount = when {
            lowerQuery.contains("我们") || lowerQuery.contains("一起") -> 2
            lowerQuery.contains("全家") || lowerQuery.contains("家人") -> 4
            else -> 1
        }
        
        return StandardizedSignals(
            timestamp = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).format(java.util.Date()),
            signals = com.music.core.api.models.Signals(
                vehicle = com.music.core.api.models.Vehicle(
                    speed_kmh = 45.0,
                    passenger_count = passengerCount,
                    gear = "D"
                ),
                environment = com.music.core.api.models.Environment(
                    time_of_day = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY).toDouble(),
                    weather = "clear",
                    temperature = 22.0,
                    date_type = "weekday"
                ),
                external_camera = com.music.core.api.models.ExternalCamera(
                    scene_description = sceneDescription,
                    primary_color = primaryColor,
                    secondary_color = secondaryColor,
                    brightness = 0.7
                ),
                internal_camera = com.music.core.api.models.InternalCamera(
                    mood = mood,
                    confidence = 0.85,
                    passengers = com.music.core.api.models.Passengers(
                        children = if (lowerQuery.contains("儿童") || lowerQuery.contains("小孩")) 1 else 0,
                        adults = passengerCount,
                        seniors = 0
                    )
                ),
                internal_mic = com.music.core.api.models.InternalMic(
                    volume_level = 0.3,
                    has_voice = true,
                    voice_count = passengerCount,
                    noise_level = 0.2
                )
            ),
            confidence = com.music.core.api.models.Confidence(overall = 0.85)
        )
    }

    private data class Tuple4<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    private fun initializeEngines() {
        Log.d(TAG, "开始初始化引擎...")
        
        perceptionEngine = PerceptionEngine(
            context = context,
            config = perceptionConfig,
            lifecycleOwner = lifecycleOwner
        )
        Log.d(TAG, "PerceptionEngine 创建完成")

        semanticEngine = SemanticEngine(
            context = context,
            llmApiKey = llmApiKey,
            llmBaseUrl = llmBaseUrl,
            llmModel = llmModel
        )
        Log.d(TAG, "SemanticEngine 创建完成")

        val semanticInitialized = semanticEngine?.initialize() ?: false
        Log.i(TAG, "SemanticEngine 初始化结果: $semanticInitialized")

        if (!Layer3SDK.isInitialized()) {
            Layer3SDK.init(context, layer3Config)
        }
        Log.i(TAG, "Layer3SDK 初始化完成: ${Layer3SDK.isInitialized()}")

        _isInitialized.value = semanticInitialized && Layer3SDK.isInitialized()
        Log.i(TAG, "引擎初始化完成，整体状态: ${_isInitialized.value}")

        setupFlowObservers()
        
        perceptionEngine?.warmup()
        Log.i(TAG, "感知引擎预热已启动")
    }

    private fun setupFlowObservers() {
        perceptionJob = viewModelScope.launch {
            perceptionEngine?.standardizedSignalsFlow?.collect { signals ->
                signals?.let {
                    Log.d(TAG, "收到感知信号: speed=${it.signals.vehicle?.speed_kmh}, weather=${it.signals.environment?.weather}")
                    _standardizedSignalsFlow.value = it
                    
                    val externalCamera = it.signals.external_camera
                    if (externalCamera != null) {
                        val primaryColor = externalCamera.primary_color
                        val secondaryColor = externalCamera.secondary_color
                        if (!primaryColor.isNullOrBlank()) {
                            CoverGenerator.setSceneColors(primaryColor, secondaryColor ?: primaryColor)
                            Log.i(TAG, "从感知信号设置封面颜色: primary=$primaryColor, secondary=$secondaryColor")
                        }
                    }
                    
                    semanticEngine?.processSignals(it)
                }
            }
        }

        semanticJob = viewModelScope.launch {
            semanticEngine?.sceneDescriptorFlow?.collect { descriptor ->
                descriptor?.let {
                    Log.i(TAG, "收到场景描述: ${it.scene_name}, type=${it.scene_type}")
                    _sceneDescriptorFlow.value = it
                    _engineStateFlow.value = EngineState.Ready(it)
                    generateEffects(it)
                    handleAnnouncement(it)
                }
            }
        }

        effectJob = viewModelScope.launch {
            Layer3SDK.getGenerationEngine().effectCommandsFlow.collect { commands ->
                commands?.let {
                    Log.i(TAG, "收到效果命令: scene_id=${it.scene_id}")
                    _effectCommandsFlow.value = it
                    
                    val lightingCommand = it.commands?.lighting
                    val colors = lightingCommand?.colors
                    if (lightingCommand != null && colors != null && colors.isNotEmpty()) {
                        val primaryColor = colors[0]
                        val secondaryColor = if (colors.size > 1) colors[1] else primaryColor
                        com.music.localmusic.util.CoverGenerator.setSceneColors(primaryColor, secondaryColor)
                        Log.i(TAG, "设置封面颜色: primary=$primaryColor, secondary=$secondaryColor")
                    }
                    
                    it.commands?.audio?.let { audioCommand ->
                        lastAudioCommand = audioCommand
                    }
                    
                    it.commands?.content?.playlist?.let { playlist ->
                        Log.i(TAG, "Layer3 播放列表: ${playlist.size} 首曲目")
                        delay(100)
                        playLayer3Playlist(playlist)
                        delay(300)
                        lastAudioCommand?.let { cmd -> applyAudioCommand(cmd) }
                    }
                }
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
        Layer3SDK.getContentEngine().stop()
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
        Layer3SDK.getContentEngine().reset()
        _sceneDescriptorFlow.value = null
        _effectCommandsFlow.value = null
    }
    
    fun playRandomTrack() {
        val tracks = localMusicIndex?.getAllTracks() ?: return
        if (tracks.isEmpty()) {
            Log.w(TAG, "没有可播放的曲目")
            return
        }
        
        val randomTrack = tracks.random()
        val storagePath = LocalMusicIndex.getConfig().storagePath
        val filePath = "$storagePath/music/${randomTrack.filePath}"
        
        Log.i(TAG, "播放随机曲目: ${randomTrack.title} - ${randomTrack.artist}, filePath: $filePath")
        musicPlayer?.play(randomTrack, filePath)
    }
    
    fun playAllTracks() {
        val tracks = localMusicIndex?.getAllTracks() ?: return
        if (tracks.isEmpty()) {
            Log.w(TAG, "没有可播放的曲目")
            return
        }
        
        val storagePath = LocalMusicIndex.getConfig().storagePath
        
        Log.i(TAG, "播放全部 ${tracks.size} 首曲目")
        musicPlayer?.play(tracks, startIndex = 0) { track ->
            "$storagePath/music/${track.filePath}"
        }
    }
    
    fun pause() {
        musicPlayer?.pause()
    }
    
    fun resume() {
        musicPlayer?.resume()
    }
    
    fun next() {
        musicPlayer?.next()
    }
    
    fun previous() {
        musicPlayer?.previous()
    }
    
    fun seekTo(position: Long) {
        musicPlayer?.seekTo(position)
    }
    
    fun stopPlayback() {
        musicPlayer?.stop()
    }

    fun toggleAudioEnhance() {
        val newState = !_audioEnhanceEnabled.value
        _audioEnhanceEnabled.value = newState
        Log.i(TAG, "音效增强: ${if (newState) "开启" else "关闭"}")

        if (newState) {
            lastAudioCommand?.let { applyAudioCommand(it) }
                ?: Log.w(TAG, "音效增强开启但无音频命令")
        } else {
            musicPlayer?.resetAudioEffects()
        }
    }

    private fun getCurrentVolume(): Float {
        val cmd = lastAudioCommand
        return if (cmd != null && _audioEnhanceEnabled.value) {
            val volumeDb = cmd.settings?.volume_db ?: 0
            (1.0f * Math.pow(10.0, volumeDb / 10.0).toFloat()).coerceIn(0.3f, 3.0f)
        } else {
            1.0f
        }
    }

    private fun applyAudioCommand(audioCommand: com.music.core.api.models.AudioCommand) {
        if (_audioEnhanceEnabled.value) {
            val volumeDb = audioCommand.settings?.volume_db ?: 0
            val volumeGain = Math.pow(10.0, volumeDb / 10.0).toFloat()
            val finalVolume = (1.0f * volumeGain).coerceIn(0.3f, 3.0f)
            musicPlayer?.setVolume(finalVolume)

            val eq = audioCommand.settings?.eq
            if (eq != null) {
                musicPlayer?.setEqEnabled(true)
                musicPlayer?.setEq(eq.bass ?: 0, eq.mid ?: 0, eq.treble ?: 0)
            }
            val speed = audioCommand.settings?.speed ?: 1.0f
            musicPlayer?.setPlaybackSpeed(speed)
            Log.i(TAG, "音效增强已应用: preset=${audioCommand.preset}, eq=(${eq?.bass}/${eq?.mid}/${eq?.treble}), vol=$finalVolume(${volumeDb}dB), speed=$speed")
        } else {
            musicPlayer?.setVolume(1.0f)
            musicPlayer?.setEqEnabled(false)
            musicPlayer?.setPlaybackSpeed(1.0f)
            Log.i(TAG, "音效增强关闭: 默认音量=1.0")
        }
    }
    
    fun toggleRepeatMode() {
        musicPlayer?.toggleRepeatMode()
        _repeatMode.value = musicPlayer?.getPlaylistManager()?.repeatMode?.value ?: RepeatMode.OFF
    }
    
    fun toggleShuffle() {
        musicPlayer?.toggleShuffle()
        val shuffleMode = musicPlayer?.getPlaylistManager()?.shuffleMode?.value
        _shuffleMode.value = shuffleMode == com.music.localmusic.player.ShuffleMode.ON
    }
    
    fun getCurrentAlbumArt(): Bitmap? {
        return musicPlayer?.getCurrentAlbumArt()
    }
    
    fun playLayer3Playlist(tracks: List<com.music.core.api.models.Track>) {
        Log.i(TAG, "playLayer3Playlist called with ${tracks.size} tracks")
        if (tracks.isEmpty()) {
            Log.w(TAG, "Layer3 播放列表为空")
            return
        }
        
        val allLocalTracks = localMusicIndex?.getAllTracks()
        Log.i(TAG, "localMusicIndex tracks: ${allLocalTracks?.size ?: 0}")
        
        if (allLocalTracks.isNullOrEmpty()) {
            Log.e(TAG, "localMusicIndex 为空或未初始化")
            return
        }
        
        val storagePath = LocalMusicIndex.getConfig().storagePath
        Log.i(TAG, "storagePath: $storagePath")
        
        tracks.forEach { t ->
            Log.i(TAG, "Layer3 track: ${t.title} - ${t.artist}")
        }
        
        val tracksToPlay = tracks.mapNotNull { layer3Track ->
            val matched = allLocalTracks.find { 
                it.title.equals(layer3Track.title, ignoreCase = true) && 
                it.artist.equals(layer3Track.artist, ignoreCase = true) 
            }
            if (matched != null) {
                Log.i(TAG, "匹配成功: ${layer3Track.title} -> ${matched.title}")
            } else {
                Log.w(TAG, "匹配失败: ${layer3Track.title} - ${layer3Track.artist}")
            }
            matched
        }
        
        if (tracksToPlay.isEmpty()) {
            Log.w(TAG, "无法匹配 Layer3 播放列表中的曲目")
            return
        }
        
        Log.i(TAG, "播放 Layer3 播放列表: ${tracksToPlay.size} 首曲目")
        _playlistSize.value = tracksToPlay.size
        _playlistIndex.value = 0
        _playlist.value = tracksToPlay
        
        musicPlayer?.play(tracksToPlay, startIndex = 0) { track ->
            "$storagePath/music/${track.filePath}"
        }
    }
    
    fun playTrackAtIndex(index: Int) {
        val playlist = _playlist.value
        if (index < 0 || index >= playlist.size) return

        musicPlayer?.seekTo(index, 0)
        musicPlayer?.resume()
        _playlistIndex.value = index
    }
    
    private fun updatePlaylistIndex() {
        val index = musicPlayer?.getPlaylistManager()?.currentIndex?.value ?: 0
        _playlistIndex.value = index
    }
    
    fun simulateScenario(scenarioId: String) {
        Log.i(TAG, "模拟场景: $scenarioId")
        
        val simulatedSignals = when (scenarioId) {
            "rainy_emo" -> createRainyEmoSignals()
            "children_board" -> createChildrenBoardSignals()
            "user_pop" -> createUserPopSignals()
            "sunny_day" -> createSunnyDaySignals()
            "beach_vacation" -> createBeachVacationSignals()
            "romantic_date" -> createRomanticDateSignals()
            "fatigue_alert" -> createFatigueAlertSignals()
            else -> return
        }
        
        _standardizedSignalsFlow.value = simulatedSignals
        
        val externalCamera = simulatedSignals.signals.external_camera
        if (externalCamera != null) {
            val primaryColor = externalCamera.primary_color
            val secondaryColor = externalCamera.secondary_color
            if (!primaryColor.isNullOrBlank()) {
                CoverGenerator.setSceneColors(primaryColor, secondaryColor ?: primaryColor)
                Log.i(TAG, "模拟场景设置封面颜色: primary=$primaryColor, secondary=$secondaryColor")
            }
        }
        
        viewModelScope.launch {
            semanticEngine?.processSignals(simulatedSignals)
        }
    }
    
    private fun createRainyEmoSignals(): StandardizedSignals {
        return StandardizedSignals(
            timestamp = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).format(java.util.Date()),
            signals = com.music.core.api.models.Signals(
                vehicle = com.music.core.api.models.Vehicle(
                    speed_kmh = 40.0,
                    passenger_count = 1,
                    gear = "D"
                ),
                environment = com.music.core.api.models.Environment(
                    time_of_day = 20.0,
                    weather = "rainy",
                    temperature = 15.0,
                    date_type = "weekday"
                ),
                external_camera = com.music.core.api.models.ExternalCamera(
                    scene_description = "rainy_night_city",
                    primary_color = "#1A237E",
                    secondary_color = "#4A148C",
                    brightness = 0.3
                ),
                internal_camera = com.music.core.api.models.InternalCamera(
                    mood = "sad",
                    confidence = 0.85,
                    passengers = com.music.core.api.models.Passengers(
                        children = 0,
                        adults = 1,
                        seniors = 0
                    )
                ),
                internal_mic = com.music.core.api.models.InternalMic(
                    volume_level = 0.2,
                    has_voice = false,
                    voice_count = 0,
                    noise_level = 0.1
                )
            ),
            confidence = com.music.core.api.models.Confidence(overall = 0.9)
        )
    }
    
    private fun createChildrenBoardSignals(): StandardizedSignals {
        return StandardizedSignals(
            timestamp = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).format(java.util.Date()),
            signals = com.music.core.api.models.Signals(
                vehicle = com.music.core.api.models.Vehicle(
                    speed_kmh = 25.0,
                    passenger_count = 4,
                    gear = "D"
                ),
                environment = com.music.core.api.models.Environment(
                    time_of_day = 9.0,
                    weather = "sunny",
                    temperature = 22.0,
                    date_type = "weekend"
                ),
                external_camera = com.music.core.api.models.ExternalCamera(
                    scene_description = "school_zone_morning",
                    primary_color = "#FF6F00",
                    secondary_color = "#FFB300",
                    brightness = 0.85
                ),
                internal_camera = com.music.core.api.models.InternalCamera(
                    mood = "playful",
                    confidence = 0.9,
                    passengers = com.music.core.api.models.Passengers(
                        children = 2,
                        adults = 2,
                        seniors = 0
                    )
                ),
                internal_mic = com.music.core.api.models.InternalMic(
                    volume_level = 0.8,
                    has_voice = true,
                    voice_count = 4,
                    noise_level = 0.6
                )
            ),
            confidence = com.music.core.api.models.Confidence(overall = 0.9)
        )
    }
    
    private fun createUserPopSignals(): StandardizedSignals {
        return StandardizedSignals(
            timestamp = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).format(java.util.Date()),
            signals = com.music.core.api.models.Signals(
                vehicle = com.music.core.api.models.Vehicle(
                    speed_kmh = 45.0,
                    passenger_count = 1,
                    gear = "D"
                ),
                environment = com.music.core.api.models.Environment(
                    time_of_day = 10.0,
                    weather = "sunny",
                    temperature = 24.0,
                    date_type = "weekday"
                ),
                external_camera = com.music.core.api.models.ExternalCamera(
                    scene_description = "highway_drive",
                    primary_color = "#00E5FF",
                    secondary_color = "#18FFFF",
                    brightness = 0.75
                ),
                internal_camera = com.music.core.api.models.InternalCamera(
                    mood = "neutral",
                    confidence = 0.7,
                    passengers = com.music.core.api.models.Passengers(
                        children = 0,
                        adults = 1,
                        seniors = 0
                    )
                ),
                internal_mic = com.music.core.api.models.InternalMic(
                    volume_level = 0.4,
                    has_voice = true,
                    voice_count = 1,
                    noise_level = 0.2
                )
            ),
            confidence = com.music.core.api.models.Confidence(overall = 0.9)
        )
    }

    private fun createSunnyDaySignals(): StandardizedSignals {
        return StandardizedSignals(
            timestamp = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).format(java.util.Date()),
            signals = com.music.core.api.models.Signals(
                vehicle = com.music.core.api.models.Vehicle(
                    speed_kmh = 60.0,
                    passenger_count = 1,
                    gear = "D"
                ),
                environment = com.music.core.api.models.Environment(
                    time_of_day = 14.0,
                    weather = "sunny",
                    temperature = 26.0,
                    date_type = "weekend"
                ),
                external_camera = com.music.core.api.models.ExternalCamera(
                    scene_description = "sunny_highway",
                    primary_color = "#FFC107",
                    secondary_color = "#FF9800",
                    brightness = 0.9
                ),
                internal_camera = com.music.core.api.models.InternalCamera(
                    mood = "happy",
                    confidence = 0.9,
                    passengers = com.music.core.api.models.Passengers(
                        children = 0,
                        adults = 1,
                        seniors = 0
                    )
                ),
                internal_mic = com.music.core.api.models.InternalMic(
                    volume_level = 0.4,
                    has_voice = false,
                    voice_count = 0,
                    noise_level = 0.2
                )
            ),
            confidence = com.music.core.api.models.Confidence(overall = 0.9)
        )
    }
    
    private fun createBeachVacationSignals(): StandardizedSignals {
        return StandardizedSignals(
            timestamp = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).format(java.util.Date()),
            signals = com.music.core.api.models.Signals(
                vehicle = com.music.core.api.models.Vehicle(
                    speed_kmh = 80.0,
                    passenger_count = 2,
                    gear = "D"
                ),
                environment = com.music.core.api.models.Environment(
                    time_of_day = 16.0,
                    weather = "sunny",
                    temperature = 30.0,
                    date_type = "weekend"
                ),
                external_camera = com.music.core.api.models.ExternalCamera(
                    scene_description = "coastal_highway_sunset_beach",
                    primary_color = "#00695C",
                    secondary_color = "#4DB6AC",
                    brightness = 0.9
                ),
                internal_camera = com.music.core.api.models.InternalCamera(
                    mood = "vacation",
                    confidence = 0.9,
                    passengers = com.music.core.api.models.Passengers(
                        children = 0,
                        adults = 2,
                        seniors = 0
                    )
                ),
                internal_mic = com.music.core.api.models.InternalMic(
                    volume_level = 0.4,
                    has_voice = true,
                    voice_count = 2,
                    noise_level = 0.15
                )
            ),
            confidence = com.music.core.api.models.Confidence(overall = 0.9)
        )
    }
    
    private fun createRomanticDateSignals(): StandardizedSignals {
        return StandardizedSignals(
            timestamp = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).format(java.util.Date()),
            signals = com.music.core.api.models.Signals(
                vehicle = com.music.core.api.models.Vehicle(
                    speed_kmh = 25.0,
                    passenger_count = 2,
                    gear = "D"
                ),
                environment = com.music.core.api.models.Environment(
                    time_of_day = 21.0,
                    weather = "clear",
                    temperature = 18.0,
                    date_type = "weekend"
                ),
                external_camera = com.music.core.api.models.ExternalCamera(
                    scene_description = "city_night_lights",
                    primary_color = "#AD1457",
                    secondary_color = "#880E4F",
                    brightness = 0.35
                ),
                internal_camera = com.music.core.api.models.InternalCamera(
                    mood = "romantic",
                    confidence = 0.85,
                    passengers = com.music.core.api.models.Passengers(
                        children = 0,
                        adults = 2,
                        seniors = 0
                    )
                ),
                internal_mic = com.music.core.api.models.InternalMic(
                    volume_level = 0.15,
                    has_voice = true,
                    voice_count = 2,
                    noise_level = 0.1
                )
            ),
            confidence = com.music.core.api.models.Confidence(overall = 0.9)
        )
    }
    
    private fun createFatigueAlertSignals(): StandardizedSignals {
        return StandardizedSignals(
            timestamp = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).format(java.util.Date()),
            signals = com.music.core.api.models.Signals(
                vehicle = com.music.core.api.models.Vehicle(
                    speed_kmh = 80.0,
                    passenger_count = 1,
                    gear = "D"
                ),
                environment = com.music.core.api.models.Environment(
                    time_of_day = 14.0,
                    weather = "sunny",
                    temperature = 28.0,
                    date_type = "weekday"
                ),
                external_camera = com.music.core.api.models.ExternalCamera(
                    scene_description = "highway_monotonous",
                    primary_color = "#FF6D00",
                    secondary_color = "#FFAB00",
                    brightness = 0.9
                ),
                internal_camera = com.music.core.api.models.InternalCamera(
                    mood = "tired",
                    confidence = 0.9,
                    passengers = com.music.core.api.models.Passengers(
                        children = 0,
                        adults = 1,
                        seniors = 0
                    )
                ),
                internal_mic = com.music.core.api.models.InternalMic(
                    volume_level = 0.1,
                    has_voice = false,
                    voice_count = 0,
                    noise_level = 0.05
                )
            ),
            confidence = com.music.core.api.models.Confidence(overall = 0.9)
        )
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
        voiceInputService.destroy()
        
        musicPlayer?.release()
        musicPlayer = null
        localMusicIndex?.close()
        localMusicIndex = null

        perceptionEngine = null
        semanticEngine = null

        _isRunning.value = false
        _isInitialized.value = false
    }
}
