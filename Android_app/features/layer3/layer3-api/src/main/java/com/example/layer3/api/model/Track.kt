package com.example.layer3.api.model

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val duration_ms: Long = 0,
    val url: String? = null,
    val cover_url: String? = null,
    val genre: String? = null,
    val year: Int? = null,
    val bpm: Int? = null,
    val energy: Double? = null,
    val valence: Double? = null,
    val popularity: Int? = null,
    val preview_url: String? = null,
    val external_ids: Map<String, String> = emptyMap()
)

data class TrackList(
    val tracks: List<Track> = emptyList(),
    val total: Int = 0,
    val offset: Int = 0,
    val limit: Int = 50
)
