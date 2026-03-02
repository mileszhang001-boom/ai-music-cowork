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
import com.example.layer3.api.model.EffectCommands
import com.example.layer3.api.model.EffectCommand
import com.example.layer3.api.model.EngineTypes
import com.example.layer3.api.model.Playlist
import com.example.layer3.api.model.LightingConfig
import com.example.layer3.api.model.AudioConfig

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
                    isActive = effectCommands != null && effectCommands.commands.isNotEmpty(),
                    label = if (effectCommands != null && effectCommands.commands.isNotEmpty()) "活跃" else "待机"
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (effectCommands != null && effectCommands.commands.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    effectCommands.sceneId?.let {
                        KeyValueRow("scene_id", it)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    val contentCommands = effectCommands.commands.filter { it.engineType == EngineTypes.CONTENT }
                    val lightingCommands = effectCommands.commands.filter { it.engineType == EngineTypes.LIGHTING }
                    val audioCommands = effectCommands.commands.filter { it.engineType == EngineTypes.AUDIO }
                    
                    if (contentCommands.isNotEmpty()) {
                        CommandSection(
                            title = "内容命令",
                            commands = contentCommands
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    
                    if (lightingCommands.isNotEmpty()) {
                        CommandSection(
                            title = "灯光命令",
                            commands = lightingCommands
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    
                    if (audioCommands.isNotEmpty()) {
                        CommandSection(
                            title = "音频命令",
                            commands = audioCommands
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
private fun CommandSection(
    title: String,
    commands: List<EffectCommand>
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        commands.forEach { command ->
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
                    if (command.params.isNotEmpty()) {
                        command.params.forEach { (key, value) ->
                            KeyValueRow(key, value.toString())
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
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
