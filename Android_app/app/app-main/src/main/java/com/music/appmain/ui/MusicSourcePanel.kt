package com.music.appmain.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.music.localmusic.source.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicSourcePanel(
    sourceManager: MusicSourceManager,
    onUsbUriSelected: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    
    val sourceInfo by remember { derivedStateOf { sourceManager.getSourceInfo() } }
    val managerState by sourceManager.managerState.collectAsState()
    val usbEvent by sourceManager.usbEventFlow.collectAsState()
    
    val usbPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            onUsbUriSelected(it)
        }
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "音乐源",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (managerState.isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "扫描中...",
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        Text(
                            text = "${managerState.totalTracks} 首",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sourceInfo.forEach { info ->
                    SourceItem(
                        info = info,
                        onEnableChange = { enabled ->
                            sourceManager.setSourceEnabled(info.sourceId, enabled)
                            if (enabled) {
                                scope.launch {
                                    sourceManager.scanSource(info.sourceId)
                                }
                            }
                        },
                        onSelectUsb = {
                            usbPickerLauncher.launch(null)
                        }
                    )
                }
            }
            
            if (usbEvent is UsbEvent.DeviceAttached) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "USB",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "检测到 USB 设备",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(onClick = { usbPickerLauncher.launch(null) }) {
                            Text("选择目录")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceItem(
    info: SourceInfo,
    onEnableChange: (Boolean) -> Unit,
    onSelectUsb: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (info.isEnabled) 
                MaterialTheme.colorScheme.surfaceVariant 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val sourceLabel = when (info.type) {
                is SourceType.Local -> "本地"
                is SourceType.Usb -> "USB"
                is SourceType.MediaStore -> "系统"
            }
            
            Surface(
                color = if (info.isEnabled) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.outline,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = sourceLabel,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (info.isEnabled)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.outline
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = info.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = getStateText(info.state),
                    style = MaterialTheme.typography.bodySmall,
                    color = getStateColor(info.state)
                )
            }
            
            if (info.type is SourceType.Usb && !info.isAvailable) {
                TextButton(onClick = onSelectUsb) {
                    Text("选择")
                }
            } else {
                Switch(
                    checked = info.isEnabled,
                    onCheckedChange = onEnableChange,
                    enabled = info.isAvailable
                )
            }
        }
    }
}

@Composable
private fun getStateText(state: SourceState): String {
    return when (state) {
        is SourceState.Idle -> "待机"
        is SourceState.Scanning -> "扫描中..."
        is SourceState.Ready -> "${state.trackCount} 首音乐"
        is SourceState.Error -> "错误: ${state.message}"
        is SourceState.Unavailable -> "不可用"
    }
}

@Composable
private fun getStateColor(state: SourceState): androidx.compose.ui.graphics.Color {
    return when (state) {
        is SourceState.Idle -> MaterialTheme.colorScheme.outline
        is SourceState.Scanning -> MaterialTheme.colorScheme.primary
        is SourceState.Ready -> MaterialTheme.colorScheme.primary
        is SourceState.Error -> MaterialTheme.colorScheme.error
        is SourceState.Unavailable -> MaterialTheme.colorScheme.outline
    }
}
