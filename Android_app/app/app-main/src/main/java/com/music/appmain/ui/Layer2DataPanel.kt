package com.music.appmain.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.music.core.api.models.SceneDescriptor

@Composable
fun Layer2DataPanel(
    sceneDescriptor: SceneDescriptor?,
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
                    text = "Layer 2 - 语义层",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                StatusBadge(
                    isActive = sceneDescriptor != null,
                    label = if (sceneDescriptor != null) "活跃" else "待机"
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (sceneDescriptor != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    KeyValueRow("scene_type", sceneDescriptor.scene_type)
                    KeyValueRow("scene_id", sceneDescriptor.scene_id)
                    sceneDescriptor.scene_name?.let { KeyValueRow("scene_name", it) }
                    
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    
                    Text(
                        text = "Intent",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    KeyValueRow("mood.valence", "%.2f".format(sceneDescriptor.intent.mood.valence))
                    KeyValueRow("mood.arousal", "%.2f".format(sceneDescriptor.intent.mood.arousal))
                    KeyValueRow("energy_level", "%.2f".format(sceneDescriptor.intent.energy_level))
                    sceneDescriptor.intent.atmosphere?.let { KeyValueRow("atmosphere", it) }
                    sceneDescriptor.intent.social_context?.let { KeyValueRow("social_context", it) }
                    
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    
                    Text(
                        text = "Hints",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    sceneDescriptor.hints?.music?.genres?.let { genres ->
                        KeyValueRow("music.genres", genres.joinToString(", "))
                    }
                    sceneDescriptor.hints?.music?.tempo?.let { KeyValueRow("music.tempo", it) }
                    sceneDescriptor.hints?.lighting?.color_theme?.let { KeyValueRow("lighting.color_theme", it) }
                    sceneDescriptor.hints?.lighting?.pattern?.let { KeyValueRow("lighting.pattern", it) }
                    sceneDescriptor.hints?.audio?.preset?.let { KeyValueRow("audio.preset", it) }
                    
                    sceneDescriptor.meta?.confidence?.let { 
                        KeyValueRow("confidence", "%.2f".format(it))
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text(
                        text = "等待语义分析...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
