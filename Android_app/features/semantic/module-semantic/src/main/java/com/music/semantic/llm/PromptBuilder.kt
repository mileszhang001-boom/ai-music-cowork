package com.music.semantic.llm

import com.music.core.api.models.StandardizedSignals
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class PromptBuilder {
    
    private val json = Json { prettyPrint = true }
    
    fun buildSystemPrompt(): String {
        return """你是一个车载座舱 AI 娱乐助手，专门负责根据当前驾驶场景为用户推荐最合适的音乐、灯光和音效配置。

你的核心能力：
1. 理解驾驶场景：时间、天气、乘客数量、车辆状态、用户情绪等
2. 推荐音乐：根据场景选择合适的音乐风格、节奏和能量级别
3. 配置灯光：根据场景调整车内氛围灯的颜色、亮度和动效
4. 调整音效：根据场景优化音效预设，提升听觉体验

输出要求：
- 必须输出符合 Scene Descriptor V2.0 规范的 JSON 格式
- 所有数值字段必须在合理范围内
- 必须包含 version, scene_id, scene_type, intent, hints 字段
- 不要输出任何额外的解释文字，只输出 JSON"""
    }
    
    fun buildUserPrompt(signals: StandardizedSignals): String {
        val contextBuilder = StringBuilder()
        contextBuilder.appendLine("当前场景信息：")
        contextBuilder.appendLine()
        
        signals.signals.vehicle?.let { vehicle ->
            contextBuilder.appendLine("【车辆信息】")
            vehicle.speed_kmh?.let { contextBuilder.appendLine("- 车速: $it km/h") }
            vehicle.passenger_count?.let { contextBuilder.appendLine("- 乘客数量: $it") }
            vehicle.gear?.let { contextBuilder.appendLine("- 档位: $it") }
            contextBuilder.appendLine()
        }
        
        signals.signals.environment?.let { env ->
            contextBuilder.appendLine("【环境信息】")
            env.time_of_day?.let { 
                val hour = (it * 24).toInt()
                contextBuilder.appendLine("- 时段: ${getTimePeriod(hour)} ($hour:00)")
            }
            env.weather?.let { contextBuilder.appendLine("- 天气: $it") }
            env.temperature?.let { contextBuilder.appendLine("- 温度: $it°C") }
            env.date_type?.let { contextBuilder.appendLine("- 日期类型: $it") }
            contextBuilder.appendLine()
        }
        
        signals.signals.internal_camera?.let { camera ->
            contextBuilder.appendLine("【用户状态】")
            camera.mood?.let { contextBuilder.appendLine("- 情绪状态: $it") }
            camera.passengers?.let { p ->
                val hasChildren = (p.children ?: 0) > 0
                val hasSeniors = (p.seniors ?: 0) > 0
                if (hasChildren) contextBuilder.appendLine("- 有儿童乘客")
                if (hasSeniors) contextBuilder.appendLine("- 有老年乘客")
            }
            contextBuilder.appendLine()
        }
        
        signals.signals.external_camera?.let { ext ->
            contextBuilder.appendLine("【外部环境】")
            ext.scene_description?.let { contextBuilder.appendLine("- 场景: $it") }
            ext.brightness?.let { contextBuilder.appendLine("- 亮度: ${(it * 100).toInt()}%") }
            contextBuilder.appendLine()
        }
        
        contextBuilder.appendLine("请根据以上信息，生成一个完整的 Scene Descriptor JSON。")
        
        return contextBuilder.toString()
    }
    
    fun buildMessages(signals: StandardizedSignals): List<LlmMessage> {
        return listOf(
            LlmMessage(role = "system", content = buildSystemPrompt()),
            LlmMessage(role = "user", content = buildUserPrompt(signals))
        )
    }
    
    private fun getTimePeriod(hour: Int): String {
        return when (hour) {
            in 5..8 -> "早晨"
            in 9..11 -> "上午"
            in 12..13 -> "中午"
            in 14..17 -> "下午"
            in 18..21 -> "傍晚"
            else -> "深夜"
        }
    }
}
