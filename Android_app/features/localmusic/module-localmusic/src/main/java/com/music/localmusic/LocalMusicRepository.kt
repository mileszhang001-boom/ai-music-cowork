package com.music.localmusic

import android.util.Log
import com.music.core.api.models.MusicHints
import com.music.localmusic.models.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalMusicRepository(
    private val musicIndex: LocalMusicIndex
) {
    companion object {
        private const val TAG = "LocalMusicRepository"
        private const val DEFAULT_LIMIT = 50
    }
    
    private val queryBuilder = MusicQueryBuilder()
    
    suspend fun queryTracks(hints: MusicHints): List<Track> = withContext(Dispatchers.IO) {
        if (!musicIndex.isReady()) {
            Log.w(TAG, "MusicIndex is not ready")
            return@withContext emptyList()
        }
        
        try {
            queryBuilder.reset()
            queryBuilder.applyHints(hints)
            queryBuilder.orderByRandom()
            queryBuilder.limit(DEFAULT_LIMIT)
            
            val result = queryBuilder.build()
            musicIndex.queryTracks(result.query, result.args)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to query tracks with hints", e)
            emptyList()
        }
    }
    
    suspend fun queryTracksWithLimit(hints: MusicHints, limit: Int): List<Track> = withContext(Dispatchers.IO) {
        if (!musicIndex.isReady()) {
            Log.w(TAG, "MusicIndex is not ready")
            return@withContext emptyList()
        }
        
        try {
            queryBuilder.reset()
            queryBuilder.applyHints(hints)
            queryBuilder.orderByRandom()
            queryBuilder.limit(limit)
            
            val result = queryBuilder.build()
            musicIndex.queryTracks(result.query, result.args)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to query tracks with hints", e)
            emptyList()
        }
    }
    
    suspend fun searchTracks(keyword: String): List<Track> = withContext(Dispatchers.IO) {
        if (!musicIndex.isReady()) {
            Log.w(TAG, "MusicIndex is not ready")
            return@withContext emptyList()
        }
        
        if (keyword.isBlank()) {
            return@withContext emptyList()
        }
        
        try {
            queryBuilder.reset()
            queryBuilder.whereKeyword(keyword)
            queryBuilder.orderByTitle()
            queryBuilder.limit(DEFAULT_LIMIT)
            
            val result = queryBuilder.build()
            musicIndex.queryTracks(result.query, result.args)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to search tracks with keyword: $keyword", e)
            emptyList()
        }
    }
    
    suspend fun searchTracksWithLimit(keyword: String, limit: Int): List<Track> = withContext(Dispatchers.IO) {
        if (!musicIndex.isReady()) {
            Log.w(TAG, "MusicIndex is not ready")
            return@withContext emptyList()
        }
        
        if (keyword.isBlank()) {
            return@withContext emptyList()
        }
        
        try {
            queryBuilder.reset()
            queryBuilder.whereKeyword(keyword)
            queryBuilder.orderByTitle()
            queryBuilder.limit(limit)
            
            val result = queryBuilder.build()
            musicIndex.queryTracks(result.query, result.args)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to search tracks with keyword: $keyword", e)
            emptyList()
        }
    }
    
    suspend fun getTracksByGenre(genre: String, limit: Int = DEFAULT_LIMIT): List<Track> = withContext(Dispatchers.IO) {
        if (!musicIndex.isReady()) {
            return@withContext emptyList()
        }
        
        try {
            queryBuilder.reset()
            queryBuilder.whereGenre(genre)
            queryBuilder.orderByRandom()
            queryBuilder.limit(limit)
            
            val result = queryBuilder.build()
            musicIndex.queryTracks(result.query, result.args)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get tracks by genre: $genre", e)
            emptyList()
        }
    }
    
    suspend fun getTracksByArtist(artist: String, limit: Int = DEFAULT_LIMIT): List<Track> = withContext(Dispatchers.IO) {
        if (!musicIndex.isReady()) {
            return@withContext emptyList()
        }
        
        try {
            queryBuilder.reset()
            queryBuilder.whereArtist(artist)
            queryBuilder.orderByTitle()
            queryBuilder.limit(limit)
            
            val result = queryBuilder.build()
            musicIndex.queryTracks(result.query, result.args)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get tracks by artist: $artist", e)
            emptyList()
        }
    }
    
    suspend fun getTracksByMood(moodTag: String, limit: Int = DEFAULT_LIMIT): List<Track> = withContext(Dispatchers.IO) {
        if (!musicIndex.isReady()) {
            return@withContext emptyList()
        }
        
        try {
            queryBuilder.reset()
            queryBuilder.whereMoodTag(moodTag)
            queryBuilder.orderByRandom()
            queryBuilder.limit(limit)
            
            val result = queryBuilder.build()
            musicIndex.queryTracks(result.query, result.args)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get tracks by mood: $moodTag", e)
            emptyList()
        }
    }
    
    suspend fun getTracksByScene(sceneTag: String, limit: Int = DEFAULT_LIMIT): List<Track> = withContext(Dispatchers.IO) {
        if (!musicIndex.isReady()) {
            return@withContext emptyList()
        }
        
        try {
            queryBuilder.reset()
            queryBuilder.whereSceneTag(sceneTag)
            queryBuilder.orderByRandom()
            queryBuilder.limit(limit)
            
            val result = queryBuilder.build()
            musicIndex.queryTracks(result.query, result.args)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get tracks by scene: $sceneTag", e)
            emptyList()
        }
    }
    
    suspend fun getTracksByBpmRange(minBpm: Int, maxBpm: Int, limit: Int = DEFAULT_LIMIT): List<Track> = withContext(Dispatchers.IO) {
        if (!musicIndex.isReady()) {
            return@withContext emptyList()
        }
        
        try {
            queryBuilder.reset()
            queryBuilder.whereBpmRange(minBpm, maxBpm)
            queryBuilder.orderByBpm()
            queryBuilder.limit(limit)
            
            val result = queryBuilder.build()
            musicIndex.queryTracks(result.query, result.args)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get tracks by BPM range: $minBpm-$maxBpm", e)
            emptyList()
        }
    }
    
    suspend fun getTracksByEnergyRange(minEnergy: Double, maxEnergy: Double, limit: Int = DEFAULT_LIMIT): List<Track> = withContext(Dispatchers.IO) {
        if (!musicIndex.isReady()) {
            return@withContext emptyList()
        }
        
        try {
            queryBuilder.reset()
            queryBuilder.whereEnergyRange(minEnergy, maxEnergy)
            queryBuilder.orderByEnergy()
            queryBuilder.limit(limit)
            
            val result = queryBuilder.build()
            musicIndex.queryTracks(result.query, result.args)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get tracks by energy range: $minEnergy-$maxEnergy", e)
            emptyList()
        }
    }
    
    suspend fun getAllTracks(limit: Int = DEFAULT_LIMIT): List<Track> = withContext(Dispatchers.IO) {
        if (!musicIndex.isReady()) {
            return@withContext emptyList()
        }
        
        try {
            queryBuilder.reset()
            queryBuilder.orderByTitle()
            queryBuilder.limit(limit)
            
            val result = queryBuilder.build()
            musicIndex.queryTracks(result.query, result.args)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get all tracks", e)
            emptyList()
        }
    }
    
    fun getTrackCount(): Int {
        return musicIndex.getTrackCount()
    }
    
    fun isReady(): Boolean {
        return musicIndex.isReady()
    }
}
