package com.music.appmain

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
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
    
    private var perceptionJob: Job? = null
    private var semanticJob: Job? = null
    private var effectJob: Job? = null

    init {
        initializeEngines()
        initializeTts()
        initializeMusicPlayer()
    }
    
    private fun initializeMusicPlayer() {
        musicPlayer = MusicPlayer(context)
        
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
        
        val externalFilesDir = context.getExternalFilesDir(null)?.absolutePath 
            ?: "/data/data/com.music.appmain/files"
        val config = LocalMusicConfig(
            storagePath = externalFilesDir,
            indexJsonPath = "$externalFilesDir/index.json"
        )
        LocalMusicIndex.setConfig(config)
        localMusicIndex = LocalMusicIndex.getInstance(context, config)
        val indexInitialized = localMusicIndex?.initialize() ?: false
        _localMusicReady.value = indexInitialized
        _trackCount.value = localMusicIndex?.getTrackCount() ?: 0
        
        if (indexInitialized) {
            Log.i(TAG, "本地音乐索引初始化成功，共 ${_trackCount.value} 首曲目")
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
                    
                    it.commands?.content?.playlist?.let { playlist ->
                        Log.i(TAG, "Layer3 播放列表: ${playlist.size} 首曲目")
                        playLayer3Playlist(playlist)
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
        if (tracks.isEmpty()) {
            Log.w(TAG, "Layer3 播放列表为空")
            return
        }
        
        val allLocalTracks = localMusicIndex?.getAllTracks() ?: return
        val storagePath = LocalMusicIndex.getConfig().storagePath
        
        val tracksToPlay = tracks.mapNotNull { layer3Track ->
            allLocalTracks.find { 
                it.title.equals(layer3Track.title, ignoreCase = true) && 
                it.artist.equals(layer3Track.artist, ignoreCase = true) 
            }
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
        
        val storagePath = LocalMusicIndex.getConfig().storagePath
        val track = playlist[index]
        
        musicPlayer?.play(track, "$storagePath/music/${track.filePath}")
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
            "rain_stops" -> createRainStopsSignals()
            "noisy_car" -> createNoisyCarSignals()
            "user_pop" -> createUserPopSignals()
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
                    speed_kmh = 30.0,
                    passenger_count = 4,
                    gear = "D"
                ),
                environment = com.music.core.api.models.Environment(
                    time_of_day = 14.0,
                    weather = "sunny",
                    temperature = 25.0,
                    date_type = "weekend"
                ),
                external_camera = com.music.core.api.models.ExternalCamera(
                    scene_description = "suburban_street",
                    primary_color = "#FF6F00",
                    secondary_color = "#FFCA28",
                    brightness = 0.8
                ),
                internal_camera = com.music.core.api.models.InternalCamera(
                    mood = "happy",
                    confidence = 0.9,
                    passengers = com.music.core.api.models.Passengers(
                        children = 2,
                        adults = 2,
                        seniors = 0
                    )
                ),
                internal_mic = com.music.core.api.models.InternalMic(
                    volume_level = 0.7,
                    has_voice = true,
                    voice_count = 4,
                    noise_level = 0.5
                )
            ),
            confidence = com.music.core.api.models.Confidence(overall = 0.9)
        )
    }
    
    private fun createRainStopsSignals(): StandardizedSignals {
        return StandardizedSignals(
            timestamp = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).format(java.util.Date()),
            signals = com.music.core.api.models.Signals(
                vehicle = com.music.core.api.models.Vehicle(
                    speed_kmh = 50.0,
                    passenger_count = 1,
                    gear = "D"
                ),
                environment = com.music.core.api.models.Environment(
                    time_of_day = 16.0,
                    weather = "sunny",
                    temperature = 22.0,
                    date_type = "weekday"
                ),
                external_camera = com.music.core.api.models.ExternalCamera(
                    scene_description = "rainbow_after_rain",
                    primary_color = "#00BCD4",
                    secondary_color = "#4DD0E1",
                    brightness = 0.85
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
                    volume_level = 0.3,
                    has_voice = false,
                    voice_count = 0,
                    noise_level = 0.15
                )
            ),
            confidence = com.music.core.api.models.Confidence(overall = 0.9)
        )
    }
    
    private fun createNoisyCarSignals(): StandardizedSignals {
        return StandardizedSignals(
            timestamp = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).format(java.util.Date()),
            signals = com.music.core.api.models.Signals(
                vehicle = com.music.core.api.models.Vehicle(
                    speed_kmh = 35.0,
                    passenger_count = 5,
                    gear = "D"
                ),
                environment = com.music.core.api.models.Environment(
                    time_of_day = 19.0,
                    weather = "cloudy",
                    temperature = 20.0,
                    date_type = "weekend"
                ),
                external_camera = com.music.core.api.models.ExternalCamera(
                    scene_description = "city_evening",
                    primary_color = "#D500F9",
                    secondary_color = "#EA80FC",
                    brightness = 0.5
                ),
                internal_camera = com.music.core.api.models.InternalCamera(
                    mood = "excited",
                    confidence = 0.8,
                    passengers = com.music.core.api.models.Passengers(
                        children = 1,
                        adults = 3,
                        seniors = 1
                    )
                ),
                internal_mic = com.music.core.api.models.InternalMic(
                    volume_level = 0.9,
                    has_voice = true,
                    voice_count = 5,
                    noise_level = 0.8
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
    
    private fun createBeachVacationSignals(): StandardizedSignals {
        return StandardizedSignals(
            timestamp = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).format(java.util.Date()),
            signals = com.music.core.api.models.Signals(
                vehicle = com.music.core.api.models.Vehicle(
                    speed_kmh = 60.0,
                    passenger_count = 2,
                    gear = "D"
                ),
                environment = com.music.core.api.models.Environment(
                    time_of_day = 14.0,
                    weather = "sunny",
                    temperature = 32.0,
                    date_type = "weekend"
                ),
                external_camera = com.music.core.api.models.ExternalCamera(
                    scene_description = "coastal_highway_beach",
                    primary_color = "#00B8D4",
                    secondary_color = "#80DEEA",
                    brightness = 0.95
                ),
                internal_camera = com.music.core.api.models.InternalCamera(
                    mood = "relaxed",
                    confidence = 0.9,
                    passengers = com.music.core.api.models.Passengers(
                        children = 0,
                        adults = 2,
                        seniors = 0
                    )
                ),
                internal_mic = com.music.core.api.models.InternalMic(
                    volume_level = 0.3,
                    has_voice = true,
                    voice_count = 2,
                    noise_level = 0.2
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
