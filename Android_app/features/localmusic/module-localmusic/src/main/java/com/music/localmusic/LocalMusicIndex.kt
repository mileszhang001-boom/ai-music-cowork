package com.music.localmusic

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Environment
import android.util.Log
import androidx.core.content.ContextCompat
import com.music.localmusic.models.Track
import org.json.JSONArray
import java.io.File

data class LocalMusicConfig(
    val storagePath: String = DEFAULT_STORAGE_PATH,
    val indexDbPath: String = DEFAULT_INDEX_DB_PATH,
    val indexJsonPath: String = DEFAULT_INDEX_JSON_PATH
) {
    companion object {
        const val DEFAULT_STORAGE_PATH = "/sdcard/Music/AiMusic/"
        const val DEFAULT_INDEX_DB_PATH = "/sdcard/Music/AiMusic/index.db"
        const val DEFAULT_INDEX_JSON_PATH = "/sdcard/Music/AiMusic/index.json"
    }
}

class LocalMusicIndex private constructor(
    private val context: Context?,
    private val config: LocalMusicConfig,
    private val useAssets: Boolean
) {
    private var tracks: List<Track> = emptyList()
    private var isInitialized = false
    
    companion object {
        private const val TAG = "LocalMusicIndex"
        private const val DEFAULT_ASSETS_PATH = "index.json"
        
        @Volatile
        private var instance: LocalMusicIndex? = null
        
        private var customConfig: LocalMusicConfig = LocalMusicConfig()
        
        fun setConfig(config: LocalMusicConfig) {
            customConfig = config
        }
        
        fun getConfig(): LocalMusicConfig = customConfig
        
        @Deprecated("Use getInstance(context) for assets loading")
        fun getInstance(jsonPath: String = customConfig.indexJsonPath): LocalMusicIndex {
            return instance ?: synchronized(this) {
                instance ?: LocalMusicIndex(null, LocalMusicConfig(indexJsonPath = jsonPath), false).also { instance = it }
            }
        }
        
        fun getInstance(context: Context): LocalMusicIndex {
            return instance ?: synchronized(this) {
                instance ?: LocalMusicIndex(context, customConfig, true).also { instance = it }
            }
        }
        
        fun getInstance(context: Context, config: LocalMusicConfig): LocalMusicIndex {
            customConfig = config
            return instance ?: synchronized(this) {
                instance ?: LocalMusicIndex(context, config, false).also { instance = it }
            }
        }
        
        fun getInstance(context: Context, jsonPath: String): LocalMusicIndex {
            val config = LocalMusicConfig(indexJsonPath = jsonPath)
            return getInstance(context, config)
        }
        
        fun hasStoragePermission(context: Context): Boolean {
            val readPermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            
            val writePermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            
            return readPermission && writePermission
        }
        
        fun isExternalStorageWritable(): Boolean {
            return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
        }
        
        fun isExternalStorageReadable(): Boolean {
            return Environment.getExternalStorageState() in 
                setOf(Environment.MEDIA_MOUNTED, Environment.MEDIA_MOUNTED_READ_ONLY)
        }
    }
    
    fun initialize(): Boolean {
        return try {
            if (!useAssets && context != null) {
                if (!hasStoragePermission(context)) {
                    Log.e(TAG, "Storage permission not granted")
                    return false
                }
                
                if (!isExternalStorageReadable()) {
                    Log.e(TAG, "External storage is not readable")
                    return false
                }
                
                ensureStorageDirectoryExists()
            }
            
            val jsonString = when {
                useAssets && context != null -> loadFromAssets()
                !useAssets -> loadFromFile(config.indexJsonPath)
                else -> {
                    Log.e(TAG, "No valid loading method specified")
                    return false
                }
            }
            
            if (jsonString == null) {
                Log.e(TAG, "Failed to load JSON data")
                return false
            }
            
            tracks = parseJsonTracks(jsonString)
            
            if (tracks.isNotEmpty()) {
                isInitialized = true
                Log.i(TAG, "LocalMusicIndex initialized with ${tracks.size} tracks from ${config.indexJsonPath}")
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
    
    private fun ensureStorageDirectoryExists(): Boolean {
        return try {
            val storageDir = File(config.storagePath)
            if (!storageDir.exists()) {
                val created = storageDir.mkdirs()
                if (created) {
                    Log.i(TAG, "Created storage directory: ${config.storagePath}")
                } else {
                    Log.w(TAG, "Failed to create storage directory: ${config.storagePath}")
                }
                return created
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error ensuring storage directory exists", e)
            false
        }
    }
    
    fun getStoragePath(): String = config.storagePath
    
    fun getIndexDbPath(): String = config.indexDbPath
    
    fun getIndexJsonPath(): String = config.indexJsonPath
    
    fun checkPermissions(): Boolean {
        if (context == null) return false
        return hasStoragePermission(context) && isExternalStorageReadable()
    }
    
    private fun loadFromAssets(): String? {
        return try {
            context?.assets?.open(DEFAULT_ASSETS_PATH)?.bufferedReader()?.use { it.readText() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load from assets: $DEFAULT_ASSETS_PATH", e)
            null
        }
    }
    
    private fun loadFromFile(path: String): String? {
        return try {
            val jsonFile = File(path)
            if (!jsonFile.exists()) {
                Log.e(TAG, "JSON file not found: $path")
                return null
            }
            jsonFile.readText()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load from file: $path", e)
            null
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
