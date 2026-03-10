package com.example.demo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.demo.DemoApplication
import com.example.layer3.api.model.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultDisplayScreen(
    scene: SceneDescriptor,
    playlist: Playlist?,
    lightingConfig: LightingConfig?,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isApplying by remember { mutableStateOf(false) }
    var applyResult by remember { mutableStateOf<String?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(scene.scene_name ?: "场景结果") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SceneInfoCard(scene = scene)
            }
            
            item {
                Text(
                    text = "推荐歌单",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            if (playlist != null) {
                item {
                    PlaylistCard(playlist = playlist)
                }
                
                items(playlist.tracks) { track ->
                    TrackItem(track = track)
                }
            }
            
            item {
                Text(
                    text = "灯光配置",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            if (lightingConfig != null) {
                item {
                    LightingConfigCard(config = lightingConfig)
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = {
                        if (lightingConfig != null) {
                            scope.launch {
                                isApplying = true
                                val controller = DemoApplication.mockAmbientLightController
                                val result = controller.applyLightingConfig(lightingConfig)
                                isApplying = false
                                applyResult = if (result.isSuccess) {
                                    "灯光配置已应用（查看日志）"
                                } else {
                                    "应用失败: ${result.exceptionOrNull()?.message}"
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isApplying && lightingConfig != null
                ) {
                    if (isApplying) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("正在应用...")
                    } else {
                        Icon(Icons.Default.Lightbulb, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("应用灯光配置")
                    }
                }
                
                if (applyResult != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = applyResult!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (applyResult!!.contains("失败")) 
                            MaterialTheme.colorScheme.error 
                        else 
                            MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun SceneInfoCard(scene: SceneDescriptor) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = scene.scene_name ?: "Unknown Scene",
                style = MaterialTheme.typography.titleLarge
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = scene.scene_narrative ?: "",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                scene.intent.mood.let { mood ->
                    InfoChip(
                        label = "心情",
                        value = when {
                            mood.valence > 0.7 -> "愉悦"
                            mood.valence > 0.4 -> "平静"
                            else -> "低落"
                        }
                    )
                }
                
                InfoChip(
                    label = "能量",
                    value = "${(scene.intent.energy_level * 100).toInt()}%"
                )
            }
        }
    }
}

@Composable
fun InfoChip(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
fun PlaylistCard(playlist: Playlist) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.QueueMusic,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(
                        text = playlist.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${playlist.track_count} 首歌曲",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            if (playlist.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(playlist.tags) { tag ->
                        SuggestionChip(
                            onClick = {},
                            label = { Text(tag) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TrackItem(track: Track) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                track.bpm?.let { bpm ->
                    Text(
                        text = "$bpm BPM",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                track.duration_ms.let { duration ->
                    Text(
                        text = formatDuration(duration),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun LightingConfigCard(config: LightingConfig) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(
                        text = config.scene.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (config.scene.sync_with_music) "音乐同步已启用" else "音乐同步已关闭",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "灯光区域",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            config.scene.zones.filter { it.enabled }.forEach { zone ->
                ZoneConfigItem(zone = zone)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun ZoneConfigItem(zone: ZoneConfig) {
    val zoneNames = mapOf(
        "dashboard" to "仪表盘",
        "door_left" to "左车门",
        "door_right" to "右车门",
        "footwell" to "脚部空间",
        "ceiling" to "车顶"
    )
    
    val patternNames = mapOf(
        "static" to "静态",
        "breathing" to "呼吸",
        "pulse" to "脉冲",
        "wave" to "波浪",
        "music_sync" to "音乐同步",
        "rainbow" to "彩虹",
        "fade" to "渐变"
    )
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(
                    parseColor(zone.color.hex),
                    RoundedCornerShape(4.dp)
                )
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = zoneNames[zone.zone_id] ?: zone.zone_id,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "${patternNames[zone.pattern] ?: zone.pattern} · ${(zone.brightness * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun parseColor(hex: String): Color {
    return try {
        val cleanHex = hex.removePrefix("#")
        val color = cleanHex.toLong(16)
        Color(
            red = ((color shr 16) and 0xFF) / 255f,
            green = ((color shr 8) and 0xFF) / 255f,
            blue = (color and 0xFF) / 255f
        )
    } catch (e: Exception) {
        Color.White
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
