package com.music.localmusic

import android.util.Log

object LocalMusicModule {
    private const val TAG = "LocalMusicModule"
    
    private const val DEFAULT_DB_PATH = "/sdcard/Music/AiMusic/index.db"
    
    @Volatile
    private var musicIndex: LocalMusicIndex? = null
    
    @Volatile
    private var repository: LocalMusicRepository? = null
    
    @Volatile
    private var isInitialized = false
    
    fun initialize(dbPath: String = DEFAULT_DB_PATH): Boolean {
        if (isInitialized) {
            Log.w(TAG, "LocalMusicModule already initialized")
            return true
        }
        
        return synchronized(this) {
            if (isInitialized) {
                return@synchronized true
            }
            
            try {
                musicIndex = LocalMusicIndex.getInstance(dbPath)
                val indexReady = musicIndex!!.initialize()
                
                if (indexReady) {
                    repository = LocalMusicRepository(musicIndex!!)
                    isInitialized = true
                    Log.i(TAG, "LocalMusicModule initialized successfully")
                    true
                } else {
                    musicIndex = null
                    Log.e(TAG, "Failed to initialize LocalMusicIndex")
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize LocalMusicModule", e)
                false
            }
        }
    }
    
    fun getRepository(): LocalMusicRepository? {
        if (!isInitialized) {
            Log.w(TAG, "LocalMusicModule not initialized")
            return null
        }
        return repository
    }
    
    fun getMusicIndex(): LocalMusicIndex? {
        if (!isInitialized) {
            Log.w(TAG, "LocalMusicModule not initialized")
            return null
        }
        return musicIndex
    }
    
    fun isReady(): Boolean = isInitialized && repository != null
    
    fun shutdown() {
        synchronized(this) {
            musicIndex?.close()
            musicIndex = null
            repository = null
            isInitialized = false
            Log.i(TAG, "LocalMusicModule shutdown complete")
        }
    }
}
