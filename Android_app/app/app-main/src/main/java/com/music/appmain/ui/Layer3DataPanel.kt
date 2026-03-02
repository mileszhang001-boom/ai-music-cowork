package com.music.appmain.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.music.core.api.models.EffectCommands
import com.music.core.api.models.ContentCommand
import com.music.core.api.models.LightingCommand
import com.music.core.api.models.AudioCommand

@Composable
fun Layer3DataPanel(
    effectCommands: EffectCommands?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Layer 3 - 执行层",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary
                )
                StatusBadge(
                    isActive = effectCommands != null && effectCommands.commands != null,
                    label = if (effectCommands != null && effectCommands.commands != null) "活跃" else "待机"
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (effectCommands != null && effectCommands.commands != null) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    effectCommands.scene_id?.let {
                        KeyValueRow("scene_id", it)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    val contentCommand = effectCommands.commands?.content
                    val lightingCommand = effectCommands.commands?.lighting
                    val audioCommand = effectCommands.commands?.audio
                    
                    if (contentCommand != null) {
                        ContentCommandSection(
                            title = "内容命令",
                            command = contentCommand
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    
                    if (lightingCommand != null) {
                        LightingCommandSection(
                            title = "灯光命令",
                            command = lightingCommand
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    
                    if (audioCommand != null) {
                        AudioCommandSection(
                            title = "音频命令",
                            command = audioCommand
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "等待执行命令...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ContentCommandSection(
    title: String,
    command: ContentCommand
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                KeyValueRow("action", command.action)
                command.playlist?.let { KeyValueRow("playlist_size", it.size.toString()) }
                command.play_mode?.let { KeyValueRow("play_mode", it) }
            }
        }
    }
}

@Composable
private fun LightingCommandSection(
    title: String,
    command: LightingCommand
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                command.action?.let { KeyValueRow("action", it) }
                command.theme?.let { KeyValueRow("theme", it) }
                command.intensity?.let { KeyValueRow("intensity", it.toString()) }
            }
        }
    }
}

@Composable
private fun AudioCommandSection(
    title: String,
    command: AudioCommand
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                command.action?.let { KeyValueRow("action", it) }
                command.preset?.let { KeyValueRow("preset", it) }
                command.settings?.volume_db?.let { KeyValueRow("volume_db", it.toString()) }
            }
        }
    }
}

@Composable
private fun KeyValueRow(
    key: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = key,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatusBadge(
    isActive: Boolean,
    label: String
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isActive) Color(0xFF4CAF50) else Color(0xFF9E9E9E)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}
