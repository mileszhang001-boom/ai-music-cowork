package com.example.layer3.api.model

data class Playlist(
    val id: String,
    val name: String,
    val description: String? = null,
    val cover_url: String? = null,
    val owner: String? = null,
    val track_count: Int = 0,
    val total_duration_ms: Long = 0,
    val tracks: List<Track> = emptyList(),
    val created_at: Long = System.currentTimeMillis(),
    val updated_at: Long = System.currentTimeMillis(),
    val is_public: Boolean = true,
    val source: String = "generated",
    val scene_id: String? = null,
    val tags: List<String> = emptyList()
)

data class PlaylistRecommendation(
    val playlists: List<Playlist> = emptyList(),
    val based_on: String = "",
    val confidence: Double = 0.0
)
