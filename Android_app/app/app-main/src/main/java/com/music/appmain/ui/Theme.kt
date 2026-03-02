package com.music.appmain.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Web UI 风格主题配色
 * 完全复刻 web-ui-preview 的视觉风格
 */
object CarTheme {
    // 背景色
    val PrimaryBg = Color(0xFF0a0a0f)
    val SecondaryBg = Color(0x9914141e) // rgba(20, 20, 30, 0.6)
    
    // 强调色
    val AccentCyan = Color(0xFF00d4ff)
    val AccentPurple = Color(0xFFb829dd)
    val AccentPink = Color(0xFFff006e)
    val AccentOrange = Color(0xFFff6b35)
    
    // 文字色
    val TextPrimary = Color(0xFFffffff)
    val TextSecondary = Color(0xB3ffffff) // rgba(255, 255, 255, 0.7)
    val TextMuted = Color(0x66ffffff) // rgba(255, 255, 255, 0.4)
    
    // 玻璃效果
    val GlassBg = Color(0x14ffffff) // rgba(255, 255, 255, 0.08)
    val GlassBorder = Color(0x1Affffff) // rgba(255, 255, 255, 0.1)
    
    // 渐变文字样式
    val GradientTitle = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp
    )
    
    // 感知芯片样式
    val ChipLabel = TextStyle(
        fontSize = 10.sp,
        letterSpacing = 1.sp,
        color = TextMuted
    )
    
    val ChipValue = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
        color = TextPrimary
    )
    
    // 歌词样式
    val LyricsCurrent = TextStyle(
        fontSize = 48.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 4.sp
    )
    
    val LyricsNormal = TextStyle(
        fontSize = 28.sp,
        fontWeight = FontWeight.Light,
        letterSpacing = 2.sp
    )
    
    val LyricsMuted = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.Light,
        color = TextMuted
    )
}
