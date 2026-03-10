package com.music.localmusic

import android.content.Context
import android.util.Log
import com.music.core.api.models.MusicHints
import com.music.localmusic.models.Track
import com.music.localmusic.source.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MultiSourceMusicRepository(
    private val context: Context,
    private val sourceManager: MusicSourceManager
) {
    companion object {
        private const val TAG = "MultiSourceRepo"
        private const val DEFAULT_LIMIT = 50
    }
    
    suspend fun initialize(): Boolean {
        return try {
            val localConfig = LocalSourceConfig(enabled = true)
            val localSource = LocalSource(localConfig)
            sourceManager.registerSource(localSource, localConfig)
            
            val mediaStoreConfig = MediaStoreSourceConfig(enabled = true)
            val mediaStoreSource = MediaStoreSource(mediaStoreConfig)
            sourceManager.registerSource(mediaStoreSource, mediaStoreConfig)
            
            val usbConfig = UsbSourceConfig(enabled = false)
            val usbSource = UsbMusicSource(usbConfig)
            sourceManager.registerSource(usbSource, usbConfig)
            
            val result = sourceManager.scanAll()
            result.isSuccess
        } catch (e: Exception) {
            Log.e(TAG, "初始化失败", e)
            false
        }
    }
    
    suspend fun enableUsbSource(uri: android.net.Uri) {
        UsbStorageHelper.takePersistablePermission(context, uri)
        UsbStorageHelper.saveUsbUri(context, uri)
        
        sourceManager.updateUsbRootUri(uri)
        sourceManager.setSourceEnabled("usb_default", true)
        sourceManager.scanSource("usb_default")
    }
    
    suspend fun disableUsbSource() {
        val storedUri = UsbStorageHelper.loadUsbUri(context)
        storedUri?.let { UsbStorageHelper.releasePersistablePermission(context, it) }
        UsbStorageHelper.clearUsbUri(context)
        
        sourceManager.setSourceEnabled("usb_default", false)
    }
    
    suspend fun queryTracks(hints: MusicHints): List<Track> = withContext(Dispatchers.IO) {
        val allTracks = sourceManager.getAllTracks()
        filterAndSortTracks(allTracks, hints, DEFAULT_LIMIT)
    }
    
    suspend fun queryTracksWithLimit(hints: MusicHints, limit: Int): List<Track> = withContext(Dispatchers.IO) {
        val allTracks = sourceManager.getAllTracks()
        filterAndSortTracks(allTracks, hints, limit)
    }
    
    private fun filterAndSortTracks(tracks: List<Track>, hints: MusicHints, limit: Int): List<Track> {
        var filtered = tracks
        
        hints.genres?.let { genres ->
            if (genres.isNotEmpty()) {
                filtered = filtered.filter { track ->
                    track.genre?.let { genre -> genres.any { it.equals(genre, ignoreCase = true) } } == true
                }
            }
        }
        
        hints.tempo?.let { tempo ->
            val (minBpm, maxBpm) = when (tempo.lowercase()) {
                "slow" -> 60 to 90
                "medium", "moderate" -> 90 to 120
                "fast" -> 120 to 180
                else -> return@let
            }
            filtered = filtered.filter { track ->
                track.bpm?.let { it in minBpm..maxBpm } != false
            }
        }
        
        filtered = filtered.shuffled()
        
        return filtered.take(limit)
    }
    
    suspend fun searchTracks(keyword: String): List<Track> {
        return sourceManager.searchAll(keyword).take(DEFAULT_LIMIT)
    }
    
    suspend fun searchTracksWithLimit(keyword: String, limit: Int): List<Track> {
        return sourceManager.searchAll(keyword).take(limit)
    }
    
    suspend fun getTracksByGenre(genre: String, limit: Int = DEFAULT_LIMIT): List<Track> {
        val allTracks = sourceManager.getAllTracks()
        return allTracks.filter { 
            it.genre?.equals(genre, ignoreCase = true) == true 
        }.shuffled().take(limit)
    }
    
    suspend fun getTracksByArtist(artist: String, limit: Int = DEFAULT_LIMIT): List<Track> {
        val allTracks = sourceManager.getAllTracks()
        return allTracks.filter { 
            it.artist.contains(artist, ignoreCase = true) 
        }.take(limit)
    }
    
    suspend fun getTracksByMood(moodTag: String, limit: Int = DEFAULT_LIMIT): List<Track> {
        val allTracks = sourceManager.getAllTracks()
        return allTracks.filter { track ->
            track.moodTags?.any { it.equals(moodTag, ignoreCase = true) } == true
        }.shuffled().take(limit)
    }
    
    suspend fun getTracksByScene(sceneTag: String, limit: Int = DEFAULT_LIMIT): List<Track> {
        val allTracks = sourceManager.getAllTracks()
        return allTracks.filter { track ->
            track.sceneTags?.any { it.equals(sceneTag, ignoreCase = true) } == true
        }.shuffled().take(limit)
    }
    
    suspend fun getTracksByBpmRange(minBpm: Int, maxBpm: Int, limit: Int = DEFAULT_LIMIT): List<Track> {
        val allTracks = sourceManager.getAllTracks()
        return allTracks.filter { track ->
            track.bpm?.let { it in minBpm..maxBpm } == true
        }.shuffled().take(limit)
    }
    
    suspend fun getTracksByEnergyRange(minEnergy: Double, maxEnergy: Double, limit: Int = DEFAULT_LIMIT): List<Track> {
        val allTracks = sourceManager.getAllTracks()
        return allTracks.filter { track ->
            track.energy?.let { it in minEnergy..maxEnergy } == true
        }.shuffled().take(limit)
    }
    
    suspend fun getAllTracks(limit: Int = DEFAULT_LIMIT): List<Track> {
        return sourceManager.getAllTracks().take(limit)
    }
    
    fun getTrackCount(): Int {
        return sourceManager.managerState.value.totalTracks
    }
    
    fun isReady(): Boolean {
        return sourceManager.managerState.value.totalTracks > 0
    }
    
    fun getSourceInfo(): List<SourceInfo> {
        return sourceManager.getSourceInfo()
    }
    
    fun setSourceEnabled(sourceId: String, enabled: Boolean) {
        sourceManager.setSourceEnabled(sourceId, enabled)
    }
    
    fun release() {
        sourceManager.releaseAll()
    }
}
