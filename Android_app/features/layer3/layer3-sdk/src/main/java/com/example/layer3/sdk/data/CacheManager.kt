package com.example.layer3.sdk.data

import com.example.layer3.sdk.util.Logger
import java.util.concurrent.ConcurrentHashMap

class CacheManager<T>(
    private val defaultTtlMs: Long = 300000L,
    private val maxSize: Int = 100
) {
    private val cache = ConcurrentHashMap<String, CacheEntry<T>>()
    private val accessOrder = mutableListOf<String>()

    data class CacheEntry<T>(
        val data: T,
        val createdAt: Long = System.currentTimeMillis(),
        val ttlMs: Long
    ) {
        fun isExpired(): Boolean {
            return System.currentTimeMillis() - createdAt > ttlMs
        }
    }

    fun put(key: String, data: T, ttlMs: Long = defaultTtlMs) {
        synchronized(this) {
            if (cache.size >= maxSize && !cache.containsKey(key)) {
                evictOldest()
            }
            cache[key] = CacheEntry(data, ttlMs = ttlMs)
            accessOrder.remove(key)
            accessOrder.add(key)
            Logger.d("CacheManager: Cached item with key=$key, size=${cache.size}")
        }
    }

    fun get(key: String): T? {
        synchronized(this) {
            val entry = cache[key]
            if (entry == null) {
                Logger.d("CacheManager: Cache miss for key=$key")
                return null
            }
            if (entry.isExpired()) {
                Logger.d("CacheManager: Cache expired for key=$key")
                remove(key)
                return null
            }
            accessOrder.remove(key)
            accessOrder.add(key)
            Logger.d("CacheManager: Cache hit for key=$key")
            return entry.data
        }
    }

    fun getOrPut(key: String, ttlMs: Long = defaultTtlMs, loader: () -> T): T {
        val cached = get(key)
        if (cached != null) {
            return cached
        }
        val data = loader()
        put(key, data, ttlMs)
        return data
    }

    fun remove(key: String) {
        synchronized(this) {
            cache.remove(key)
            accessOrder.remove(key)
        }
    }

    fun clear() {
        synchronized(this) {
            cache.clear()
            accessOrder.clear()
            Logger.d("CacheManager: Cache cleared")
        }
    }

    fun contains(key: String): Boolean {
        val entry = cache[key] ?: return false
        return !entry.isExpired()
    }

    fun size(): Int = cache.size

    private fun evictOldest() {
        if (accessOrder.isEmpty()) return
        val oldestKey = accessOrder.first()
        cache.remove(oldestKey)
        accessOrder.removeAt(0)
        Logger.d("CacheManager: Evicted oldest item with key=$oldestKey")
    }

    fun cleanupExpired() {
        synchronized(this) {
            val expiredKeys = cache.filter { it.value.isExpired() }.keys
            expiredKeys.forEach { key ->
                cache.remove(key)
                accessOrder.remove(key)
            }
            if (expiredKeys.isNotEmpty()) {
                Logger.d("CacheManager: Cleaned up ${expiredKeys.size} expired items")
            }
        }
    }
}
