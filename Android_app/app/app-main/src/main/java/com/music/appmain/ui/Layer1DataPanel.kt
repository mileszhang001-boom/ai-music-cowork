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
                    text = "👁",
                    fontSize = 16.sp
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            val themeColors = LocalThemeColors.current
            val primaryColor = themeColors.getOrElse(0) { CarTheme.AccentCyan }
            val secondaryColor = themeColors.getOrElse(1) { CarTheme.AccentPurple }
            
            Text(
                text = "感知层",
                style = CarTheme.GradientTitle.copy(
                    brush = Brush.linearGradient(
                        colors = listOf(primaryColor, secondaryColor)
                    ),
                    fontSize = 16.sp
                )
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // 感知芯片网格 - 可滚动
        if (signals != null) {
            val chips = extractPerceptionChips(signals)
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
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
            .fillMaxWidth()
            .background(
                color = CarTheme.GlassBg,
                shape = RoundedCornerShape(14.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
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
    
    // 环境感知 - 北京 + 天气 + 温度
    signals.signals.environment?.let { env ->
        val weatherDesc = buildString {
            append("北京")
            val weather = env.weather
            val temp = env.temperature
            if (!weather.isNullOrEmpty()) {
                append(" | ")
                append(mapWeather(weather))
            }
            if (temp != null) {
                append(" | ${temp.toInt()}°C")
            }
        }
        chips.add(PerceptionChipData("🌍", "环境", weatherDesc))
        
        // 时间感知 - 工作日/周末 + 时间段
        val timeDesc = buildString {
            val dateType = env.date_type
            val timeOfDay = env.time_of_day
            append(mapDateType(dateType))
            if (!isEmpty()) append(" | ")
            append(mapTimeOfDay(timeOfDay))
        }
        chips.add(PerceptionChipData("🕐", "时间", timeDesc))
    }
    
    // 乘客信息 - 人数 + 成人/儿童
    signals.signals.internal_camera?.passengers?.let { passengers ->
        val totalCount = (passengers.children ?: 0) + (passengers.adults ?: 0) + (passengers.seniors ?: 0)
        val passengerDesc = buildString {
            append("${totalCount}人")
            val parts = mutableListOf<String>()
            if ((passengers.adults ?: 0) > 0) parts.add("${passengers.adults}成人")
            if ((passengers.children ?: 0) > 0) parts.add("${passengers.children}儿童")
            if ((passengers.seniors ?: 0) > 0) parts.add("${passengers.seniors}老人")
            if (parts.isNotEmpty()) {
                append(" | ")
                append(parts.joinToString(" "))
            }
        }
        chips.add(PerceptionChipData("👤", "乘客", passengerDesc))
    }
    
    // 表情感知 - 放在最后
    signals.signals.internal_camera?.mood?.let { mood ->
        chips.add(PerceptionChipData("😊", "表情", mapMood(mood)))
    }
    
    return chips
}

/**
 * 从时间值映射到时间段
 */
private fun mapTimeOfDay(timeOfDay: Double?): String {
    if (timeOfDay == null) return "未知"
    return when {
        timeOfDay >= 5.0 && timeOfDay < 7.0 -> "凌晨"
        timeOfDay >= 7.0 && timeOfDay < 9.0 -> "早晨"
        timeOfDay >= 9.0 && timeOfDay < 12.0 -> "上午"
        timeOfDay >= 12.0 && timeOfDay < 14.0 -> "中午"
        timeOfDay >= 14.0 && timeOfDay < 18.0 -> "下午"
        timeOfDay >= 18.0 && timeOfDay < 22.0 -> "傍晚"
        else -> "晚上"
    }
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
