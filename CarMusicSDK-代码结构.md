# CarMusicSDK 代码结构

## 项目结构

```
car-music-sdk/
├── build.gradle
├── src/main/java/com/carmusic/sdk/
│   ├── CarMusicSDK.kt                 # SDK 入口类
│   ├── CarMusicConfig.kt              # 配置类
│   ├── data/
│   │   ├── MusicTrack.kt              # 歌曲数据模型
│   │   └── MusicManifest.kt           # 清单数据模型
│   ├── player/
│   │   ├── CarMusicPlayer.kt          # 播放器接口
│   │   ├── CarMusicPlayerImpl.kt      # 播放器实现
│   │   ├── OnlineMusicPlayer.kt       # ExoPlayer 封装
│   │   ├── PlaybackQueueManager.kt    # 播放队列管理
│   │   └── MusicPreloader.kt          # 预加载管理
│   ├── session/
│   │   ├── MediaSessionManager.kt     # MediaSession 管理
│   │   └── MusicNotificationReceiver.kt # 通知广播接收器
│   └── util/
│       ├── AudioFocusManager.kt       # 音频焦点管理
│       └── CoverGenerator.kt          # 封面生成器
└── src/main/AndroidManifest.xml
```

## 核心类代码示例

### 1. CarMusicSDK.kt (SDK 入口)

```kotlin
package com.carmusic.sdk

import android.content.Context
import com.carmusic.sdk.data.MusicManifest
import com.carmusic.sdk.data.MusicTrack
import com.carmusic.sdk.player.CarMusicPlayer
import com.carmusic.sdk.player.CarMusicPlayerImpl
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * CarMusicSDK 入口类
 * 
 * 使用示例：
 * ```kotlin
 * // 初始化
 * CarMusicSDK.initialize(context)
 * 
 * // 获取播放器
 * val player = CarMusicSDK.getPlayer()
 * 
 * // 加载音乐
 * val tracks = CarMusicSDK.loadLocalPlaylist()
 * ```
 */
object CarMusicSDK {
    private lateinit var applicationContext: Context
    private var config: CarMusicConfig = CarMusicConfig()
    private var player: CarMusicPlayer? = null
    
    /**
     * 初始化 SDK
     * 必须在 Application.onCreate 或 Activity.onCreate 中调用
     * 
     * @param context Application Context
     */
    fun initialize(context: Context) {
        applicationContext = context.applicationContext
    }
    
    /**
     * 设置配置
     * 必须在 getPlayer() 之前调用
     * 
     * @param config 配置对象
     */
    fun setConfig(config: CarMusicConfig) {
        this.config = config
    }
    
    /**
     * 获取播放器实例
     * 单例模式，全局只有一个播放器实例
     * 
     * @return CarMusicPlayer 播放器实例
     */
    fun getPlayer(): CarMusicPlayer {
        if (player == null) {
            player = CarMusicPlayerImpl(applicationContext, config)
        }
        return player!!
    }
    
    /**
     * 加载本地音乐列表
     * 从 assets/music/manifest.json 加载
     * 
     * @return Result<List<MusicTrack>> 音乐列表或错误
     */
    suspend fun loadLocalPlaylist(): Result<List<MusicTrack>> {
        return withContext(Dispatchers.IO) {
            try {
                val json = applicationContext.assets.open("manifest.json")
                    .bufferedReader()
                    .readText()
                
                val manifest = Gson().fromJson(json, MusicManifest::class.java)
                val tracks = manifest.tracks ?: emptyList()
                
                Result.success(tracks)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * 加载精选音乐列表
     * 
     * @return Result<List<MusicTrack>> 精选音乐列表
     */
    suspend fun loadFeaturedTracks(): Result<List<MusicTrack>> {
        return withContext(Dispatchers.IO) {
            try {
                val json = applicationContext.assets.open("manifest.json")
                    .bufferedReader()
                    .readText()
                
                val manifest = Gson().fromJson(json, MusicManifest::class.java)
                val tracks = manifest.featured_tracks ?: emptyList()
                
                Result.success(tracks)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * 释放 SDK 资源
     * 在应用退出时调用
     */
    fun release() {
        player?.release()
        player = null
    }
}
```

### 2. CarMusicPlayer.kt (播放器接口)

