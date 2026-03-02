package com.music.localmusic.source

import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import android.util.Log
import com.music.localmusic.models.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class MediaStoreSource(
    private val config: MediaStoreSourceConfig
) : MusicSource {
    
    companion object {
        private const val TAG = "MediaStoreSource"
    }
    
    override val sourceId: String = config.sourceId
    override val sourceType: SourceType = SourceType.MediaStore
    override val name: String = "系统媒体库"
    override val description: String = "从系统媒体库加载音乐"
    
    private val _stateFlow = MutableStateFlow<SourceState>(SourceState.Idle)
    override val stateFlow: StateFlow<SourceState> = _stateFlow.asStateFlow()
    
    private var tracks: MutableList<Track> = mutableListOf()
    
    override val isAvailable: Boolean = true
    
    override suspend fun scan(context: Context): Result<Int> = withContext(Dispatchers.IO) {
        _stateFlow.value = SourceState.Scanning
        tracks.clear()
        
        try {
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.MIME_TYPE,
                MediaStore.Audio.Media.GENRE
            )
            
            val selection = "${MediaStore.Audio.Media.DURATION} >= ?"
            val selectionArgs = arrayOf(config.minDurationMs.toString())
            
            val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"
            
            val cursor: Cursor? = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )
            
            cursor?.use {
                val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val genreColumn = it.getColumnIndex(MediaStore.Audio.Media.GENRE)
                
                while (it.moveToNext()) {
                    val id = it.getLong(idColumn)
                    val title = it.getString(titleColumn) ?: "Unknown"
                    val artist = it.getString(artistColumn) ?: "Unknown Artist"
                    val album = it.getString(albumColumn)
                    val duration = it.getInt(durationColumn)
                    val data = it.getString(dataColumn) ?: ""
                    val genre = if (genreColumn >= 0) it.getString(genreColumn) else null
                    
                    if (config.includeGenres != null && genre !in config.includeGenres) {
                        continue
                    }
                    if (config.excludeGenres != null && genre in config.excludeGenres) {
                        continue
                    }
                    
                    val track = Track(
                        id = id.toString(),
                        title = title,
                        artist = artist,
                        album = album,
                        genre = genre,
                        durationMs = duration,
                        filePath = data,
                        format = data.substringAfterLast('.', "").lowercase()
                    )
                    tracks.add(track)
                }
            }
            
            _stateFlow.value = SourceState.Ready(tracks.size)
            Log.i(TAG, "扫描完成，共找到 ${tracks.size} 首音乐")
            Result.success(tracks.size)
        } catch (e: Exception) {
            Log.e(TAG, "扫描失败", e)
            _stateFlow.value = SourceState.Error(e.message ?: "扫描失败")
            Result.failure(e)
        }
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
            it.album?.lowercase()?.contains(lowerQuery) == true ||
            it.genre?.lowercase()?.contains(lowerQuery) == true
        }
    }
    
    override fun release() {
        tracks.clear()
        _stateFlow.value = SourceState.Idle
    }
}
