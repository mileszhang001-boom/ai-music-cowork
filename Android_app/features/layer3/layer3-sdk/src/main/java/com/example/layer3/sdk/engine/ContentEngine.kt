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
        private const val PLAYLIST_SIZE = 10
    }
    
    private val _contentStateFlow = MutableStateFlow(ContentCommand(action = "play"))
    val contentStateFlow: Flow<ContentCommand> = _contentStateFlow.asStateFlow()

    private val _playlistFlow = MutableStateFlow<List<Track>>(emptyList())
    override val playlistFlow: Flow<List<Track>> = _playlistFlow.asStateFlow()

    override suspend fun generatePlaylist(scene: SceneDescriptor): Result<List<Track>> {
        return try {
            val localMusicIndex = LocalMusicIndex.getInstance(context)
            if (!localMusicIndex.initialize()) {
                Log.e(TAG, "Failed to initialize LocalMusicIndex")
            }
            
            val localTracks = localMusicIndex.getAllTracks()
            Log.i(TAG, "LocalMusicIndex tracks count: ${localTracks.size}")
            
            if (localTracks.isEmpty()) {
                Log.w(TAG, "No tracks available in LocalMusicIndex")
                return Result.success(emptyList())
            }
            
            val matchedTracks = matchTracksByScene(localTracks, scene)
            Log.i(TAG, "Matched ${matchedTracks.size} tracks for scene: ${scene.scene_name}")
            
            // 优化编排：考虑 BPM 渐进、中英文混合、风格连贯
            val arrangedTracks = arrangePlaylist(matchedTracks, scene)
            
            val playlist = arrangedTracks.take(PLAYLIST_SIZE).map { localTrack ->
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
            
            val isChinese = track.genre?.contains("chinese", ignoreCase = true) == true
            
            hints?.music?.genres?.let { genres ->
                if (track.genre != null && genres.any { it.equals(track.genre, ignoreCase = true) }) {
                    score += 25.0
                }
                val needsChinese = genres.any { it.contains("chinese", ignoreCase = true) }
                if (needsChinese && isChinese) {
                    score += 20.0
                }
            }
            
            if (isChinese) {
                score += 10.0
            }
            
            hints?.music?.tempo?.let { tempo ->
                val targetBpm = when (tempo.lowercase()) {
                    "slow" -> 60..90
                    "moderate", "medium" -> 90..120
                    "fast" -> 120..160
                    else -> 90..120
                }
                track.bpm?.let { 
                    if (it in targetBpm) {
                        score += 20.0
                    } else {
                        val diff = minOf(
                            kotlin.math.abs(it - targetBpm.first),
                            kotlin.math.abs(it - targetBpm.last)
                        )
                        if (diff < 20) score += 15.0
                    }
                }
            }
            
            val valenceDiff = kotlin.math.abs((track.valence ?: 0.5) - intent.mood.valence)
            val arousalDiff = kotlin.math.abs((track.energy ?: 0.5) - intent.mood.arousal)
            score += (1.0 - valenceDiff) * 40.0
            score += (1.0 - arousalDiff) * 40.0
            
            val energyDiff = kotlin.math.abs((track.energy ?: 0.5) - intent.energy_level)
            score += (1.0 - energyDiff) * 35.0
            
            track.moodTags?.let { tags ->
                intent.atmosphere?.let { atm ->
                    if (tags.any { it.contains(atm, ignoreCase = true) }) {
                        score += 25.0
                    }
                }
            }
            
            track.sceneTags?.let { tags ->
                if (tags.any { it.equals(scene.scene_type, ignoreCase = true) }) {
                    score += 25.0
                }
            }
            
            track to score
        }.sortedByDescending { it.second }
        
        return scoredTracks.map { it.first }
    }
    
    /**
     * 智能编排播放列表
     * - BPM 渐进：从低到高或保持相近
     * - 中英文混合：避免连续切换
     * - 风格连贯：同一风格的歌曲放在一起
     */
    private fun arrangePlaylist(
        tracks: List<com.music.localmusic.models.Track>,
        scene: SceneDescriptor
    ): List<com.music.localmusic.models.Track> {
        if (tracks.size <= PLAYLIST_SIZE) return tracks
        
        // 获取场景的 BPM 趋势
        val tempo = scene.hints?.music?.tempo?.lowercase() ?: "medium"
        val ascendingBpm = tempo == "fast" || scene.intent.energy_level > 0.6
        
        // 分组：中文歌曲、英文歌曲
        val chineseTracks = tracks.filter { 
            it.genre?.contains("chinese", ignoreCase = true) == true 
        }.sortedBy { it.bpm ?: 120 }
        
        val englishTracks = tracks.filter { 
            it.genre?.contains("chinese", ignoreCase = true) != true 
        }.sortedBy { it.bpm ?: 120 }
        
        // 根据 BPM 趋势排序
        val sortedChinese = if (ascendingBpm) chineseTracks else chineseTracks.sortedByDescending { it.bpm ?: 120 }
        val sortedEnglish = if (ascendingBpm) englishTracks else englishTracks.sortedByDescending { it.bpm ?: 120 }
        
        // 混合编排策略：70% 中文 + 30% 英文
        val chineseRatio = 0.7
        val chineseCount = (PLAYLIST_SIZE * chineseRatio).toInt().coerceIn(3, 7)
        val englishCount = PLAYLIST_SIZE - chineseCount
        
        // 交替插入，保持 BPM 渐进
        val result = mutableListOf<com.music.localmusic.models.Track>()
        val chineseIterator = sortedChinese.iterator()
        val englishIterator = sortedEnglish.iterator()
        
        var chineseAdded = 0
        var englishAdded = 0
        var lastWasChinese = true
        
        while (result.size < PLAYLIST_SIZE) {
            // 优先添加中文歌曲，每 2-3 首中文后插入 1 首英文
            val shouldAddChinese = lastWasChinese || englishAdded >= englishCount || 
                (chineseAdded < chineseCount && (result.size % 3 != 2 || englishAdded >= englishCount))
            
            if (shouldAddChinese && chineseIterator.hasNext() && chineseAdded < chineseCount + 2) {
                chineseIterator.next()?.let {
                    result.add(it)
                    chineseAdded++
                    lastWasChinese = true
                }
            } else if (englishIterator.hasNext() && englishAdded < englishCount + 1) {
                englishIterator.next()?.let {
                    result.add(it)
                    englishAdded++
                    lastWasChinese = false
                }
            } else if (chineseIterator.hasNext()) {
                chineseIterator.next()?.let { result.add(it) }
            } else if (englishIterator.hasNext()) {
                englishIterator.next()?.let { result.add(it) }
            } else {
                break
            }
        }
        
        Log.i(TAG, "Arranged playlist: ${result.size} tracks (Chinese: $chineseAdded, English: $englishAdded)")
        return result
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
