package com.music.localmusic.models

import kotlinx.serialization.Serializable

@Serializable
data class Track(
    val id: Long,
    val title: String,
    val titlePinyin: String? = null,
    val artist: String,
    val artistPinyin: String? = null,
    val album: String? = null,
    val genre: String? = null,
    val bpm: Int? = null,
    val energy: Double? = null,
    val valence: Double? = null,
    val moodTags: List<String>? = null,
    val sceneTags: List<String>? = null,
    val durationMs: Int,
    val filePath: String,
    val format: String? = null
) {
    fun hasMoodTag(tag: String): Boolean {
        return moodTags?.contains(tag, ignoreCase = true) == true
    }
    
    fun hasSceneTag(tag: String): Boolean {
        return sceneTags?.contains(tag, ignoreCase = true) == true
    }
    
    fun matchesGenre(targetGenre: String): Boolean {
        return genre?.equals(targetGenre, ignoreCase = true) == true
    }
    
    fun matchesBpmRange(minBpm: Int, maxBpm: Int): Boolean {
        return bpm?.let { it in minBpm..maxBpm } == true
    }
    
    fun matchesEnergyRange(minEnergy: Double, maxEnergy: Double): Boolean {
        return energy?.let { it in minEnergy..maxEnergy } == true
    }
    
    fun matchesValenceRange(minValence: Double, maxValence: Double): Boolean {
        return valence?.let { it in minValence..maxValence } == true
    }
}
