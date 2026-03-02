package com.example.layer3.api

import com.music.core.api.models.SceneDescriptor
import com.music.core.api.models.Track
import kotlinx.coroutines.flow.Flow

interface IContentEngine {
    val playlistFlow: Flow<List<Track>>
    
    suspend fun generatePlaylist(scene: SceneDescriptor): Result<List<Track>>
    
    fun play(playlist: List<Track>)
    
    fun pause()
    
    fun resume()
    
    fun stop()
    
    fun next()
    
    fun previous()
    
    fun reset()
}
