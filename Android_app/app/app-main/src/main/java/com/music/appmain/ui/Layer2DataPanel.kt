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
    ) {
        // 标题栏 - 固定在顶部
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(CarTheme.AccentCyan, CarTheme.AccentPurple)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🧠",
                    fontSize = 16.sp
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            val themeColors = LocalThemeColors.current
            val primaryColor = themeColors.getOrElse(0) { CarTheme.AccentCyan }
            val secondaryColor = themeColors.getOrElse(1) { CarTheme.AccentPurple }
            
            Text(
                text = "推理层",
                style = CarTheme.GradientTitle.copy(
                    brush = Brush.linearGradient(
                        colors = listOf(primaryColor, secondaryColor)
                    ),
                    fontSize = 16.sp
                )
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // 推理流展示 - 可滚动
        if (sceneDescriptor != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 辅助推理
                val hints = extractHints(sceneDescriptor)
                hints.forEach { hint ->
                    val parts = hint.split(" → ", "->")
                    if (parts.size >= 2) {
                        InferenceItem(
                            cause = parts[0].trim(),
                            result = parts.drop(1).joinToString(" → ").trim(),
                            isHighlighted = false
                        )
                    } else {
                        InferenceItem(
                            cause = "推理",
                            result = hint,
                            isHighlighted = false
                        )
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
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
 * 推理项组件
 */
@Composable
private fun InferenceItem(
    cause: String,
    result: String,
    isHighlighted: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = if (isHighlighted) {
                    Brush.linearGradient(
                        colors = listOf(
                            CarTheme.AccentCyan.copy(alpha = 0.15f),
                            CarTheme.AccentPurple.copy(alpha = 0.15f)
                        )
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(
                            CarTheme.GlassBg,
                            CarTheme.GlassBg
                        )
                    )
                },
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = cause,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = if (isHighlighted) CarTheme.AccentCyan else CarTheme.TextSecondary,
                    fontWeight = if (isHighlighted) FontWeight.Medium else FontWeight.Normal
                )
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = "→",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = CarTheme.TextMuted
                )
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = result,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = if (isHighlighted) CarTheme.AccentPurple else CarTheme.TextPrimary,
                    fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium
                ),
                modifier = Modifier.weight(1f)
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