```kotlin
package com.carmusic.sdk.player

import android.graphics.Bitmap
import com.carmusic.sdk.data.MusicTrack
import com.carmusic.sdk.util.AudioFocusManager

/**
 * 播放器接口
 */
interface CarMusicPlayer {
    /**
     * 播放指定歌曲
     * 
     * @param track 歌曲信息
     */
    fun play(track: MusicTrack)
    
    /**
     * 播放/暂停切换
     */
    fun togglePlayPause()
    
    /**
     * 暂停播放
     */
    fun pause()
    
    /**
     * 恢复播放
     */
    fun resume()
    
    /**
     * 停止播放
     */
    fun stop()
    
    /**
     * 播放下一首
     */
    fun playNext()
    
    /**
     * 播放上一首
     */
    fun playPrevious()
    
    /**
     * 跳转到指定位置
     * 
     * @param positionMs 位置（毫秒）
     */
    fun seekTo(positionMs: Long)
    
    /**
     * 设置播放列表
     * 
     * @param tracks 歌曲列表
     * @param startPosition 开始位置（默认0）
     */
    fun setPlaylist(tracks: List<MusicTrack>, startPosition: Int = 0)
    
    /**
     * 切换循环模式
     * 
     * @return 切换后的模式
     */
    fun toggleRepeatMode(): RepeatMode
    
    /**
     * 切换随机播放
     * 
     * @return 是否启用随机播放
     */
    fun toggleShuffle(): Boolean
    
    /**
     * 获取当前队列
     * 
     * @return 歌曲列表
     */
    fun getQueue(): List<MusicTrack>
    
    /**
     * 获取当前歌曲
     * 
     * @return 当前播放的歌曲
     */
    fun getCurrentTrack(): MusicTrack?
    
    /**
     * 获取当前播放状态
     * 
     * @return 播放状态
     */
    fun getCurrentState(): PlayerState
    
    /**
     * 获取当前播放位置
     * 
     * @return 位置（毫秒）
     */
    fun getCurrentPosition(): Long
    
    /**
     * 获取总时长
     * 
     * @return 总时长（毫秒）
     */
    fun getDuration(): Long
    
    /**
     * 获取当前专辑封面
     * 根据 coverColor 自动生成
     * 
     * @return Bitmap 封面图片
     */
    fun getCurrentAlbumArt(): Bitmap?
    
    /**
     * 设置播放器监听器
     * 
     * @param listener 监听器
     */
    fun setListener(listener: Listener)
    
    /**
     * 设置音频焦点监听器
     * 
     * @param callback 回调
     */
    fun setAudioFocusListener(callback: AudioFocusManager.AudioFocusCallback)
    
    /**
     * 释放播放器资源
     */
    fun release()
    
    /**
     * 播放状态枚举
     */
    enum class PlayerState {
        IDLE,       // 空闲
        LOADING,    // 加载中
        BUFFERING,  // 缓冲中
        PLAYING,    // 播放中
        PAUSED,     // 已暂停
        ERROR       // 错误
    }
    
    /**
     * 循环模式枚举
     */
    enum class RepeatMode {
        OFF,        // 关闭
        ONE,        // 单曲循环
        ALL         // 列表循环
    }
    
    /**
     * 播放器监听器
     */
    interface Listener {
        /**
         * 播放状态变化
         */
        fun onStateChanged(state: PlayerState)
        
        /**
         * 歌曲切换
         */
        fun onTrackChanged(track: MusicTrack)
        
        /**
         * 播放位置变化
         */
        fun onPositionChanged(position: Long, duration: Long)
        
        /**
         * 缓冲状态变化
         */
        fun onBuffering(isBuffering: Boolean)
        
        /**
         * 播放错误
         */
        fun onError(error: String)
        
        /**
         * 播放完成
         */
        fun onPlaybackCompleted()
    }
}
```

### 3. CarMusicPlayerImpl.kt (播放器实现)

