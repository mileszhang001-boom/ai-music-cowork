package com.music.localmusic.source

import android.content.Context
import android.net.Uri
import android.util.Log
import com.music.localmusic.models.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ManagerState(
    val totalTracks: Int = 0,
    val sourceCount: Int = 0,
    val isScanning: Boolean = false,
    val lastError: String? = null
)

class MusicSourceManager(private val context: Context) {
    
    companion object {
        private const val TAG = "MusicSourceManager"
        
        @Volatile
        private var instance: MusicSourceManager? = null
        
        fun getInstance(context: Context): MusicSourceManager {
            return instance ?: synchronized(this) {
                instance ?: MusicSourceManager(context.applicationContext).also { instance = it }
            }
        }
    }
    
    private val sources = mutableMapOf<String, MusicSource>()
    private val sourceConfigs = mutableMapOf<String, MusicSourceConfig>()
    
    private val _managerState = MutableStateFlow(ManagerState())
    val managerState: StateFlow<ManagerState> = _managerState.asStateFlow()
    
    private val _activeSourceIds = MutableStateFlow<Set<String>>(emptySet())
    val activeSourceIds: StateFlow<Set<String>> = _activeSourceIds.asStateFlow()
    
    private val usbMonitor = UsbStateMonitor(context)
    val usbEventFlow: StateFlow<UsbEvent?> = usbMonitor.usbEventFlow
    val isUsbConnected: StateFlow<Boolean> = usbMonitor.isUsbConnected
    
    fun startUsbMonitoring() {
        usbMonitor.register()
    }
    
    fun stopUsbMonitoring() {
        usbMonitor.unregister()
    }
    
    fun registerSource(source: MusicSource, config: MusicSourceConfig) {
        sources[source.sourceId] = source
        sourceConfigs[source.sourceId] = config
        Log.i(TAG, "注册音乐源: ${source.name} (${source.sourceId})")
    }
    
    fun unregisterSource(sourceId: String) {
        sources[sourceId]?.release()
        sources.remove(sourceId)
        sourceConfigs.remove(sourceId)
        Log.i(TAG, "注销音乐源: $sourceId")
    }
    
    fun getUsbSource(): UsbMusicSource? {
        return sources.values.filterIsInstance<UsbMusicSource>().firstOrNull()
    }
    
    fun getMediaStoreSource(): MediaStoreSource? {
        return sources.values.filterIsInstance<MediaStoreSource>().firstOrNull()
    }
    
    fun getLocalSource(): LocalSource? {
        return sources.values.filterIsInstance<LocalSource>().firstOrNull()
    }
    
    suspend fun scanAll(): Result<Int> {
        _managerState.value = _managerState.value.copy(isScanning = true)
        var totalCount = 0
        
        try {
            for ((sourceId, source) in sources) {
                val config = sourceConfigs[sourceId]
                if (config?.enabled == true && source.isAvailable) {
                    val result = source.scan(context)
                    result.getOrNull()?.let { count ->
                        totalCount += count
                        Log.i(TAG, "源 $sourceId 扫描完成: $count 首音乐")
                    }
                }
            }
            
            _managerState.value = ManagerState(
                totalTracks = totalCount,
                sourceCount = sources.size,
                isScanning = false
            )
            
            _activeSourceIds.value = sources.keys
            
            Log.i(TAG, "所有源扫描完成，共 $totalCount 首音乐")
            return Result.success(totalCount)
        } catch (e: Exception) {
            _managerState.value = _managerState.value.copy(
                isScanning = false,
                lastError = e.message
            )
            return Result.failure(e)
        }
    }
    
    suspend fun scanSource(sourceId: String): Result<Int> {
        val source = sources[sourceId] ?: return Result.failure(Exception("源不存在: $sourceId"))
        val config = sourceConfigs[sourceId]
        
        if (config?.enabled != true) {
            return Result.failure(Exception("源已禁用: $sourceId"))
        }
        
        if (!source.isAvailable) {
            return Result.failure(Exception("源不可用: $sourceId"))
        }
        
        return source.scan(context)
    }
    
    suspend fun getAllTracks(): List<Track> {
        val allTracks = mutableListOf<Track>()
        for ((sourceId, source) in sources) {
            val config = sourceConfigs[sourceId]
            if (config?.enabled == true) {
                allTracks.addAll(source.getTracks())
            }
        }
        return allTracks
    }
    
    suspend fun getTracksBySource(sourceId: String): List<Track> {
        return sources[sourceId]?.getTracks() ?: emptyList()
    }
    
    suspend fun searchAll(query: String): List<Track> {
        val results = mutableListOf<Track>()
        for ((sourceId, source) in sources) {
            val config = sourceConfigs[sourceId]
            if (config?.enabled == true) {
                results.addAll(source.search(query))
            }
        }
        return results
    }
    
    suspend fun getTrackById(id: String): Track? {
        for (source in sources.values) {
            val track = source.getTrackById(id)
            if (track != null) return track
        }
        return null
    }
    
    fun getSourceStates(): Map<String, SourceState> {
        return sources.mapValues { (_, source) -> source.stateFlow.value }
    }
    
    fun setSourceEnabled(sourceId: String, enabled: Boolean) {
        val config = sourceConfigs[sourceId] ?: return
        when (config) {
            is UsbSourceConfig -> sourceConfigs[sourceId] = config.copy(enabled = enabled)
            is MediaStoreSourceConfig -> sourceConfigs[sourceId] = config.copy(enabled = enabled)
            is LocalSourceConfig -> sourceConfigs[sourceId] = config.copy(enabled = enabled)
        }
        Log.i(TAG, "源 $sourceId ${if (enabled) "已启用" else "已禁用"}")
    }
    
    fun updateUsbRootUri(uri: Uri) {
        val usbSource = getUsbSource()
        if (usbSource != null) {
            usbSource.updateRootUri(uri)
            Log.i(TAG, "USB 根目录已更新: $uri")
        }
    }
    
    fun releaseAll() {
        for (source in sources.values) {
            source.release()
        }
        sources.clear()
        sourceConfigs.clear()
        _managerState.value = ManagerState()
        _activeSourceIds.value = emptySet()
        Log.i(TAG, "所有源已释放")
    }
    
    fun getSourceInfo(): List<SourceInfo> {
        return sources.map { (id, source) ->
            SourceInfo(
                sourceId = id,
                name = source.name,
                description = source.description,
                type = source.sourceType,
                isAvailable = source.isAvailable,
                isEnabled = sourceConfigs[id]?.enabled ?: false,
                state = source.stateFlow.value
            )
        }
    }
}

data class SourceInfo(
    val sourceId: String,
    val name: String,
    val description: String,
    val type: SourceType,
    val isAvailable: Boolean,
    val isEnabled: Boolean,
    val state: SourceState
)
