package com.example.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.demo.ui.SceneSelectionScreen
import com.example.demo.ui.ResultDisplayScreen
import com.example.demo.ui.Theme
import com.example.layer3.api.model.*

class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DemoApp()
                }
            }
        }
    }
}

@Composable
fun DemoApp() {
    var selectedScene by remember { mutableStateOf<SceneDescriptor?>(null) }
    var generatedPlaylist by remember { mutableStateOf<Playlist?>(null) }
    var lightingConfig by remember { mutableStateOf<LightingConfig?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var showResult by remember { mutableStateOf(false) }
    
    val scenes = remember {
        listOf(
            SceneDescriptor(
                scene_id = "morning_commute",
                scene_name = "早间通勤",
                scene_narrative = "清晨上班路上，阳光明媚，心情愉悦",
                intent = Intent(
                    mood = Mood(valence = 0.7, arousal = 0.6),
                    energy_level = 0.6,
                    atmosphere = "energetic_morning"
                ),
                hints = Hints(
                    music = MusicHints(
                        genres = listOf("pop", "rock"),
                        tempo = "medium",
                        language = "chinese"
                    ),
                    lighting = LightingHints(
                        color_theme = "warm_orange",
                        pattern = "breathing",
                        intensity = 0.7
                    )
                )
            ),
            SceneDescriptor(
                scene_id = "night_drive",
                scene_name = "夜间驾驶",
                scene_narrative = "夜晚独自驾车回家，城市霓虹闪烁",
                intent = Intent(
                    mood = Mood(valence = 0.4, arousal = 0.3),
                    energy_level = 0.3,
                    atmosphere = "calm_night"
                ),
                hints = Hints(
                    music = MusicHints(
                        genres = listOf("jazz", "r&b"),
                        tempo = "slow",
                        language = "any"
                    ),
                    lighting = LightingHints(
                        color_theme = "cool_blue",
                        pattern = "static",
                        intensity = 0.4
                    )
                )
            ),
            SceneDescriptor(
                scene_id = "weekend_trip",
                scene_name = "周末出游",
                scene_narrative = "周末和朋友一起出游，欢声笑语",
                intent = Intent(
                    mood = Mood(valence = 0.9, arousal = 0.8),
                    energy_level = 0.8,
                    atmosphere = "happy_excited"
                ),
                hints = Hints(
                    music = MusicHints(
                        genres = listOf("pop", "electronic"),
                        tempo = "fast",
                        language = "any"
                    ),
                    lighting = LightingHints(
                        color_theme = "vibrant_rainbow",
                        pattern = "music_sync",
                        intensity = 0.9
                    )
                )
            ),
            SceneDescriptor(
                scene_id = "rainy_day",
                scene_name = "雨天行车",
                scene_narrative = "细雨绵绵，窗外雨滴敲打车窗",
                intent = Intent(
                    mood = Mood(valence = 0.3, arousal = 0.2),
                    energy_level = 0.2,
                    atmosphere = "melancholic_calm"
                ),
                hints = Hints(
                    music = MusicHints(
                        genres = listOf("ballad", "acoustic"),
                        tempo = "slow",
                        language = "chinese"
                    ),
                    lighting = LightingHints(
                        color_theme = "soft_white",
                        pattern = "fade",
                        intensity = 0.3
                    )
                )
            ),
            SceneDescriptor(
                scene_id = "family_time",
                scene_name = "家庭时光",
                scene_narrative = "载着家人出行，温馨和谐",
                intent = Intent(
                    mood = Mood(valence = 0.8, arousal = 0.5),
                    energy_level = 0.5,
                    atmosphere = "warm_family"
                ),
                hints = Hints(
                    music = MusicHints(
                        genres = listOf("pop", "folk"),
                        tempo = "medium",
                        language = "chinese"
                    ),
                    lighting = LightingHints(
                        color_theme = "warm_yellow",
                        pattern = "pulse",
                        intensity = 0.6
                    )
                )
            )
        )
    }
    
    if (showResult && selectedScene != null) {
        ResultDisplayScreen(
            scene = selectedScene!!,
            playlist = generatedPlaylist,
            lightingConfig = lightingConfig,
            onBack = {
                showResult = false
                selectedScene = null
                generatedPlaylist = null
                lightingConfig = null
            }
        )
    } else {
        SceneSelectionScreen(
            scenes = scenes,
            isLoading = isLoading,
            onSceneSelected = { scene ->
                selectedScene = scene
                isLoading = true
                
                generatedPlaylist = Playlist(
                    id = "playlist_${scene.scene_id}",
                    name = "${scene.scene_name}歌单",
                    description = scene.scene_narrative,
                    track_count = 5,
                    tracks = generateMockTracks(scene),
                    tags = scene.hints?.music?.genres ?: emptyList()
                )
                
                lightingConfig = LightingConfig(
                    config_id = "config_${scene.scene_id}",
                    scene = LightingScene(
                        scene_id = scene.scene_id,
                        name = scene.scene_name ?: "Unknown",
                        zones = generateMockZones(scene),
                        sync_with_music = scene.hints?.lighting?.pattern == "music_sync"
                    ),
                    active = true
                )
                
                isLoading = false
                showResult = true
            }
        )
    }
}

private fun generateMockTracks(scene: SceneDescriptor): List<Track> {
    val genres = scene.hints?.music?.genres ?: listOf("pop")
    val baseTracks = listOf(
        Track(
            id = "track_1",
            title = "晴天",
            artist = "周杰伦",
            album = "叶惠美",
            duration_ms = 269000,
            genre = genres.firstOrNull() ?: "pop",
            bpm = 90,
            energy = scene.intent.energy_level
        ),
        Track(
            id = "track_2",
            title = "稻香",
            artist = "周杰伦",
            album = "魔杰座",
            duration_ms = 223000,
            genre = genres.firstOrNull() ?: "pop",
            bpm = 85,
            energy = scene.intent.energy_level * 0.9
        ),
        Track(
            id = "track_3",
            title = "告白气球",
            artist = "周杰伦",
            album = "周杰伦的床边故事",
            duration_ms = 215000,
            genre = genres.firstOrNull() ?: "pop",
            bpm = 95,
            energy = scene.intent.energy_level * 1.1
        ),
        Track(
            id = "track_4",
            title = "七里香",
            artist = "周杰伦",
            album = "七里香",
            duration_ms = 299000,
            genre = genres.firstOrNull() ?: "pop",
            bpm = 88,
            energy = scene.intent.energy_level
        ),
        Track(
            id = "track_5",
            title = "简单爱",
            artist = "周杰伦",
            album = "范特西",
            duration_ms = 267000,
            genre = genres.firstOrNull() ?: "pop",
            bpm = 92,
            energy = scene.intent.energy_level * 0.95
        )
    )
    return baseTracks
}

private fun generateMockZones(scene: SceneDescriptor): List<ZoneConfig> {
    val lightingHints = scene.hints?.lighting
    val colorMap = mapOf(
        "warm_orange" to ColorConfig(hex = "#FFA500", name = "Warm Orange"),
        "cool_blue" to ColorConfig(hex = "#4169E1", name = "Cool Blue"),
        "vibrant_rainbow" to ColorConfig(hex = "#FF69B4", name = "Vibrant Pink"),
        "soft_white" to ColorConfig(hex = "#F5F5DC", name = "Soft White"),
        "warm_yellow" to ColorConfig(hex = "#FFD700", name = "Warm Yellow")
    )
    
    val color = colorMap[lightingHints?.color_theme] ?: ColorConfig(hex = "#FFFFFF", name = "White")
    val pattern = lightingHints?.pattern ?: "static"
    val intensity = lightingHints?.intensity ?: 0.5
    
    return listOf(
        ZoneConfig(
            zone_id = "dashboard",
            enabled = true,
            color = color,
            brightness = intensity,
            pattern = pattern,
            speed = 1.0
        ),
        ZoneConfig(
            zone_id = "door_left",
            enabled = true,
            color = color,
            brightness = intensity * 0.8,
            pattern = pattern,
            speed = 1.0
        ),
        ZoneConfig(
            zone_id = "door_right",
            enabled = true,
            color = color,
            brightness = intensity * 0.8,
            pattern = pattern,
            speed = 1.0
        ),
        ZoneConfig(
            zone_id = "footwell",
            enabled = true,
            color = color,
            brightness = intensity * 0.6,
            pattern = "static",
            speed = 1.0
        ),
        ZoneConfig(
            zone_id = "ceiling",
            enabled = scene.intent.energy_level > 0.5,
            color = color,
            brightness = intensity * 0.5,
            pattern = pattern,
            speed = 1.0
        )
    )
}
