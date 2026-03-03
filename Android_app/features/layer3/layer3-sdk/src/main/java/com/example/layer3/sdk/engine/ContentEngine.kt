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
        
        val excludedGenres = getExcludedGenres(sceneType)
        
        val scoredTracks = localTracks.mapNotNull { track ->
            val trackGenre = track.genre?.lowercase() ?: ""
            if (excludedGenres.any { ex -> trackGenre.contains(ex.lowercase()) }) {
                return@mapNotNull null
            }
            
            var score = 0.0
            
            val isChinese = track.genre?.contains("chinese", ignoreCase = true) == true
            val baseGenre = trackGenre.removePrefix("chinese_")
            
            hints?.music?.genres?.let { genres ->
                if (track.genre != null && genres.any { it.equals(track.genre, ignoreCase = true) }) {
                    score += 25.0
                } else if (isChinese && genres.any { it.equals(baseGenre, ignoreCase = true) }) {
                    score += 22.0
                }
                val needsChinese = genres.any { it.contains("chinese", ignoreCase = true) }
                if (needsChinese && isChinese) {
                    score += 20.0
                }
            }
            
            if (sceneType == "kids_mode" && trackGenre.contains("children")) {
                score += 50.0
            }
            
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
                    val atmParts = atm.split("_")
                    if (tags.any { tag ->
                        tag.contains(atm, ignoreCase = true) ||
                        atmParts.any { part -> part.length > 3 && tag.contains(part, ignoreCase = true) }
                    }) {
                        score += 25.0
                    }
                }
            }
            
            track.sceneTags?.let { tags ->
                if (tags.any { it.equals(sceneType, ignoreCase = true) }) {
                    score += 25.0
                }
            }
            
            track to score
        }.map { (track, score) ->
            track to score + kotlin.random.Random.nextDouble(0.0, 8.0)
        }.sortedByDescending { it.second }
        
        return scoredTracks.map { it.first }
    }
    
    private fun getExcludedGenres(sceneType: String): List<String> {
        return when (sceneType) {
            "kids_mode" -> emptyList()
            "fatigue_alert" -> listOf("children", "disney", "lullaby")
            "couple_date", "romantic_mode" -> listOf("children", "disney")
            "night_drive", "late_night_solo" -> listOf("children", "disney")
            "solo_drive", "focus_mode", "meditation_mode" -> listOf("children", "disney")
            "road_trip", "friends_gathering", "party_mode" -> listOf("children", "disney", "lullaby")
            "morning_commute", "evening_commute", "dawn_commute" -> listOf("children", "disney")
            "highway_drive", "long_distance" -> listOf("children", "disney")
            "traffic_jam", "construction_zone" -> listOf("children", "disney")
            "sunset_drive", "scenic_route" -> listOf("children", "disney")
            "workout", "energetic_mode" -> listOf("children", "disney", "lullaby")
            "stress_mode", "calm_mode" -> listOf("children", "disney")
            "special_event", "holiday_travel" -> listOf("children", "disney")
            "nostalgic_mode", "adventure_mode" -> listOf("children", "disney")
            else -> listOf("children", "disney")
        }
    }
    
    /**
     * 获取场景类型对应的中文歌曲权重
     */
    private fun getChineseWeight(sceneType: String): Double {
        return when (sceneType) {
            "kids_mode" -> 15.0
            "family_outing" -> 10.0
            "couple_date", "romantic_mode" -> 12.0
            "morning_commute", "evening_commute", "dawn_commute" -> 10.0
            "road_trip", "friends_gathering", "party_mode" -> 10.0
            "traffic_jam", "construction_zone" -> 10.0
            "rainy_day", "rainy_night", "rainy_mood" -> 10.0
            "nostalgic_mode" -> 12.0
            "fatigue_alert" -> 8.0
            "focus_mode", "meditation_mode" -> 3.0
            "workout", "energetic_mode" -> 5.0
            else -> 8.0
        }
    }
    
    /**
     * 获取场景类型对应的中英文比例
     */
    private fun getChineseRatio(sceneType: String): Double {
        return when (sceneType) {
            "kids_mode" -> 0.9
            "family_outing" -> 0.8
            "couple_date", "romantic_mode" -> 0.6
            "morning_commute", "evening_commute", "dawn_commute" -> 0.6
            "road_trip", "friends_gathering" -> 0.5
            "party_mode" -> 0.4
            "traffic_jam", "construction_zone" -> 0.6
            "rainy_day", "rainy_night", "rainy_mood" -> 0.6
            "nostalgic_mode" -> 0.7
            "fatigue_alert" -> 0.4
            "focus_mode", "meditation_mode" -> 0.3
            "workout", "energetic_mode" -> 0.3
            else -> 0.6
        }
    }
    
    /**
     * 智能编排播放列表
     * - 优先选择高分歌曲
     * - 中英文混合：根据场景类型动态调整比例
     * - BPM 渐进：在满足比例的前提下按 BPM 排序
     */
    private fun isChinesesTrack(track: com.music.localmusic.models.Track): Boolean {
        if (track.genre?.contains("chinese", ignoreCase = true) == true) return true
        return track.title.any { it in '\u4e00'..'\u9fff' }
    }

    private fun arrangePlaylist(
        tracks: List<com.music.localmusic.models.Track>,
        scene: SceneDescriptor
    ): List<com.music.localmusic.models.Track> {
        if (tracks.size <= PLAYLIST_SIZE) return tracks
        
        val sceneType = scene.scene_type
        
        if (sceneType == "kids_mode") {
            return tracks.take(PLAYLIST_SIZE).shuffled()
        }
        
        val chineseTracks = tracks.filter { isChinesesTrack(it) }
        
        val englishTracks = tracks.filter { !isChinesesTrack(it) }
        
        val chineseRatio = getChineseRatio(sceneType)
        val targetChineseCount = (PLAYLIST_SIZE * chineseRatio).toInt().coerceIn(2, 8)
        val targetEnglishCount = PLAYLIST_SIZE - targetChineseCount
        
        val selectedChinese = chineseTracks.take(targetChineseCount).shuffled()
        val selectedEnglish = englishTracks.take(targetEnglishCount).shuffled()
        
        val result = mutableListOf<com.music.localmusic.models.Track>()
        var ci = 0
        var ei = 0
        while (result.size < (selectedChinese.size + selectedEnglish.size)) {
            if (ci < selectedChinese.size) { result.add(selectedChinese[ci]); ci++ }
            if (ei < selectedEnglish.size) { result.add(selectedEnglish[ei]); ei++ }
        }
        
        val remaining = (chineseTracks.drop(targetChineseCount) + englishTracks.drop(targetEnglishCount)).shuffled()
        var idx = 0
        while (result.size < PLAYLIST_SIZE && idx < remaining.size) {
            result.add(remaining[idx])
            idx++
        }
        
        val chineseAdded = result.count { isChinesesTrack(it) }
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
