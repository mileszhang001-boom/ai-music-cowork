package com.music.localmusic.player

import com.music.localmusic.models.Track
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
    
    private var originalPlaylist: List<Track> = emptyList()
    private var shuffledPlaylist: List<Track> = emptyList()
    private var currentTrackId: String? = null
    
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
                _shuffleMode.value == ShuffleMode.ON -> shuffledPlaylist.isNotEmpty()
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
                _shuffleMode.value == ShuffleMode.ON -> shuffledPlaylist.isNotEmpty()
                else -> _currentIndex.value > 0
            }
        }
    
    fun setPlaylist(tracks: List<Track>, startIndex: Int = 0) {
        originalPlaylist = tracks.toList()
        shuffledPlaylist = tracks.shuffled()
        
        val actualStartIndex = if (startIndex in tracks.indices) startIndex else 0
        currentTrackId = if (tracks.isNotEmpty()) tracks[actualStartIndex].id else null
        
        if (_shuffleMode.value == ShuffleMode.ON) {
            _playlist.value = shuffledPlaylist
            val track = tracks.getOrNull(actualStartIndex)
            _currentIndex.value = if (track != null) shuffledPlaylist.indexOfFirst { it.id == track.id } else 0
        } else {
            _playlist.value = originalPlaylist
            _currentIndex.value = actualStartIndex
        }
    }
    
    fun addToPlaylist(tracks: List<Track>) {
        val currentList = originalPlaylist.toMutableList()
        currentList.addAll(tracks)
        originalPlaylist = currentList
        shuffledPlaylist = currentList.shuffled()
        
        if (_shuffleMode.value == ShuffleMode.ON) {
            val currentTrack = currentTrack
            _playlist.value = shuffledPlaylist
            _currentIndex.value = if (currentTrack != null) shuffledPlaylist.indexOfFirst { it.id == currentTrack.id } else 0
        } else {
            _playlist.value = originalPlaylist
        }
    }
    
    fun removeFromPlaylist(index: Int) {
        val currentList = originalPlaylist.toMutableList()
        if (index in currentList.indices) {
            val removedTrack = currentList.removeAt(index)
            originalPlaylist = currentList
            shuffledPlaylist = currentList.shuffled()
            
            if (_shuffleMode.value == ShuffleMode.ON) {
                val currentTrack = currentTrack
                _playlist.value = shuffledPlaylist
                _currentIndex.value = if (currentTrack != null) shuffledPlaylist.indexOfFirst { it.id == currentTrack.id } else 0
            } else {
                _playlist.value = originalPlaylist
                
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
            }
            
            currentTrackId = currentTrack?.id
        }
    }
    
    fun clearPlaylist() {
        originalPlaylist = emptyList()
        shuffledPlaylist = emptyList()
        _playlist.value = emptyList()
        _currentIndex.value = -1
        currentTrackId = null
    }
    
    fun moveToNext(): Track? {
        val list = _playlist.value
        if (list.isEmpty()) return null
        
        when {
            _shuffleMode.value == ShuffleMode.ON -> {
                if (_currentIndex.value < list.size - 1) {
                    _currentIndex.value += 1
                } else if (_repeatMode.value == RepeatMode.ALL) {
                    _currentIndex.value = 0
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
        
        currentTrackId = currentTrack?.id
        return currentTrack
    }
    
    fun moveToPrevious(): Track? {
        val list = _playlist.value
        if (list.isEmpty()) return null
        
        when {
            _shuffleMode.value == ShuffleMode.ON -> {
                if (_currentIndex.value > 0) {
                    _currentIndex.value -= 1
                } else if (_repeatMode.value == RepeatMode.ALL) {
                    _currentIndex.value = list.size - 1
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
        
        currentTrackId = currentTrack?.id
        return currentTrack
    }
    
    fun setCurrentIndex(index: Int) {
        if (index in _playlist.value.indices) {
            _currentIndex.value = index
            currentTrackId = currentTrack?.id
        }
    }
    
    fun toggleRepeatMode(): RepeatMode {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        return _repeatMode.value
    }
    
    fun setRepeatMode(mode: RepeatMode) {
        _repeatMode.value = mode
    }
    
    fun toggleShuffle(): Boolean {
        val wasShuffleOn = _shuffleMode.value == ShuffleMode.ON
        val currentTrack = this.currentTrack
        
        _shuffleMode.value = if (wasShuffleOn) {
            ShuffleMode.OFF
        } else {
            ShuffleMode.ON
        }
        
        if (_shuffleMode.value == ShuffleMode.ON) {
            shuffledPlaylist = originalPlaylist.shuffled()
            _playlist.value = shuffledPlaylist
            _currentIndex.value = if (currentTrack != null) {
                shuffledPlaylist.indexOfFirst { it.id == currentTrack.id }.takeIf { it >= 0 } ?: 0
            } else 0
        } else {
            _playlist.value = originalPlaylist
            _currentIndex.value = if (currentTrack != null) {
                originalPlaylist.indexOfFirst { it.id == currentTrack.id }.takeIf { it >= 0 } ?: 0
            } else 0
        }
        
        return _shuffleMode.value == ShuffleMode.ON
    }
    
    fun toggleShuffleMode() {
        toggleShuffle()
    }
    
    fun setShuffleMode(mode: ShuffleMode) {
        if (_shuffleMode.value != mode) {
            toggleShuffle()
        }
    }
    
    fun shufflePlaylist() {
        if (originalPlaylist.isEmpty()) return
        
        val currentTrack = this.currentTrack
        shuffledPlaylist = originalPlaylist.shuffled()
        
        if (_shuffleMode.value == ShuffleMode.ON) {
            _playlist.value = shuffledPlaylist
            _currentIndex.value = if (currentTrack != null) {
                shuffledPlaylist.indexOfFirst { it.id == currentTrack.id }.takeIf { it >= 0 } ?: 0
            } else 0
        }
    }
    
    fun findTrackById(trackId: String): Pair<Int, Track>? {
        val list = _playlist.value
        val index = list.indexOfFirst { it.id == trackId }
        return if (index >= 0) index to list[index] else null
    }
    
    fun getTrackAt(index: Int): Track? {
        val list = _playlist.value
        return if (index in list.indices) list[index] else null
    }
}
