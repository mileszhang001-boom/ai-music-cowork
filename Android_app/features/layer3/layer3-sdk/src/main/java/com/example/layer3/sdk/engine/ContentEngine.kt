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
        val sceneType = scene.scene_type
        
        // 场景类型过滤：排除不合适的内容
        val excludedGenres = getExcludedGenres(sceneType)
        
        val scoredTracks = localTracks.mapNotNull { track ->
            // 检查是否在排除列表
            val trackGenre = track.genre?.lowercase() ?: ""
            if (excludedGenres.any { ex -> trackGenre.contains(ex.lowercase()) }) {
                return@mapNotNull null
            }
            
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
            
            // 根据场景类型动态调整中文歌曲权重
            val chineseWeight = getChineseWeight(sceneType)
            if (isChinese) {
                score += chineseWeight
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
                    score += 35.0
                }
            }
            
            track to score
        }.sortedByDescending { it.second }
        
        return scoredTracks.map { it.first }
    }
    
    /**
     * 获取场景类型对应的排除 Genre 列表
     */
    private fun getExcludedGenres(sceneType: String): List<String> {
        return when (sceneType) {
            "fatigue_alert" -> listOf("children", "disney", "lullaby")
            "romantic_date" -> listOf("children", "disney")
            "road_trip" -> listOf("lullaby")
            else -> emptyList()
        }
    }
    
    /**
     * 获取场景类型对应的中文歌曲权重
     */
    private fun getChineseWeight(sceneType: String): Double {
        return when (sceneType) {
            "fatigue_alert" -> 5.0   // 疲劳提醒：降低中文权重，更多英文高能量歌曲
            "kids_mode" -> 15.0      // 儿童模式：提高中文权重
            "family_outing" -> 12.0  // 家庭出行：提高中文权重
            "romantic_date" -> 8.0   // 浪漫约会：适中
            else -> 10.0             // 默认
        }
    }
    
    /**
     * 获取场景类型对应的中英文比例
     */
    private fun getChineseRatio(sceneType: String): Double {
        return when (sceneType) {
            "fatigue_alert" -> 0.4   // 疲劳提醒：40% 中文
            "kids_mode" -> 0.9       // 儿童模式：90% 中文
            "family_outing" -> 0.8   // 家庭出行：80% 中文
            "romantic_date" -> 0.6   // 浪漫约会：60% 中文
            "rainy_night" -> 0.6     // 雨夜行车：60% 中文
            "road_trip" -> 0.5       // 朋友出游：50% 中文
            else -> 0.6              // 默认：60% 中文
        }
    }
    
    /**
     * 智能编排播放列表
     * - 优先选择高分歌曲
     * - 中英文混合：根据场景类型动态调整比例
     * - BPM 渐进：在满足比例的前提下按 BPM 排序
     */
    private fun arrangePlaylist(
        tracks: List<com.music.localmusic.models.Track>,
        scene: SceneDescriptor
    ): List<com.music.localmusic.models.Track> {
        if (tracks.size <= PLAYLIST_SIZE) return tracks
        
        val sceneType = scene.scene_type
        
        // 分组：中文歌曲、英文歌曲（已按分数排序）
        val chineseTracks = tracks.filter { 
            it.genre?.contains("chinese", ignoreCase = true) == true 
        }
        
        val englishTracks = tracks.filter { 
            it.genre?.contains("chinese", ignoreCase = true) != true 
        }
        
        // 动态编排策略：根据场景类型调整中英文比例
        val chineseRatio = getChineseRatio(sceneType)
        val targetChineseCount = (PLAYLIST_SIZE * chineseRatio).toInt().coerceIn(2, 8)
        val targetEnglishCount = PLAYLIST_SIZE - targetChineseCount
        
        // 从各自列表中选择最高分的歌曲
        val selectedChinese = chineseTracks.take(targetChineseCount)
        val selectedEnglish = englishTracks.take(targetEnglishCount)
        
        // 合并并按 BPM 排序（保持能量渐进）
        val tempo = scene.hints?.music?.tempo?.lowercase() ?: "medium"
        val ascendingBpm = tempo == "fast" || scene.intent.energy_level > 0.6
        
        val combined = (selectedChinese + selectedEnglish).sortedBy { track ->
            val bpm = track.bpm ?: 120
            if (ascendingBpm) bpm else -bpm
        }
        
        // 如果歌曲不足，用剩余歌曲补充
        val result = combined.toMutableList()
        val remaining = (chineseTracks.drop(targetChineseCount) + englishTracks.drop(targetEnglishCount))
            .sortedBy { it.bpm ?: 120 }
        
        while (result.size < PLAYLIST_SIZE && remaining.isNotEmpty()) {
            result.add(remaining.first())
        }
        
        val chineseAdded = result.count { it.genre?.contains("chinese", ignoreCase = true) == true }
        val englishAdded = result.size - chineseAdded
        
        Log.i(TAG, "Arranged playlist for '$sceneType': ${result.size} tracks (Chinese: $chineseAdded, English: $englishAdded, target ratio: ${chineseRatio * 100}%)")
        return result.take(PLAYLIST_SIZE)
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