```kotlin
package com.carmusic.sdk.player

import android.content.Context
import android.graphics.Bitmap
import android.os.PowerManager
import com.carmusic.sdk.CarMusicConfig
import com.carmusic.sdk.data.MusicTrack
import com.carmusic.sdk.session.MediaSessionManager
import com.carmusic.sdk.util.AudioFocusManager
import com.carmusic.sdk.util.CoverGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * 播放器实现类
 */
internal class CarMusicPlayerImpl(
    private val context: Context,
    private val config: CarMusicConfig
) : CarMusicPlayer {
    
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // 核心组件
    private val onlinePlayer: OnlineMusicPlayer = OnlineMusicPlayer.getInstance(context)
    private val queueManager: PlaybackQueueManager = PlaybackQueueManager()
    private val preloader: MusicPreloader = MusicPreloader(scope)
    private val audioFocusManager: AudioFocusManager = AudioFocusManager(context)
    private var mediaSessionManager: MediaSessionManager? = null
    
    // 唤醒锁
    private val wakeLock: PowerManager.WakeLock? by lazy {
        if (config.enableWakeLock) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "CarMusicSDK::MusicPlayerWakeLock"
            ).apply {
                setReferenceCounted(false)
            }
        } else null
    }
    
    // 监听器
    private var listener: CarMusicPlayer.Listener? = null
    private var currentTrack: MusicTrack? = null
    
    init {
        initializePlayer()
        initializeAudioFocus()
        if (config.enableMediaSession) {
            initializeMediaSession()
        }
    }
    
    private fun initializePlayer() {
        onlinePlayer.initialize(null)
        
        onlinePlayer.setPlayerListener(object : OnlineMusicPlayer.PlayerListener {
            override fun onStateChanged(state: OnlineMusicPlayer.PlayerState) {
                val playerState = when (state) {
                    OnlineMusicPlayer.PlayerState.IDLE -> CarMusicPlayer.PlayerState.IDLE
                    OnlineMusicPlayer.PlayerState.LOADING -> CarMusicPlayer.PlayerState.LOADING
                    OnlineMusicPlayer.PlayerState.BUFFERING -> CarMusicPlayer.PlayerState.BUFFERING
                    OnlineMusicPlayer.PlayerState.PLAYING -> {
                        acquireWakeLock()
                        CarMusicPlayer.PlayerState.PLAYING
                    }
                    OnlineMusicPlayer.PlayerState.PAUSED -> {
                        releaseWakeLock()
                        CarMusicPlayer.PlayerState.PAUSED
                    }
                    OnlineMusicPlayer.PlayerState.ERROR -> CarMusicPlayer.PlayerState.ERROR
                    OnlineMusicPlayer.PlayerState.ENDED -> {
                        playNext()
                        return
                    }
                }
                
                listener?.onStateChanged(playerState)
                updateMediaSessionState(playerState)
            }
            
            override fun onError(error: String) {
                listener?.onError(error)
            }
            
            override fun onPositionChanged(positionMs: Long, durationMs: Long) {
                listener?.onPositionChanged(positionMs, durationMs)
                updateMediaSessionPosition(positionMs)
            }
            
            override fun onPlaybackCompleted() {
                listener?.onPlaybackCompleted()
            }
            
            override fun onBuffering(isBuffering: Boolean) {
                listener?.onBuffering(isBuffering)
            }
        })
    }
    
    private fun initializeAudioFocus() {
        audioFocusManager.setCallback(object : AudioFocusManager.AudioFocusCallback {
            override fun onFocusGained() {
                if (getCurrentState() == CarMusicPlayer.PlayerState.PAUSED) {
                    resume()
                }
            }
            
            override fun onFocusLost() {
                pause()
            }
            
            override fun onFocusLostTransient() {
                pause()
            }
            
            override fun onFocusLostTransientCanDuck() {
                // 可以降低音量，这里选择暂停
                pause()
            }
        })
    }
    
    private fun initializeMediaSession() {
        mediaSessionManager = MediaSessionManager(context).apply {
            initialize()
            setCallback(object : MediaSessionManager.MediaSessionCallback {
                override fun onPlay() {
                    togglePlayPause()
                }
                
                override fun onPause() {
                    togglePlayPause()
                }
                
                override fun onStop() {
                    stop()
                }
                
                override fun onSkipToNext() {
                    playNext()
                }
                
                override fun onSkipToPrevious() {
                    playPrevious()
                }
                
                override fun onSeekTo(positionMs: Long) {
                    seekTo(positionMs)
                }
            })
        }
    }
    
    override fun play(track: MusicTrack) {
        if (config.autoRequestAudioFocus) {
            audioFocusManager.requestAudioFocus()
        }
        
        currentTrack = track
        queueManager.moveToTrack(track.id)
        
        val path = "file:///android_asset/${track.file}"
        onlinePlayer.play(track, path, isLocal = true)
        
        listener?.onTrackChanged(track)
        updateMediaSessionMetadata(track)
    }
    
    override fun togglePlayPause() {
        when (getCurrentState()) {
            CarMusicPlayer.PlayerState.PLAYING -> pause()
            CarMusicPlayer.PlayerState.PAUSED,
            CarMusicPlayer.PlayerState.IDLE -> {
                currentTrack?.let { play(it) } ?: run {
                    queueManager.getCurrentTrack()?.let { play(it) }
                }
            }
            else -> {}
        }
    }
    
    override fun pause() {
        onlinePlayer.pause()
    }
    
    override fun resume() {
        if (config.autoRequestAudioFocus) {
            audioFocusManager.requestAudioFocus()
        }
        onlinePlayer.resume()
    }
    
    override fun stop() {
        onlinePlayer.stop()
        releaseWakeLock()
    }
    
    override fun playNext() {
        val nextTrack = queueManager.moveToNext()
        if (nextTrack != null) {
            play(nextTrack)
        }
    }
    
    override fun playPrevious() {
        val prevTrack = queueManager.moveToPrevious()
        if (prevTrack != null) {
            play(prevTrack)
        }
    }
    
    override fun seekTo(positionMs: Long) {
        onlinePlayer.seekTo(positionMs)
    }
    
    override fun setPlaylist(tracks: List<MusicTrack>, startPosition: Int) {
        queueManager.setQueue(tracks, startPosition)
    }
    
    override fun toggleRepeatMode(): CarMusicPlayer.RepeatMode {
        val mode = queueManager.toggleRepeatMode()
        return when (mode) {
            PlaybackQueueManager.RepeatMode.OFF -> CarMusicPlayer.RepeatMode.OFF
            PlaybackQueueManager.RepeatMode.ONE -> CarMusicPlayer.RepeatMode.ONE
            PlaybackQueueManager.RepeatMode.ALL -> CarMusicPlayer.RepeatMode.ALL
        }
    }
    
    override fun toggleShuffle(): Boolean {
        return queueManager.toggleShuffle()
    }
    
    override fun getQueue(): List<MusicTrack> {
        return queueManager.getQueue()
    }
    
    override fun getCurrentTrack(): MusicTrack? {
        return currentTrack ?: queueManager.getCurrentTrack()
    }
    
    override fun getCurrentState(): CarMusicPlayer.PlayerState {
        return when (onlinePlayer.getState()) {
            OnlineMusicPlayer.PlayerState.IDLE -> CarMusicPlayer.PlayerState.IDLE
            OnlineMusicPlayer.PlayerState.LOADING -> CarMusicPlayer.PlayerState.LOADING
            OnlineMusicPlayer.PlayerState.BUFFERING -> CarMusicPlayer.PlayerState.BUFFERING
            OnlineMusicPlayer.PlayerState.PLAYING -> CarMusicPlayer.PlayerState.PLAYING
            OnlineMusicPlayer.PlayerState.PAUSED -> CarMusicPlayer.PlayerState.PAUSED
            OnlineMusicPlayer.PlayerState.ERROR -> CarMusicPlayer.PlayerState.ERROR
            OnlineMusicPlayer.PlayerState.ENDED -> CarMusicPlayer.PlayerState.IDLE
        }
    }
    
    override fun getCurrentPosition(): Long {
        return onlinePlayer.getCurrentPosition()
    }
    
    override fun getDuration(): Long {
        return onlinePlayer.getDuration()
    }
    
    override fun getCurrentAlbumArt(): Bitmap? {
        return currentTrack?.let { CoverGenerator.generate(it) }
    }
    
    override fun setListener(listener: CarMusicPlayer.Listener) {
        this.listener = listener
    }
    
    override fun setAudioFocusListener(callback: AudioFocusManager.AudioFocusCallback) {
        audioFocusManager.setCallback(callback)
    }
    
    override fun release() {
        scope.cancel()
        onlinePlayer.release()
        audioFocusManager.abandonAudioFocus()
        mediaSessionManager?.release()
        releaseWakeLock()
    }
    
    private fun acquireWakeLock() {
        wakeLock?.acquire(config.wakeLockTimeout)
    }
    
    private fun releaseWakeLock() {
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
    }
    
    private fun updateMediaSessionState(state: CarMusicPlayer.PlayerState) {
        val playbackState = when (state) {
            CarMusicPlayer.PlayerState.PLAYING -> 
                android.media.session.PlaybackState.STATE_PLAYING
            CarMusicPlayer.PlayerState.PAUSED -> 
                android.media.session.PlaybackState.STATE_PAUSED
            else -> 
                android.media.session.PlaybackState.STATE_STOPPED
        }
        mediaSessionManager?.updatePlaybackState(playbackState)
    }
    
    private fun updateMediaSessionPosition(position: Long) {
        val state = if (getCurrentState() == CarMusicPlayer.PlayerState.PLAYING) {
            android.media.session.PlaybackState.STATE_PLAYING
        } else {
            android.media.session.PlaybackState.STATE_PAUSED
        }
        mediaSessionManager?.updatePlaybackState(state, position)
    }
    
    private fun updateMediaSessionMetadata(track: MusicTrack) {
        mediaSessionManager?.updateMetadata(track, getDuration())
    }
}
```

