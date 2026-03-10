package com.music.localmusic.source

import android.content.Context
import android.util.Log
import com.music.localmusic.LocalMusicIndex
import com.music.localmusic.models.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class LocalSource(
    private val config: LocalSourceConfig
) : MusicSource {
    
    companion object {
        private const val TAG = "LocalSource"
    }
    
    override val sourceId: String = config.sourceId
    override val sourceType: SourceType = SourceType.Local
    override val name: String = "本地存储"
    override val description: String = "从本地索引文件加载音乐"
    
    private val _stateFlow = MutableStateFlow<SourceState>(SourceState.Idle)
    override val stateFlow: StateFlow<SourceState> = _stateFlow.asStateFlow()
    
    private var tracks: List<Track> = emptyList()
    
    override val isAvailable: Boolean = true
    
    override suspend fun scan(context: Context): Result<Int> = withContext(Dispatchers.IO) {
        _stateFlow.value = SourceState.Scanning
        
        try {
            val musicIndex = LocalMusicIndex.getInstance(context)
            
            if (!musicIndex.isReady()) {
                val initResult = musicIndex.initialize()
                
                if (!initResult) {
                    _stateFlow.value = SourceState.Error("初始化失败")
                    return@withContext Result.failure(Exception("初始化失败"))
                }
            }
            
            tracks = musicIndex.getAllTracks()
            _stateFlow.value = SourceState.Ready(tracks.size)
            Log.i(TAG, "加载完成，共 ${tracks.size} 首音乐")
            Result.success(tracks.size)
        } catch (e: Exception) {
            Log.e(TAG, "加载失败", e)
            _stateFlow.value = SourceState.Error(e.message ?: "加载失败")
            Result.failure(e)
        }
    }
    
    override suspend fun getTracks(): List<Track> {
        return tracks
    }
    
    override suspend fun getTrackById(id: String): Track? {
        return tracks.find { it.id == id }
    }
    
    override suspend fun search(query: String): List<Track> {
        val lowerQuery = query.lowercase()
        return tracks.filter { 
            it.title.lowercase().contains(lowerQuery) ||
            it.artist.lowercase().contains(lowerQuery) ||
            it.album?.lowercase()?.contains(lowerQuery) == true
        }
    }
    
    override fun release() {
        tracks = emptyList()
        _stateFlow.value = SourceState.Idle
    }
}
