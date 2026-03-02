package com.music.appmain.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.core.api.models.SceneDescriptor

/**
 * Layer 2 推理层 - Web UI 风格
 * 将 SceneDescriptor 数据转换为推理流展示
 */
@Composable
fun Layer2DataPanel(
    sceneDescriptor: SceneDescriptor?,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 标题栏
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 图标
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(CarTheme.AccentCyan, CarTheme.AccentPurple)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🧠",
                        fontSize = 20.sp
                    )
                }
                
                // 渐变标题
                Text(
                    text = "推理层 Layer 2",
                    style = CarTheme.GradientTitle.copy(
                        brush = Brush.linearGradient(
                            colors = listOf(CarTheme.AccentCyan, CarTheme.AccentPurple)
                        )
                    )
                )
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // 推理流展示
        if (sceneDescriptor != null) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 主要推理
                MainInference(sceneDescriptor)
                
                // 辅助推理
                SubInference(sceneDescriptor)
            }
        } else {
            // 空状态
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "等待语义分析...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CarTheme.TextMuted
                )
            }
        }
    }
}

/**
 * 主要推理展示
 */
@Composable
private fun MainInference(sceneDescriptor: SceneDescriptor) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        CarTheme.AccentCyan.copy(alpha = 0.12f),
                        CarTheme.AccentPurple.copy(alpha = 0.12f)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 原因
            Text(
                text = extractCause(sceneDescriptor),
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = CarTheme.AccentCyan,
                    fontWeight = FontWeight.Medium
                )
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // 箭头
            Text(
                text = "→",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = CarTheme.TextMuted
                )
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // 结果
            Text(
                text = extractResult(sceneDescriptor),
                style = MaterialTheme.typography.titleMedium.copy(
                    color = CarTheme.AccentPurple,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

/**
 * 辅助推理展示
 */
@Composable
private fun SubInference(sceneDescriptor: SceneDescriptor) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp)
    ) {
        // 标签
        Text(
            text = "辅助推理".uppercase(),
            style = CarTheme.ChipLabel,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // 辅助推理内容
        val hints = extractHints(sceneDescriptor)
        hints.forEach { hint ->
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = CarTheme.TextSecondary,
                    lineHeight = 24.sp
                )
            )
        }
    }
}

/**
 * 提取原因
 */
private fun extractCause(sceneDescriptor: SceneDescriptor): String {
    val causes = mutableListOf<String>()
    
    sceneDescriptor.intent.mood.let { mood ->
        when {
            mood.valence < 0.3f -> causes.add("情绪低落")
            mood.valence > 0.7f -> causes.add("情绪愉悦")
            else -> causes.add("情绪平稳")
        }
    }
    
    causes.add(mapSceneType(sceneDescriptor.scene_type))
    
    return causes.firstOrNull() ?: "场景触发"
}

/**
 * 提取结果
 */
private fun extractResult(sceneDescriptor: SceneDescriptor): String {
    val results = mutableListOf<String>()
    
    sceneDescriptor.hints.music?.genres?.firstOrNull()?.let { results.add(mapMusicGenre(it)) }
    results.add(mapSceneName(sceneDescriptor.scene_name))
    
    return results.firstOrNull() ?: "智能推荐"
}

/**
 * 提取辅助推理
 */
private fun extractHints(sceneDescriptor: SceneDescriptor): List<String> {
    val hints = mutableListOf<String>()
    
    // 场景叙事
    sceneDescriptor.scene_narrative?.let {
        hints.add("场景: $it")
    }
    
    // 能量等级
    sceneDescriptor.intent.energy_level.let { energy ->
        val level = when {
            energy < 0.3f -> "低能量"
            energy > 0.7f -> "高能量"
            else -> "中等能量"
        }
        hints.add("$level → 调节节奏强度")
    }
    
    // 社交场景
    sceneDescriptor.intent.social_context?.let { social ->
        hints.add("$social → 调整音乐风格")
    }
    
    // 音乐偏好
    sceneDescriptor.hints.music?.tempo?.let { tempo ->
        hints.add("偏好 $tempo → 匹配 BPM")
    }
    
    // 灯光提示
    sceneDescriptor.hints.lighting?.color_theme?.let { theme ->
        hints.add("灯光主题: $theme")
    }
    
    return hints.ifEmpty { listOf("综合分析中...") }
}

private fun mapSceneType(sceneType: String?): String {
    return when (sceneType) {
        "emotional_ride" -> "情绪旅途"
        "family_trip" -> "家庭出行"
        "commute" -> "日常通勤"
        "road_trip" -> "公路旅行"
        "date_night" -> "约会之夜"
        "fatigue_driving" -> "疲劳驾驶"
        "scenic_drive" -> "风景驾驶"
        else -> sceneType ?: ""
    }
}

private fun mapSceneName(sceneName: String?): String {
    return when (sceneName) {
        "rainy_emo" -> "雨天情绪"
        "children_board" -> "小朋友上车"
        "rain_stops" -> "雨过天晴"
        "noisy_car" -> "车内热闹"
        "user_pop" -> "流行偏好"
        "beach_vacation" -> "海滩度假"
        "romantic_date" -> "浪漫约会"
        "fatigue_alert" -> "疲劳提醒"
        "melancholy_night" -> "忧郁雨夜"
        "cheerful_day" -> "晴朗心情"
        "energetic_group" -> "活力群体"
        "relaxed_vacation" -> "放松度假"
        else -> sceneName ?: ""
    }
}

private fun mapMusicGenre(genre: String?): String {
    return when (genre) {
        "pop" -> "流行"
        "rock" -> "摇滚"
        "jazz" -> "爵士"
        "classical" -> "古典"
        "electronic" -> "电子"
        "r&b" -> "节奏布鲁斯"
        "country" -> "乡村"
        "indie" -> "独立音乐"
        "hip-hop" -> "嘻哈"
        "ambient" -> "氛围"
        "chillout" -> "放松"
        "lo-fi" -> "低保真"
        else -> genre ?: ""
    }
}