### 4. CoverGenerator.kt (封面生成器)

```kotlin
package com.carmusic.sdk.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.carmusic.sdk.data.MusicTrack

/**
 * 封面生成器
 * 根据歌曲的 coverColor 自动生成占位封面
 */
object CoverGenerator {
    private var defaultConfig = Config()
    
    /**
     * 生成封面
     * 
     * @param track 歌曲信息
     * @return Bitmap 封面图片
     */
    fun generate(track: MusicTrack): Bitmap {
        return generate(track, defaultConfig)
    }
    
    /**
     * 生成封面（自定义配置）
     * 
     * @param track 歌曲信息
     * @param config 生成配置
     * @return Bitmap 封面图片
     */
    fun generate(track: MusicTrack, config: Config): Bitmap {
        val bitmap = Bitmap.createBitmap(config.width, config.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // 解析颜色
        val color = try {
            Color.parseColor("#${track.coverColor}")
        } catch (e: Exception) {
            Color.parseColor("#3D5AFE")
        }
        
        // 绘制背景
        canvas.drawColor(color)
        
        // 绘制文字（歌曲名首字母或音符）
        val displayText = track.title.firstOrNull()?.toString() ?: "♪"
        
        val paint = Paint().apply {
            this.color = config.textColor
            this.textSize = config.textSize
            this.textAlign = Paint.Align.CENTER
            this.isAntiAlias = true
            this.isFakeBoldText = true
        }
        
        val x = config.width / 2f
        val y = config.height / 2f - (paint.descent() + paint.ascent()) / 2
        
        canvas.drawText(displayText, x, y, paint)
        
        return bitmap
    }
    
    /**
     * 设置默认配置
     * 
     * @param config 配置对象
     */
    fun setDefaultConfig(config: Config) {
        defaultConfig = config
    }
    
    /**
     * 获取默认配置
     * 
     * @return 默认配置
     */
    fun getDefaultConfig(): Config = defaultConfig
    
    /**
     * 封面生成配置
     */
    data class Config(
        val width: Int = 512,
        val height: Int = 512,
        val textSize: Float = 200f,
        val textColor: Int = Color.WHITE
    )
}
```

