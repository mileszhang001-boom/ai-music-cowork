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
import com.music.core.api.models.StandardizedSignals

/**
 * Layer 1 感知层 - Web UI 风格
 * 将 StandardizedSignals 数据转换为感知芯片展示
 */
@Composable
fun Layer1DataPanel(
    signals: StandardizedSignals?,
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
                        text = "👁",
                        fontSize = 20.sp
                    )
                }
                
                // 渐变标题
                Text(
                    text = "感知层 Layer 1",
                    style = CarTheme.GradientTitle.copy(
                        brush = Brush.linearGradient(
                            colors = listOf(CarTheme.AccentCyan, CarTheme.AccentPurple)
                        )
                    )
                )
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // 感知芯片网格
        if (signals != null) {
            val chips = extractPerceptionChips(signals)
            
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                chips.forEach { chip ->
                    PerceptionChip(
                        icon = chip.icon,
                        label = chip.label,
                        value = chip.value
                    )
                }
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
                    text = "等待感知数据...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CarTheme.TextMuted
                )
            }
        }
    }
}

/**
 * 感知芯片组件
 */
@Composable
private fun PerceptionChip(
    icon: String,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .background(
                color = CarTheme.GlassBg,
                shape = RoundedCornerShape(14.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 图标
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            CarTheme.AccentCyan.copy(alpha = 0.2f),
                            CarTheme.AccentPurple.copy(alpha = 0.2f)
                        )
                    ),
                    shape = RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = icon,
                fontSize = 18.sp
            )
        }
        
        // 内容
        Column {
            Text(
                text = label.uppercase(),
                style = CarTheme.ChipLabel
            )
            Text(
                text = value,
                style = CarTheme.ChipValue
            )
        }
    }
}

/**
 * 从 StandardizedSignals 提取感知数据
 */
private fun extractPerceptionChips(signals: StandardizedSignals): List<PerceptionChipData> {
    val chips = mutableListOf<PerceptionChipData>()
    
    // 环境感知 - 仅保留环境信息
    signals.signals.environment?.let { env ->
        val desc = buildString {
            append(mapWeather(env.weather))
            if (isNotEmpty()) append(" | ")
            append(mapDateType(env.date_type))
        }
        if (desc.isNotEmpty()) {
            chips.add(PerceptionChipData("🌧", "环境", desc))
        }
    }
    
    // 音频感知 - 内部麦克风
    signals.signals.internal_mic?.let { mic ->
        val noiseLevel = mic.noise_level
        val desc = when {
            mic.has_voice == true -> "语音检测"
            noiseLevel != null -> "噪音 ${noiseLevel.toInt()}dB"
            else -> "环境音"
        }
        chips.add(PerceptionChipData("🎵", "音频", desc))
    }
    
    // 乘客信息
    signals.signals.internal_camera?.passengers?.let { passengers ->
        val count = (passengers.children ?: 0) + (passengers.adults ?: 0) + (passengers.seniors ?: 0)
        val position = when {
            (passengers.children ?: 0) > 0 -> "有儿童"
            (passengers.seniors ?: 0) > 0 -> "有老人"
            else -> "成人"
        }
        chips.add(PerceptionChipData("👤", "乘客", "${count}人 $position"))
    }
    
    return chips
}

private data class PerceptionChipData(
    val icon: String,
    val label: String,
    val value: String
)

private fun mapSceneDescription(desc: String?): String {
    return when (desc) {
        "rainy_night_city" -> "雨夜城市"
        "suburban_street" -> "郊区街道"
        "rainbow_after_rain" -> "雨后彩虹"
        "city_evening" -> "城市傍晚"
        "highway_drive" -> "高速行驶"
        "coastal_highway_beach" -> "沿海公路"
        "city_night_lights" -> "城市夜景"
        "highway_monotonous" -> "高速单调"
        else -> desc ?: ""
    }
}

private fun mapMood(mood: String?): String {
    return when (mood) {
        "sad" -> "低落"
        "happy" -> "开心"
        "excited" -> "兴奋"
        "neutral" -> "平静"
        "relaxed" -> "放松"
        "romantic" -> "浪漫"
        "tired" -> "疲劳"
        else -> mood ?: ""
    }
}

private fun mapWeather(weather: String?): String {
    return when (weather) {
        "rainy" -> "雨天"
        "sunny" -> "晴天"
        "cloudy" -> "多云"
        "clear" -> "晴朗"
        else -> weather ?: ""
    }
}

private fun mapDateType(dateType: String?): String {
    return when (dateType) {
        "weekday" -> "工作日"
        "weekend" -> "周末"
        else -> dateType ?: ""
    }
}

private fun mapColorToName(hexColor: String?): String {
    return when (hexColor?.uppercase()) {
        "#1A237E" -> "深蓝"
        "#4A148C" -> "深紫"
        "#FF6F00" -> "橙色"
        "#FFCA28" -> "金黄"
        "#00BCD4" -> "青色"
        "#4DD0E1" -> "浅青"
        "#D500F9" -> "紫色"
        "#EA80FC" -> "浅紫"
        "#00E5FF" -> "亮青"
        "#18FFFF" -> "亮蓝"
        "#00B8D4" -> "湖蓝"
        "#80DEEA" -> "浅蓝"
        "#AD1457" -> "玫红"
        "#880E4F" -> "暗红"
        "#FF6D00" -> "橙黄"
        "#FFAB00" -> "金橙"
        else -> hexColor ?: ""
    }
}
