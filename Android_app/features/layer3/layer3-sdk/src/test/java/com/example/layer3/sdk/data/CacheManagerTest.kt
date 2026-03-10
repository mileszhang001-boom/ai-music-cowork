package com.example.layer3.sdk.data

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CacheManagerTest {

    private lateinit var cacheManager: CacheManager<String>

    @Before
    fun setUp() {
        cacheManager = CacheManager(defaultTtlMs = 1000, maxSize = 3)
    }

    @Test
    fun testPut_AndGet_ReturnsValue() {
        cacheManager.put("key1", "value1")

        val result = cacheManager.get("key1")

        assertEquals("value1", result)
    }

    @Test
    fun testGet_NonExistentKey_ReturnsNull() {
        val result = cacheManager.get("non_existent")

        assertNull(result)
    }

    @Test
    fun testPut_OverwritesExistingKey() {
        cacheManager.put("key1", "value1")
        cacheManager.put("key1", "value2")

        val result = cacheManager.get("key1")

        assertEquals("value2", result)
    }

    @Test
    fun testRemove_RemovesKey() {
        cacheManager.put("key1", "value1")
        cacheManager.remove("key1")

        val result = cacheManager.get("key1")

        assertNull(result)
    }

    @Test
    fun testClear_RemovesAllKeys() {
        cacheManager.put("key1", "value1")
        cacheManager.put("key2", "value2")
        cacheManager.put("key3", "value3")

        cacheManager.clear()

        assertEquals(0, cacheManager.size())
    }

    @Test
    fun testContains_ExistingKey_ReturnsTrue() {
        cacheManager.put("key1", "value1")

        assertTrue(cacheManager.contains("key1"))
    }

    @Test
    fun testContains_NonExistentKey_ReturnsFalse() {
        assertFalse(cacheManager.contains("non_existent"))
    }

    @Test
    fun testSize_ReturnsCorrectCount() {
        cacheManager.put("key1", "value1")
        cacheManager.put("key2", "value2")

        assertEquals(2, cacheManager.size())
    }

    @Test
    fun testPut_ExceedsMaxSize_EvictsOldest() {
        cacheManager.put("key1", "value1")
        cacheManager.put("key2", "value2")
        cacheManager.put("key3", "value3")
        cacheManager.put("key4", "value4")

        assertNull("Oldest key should be evicted", cacheManager.get("key1"))
        assertNotNull("Newest key should exist", cacheManager.get("key4"))
        assertEquals(3, cacheManager.size())
    }

    @Test
    fun testGetOrPut_ExistingKey_ReturnsCachedValue() {
        cacheManager.put("key1", "value1")

        val result = cacheManager.getOrPut("key1") { "new_value" }

        assertEquals("value1", result)
    }

    @Test
    fun testGetOrPut_NewKey_LoadsAndCachesValue() {
        val result = cacheManager.getOrPut("key1") { "loaded_value" }

        assertEquals("loaded_value", result)
        assertEquals("loaded_value", cacheManager.get("key1"))
    }

    @Test
    fun testPut_CustomTtl() {
        val shortTtlCache = CacheManager<String>(defaultTtlMs = 50, maxSize = 10)
        
        shortTtlCache.put("key1", "value1", ttlMs = 200)
        
        assertEquals("value1", shortTtlCache.get("key1"))
    }

    @Test
    fun testContains_ExpiredEntry_ReturnsFalse() {
        val shortTtlCache = CacheManager<String>(defaultTtlMs = 10, maxSize = 10)
        
        shortTtlCache.put("key1", "value1")
        Thread.sleep(50)
        
        assertFalse(shortTtlCache.contains("key1"))
    }

    @Test
    fun testGet_ExpiredEntry_ReturnsNull() {
        val shortTtlCache = CacheManager<String>(defaultTtlMs = 10, maxSize = 10)
        
        shortTtlCache.put("key1", "value1")
        Thread.sleep(50)
        
        assertNull(shortTtlCache.get("key1"))
    }

    @Test
    fun testCleanupExpired_RemovesExpiredEntries() {
        val cache = CacheManager<String>(defaultTtlMs = 10, maxSize = 10)
        
        cache.put("key1", "value1")
        cache.put("key2", "value2")
        Thread.sleep(50)
        cache.put("key3", "value3")
        
        cache.cleanupExpired()
        
        assertNull(cache.get("key1"))
        assertNull(cache.get("key2"))
        assertNotNull(cache.get("key3"))
    }

    @Test
    fun testPut_SameKeyMultipleTimes_DoesNotEvictOtherKeys() {
        cacheManager.put("key1", "value1")
        cacheManager.put("key2", "value2")
        cacheManager.put("key1", "updated_value1")
        cacheManager.put("key1", "updated_again_value1")

        assertEquals("updated_again_value1", cacheManager.get("key1"))
        assertEquals("value2", cacheManager.get("key2"))
        assertEquals(2, cacheManager.size())
    }

    @Test
    fun testCacheManager_WithDifferentTypes() {
        val intCache = CacheManager<Int>(maxSize = 5)
        
        intCache.put("one", 1)
        intCache.put("two", 2)
        
        assertEquals(1, intCache.get("one"))
        assertEquals(2, intCache.get("two"))
    }

    @Test
    fun testGet_UpdatesAccessOrder() {
        cacheManager.put("key1", "value1")
        cacheManager.put("key2", "value2")
        cacheManager.put("key3", "value3")
        
        cacheManager.get("key1")
        
        cacheManager.put("key4", "value4")
        
        assertNotNull("key1 should still exist after being accessed", cacheManager.get("key1"))
        assertNull("key2 should be evicted (oldest)", cacheManager.get("key2"))
    }
}
