package com.music.appmain.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.music.appmain.permission.PermissionsState
import com.music.semantic.EngineState

@Composable
fun StatusIndicator(
    permissionsState: PermissionsState,
    isInitialized: Boolean,
    isRunning: Boolean,
    engineState: EngineState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "系统状态",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusItem(
                    label = "权限",
                    status = if (permissionsState.allGranted) "已授权" else "未授权",
                    isOk = permissionsState.allGranted,
                    modifier = Modifier.weight(1f)
                )
                
                StatusItem(
                    label = "SDK",
                    status = if (isInitialized) "已初始化" else "未初始化",
                    isOk = isInitialized,
                    modifier = Modifier.weight(1f)
                )
                
                StatusItem(
                    label = "运行",
                    status = if (isRunning) "运行中" else "已停止",
                    isOk = isRunning,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            val engineStatus = when (engineState) {
                is EngineState.Idle -> "空闲"
                is EngineState.Processing -> "处理中..."
                is EngineState.Ready -> "就绪"
                is EngineState.Error -> "错误: ${engineState.message}"
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "引擎状态:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = when (engineState) {
                        is EngineState.Ready -> Color(0xFF4CAF50)
                        is EngineState.Processing -> Color(0xFFFFA726)
                        is EngineState.Error -> Color(0xFFEF5350)
                        is EngineState.Idle -> Color(0xFF9E9E9E)
                    }
                ) {
                    Text(
                        text = engineStatus,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
            
            if (!permissionsState.allGranted) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "未授权权限: ${permissionsState.deniedPermissions.size} 项",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun StatusItem(
    label: String,
    status: String,
    isOk: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isOk) Color(0xFF4CAF50) else Color(0xFFEF5350)
        ) {
            Text(
                text = status,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}
