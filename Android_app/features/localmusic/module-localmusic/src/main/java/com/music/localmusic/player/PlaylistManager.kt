package com.music.localmusic.player

import com.music.core.api.models.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PlaylistManager {
    private val _playlist = MutableStateFlow<List<Track>>(emptyList())
    val playlist: StateFlow<List<Track>> = _playlist.asStateFlow()
    
    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()
    
    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()
    
    private val _shuffleMode = MutableStateFlow(ShuffleMode.OFF)
    val shuffleMode: StateFlow<ShuffleMode> = _shuffleMode.asStateFlow()
    
    private var shuffledIndices: List<Int> = emptyList()
    
    val currentTrack: Track?
        get() {
            val index = _currentIndex.value
            val list = _playlist.value
            return if (index >= 0 && index < list.size) list[index] else null
        }
    
    val hasNext: Boolean
        get() {
            val list = _playlist.value
            val repeat = _repeatMode.value
            return when {
                list.isEmpty() -> false
                repeat == RepeatMode.ALL -> true
                _shuffleMode.value == ShuffleMode.ON -> shuffledIndices.isNotEmpty()
                else -> _currentIndex.value < list.size - 1
            }
        }
    
    val hasPrevious: Boolean
        get() {
            val list = _playlist.value
            val repeat = _repeatMode.value
            return when {
                list.isEmpty() -> false
                repeat == RepeatMode.ALL -> true
                _shuffleMode.value == ShuffleMode.ON -> shuffledIndices.isNotEmpty()
                else -> _currentIndex.value > 0
            }
        }
    
    fun setPlaylist(tracks: List<Track>, startIndex: Int = 0) {
        _playlist.value = tracks
        _currentIndex.value = if (startIndex in tracks.indices) startIndex else 0
        regenerateShuffleIndices()
    }
    
    fun addToPlaylist(tracks: List<Track>) {
        val currentList = _playlist.value.toMutableList()
        currentList.addAll(tracks)
        _playlist.value = currentList
        regenerateShuffleIndices()
    }
    
    fun removeFromPlaylist(index: Int) {
        val currentList = _playlist.value.toMutableList()
        if (index in currentList.indices) {
            currentList.removeAt(index)
            _playlist.value = currentList
            
            when {
                _currentIndex.value > index -> _currentIndex.value -= 1
                _currentIndex.value == index -> {
                    if (currentList.isEmpty()) {
                        _currentIndex.value = -1
                    } else if (_currentIndex.value >= currentList.size) {
                        _currentIndex.value = 0
                    }
                }
            }
            regenerateShuffleIndices()
        }
    }
    
    fun clearPlaylist() {
        _playlist.value = emptyList()
        _currentIndex.value = -1
        shuffledIndices = emptyList()
    }
    
    fun moveToNext(): Track? {
        val list = _playlist.value
        if (list.isEmpty()) return null
        
        when {
            _shuffleMode.value == ShuffleMode.ON -> {
                val currentShuffleIndex = shuffledIndices.indexOf(_currentIndex.value)
                if (currentShuffleIndex < shuffledIndices.size - 1) {
                    _currentIndex.value = shuffledIndices[currentShuffleIndex + 1]
                } else if (_repeatMode.value == RepeatMode.ALL) {
                    _currentIndex.value = shuffledIndices.first()
                } else {
                    return null
                }
            }
            _currentIndex.value < list.size - 1 -> {
                _currentIndex.value += 1
            }
            _repeatMode.value == RepeatMode.ALL -> {
                _currentIndex.value = 0
            }
            else -> return null
        }
        
        return currentTrack
    }
    
    fun moveToPrevious(): Track? {
        val list = _playlist.value
        if (list.isEmpty()) return null
        
        when {
            _shuffleMode.value == ShuffleMode.ON -> {
                val currentShuffleIndex = shuffledIndices.indexOf(_currentIndex.value)
                if (currentShuffleIndex > 0) {
                    _currentIndex.value = shuffledIndices[currentShuffleIndex - 1]
                } else if (_repeatMode.value == RepeatMode.ALL) {
                    _currentIndex.value = shuffledIndices.last()
                } else {
                    return null
                }
            }
            _currentIndex.value > 0 -> {
                _currentIndex.value -= 1
            }
            _repeatMode.value == RepeatMode.ALL -> {
                _currentIndex.value = list.size - 1
            }
            else -> return null
        }
        
        return currentTrack
    }
    
    fun setCurrentIndex(index: Int) {
        if (index in _playlist.value.indices) {
            _currentIndex.value = index
        }
    }
    
    fun toggleRepeatMode() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
    }
    
    fun setRepeatMode(mode: RepeatMode) {
        _repeatMode.value = mode
    }
    
    fun toggleShuffleMode() {
        _shuffleMode.value = if (_shuffleMode.value == ShuffleMode.OFF) {
            ShuffleMode.ON
        } else {
            ShuffleMode.OFF
        }
        regenerateShuffleIndices()
    }
    
    fun setShuffleMode(mode: ShuffleMode) {
        _shuffleMode.value = mode
        regenerateShuffleIndices()
    }
    
    private fun regenerateShuffleIndices() {
        val list = _playlist.value
        if (list.isEmpty()) {
            shuffledIndices = emptyList()
            return
        }
        
        shuffledIndices = list.indices.shuffled()
    }
    
    fun findTrackById(trackId: String): Pair<Int, Track>? {
        val list = _playlist.value
        val index = list.indexOfFirst { it.track_id == trackId }
        return if (index >= 0) index to list[index] else null
    }
    
    fun getTrackAt(index: Int): Track? {
        val list = _playlist.value
        return if (index in list.indices) list[index] else null
    }
}
