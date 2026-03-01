package com.music.localmusic

import android.util.Log
import com.music.localmusic.models.Track
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class LocalMusicIndex private constructor(
    private val jsonPath: String
) {
    private var tracks: List<Track> = emptyList()
    private var isInitialized = false
    
    companion object {
        private const val TAG = "LocalMusicIndex"
        private const val DEFAULT_JSON_PATH = "/sdcard/Music/AiMusic/index.json"
        
        @Volatile
        private var instance: LocalMusicIndex? = null
        
        fun getInstance(jsonPath: String = DEFAULT_JSON_PATH): LocalMusicIndex {
            return instance ?: synchronized(this) {
                instance ?: LocalMusicIndex(jsonPath).also { instance = it }
            }
        }
    }
    
    fun initialize(): Boolean {
        return try {
            val jsonFile = File(jsonPath)
            if (!jsonFile.exists()) {
                Log.e(TAG, "JSON file not found: $jsonPath")
                return false
            }
            
            val jsonString = jsonFile.readText()
            tracks = parseJsonTracks(jsonString)
            
            if (tracks.isNotEmpty()) {
                isInitialized = true
                Log.i(TAG, "LocalMusicIndex initialized with ${tracks.size} tracks")
                true
            } else {
                Log.e(TAG, "No tracks found in JSON file")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize LocalMusicIndex", e)
            false
        }
    }
    
    private fun parseJsonTracks(jsonString: String): List<Track> {
        val tracks = mutableListOf<Track>()
        
        try {
            val jsonArray = JSONArray(jsonString)
            
            for (i in 0 until jsonArray.length()) {
                val json = jsonArray.getJSONObject(i)
                val track = Track(
                    id = json.optLong("id", 0L),
                    title = json.optString("title", ""),
                    titlePinyin = json.optString("title_pinyin", null),
                    artist = json.optString("artist", "未知艺术家"),
                    artistPinyin = json.optString("artist_pinyin", null),
                    album = json.optString("album", null),
                    genre = json.optString("genre", null),
                    bpm = json.optInt("bpm", 0),
                    energy = json.optDouble("energy", 0.5),
                    valence = json.optDouble("valence", 0.5),
                    moodTags = parseJsonArray(json.optString("mood_tags", "[]")),
                    sceneTags = parseJsonArray(json.optString("scene_tags", "[]")),
                    durationMs = json.optInt("duration_ms", 0),
                    filePath = json.optString("file_path", ""),
                    format = json.optString("format", null)
                )
                tracks.add(track)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse JSON", e)
        }
        
        return tracks
    }
    
    private fun parseJsonArray(jsonString: String): List<String>? {
        return try {
            if (jsonString.isBlank() || jsonString == "[]") return null
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getString(i))
            }
            list
        } catch (e: Exception) {
            null
        }
    }
    
    fun queryTracks(query: String, selectionArgs: Array<String>? = null): List<Track> {
        if (!isInitialized) {
            Log.w(TAG, "LocalMusicIndex not initialized")
            return emptyList()
        }
        
        return tracks
    }
    
    fun getAllTracks(): List<Track> {
        if (!isInitialized) return emptyList()
        return tracks
    }
    
    fun getTrackCount(): Int = tracks.size
    
    fun close() {
        tracks = emptyList()
        isInitialized = false
        Log.i(TAG, "LocalMusicIndex closed")
    }
    
    fun isReady(): Boolean = isInitialized && tracks.isNotEmpty()
}
