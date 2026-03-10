package com.music.localmusic.source

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.music.localmusic.models.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

class UsbMusicSource(
    private val config: UsbSourceConfig
) : MusicSource {
    
    companion object {
        private const val TAG = "UsbMusicSource"
        private val SUPPORTED_EXTENSIONS = setOf("mp3", "flac", "wav", "m4a", "aac", "ogg", "wma")
    }
    
    override val sourceId: String = config.sourceId
    override val sourceType: SourceType = SourceType.Usb
    override val name: String = "USB 存储"
    override val description: String = "从 USB 存储设备加载音乐"
    
    private val _stateFlow = MutableStateFlow<SourceState>(SourceState.Idle)
    override val stateFlow: StateFlow<SourceState> = _stateFlow.asStateFlow()
    
    private var tracks: MutableList<Track> = mutableListOf()
    private val idGenerator = AtomicLong(1)
    
    override val isAvailable: Boolean
        get() = config.rootUri != null
    
    override suspend fun scan(context: Context): Result<Int> = withContext(Dispatchers.IO) {
        val rootUri = config.rootUri
        if (rootUri == null) {
            _stateFlow.value = SourceState.Error("未选择 USB 目录")
            return@withContext Result.failure(Exception("未选择 USB 目录"))
        }
        
        _stateFlow.value = SourceState.Scanning
        tracks.clear()
        
        try {
            val rootDoc = DocumentFile.fromTreeUri(context, rootUri)
            if (rootDoc == null || !rootDoc.exists()) {
                _stateFlow.value = SourceState.Error("无法访问 USB 目录")
                return@withContext Result.failure(Exception("无法访问 USB 目录"))
            }
            
            scanDirectory(context, rootDoc, 0)
            
            _stateFlow.value = SourceState.Ready(tracks.size)
            Log.i(TAG, "扫描完成，共找到 ${tracks.size} 首音乐")
            Result.success(tracks.size)
        } catch (e: Exception) {
            Log.e(TAG, "扫描失败", e)
            _stateFlow.value = SourceState.Error(e.message ?: "扫描失败")
            Result.failure(e)
        }
    }
    
    private fun scanDirectory(context: Context, directory: DocumentFile, depth: Int) {
        if (depth > config.maxScanDepth) return
        
        try {
            val children = directory.listFiles()
            for (child in children) {
                if (child.isDirectory && config.scanSubdirectories) {
                    scanDirectory(context, child, depth + 1)
                } else if (child.isFile) {
                    val name = child.name ?: continue
                    val extension = name.substringAfterLast('.', "").lowercase()
                    
                    if (extension in SUPPORTED_EXTENSIONS) {
                        val track = createTrackFromDocument(child)
                        tracks.add(track)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "扫描目录失败: ${directory.name}", e)
        }
    }
    
    private fun createTrackFromDocument(document: DocumentFile): Track {
        val name = document.name ?: "Unknown"
        val title = name.substringBeforeLast('.')
        val uri = document.uri.toString()
        
        return Track(
            id = uri.hashCode().toString(),
            title = title,
            artist = guessArtistFromPath(uri),
            album = guessAlbumFromPath(uri),
            durationMs = 0,
            filePath = uri,
            format = name.substringAfterLast('.', "").lowercase()
        )
    }
    
    private fun guessArtistFromPath(path: String): String {
        val segments = path.split("/")
        val artistIndex = segments.indexOfFirst { 
            it.equals("artist", ignoreCase = true) || 
            it.equals("artists", ignoreCase = true) 
        }
        if (artistIndex >= 0 && artistIndex + 1 < segments.size) {
            return segments[artistIndex + 1]
        }
        return "Unknown Artist"
    }
    
    private fun guessAlbumFromPath(path: String): String? {
        val segments = path.split("/")
        val albumIndex = segments.indexOfFirst { 
            it.equals("album", ignoreCase = true) || 
            it.equals("albums", ignoreCase = true) 
        }
        if (albumIndex >= 0 && albumIndex + 1 < segments.size) {
            return segments[albumIndex + 1]
        }
        return null
    }
    
    override suspend fun getTracks(): List<Track> {
        return tracks.toList()
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
        tracks.clear()
        _stateFlow.value = SourceState.Idle
    }
    
    fun updateRootUri(uri: Uri) {
        val newConfig = config.copy(rootUri = uri)
        tracks.clear()
        _stateFlow.value = SourceState.Idle
    }
}
