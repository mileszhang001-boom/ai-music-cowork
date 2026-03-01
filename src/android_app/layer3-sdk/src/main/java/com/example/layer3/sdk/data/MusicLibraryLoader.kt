package com.example.layer3.sdk.data

import android.content.Context
import com.example.layer3.api.model.Track
import com.example.layer3.api.model.TrackList
import com.example.layer3.sdk.util.JsonLoader
import com.example.layer3.sdk.util.Logger
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken

data class MusicLibrary(
    @SerializedName("tracks") val tracks: List<TrackData> = emptyList(),
    @SerializedName("version") val version: String = "1.0",
    @SerializedName("last_updated") val lastUpdated: Long = 0
)

data class TrackData(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("artist") val artist: String,
    @SerializedName("album") val album: String? = null,
    @SerializedName("duration_ms") val durationMs: Long = 0,
    @SerializedName("genre") val genre: String? = null,
    @SerializedName("year") val year: Int? = null,
    @SerializedName("bpm") val bpm: Int? = null,
    @SerializedName("energy") val energy: Double? = null,
    @SerializedName("valence") val valence: Double? = null,
    @SerializedName("popularity") val popularity: Int? = null,
    @SerializedName("mood") val mood: String? = null,
    @SerializedName("tags") val tags: List<String> = emptyList()
)

class MusicLibraryLoader(private val context: Context) {
    private val gson = Gson()
    private var library: MusicLibrary? = null
    private val cache = CacheManager<TrackList>(maxSize = 10)

    suspend fun loadLibrary(fileName: String = "music_library.json"): Result<MusicLibrary> {
        return try {
            val json = JsonLoader.loadJsonFromAssets(context, fileName)
            if (json == null) {
                Logger.w("MusicLibraryLoader: Library file not found: $fileName")
                Result.success(MusicLibrary())
            } else {
                val library = gson.fromJson(json, MusicLibrary::class.java)
                this.library = library
                Logger.i("MusicLibraryLoader: Loaded ${library.tracks.size} tracks")
                Result.success(library)
            }
        } catch (e: Exception) {
            Logger.e("MusicLibraryLoader: Failed to load library", e)
            Result.failure(e)
        }
    }

    fun getLibrary(): MusicLibrary? = library

    fun getAllTracks(): List<TrackData> = library?.tracks ?: emptyList()

    fun getTracksByGenre(genre: String): List<TrackData> {
        return getAllTracks().filter { it.genre?.equals(genre, ignoreCase = true) == true }
    }

    fun getTracksByMood(mood: String): List<TrackData> {
        return getAllTracks().filter { it.mood?.equals(mood, ignoreCase = true) == true }
    }

    fun getTracksByArtist(artist: String): List<TrackData> {
        return getAllTracks().filter { it.artist.contains(artist, ignoreCase = true) }
    }

    fun getTracksByTags(tags: List<String>): List<TrackData> {
        if (tags.isEmpty()) return emptyList()
        return getAllTracks().filter { track ->
            tags.any { tag -> track.tags.any { it.equals(tag, ignoreCase = true) } }
        }
    }

    fun searchTracks(query: String): List<TrackData> {
        val lowerQuery = query.lowercase()
        return getAllTracks().filter { track ->
            track.title.lowercase().contains(lowerQuery) ||
            track.artist.lowercase().contains(lowerQuery) ||
            track.album?.lowercase()?.contains(lowerQuery) == true ||
            track.genre?.lowercase()?.contains(lowerQuery) == true
        }
    }

    fun toTrack(trackData: TrackData): Track {
        return Track(
            id = trackData.id,
            title = trackData.title,
            artist = trackData.artist,
            album = trackData.album,
            duration_ms = trackData.durationMs,
            genre = trackData.genre,
            year = trackData.year,
            bpm = trackData.bpm,
            energy = trackData.energy,
            valence = trackData.valence,
            popularity = trackData.popularity
        )
    }

    fun toTrackList(tracks: List<TrackData>): TrackList {
        return TrackList(
            tracks = tracks.map { toTrack(it) },
            total = tracks.size
        )
    }
}
