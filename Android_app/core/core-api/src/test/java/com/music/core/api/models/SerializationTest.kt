package com.music.core.api.models

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SerializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testStandardizedSignalsSerialization() {
        val jsonString = """
            {
              "version": "1.0",
              "timestamp": "2026-02-28T08:30:05Z",
              "signals": {
                "vehicle": {
                  "speed_kmh": 70.0,
                  "passenger_count": 1,
                  "gear": "D"
                },
                "environment": {
                  "time_of_day": 0.35,
                  "weather": "clear",
                  "temperature": 22.0,
                  "date_type": "weekday"
                }
              },
              "confidence": {
                "overall": 0.85,
                "by_source": { "vhal": 0.95, "environment": 0.90 }
              }
            }
        """.trimIndent()

        val obj = json.decodeFromString<StandardizedSignals>(jsonString)
        assertNotNull(obj)
        assertEquals("1.0", obj.version)
        assertEquals(70.0, obj.signals.vehicle?.speed_kmh)
        assertEquals(0.85, obj.confidence.overall, 0.01)

        val encoded = json.encodeToString(obj)
        assertNotNull(encoded)
    }

    @Test
    fun testSceneDescriptorSerialization() {
        val jsonString = """
            {
              "version": "2.0",
              "scene_id": "scene_1709123456789",
              "scene_type": "morning_commute",
              "intent": {
                "mood": { "valence": 0.6, "arousal": 0.4 },
                "energy_level": 0.4
              },
              "hints": {
                "music": { "genres": ["pop", "indie"], "tempo": "moderate" }
              }
            }
        """.trimIndent()

        val obj = json.decodeFromString<SceneDescriptor>(jsonString)
        assertNotNull(obj)
        assertEquals("scene_1709123456789", obj.scene_id)
        assertEquals(0.6, obj.intent.mood.valence, 0.01)
        assertEquals("pop", obj.hints.music?.genres?.get(0))
    }

    @Test
    fun testEffectCommandsSerialization() {
        val jsonString = """
            {
              "version": "1.0",
              "scene_id": "scene_1709123456789",
              "commands": {
                "content": {
                  "action": "play_playlist",
                  "playlist": [
                    {
                      "track_id": "track_001",
                      "title": "清晨",
                      "artist": "艺术家A",
                      "duration_sec": 210,
                      "energy": 0.4
                    }
                  ]
                }
              }
            }
        """.trimIndent()

        val obj = json.decodeFromString<EffectCommands>(jsonString)
        assertNotNull(obj)
        assertEquals("play_playlist", obj.commands.content?.action)
        assertEquals("track_001", obj.commands.content?.playlist?.get(0)?.track_id)
    }
}
