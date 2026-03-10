package com.music.localmusic.source

import android.content.Context
import android.net.Uri
import com.music.localmusic.models.Track
import kotlinx.coroutines.flow.StateFlow

sealed class SourceState {
    object Idle : SourceState()
    object Scanning : SourceState()
    data class Ready(val trackCount: Int) : SourceState()
    data class Error(val message: String) : SourceState()
    object Unavailable : SourceState()
}

sealed class SourceType(val displayName: String) {
    object Local : SourceType("本地存储")
    object Usb : SourceType("USB存储")
    object MediaStore : SourceType("系统媒体库")
}

interface MusicSource {
    val sourceId: String
    val sourceType: SourceType
    val stateFlow: StateFlow<SourceState>
    
    val name: String
    val description: String
    val isAvailable: Boolean
    
    suspend fun scan(context: Context): Result<Int>
    
    suspend fun getTracks(): List<Track>
    
    suspend fun getTrackById(id: String): Track?
    
    suspend fun search(query: String): List<Track>
    
    fun release()
}

interface MusicSourceConfig {
    val sourceId: String
    val enabled: Boolean
}

data class UsbSourceConfig(
    override val sourceId: String = "usb_default",
    override val enabled: Boolean = true,
    val rootUri: Uri? = null,
    val supportedFormats: List<String> = listOf("mp3", "flac", "wav", "m4a", "aac", "ogg"),
    val scanSubdirectories: Boolean = true,
    val maxScanDepth: Int = 5
) : MusicSourceConfig

data class MediaStoreSourceConfig(
    override val sourceId: String = "mediastore_default",
    override val enabled: Boolean = true,
    val includeGenres: List<String>? = null,
    val excludeGenres: List<String>? = null,
    val minDurationMs: Int = 30000
) : MusicSourceConfig

data class LocalSourceConfig(
    override val sourceId: String = "local_default",
    override val enabled: Boolean = true,
    val indexPath: String = "",
    val musicPath: String = ""
) : MusicSourceConfig