### 5. build.gradle (SDK 构建配置)

```gradle
plugins {
    id 'com.android.library'
    id 'org.jetbrains.kotlin.android'
}

android {
    namespace 'com.carmusic.sdk'
    compileSdk 34

    defaultConfig {
        minSdk 26
        targetSdk 34
        
        versionCode 1
        versionName "1.0.0"
    }
    
    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
    
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = '17'
    }
}

dependencies {
    implementation 'androidx.core:core-ktx:1.12.0'
    implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0'
    implementation 'androidx.lifecycle:lifecycle-livedata-ktx:2.7.0'
    
    // ExoPlayer
    implementation 'com.google.android.exoplayer:exoplayer:2.19.1'
    
    // JSON 解析
    implementation 'com.google.code.gson:gson:2.10.1'
    
    // 图片加载（可选，用于加载网络封面）
    compileOnly 'com.github.bumptech.glide:glide:4.16.0'
}
```

### 6. AndroidManifest.xml (SDK 清单)

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.carmusic.sdk">

    <!-- 音频焦点 -->
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    
    <!-- 前台服务（用于后台播放） -->
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    
    <!-- 媒体控制 -->
    <uses-permission android:name="android.permission.MEDIA_CONTENT_CONTROL" />

</manifest>
```

## 使用示例

```kotlin
// 1. 初始化
CarMusicSDK.initialize(applicationContext)

// 2. 配置（可选）
val config = CarMusicConfig(
    enableNotification = true,
    enableMediaSession = true,
    enableWakeLock = true
)
CarMusicSDK.setConfig(config)

// 3. 获取播放器
val player = CarMusicSDK.getPlayer()

// 4. 加载音乐
lifecycleScope.launch {
    val result = CarMusicSDK.loadLocalPlaylist()
    result.onSuccess { tracks ->
        player.setPlaylist(tracks)
        player.play(tracks[0])
    }
}

// 5. 设置监听
player.setListener(object : CarMusicPlayer.Listener {
    override fun onStateChanged(state: CarMusicPlayer.PlayerState) {
        // 更新 UI
    }
    
    override fun onTrackChanged(track: MusicTrack) {
        // 显示歌曲信息
        titleText.text = track.title
        albumArt.setImageBitmap(player.getCurrentAlbumArt())
    }
    
    override fun onPositionChanged(position: Long, duration: Long) {
        // 更新进度条
    }
    
    override fun onBuffering(isBuffering: Boolean) {}
    override fun onError(error: String) {}
    override fun onPlaybackCompleted() {}
})
```

---

**版本**: 1.0.0  
**最后更新**: 2026-03-01
