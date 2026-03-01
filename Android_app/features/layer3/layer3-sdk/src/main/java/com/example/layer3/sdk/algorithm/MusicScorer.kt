package com.example.layer3.sdk.algorithm

import com.example.layer3.api.model.SceneDescriptor
import com.example.layer3.sdk.data.TrackData
import com.example.layer3.sdk.util.Logger
import kotlin.math.abs

class MusicScorer {
    fun scoreTracks(
        tracks: List<TrackData>,
        scene: SceneDescriptor
    ): List<Pair<TrackData, Double>> {
        val intent = scene.intent
        val hints = scene.hints?.music
        
        return tracks.map { track ->
            val score = calculateScore(track, intent, hints)
            track to score
        }.sortedByDescending { it.second }
    }

    private fun calculateScore(
        track: TrackData,
        intent: com.example.layer3.api.model.Intent,
        hints: com.example.layer3.api.model.MusicHints?
    ): Double {
        var score = 0.0
        var totalWeight = 0.0

        val moodScore = calculateMoodScore(track, intent.mood)
        score += moodScore * MOOD_WEIGHT
        totalWeight += MOOD_WEIGHT

        val energyScore = calculateEnergyScore(track, intent.energyLevel)
        score += energyScore * ENERGY_WEIGHT
        totalWeight += ENERGY_WEIGHT

        if (hints != null) {
            val genreScore = calculateGenreScore(track, hints.genres)
            score += genreScore * GENRE_WEIGHT
            totalWeight += GENRE_WEIGHT

            val artistScore = calculateArtistScore(track, hints.artists)
            score += artistScore * ARTIST_WEIGHT
            totalWeight += ARTIST_WEIGHT

            val tempoScore = calculateTempoScore(track, hints.tempo)
            score += tempoScore * TEMPO_WEIGHT
            totalWeight += TEMPO_WEIGHT
        }

        val popularityScore = calculatePopularityScore(track)
        score += popularityScore * POPULARITY_WEIGHT
        totalWeight += POPULARITY_WEIGHT

        return if (totalWeight > 0) score / totalWeight else 0.0
    }

    private fun calculateMoodScore(
        track: TrackData,
        mood: com.example.layer3.api.model.Mood
    ): Double {
        val trackValence = track.valence ?: 0.5
        val trackEnergy = track.energy ?: 0.5
        
        val valenceDiff = abs(trackValence - mood.valence)
        val arousalDiff = abs(trackEnergy - mood.arousal)
        
        return 1.0 - (valenceDiff + arousalDiff) / 2.0
    }

    private fun calculateEnergyScore(track: TrackData, targetEnergy: Double): Double {
        val trackEnergy = track.energy ?: return 0.5
        val diff = abs(trackEnergy - targetEnergy)
        return 1.0 - diff
    }

    private fun calculateGenreScore(track: TrackData, preferredGenres: List<String>): Double {
        if (preferredGenres.isEmpty()) return 0.5
        val trackGenre = track.genre ?: return 0.0
        return if (preferredGenres.any { it.equals(trackGenre, ignoreCase = true) }) 1.0 else 0.0
    }

    private fun calculateArtistScore(track: TrackData, preferredArtists: List<String>): Double {
        if (preferredArtists.isEmpty()) return 0.5
        return if (preferredArtists.any { track.artist.contains(it, ignoreCase = true) }) 1.0 else 0.0
    }

    private fun calculateTempoScore(track: TrackData, tempoHint: String?): Double {
        if (tempoHint == null) return 0.5
        val bpm = track.bpm ?: return 0.5
        
        val targetRange = when (tempoHint.lowercase()) {
            "slow" -> 60..90
            "medium" -> 90..120
            "fast" -> 120..180
            else -> return 0.5
        }
        
        return if (bpm in targetRange) 1.0 else {
            val diff = minOf(
                abs(bpm - targetRange.first),
                abs(bpm - targetRange.last)
            )
            (1.0 - diff / 60.0).coerceIn(0.0, 1.0)
        }
    }

    private fun calculatePopularityScore(track: TrackData): Double {
        val popularity = track.popularity ?: return 0.5
        return popularity / 100.0
    }

    companion object {
        private const val MOOD_WEIGHT = 0.25
        private const val ENERGY_WEIGHT = 0.20
        private const val GENRE_WEIGHT = 0.20
        private const val ARTIST_WEIGHT = 0.10
        private const val TEMPO_WEIGHT = 0.10
        private const val POPULARITY_WEIGHT = 0.15
    }
}
