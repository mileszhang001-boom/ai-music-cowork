package com.example.layer3.sdk.algorithm

import com.example.layer3.sdk.data.TrackData
import com.example.layer3.sdk.util.Logger

class ArtistDiversityFilter(
    private val maxTracksPerArtist: Int = 2
) {
    fun applyDiversity(tracks: List<TrackData>): List<TrackData> {
        val artistCounts = mutableMapOf<String, Int>()
        val result = mutableListOf<TrackData>()
        
        for (track in tracks) {
            val count = artistCounts.getOrDefault(track.artist, 0)
            if (count < maxTracksPerArtist) {
                result.add(track)
                artistCounts[track.artist] = count + 1
            }
        }
        
        Logger.d("ArtistDiversityFilter: Filtered ${tracks.size} tracks to ${result.size} with max $maxTracksPerArtist per artist")
        return result
    }

    fun applyDiversityWithScore(
        scoredTracks: List<Pair<TrackData, Double>>
    ): List<Pair<TrackData, Double>> {
        val artistCounts = mutableMapOf<String, Int>()
        val result = mutableListOf<Pair<TrackData, Double>>()
        
        for ((track, score) in scoredTracks) {
            val count = artistCounts.getOrDefault(track.artist, 0)
            if (count < maxTracksPerArtist) {
                result.add(track to score)
                artistCounts[track.artist] = count + 1
            }
        }
        
        Logger.d("ArtistDiversityFilter: Filtered ${scoredTracks.size} scored tracks to ${result.size}")
        return result
    }

    fun setMaxTracksPerArtist(max: Int) {
        Logger.d("ArtistDiversityFilter: Set max tracks per artist to $max")
    }
}
