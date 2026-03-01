package com.example.layer3.sdk.engine

import android.content.Context
import com.example.layer3.api.*
import com.example.layer3.api.model.*
import com.example.layer3.sdk.algorithm.ArtistDiversityFilter
import com.example.layer3.sdk.algorithm.MusicScorer
import com.example.layer3.sdk.data.CacheManager
import com.example.layer3.sdk.data.MusicLibraryLoader
import com.example.layer3.sdk.data.TrackData
import com.example.layer3.sdk.util.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class ContentEngine(
    private val context: Context,
    private val config: ContentProviderConfig
) : IContentEngine {
    private val musicLoader = MusicLibraryLoader(context)
    private val scorer = MusicScorer()
    private val diversityFilter = ArtistDiversityFilter(maxTracksPerArtist = 2)
    private val playlistCache = CacheManager<Playlist>(maxSize = 20)
    
    private val _currentPlaylistFlow = MutableStateFlow<Playlist?>(null)
    private val _currentTrackFlow = MutableStateFlow<Track?>(null)
    private val _playbackStateFlow = MutableStateFlow(PlaybackState())
    
    private var currentPlaylist: Playlist? = null
    private var currentTrackIndex = 0
    private var playbackState = PlaybackState()

    override val currentPlaylistFlow: Flow<Playlist?> = _currentPlaylistFlow.asStateFlow()
    override val currentTrackFlow: Flow<Track?> = _currentTrackFlow.asStateFlow()
    override val playbackStateFlow: Flow<PlaybackState> = _playbackStateFlow.asStateFlow()

    suspend fun initialize() {
        musicLoader.loadLibrary()
        Logger.i("ContentEngine: Initialized")
    }

    override suspend fun generatePlaylist(scene: SceneDescriptor): Result<Playlist> {
        return try {
            val cacheKey = "playlist_${scene.sceneId}"
            playlistCache.get(cacheKey)?.let {
                Logger.d("ContentEngine: Returning cached playlist for scene ${scene.sceneId}")
                return Result.success(it)
            }

            val allTracks = musicLoader.getAllTracks()
            if (allTracks.isEmpty()) {
                return Result.failure(Layer3Error.ContentError("No tracks available in library"))
            }

            val scoredTracks = scorer.scoreTracks(allTracks, scene)
            val diverseTracks = diversityFilter.applyDiversityWithScore(scoredTracks)
            
            val playlistSize = calculatePlaylistSize(scene)
            val selectedTracks = diverseTracks
                .take(playlistSize)
                .map { musicLoader.toTrack(it.first) }

            val playlist = Playlist(
                id = UUID.randomUUID().toString(),
                name = generatePlaylistName(scene),
                description = "Generated for scene: ${scene.sceneName ?: scene.sceneId}",
                tracks = selectedTracks,
                trackCount = selectedTracks.size,
                totalDurationMs = selectedTracks.sumOf { it.duration_ms },
                sceneId = scene.sceneId,
                tags = extractTags(scene)
            )

            playlistCache.put(cacheKey, playlist)
            currentPlaylist = playlist
            currentTrackIndex = 0
            _currentPlaylistFlow.value = playlist
            
            Logger.i("ContentEngine: Generated playlist with ${playlist.tracks.size} tracks")
            Result.success(playlist)
        } catch (e: Exception) {
            Logger.e("ContentEngine: Failed to generate playlist", e)
            Result.failure(Layer3Error.ContentError("Failed to generate playlist: ${e.message}"))
        }
    }

    override suspend fun getRecommendations(basedOn: String, limit: Int): Result<PlaylistRecommendation> {
        return try {
            val tracks = musicLoader.searchTracks(basedOn)
            val selectedTracks = tracks.take(limit).map { musicLoader.toTrack(it) }
            
            val playlist = Playlist(
                id = UUID.randomUUID().toString(),
                name = "Recommendations for: $basedOn",
                tracks = selectedTracks,
                trackCount = selectedTracks.size
            )
            
            Result.success(PlaylistRecommendation(
                playlists = listOf(playlist),
                basedOn = basedOn,
                confidence = if (tracks.isNotEmpty()) 0.8 else 0.0
            ))
        } catch (e: Exception) {
            Result.failure(Layer3Error.ContentError("Failed to get recommendations: ${e.message}"))
        }
    }

    override suspend fun playPlaylist(playlist: Playlist): Result<Unit> {
        return try {
            currentPlaylist = playlist
            currentTrackIndex = 0
            playbackState = playbackState.copy(is_playing = true)
            updateFlows()
            Logger.i("ContentEngine: Started playing playlist ${playlist.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Layer3Error.ContentError("Failed to play playlist: ${e.message}"))
        }
    }

    override suspend fun playTrack(track: Track): Result<Unit> {
        return try {
            _currentTrackFlow.value = track
            playbackState = playbackState.copy(is_playing = true)
            _playbackStateFlow.value = playbackState
            Logger.i("ContentEngine: Playing track ${track.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Layer3Error.ContentError("Failed to play track: ${e.message}"))
        }
    }

    override suspend fun pause(): Result<Unit> {
        playbackState = playbackState.copy(is_playing = false)
        _playbackStateFlow.value = playbackState
        return Result.success(Unit)
    }

    override suspend fun resume(): Result<Unit> {
        playbackState = playbackState.copy(is_playing = true)
        _playbackStateFlow.value = playbackState
        return Result.success(Unit)
    }

    override suspend fun next(): Result<Unit> {
        val playlist = currentPlaylist ?: return Result.failure(Layer3Error.ContentError("No playlist loaded"))
        if (currentTrackIndex < playlist.tracks.size - 1) {
            currentTrackIndex++
            updateFlows()
            return Result.success(Unit)
        }
        return Result.failure(Layer3Error.ContentError("Already at last track"))
    }

    override suspend fun previous(): Result<Unit> {
        if (currentTrackIndex > 0) {
            currentTrackIndex--
            updateFlows()
            return Result.success(Unit)
        }
        return Result.failure(Layer3Error.ContentError("Already at first track"))
    }

    override suspend fun seek(positionMs: Long): Result<Unit> {
        playbackState = playbackState.copy(position_ms = positionMs)
        _playbackStateFlow.value = playbackState
        return Result.success(Unit)
    }

    override suspend fun setVolume(level: Double): Result<Unit> {
        return Result.success(Unit)
    }

    override fun getCurrentTrack(): Track? {
        return currentPlaylist?.tracks?.getOrNull(currentTrackIndex)
    }

    override fun getCurrentPlaylist(): Playlist? = currentPlaylist

    override fun getPlaybackState(): PlaybackState = playbackState

    private fun updateFlows() {
        val track = currentPlaylist?.tracks?.getOrNull(currentTrackIndex)
        _currentTrackFlow.value = track
        _playbackStateFlow.value = playbackState
    }

    private fun calculatePlaylistSize(scene: SceneDescriptor): Int {
        val energy = scene.intent.energyLevel
        return when {
            energy > 0.7 -> 20
            energy > 0.4 -> 15
            else -> 10
        }
    }

    private fun generatePlaylistName(scene: SceneDescriptor): String {
        return scene.sceneName ?: "Scene ${scene.sceneId}"
    }

    private fun extractTags(scene: SceneDescriptor): List<String> {
        val tags = mutableListOf<String>()
        scene.hints?.music?.genres?.let { tags.addAll(it) }
        scene.intent.atmosphere.takeIf { it.isNotEmpty() }?.let { tags.add(it) }
        return tags.distinct()
    }

    fun destroy() {
        playlistCache.clear()
        _currentPlaylistFlow.value = null
        _currentTrackFlow.value = null
        Logger.i("ContentEngine: Destroyed")
    }
}
