package com.music.localmusic

import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.music.localmusic.models.Track
import java.io.File

class LocalMusicIndex private constructor(
    private val dbPath: String
) {
    private var database: SQLiteDatabase? = null
    private var isInitialized = false
    
    companion object {
        private const val TAG = "LocalMusicIndex"
        private const val DEFAULT_DB_PATH = "/sdcard/Music/AiMusic/index.db"
        private const val REQUIRED_TABLE = "tracks"
        
        @Volatile
        private var instance: LocalMusicIndex? = null
        
        fun getInstance(dbPath: String = DEFAULT_DB_PATH): LocalMusicIndex {
            return instance ?: synchronized(this) {
                instance ?: LocalMusicIndex(dbPath).also { instance = it }
            }
        }
    }
    
    fun initialize(): Boolean {
        return try {
            val dbFile = File(dbPath)
            if (!dbFile.exists()) {
                Log.e(TAG, "Database file not found: $dbPath")
                return false
            }
            
            database = SQLiteDatabase.openDatabase(
                dbPath,
                null,
                SQLiteDatabase.OPEN_READONLY
            )
            
            val isValid = validateDatabase()
            if (isValid) {
                isInitialized = true
                Log.i(TAG, "LocalMusicIndex initialized successfully")
            } else {
                close()
                Log.e(TAG, "Database validation failed")
            }
            
            isValid
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize LocalMusicIndex", e)
            false
        }
    }
    
    private fun validateDatabase(): Boolean {
        val db = database ?: return false
        
        return try {
            val cursor = db.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
                arrayOf(REQUIRED_TABLE)
            )
            val hasTable = cursor.count > 0
            cursor.close()
            
            if (!hasTable) {
                Log.e(TAG, "Required table '$REQUIRED_TABLE' not found")
                return false
            }
            
            val columnsCursor = db.rawQuery("PRAGMA table_info($REQUIRED_TABLE)", null)
            val columns = mutableListOf<String>()
            while (columnsCursor.moveToNext()) {
                columns.add(columnsCursor.getString(1).lowercase())
            }
            columnsCursor.close()
            
            val requiredColumns = listOf("id", "title", "artist", "file_path", "duration_ms")
            val missingColumns = requiredColumns.filter { it !in columns }
            
            if (missingColumns.isNotEmpty()) {
                Log.e(TAG, "Missing required columns: $missingColumns")
                return false
            }
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Database validation error", e)
            false
        }
    }
    
    fun queryTracks(query: String, selectionArgs: Array<String>? = null): List<Track> {
        if (!isInitialized) {
            Log.w(TAG, "LocalMusicIndex not initialized")
            return emptyList()
        }
        
        val db = database ?: return emptyList()
        val tracks = mutableListOf<Track>()
        
        return try {
            val cursor = db.rawQuery(query, selectionArgs)
            
            while (cursor.moveToNext()) {
                val track = parseTrack(cursor)
                tracks.add(track)
            }
            cursor.close()
            
            tracks
        } catch (e: Exception) {
            Log.e(TAG, "Query failed: $query", e)
            emptyList()
        }
    }
    
    private fun parseTrack(cursor: android.database.Cursor): Track {
        fun getString(column: String): String? {
            val index = cursor.getColumnIndex(column)
            return if (index >= 0 && !cursor.isNull(index)) cursor.getString(index) else null
        }
        
        fun getLong(column: String): Long? {
            val index = cursor.getColumnIndex(column)
            return if (index >= 0 && !cursor.isNull(index)) cursor.getLong(index) else null
        }
        
        fun getInt(column: String): Int? {
            val index = cursor.getColumnIndex(column)
            return if (index >= 0 && !cursor.isNull(index)) cursor.getInt(index) else null
        }
        
        fun getDouble(column: String): Double? {
            val index = cursor.getColumnIndex(column)
            return if (index >= 0 && !cursor.isNull(index)) cursor.getDouble(index) else null
        }
        
        fun getStringList(column: String): List<String>? {
            val value = getString(column) ?: return null
            return if (value.isNotEmpty()) value.split(",").map { it.trim() } else null
        }
        
        return Track(
            id = getLong("id") ?: 0L,
            title = getString("title") ?: "",
            titlePinyin = getString("title_pinyin"),
            artist = getString("artist") ?: "",
            artistPinyin = getString("artist_pinyin"),
            album = getString("album"),
            genre = getString("genre"),
            bpm = getInt("bpm"),
            energy = getDouble("energy"),
            valence = getDouble("valence"),
            moodTags = getStringList("mood_tags"),
            sceneTags = getStringList("scene_tags"),
            durationMs = getInt("duration_ms") ?: 0,
            filePath = getString("file_path") ?: "",
            format = getString("format")
        )
    }
    
    fun getTrackCount(): Int {
        if (!isInitialized) return 0
        val db = database ?: return 0
        
        return try {
            val cursor = db.rawQuery("SELECT COUNT(*) FROM $REQUIRED_TABLE", null)
            var count = 0
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0)
            }
            cursor.close()
            count
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get track count", e)
            0
        }
    }
    
    fun close() {
        database?.close()
        database = null
        isInitialized = false
        Log.i(TAG, "LocalMusicIndex closed")
    }
    
    fun isReady(): Boolean = isInitialized && database != null
}
