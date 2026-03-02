package com.example.layer3.sdk.engine

import android.content.Context
import android.util.Log
import com.example.layer3.api.IContentEngine
import com.music.core.api.models.SceneDescriptor
import com.music.core.api.models.ContentCommand
import com.music.core.api.models.Track
import com.example.layer3.sdk.util.Logger
import com.music.localmusic.LocalMusicIndex
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ContentEngine(
    private val context: Context
) : IContentEngine {
    
    companion object {
        private const val TAG = "ContentEngine"
    }
    
    private val _contentStateFlow = MutableStateFlow(ContentCommand(action = "play"))
    val contentStateFlow: Flow<ContentCommand> = _contentStateFlow.asStateFlow()

    private val _playlistFlow = MutableStateFlow<List<Track>>(emptyList())
    override val playlistFlow: Flow<List<Track>> = _playlistFlow.asStateFlow()

    override suspend fun generatePlaylist(scene: SceneDescriptor): Result<List<Track>> {
        return try {
            val localTracks = LocalMusicIndex.getInstance(context).getAllTracks()
            Log.i(TAG, "LocalMusicIndex tracks count: ${localTracks.size}")
            
            if (localTracks.isEmpty()) {
                Log.w(TAG, "No tracks available in LocalMusicIndex")
                return Result.success(emptyList())
            }
            
            val matchedTracks = matchTracksByScene(localTracks, scene)
            Log.i(TAG, "Matched ${matchedTracks.size} tracks for scene: ${scene.scene_name}")
            
            val playlist = matchedTracks.take(10).map { localTrack ->
                Track(
                    track_id = localTrack.id.toString(),
                    title = localTrack.title,
                    artist = localTrack.artist,
                    duration_sec = localTrack.durationMs / 1000,
                    energy = localTrack.energy
                )
            }
            
            _playlistFlow.value = playlist
            Result.success(playlist)
        } catch (e: Exception) {
            Logger.e("ContentEngine: Failed to generate content", e)
            Result.failure(e)
        }
    }
    
    private fun matchTracksByScene(
        localTracks: List<com.music.localmusic.models.Track>,
        scene: SceneDescriptor
    ): List<com.music.localmusic.models.Track> {
        val hints = scene.hints
        val intent = scene.intent
        
        val scoredTracks = localTracks.map { track ->
            var score = 0.0
            
            hints?.music?.genres?.let { genres ->
                if (track.genre != null && genres.any { it.equals(track.genre, ignoreCase = true) }) {
                    score += 30.0
                }
            }
            
            hints?.music?.tempo?.let { tempo ->
                val targetBpm = when (tempo.lowercase()) {
                    "slow" -> 70..90
                    "moderate", "medium" -> 90..120
                    "fast" -> 120..150
                    else -> 90..120
                }
                track.bpm?.let { if (it in targetBpm) score += 20.0 }
            }
            
            val valenceDiff = kotlin.math.abs((track.valence ?: 0.5) - intent.mood.valence)
            val arousalDiff = kotlin.math.abs((track.energy ?: 0.5) - intent.mood.arousal)
            score += (1.0 - valenceDiff) * 25.0
            score += (1.0 - arousalDiff) * 25.0
            
            val energyDiff = kotlin.math.abs((track.energy ?: 0.5) - intent.energy_level)
            score += (1.0 - energyDiff) * 20.0
            
            track.moodTags?.let { tags ->
                intent.atmosphere?.let { atm ->
                    if (tags.any { it.contains(atm, ignoreCase = true) }) {
                        score += 15.0
                    }
                }
            }
            
            track.sceneTags?.let { tags ->
                if (tags.any { it.equals(scene.scene_type, ignoreCase = true) }) {
                    score += 15.0
                }
            }
            
            track to score
        }.sortedByDescending { it.second }
        
        return scoredTracks.map { it.first }
    }

    override fun resume() {
        val current = _contentStateFlow.value
        _contentStateFlow.value = current.copy(action = "play")
    }

    override fun play(playlist: List<Track>) {
        val current = _contentStateFlow.value
        _contentStateFlow.value = current.copy(action = "play", playlist = playlist)
    }

    override fun pause() {
        val current = _contentStateFlow.value
        _contentStateFlow.value = current.copy(action = "pause")
    }

    override fun stop() {
        val current = _contentStateFlow.value
        _contentStateFlow.value = current.copy(action = "stop")
    }

    override fun next() {
        val current = _contentStateFlow.value
        _contentStateFlow.value = current.copy(action = "next")
    }

    override fun previous() {
        val current = _contentStateFlow.value
        _contentStateFlow.value = current.copy(action = "previous")
    }

    override fun reset() {
        _contentStateFlow.value = ContentCommand(action = "stop")
        _playlistFlow.value = emptyList()
        Logger.i("ContentEngine: Reset")
    }

    fun destroy() {
        Logger.i("ContentEngine: Destroyed")
    }
}
