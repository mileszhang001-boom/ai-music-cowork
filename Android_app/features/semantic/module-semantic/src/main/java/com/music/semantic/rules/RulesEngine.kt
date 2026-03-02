package com.music.semantic.rules

import android.util.Log
import com.music.core.api.models.StandardizedSignals
import com.music.core.api.models.Signals
import java.util.Calendar

data class RuleMatchResult(
    val matched: Boolean,
    val templateId: String?,
    val score: Double,
    val ruleName: String?
)

class RulesEngine {
    
    companion object {
        private const val TAG = "RulesEngine"
    }
    
    fun matchTemplate(signals: StandardizedSignals): RuleMatchResult {
        val context = buildRuleContext(signals)
        val candidates = mutableListOf<Pair<String, Double>>()
        
        Log.d(TAG, "规则匹配上下文: hour=${context.hour}, weather=${context.weather}, mood=${context.mood}, scene=${context.sceneDescription}")
        
        if (isFatigueAlert(context, signals)) {
            candidates.add("TPL_004" to 3.0)
        }
        
        if (isRomanticDate(context, signals)) {
            candidates.add("TPL_032" to 2.5)
        }
        
        if (isBeachVacation(context, signals)) {
            candidates.add("TPL_047" to 2.5)
        }
        
        if (isRainyEmo(context, signals)) {
            candidates.add("TPL_005" to 2.0)
        }
        
        if (hasChildren(context, signals)) {
            candidates.add("TPL_013" to 2.8)
        }
        
        if (isNoisyCar(context, signals)) {
            candidates.add("TPL_041" to 1.8)
        }
        
        if (isRainStopsToSunny(context, signals)) {
            candidates.add("TPL_021" to 1.7)
        }
        
        if (isRainyNight(context, signals)) {
            candidates.add("TPL_005" to 1.5)
        }
        
        if (isHappyMood(context, signals)) {
            candidates.add("TPL_058" to 1.5)
        }
        
        if (hasMultiplePassengers(context, signals)) {
            candidates.add("TPL_003" to 1.0)
        }
        
        if (isSunnyMorning(context, signals)) {
            candidates.add("TPL_010" to 1.2)
        }
        
        if (isSunnyDay(context, signals)) {
            candidates.add("TPL_021" to 1.0)
        }
        
        if (isRainyDay(context, signals)) {
            candidates.add("TPL_022" to 1.0)
        }
        
        if (isMorningCommute(context)) {
            candidates.add("TPL_001" to 0.8)
        }
        
        if (isAfternoonDrive(context)) {
            candidates.add("TPL_012" to 0.8)
        }
        
        if (isEveningCommute(context)) {
            candidates.add("TPL_014" to 0.8)
        }
        
        if (isNightDrive(context)) {
            candidates.add("TPL_002" to 0.8)
        }
        
        if (isSoloDrive(context, signals)) {
            candidates.add("TPL_031" to 0.6)
        }
        
        if (candidates.isEmpty()) {
            val defaultTemplate = getDefaultTemplate(context)
            Log.d(TAG, "无匹配规则，使用默认模板: $defaultTemplate")
            return RuleMatchResult(
                matched = true,
                templateId = defaultTemplate,
                score = 0.0,
                ruleName = "default"
            )
        }
        
        val best = candidates.maxByOrNull { it.second }
        Log.i(TAG, "规则匹配成功: ${best!!.first}, score=${best.second}, 场景=${getRuleNameForTemplate(best.first)}")
        return RuleMatchResult(
            matched = true,
            templateId = best.first,
            score = best.second,
            ruleName = getRuleNameForTemplate(best.first)
        )
    }
    
    private fun getDefaultTemplate(context: RuleContext): String {
        return when {
            context.hour in 6..9 -> "TPL_001"
            context.hour in 10..12 -> "TPL_012"
            context.hour in 13..17 -> "TPL_012"
            context.hour in 17..20 -> "TPL_014"
            context.hour in 20..24 -> "TPL_002"
            else -> "TPL_002"
        }
    }
    
    private fun buildRuleContext(signals: StandardizedSignals): RuleContext {
        val timeOfDay = signals.signals.environment?.time_of_day
        val hour = if (timeOfDay != null) {
            timeOfDay.toInt()
        } else {
            Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        }
        
        val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        
        return RuleContext(
            hour = hour,
            dayOfWeek = dayOfWeek,
            speed = signals.signals.vehicle?.speed_kmh,
            passengerCount = signals.signals.vehicle?.passenger_count,
            weather = signals.signals.environment?.weather,
            mood = signals.signals.internal_camera?.mood,
            hasChildren = (signals.signals.internal_camera?.passengers?.children ?: 0) > 0,
            hasSeniors = (signals.signals.internal_camera?.passengers?.seniors ?: 0) > 0,
            timeOfDay = signals.signals.environment?.time_of_day,
            dateType = signals.signals.environment?.date_type,
            sceneDescription = signals.signals.external_camera?.scene_description,
            noiseLevel = signals.signals.internal_mic?.noise_level
        )
    }
    
    private fun isMorningCommute(context: RuleContext): Boolean {
        return context.hour in 6..9 && 
               (context.passengerCount == null || context.passengerCount == 1) &&
               context.dateType != "weekend"
    }
    
    private fun isNightDrive(context: RuleContext): Boolean {
        return context.hour >= 22 || context.hour < 6
    }
    
    private fun isRainyEmo(context: RuleContext, signals: StandardizedSignals): Boolean {
        val isRainy = context.weather?.lowercase()?.contains("rain") == true
        val isSad = signals.signals.internal_camera?.mood?.lowercase() == "sad"
        return isRainy && isSad
    }
    
    private fun isRainStopsToSunny(context: RuleContext, signals: StandardizedSignals): Boolean {
        val isSunny = context.weather?.lowercase()?.contains("sunny") == true ||
                     signals.signals.environment?.weather?.lowercase()?.contains("sunny") == true
        val isHappy = signals.signals.internal_camera?.mood?.lowercase() == "happy"
        val hasRainbow = context.sceneDescription?.contains("rainbow", ignoreCase = true) == true
        return (hasRainbow || isSunny) && isHappy
    }
    
    private fun isNoisyCar(context: RuleContext, signals: StandardizedSignals): Boolean {
        val total = (signals.signals.internal_camera?.passengers?.adults ?: 0) +
                    (signals.signals.internal_camera?.passengers?.children ?: 0) +
                    (signals.signals.internal_camera?.passengers?.seniors ?: 0)
        val isNoisy = (context.noiseLevel ?: 0.0) > 0.5
        return total >= 3 && isNoisy
    }
    
    private fun isBeachVacation(context: RuleContext, signals: StandardizedSignals): Boolean {
        val isBeachScene = context.sceneDescription?.contains("beach", ignoreCase = true) == true ||
                          context.sceneDescription?.contains("coastal", ignoreCase = true) == true
        val isRelaxed = signals.signals.internal_camera?.mood?.lowercase() == "relaxed"
        return isBeachScene || isRelaxed
    }
    
    private fun isRomanticDate(context: RuleContext, signals: StandardizedSignals): Boolean {
        val isRomantic = signals.signals.internal_camera?.mood?.lowercase() == "romantic"
        val childrenCount = signals.signals.internal_camera?.passengers?.children ?: 0
        if (childrenCount > 0) return false
        val total = (signals.signals.internal_camera?.passengers?.adults ?: 0) +
                    childrenCount +
                    (signals.signals.internal_camera?.passengers?.seniors ?: 0)
        val isCouple = total == 2
        return isRomantic || (isCouple && context.hour >= 18)
    }
    
    private fun isRainyNight(context: RuleContext, signals: StandardizedSignals): Boolean {
        val isRainy = context.weather?.lowercase()?.contains("rain") == true ||
                     signals.signals.environment?.weather?.lowercase()?.contains("rain") == true
        return isRainy && (context.hour >= 18 || context.hour < 6)
    }
    
    private fun isSunnyMorning(context: RuleContext, signals: StandardizedSignals): Boolean {
        val isSunny = signals.signals.environment?.weather?.lowercase()?.contains("sunny") == true ||
                       signals.signals.environment?.weather?.lowercase()?.contains("clear") == true
        return context.hour in 6..9 && isSunny
    }
    
    private fun isSunnyDay(context: RuleContext, signals: StandardizedSignals): Boolean {
        val isSunny = signals.signals.environment?.weather?.lowercase()?.contains("sunny") == true ||
                       signals.signals.environment?.weather?.lowercase()?.contains("clear") == true
        return isSunny
    }
    
    private fun isRainyDay(context: RuleContext, signals: StandardizedSignals): Boolean {
        return signals.signals.environment?.weather?.lowercase()?.contains("rain") == true
    }
    
    private fun isAfternoonDrive(context: RuleContext): Boolean {
        return context.hour in 14..18
    }
    
    private fun isEveningCommute(context: RuleContext): Boolean {
        return context.hour in 17..20
    }
    
    private fun isSoloDrive(context: RuleContext, signals: StandardizedSignals): Boolean {
        val total = (signals.signals.internal_camera?.passengers?.adults ?: 0) +
                    (signals.signals.internal_camera?.passengers?.children ?: 0) +
                    (signals.signals.internal_camera?.passengers?.seniors ?: 0)
        return total <= 1
    }
    
    private fun isFatigueAlert(context: RuleContext, signals: StandardizedSignals): Boolean {
        val mood = signals.signals.internal_camera?.mood?.lowercase()
        return mood == "tired" || mood == "fatigue"
    }
    
    private fun isHappyMood(context: RuleContext, signals: StandardizedSignals): Boolean {
        val mood = signals.signals.internal_camera?.mood?.lowercase()
        return mood == "happy" || mood == "excited"
    }
    
    private fun hasChildren(context: RuleContext, signals: StandardizedSignals): Boolean {
        return context.hasChildren || (signals.signals.internal_camera?.passengers?.children ?: 0) > 0
    }
    
    private fun hasMultiplePassengers(context: RuleContext, signals: StandardizedSignals): Boolean {
        val total = (signals.signals.internal_camera?.passengers?.adults ?: 0) +
                    (signals.signals.internal_camera?.passengers?.children ?: 0) +
                    (signals.signals.internal_camera?.passengers?.seniors ?: 0)
        return total >= 2
    }
    
    private fun getRuleNameForTemplate(templateId: String): String {
        return when (templateId) {
            "TPL_001" -> "早晨通勤"
            "TPL_002" -> "深夜驾驶"
            "TPL_003" -> "朋友出游"
            "TPL_004" -> "疲劳提醒"
            "TPL_005" -> "雨夜行车"
            "TPL_006" -> "家庭出行"
            "TPL_010" -> "阳光早晨"
            "TPL_021" -> "晴天驾驶"
            "TPL_022" -> "雨天驾驶"
            "TPL_012" -> "下午兜风"
            "TPL_013" -> "儿童模式"
            "TPL_014" -> "傍晚通勤"
            "TPL_031" -> "独自驾驶"
            "TPL_032" -> "情侣约会"
            "TPL_041" -> "派对模式"
            "TPL_047" -> "海边度假"
            "TPL_058" -> "开心时刻"
            else -> "未知场景"
        }
    }
    
    data class RuleContext(
        val hour: Int,
        val dayOfWeek: Int,
        val speed: Double?,
        val passengerCount: Int?,
        val weather: String?,
        val mood: String?,
        val hasChildren: Boolean,
        val hasSeniors: Boolean,
        val timeOfDay: Double?,
        val dateType: String?,
        val sceneDescription: String?,
        val noiseLevel: Double?
    )
}
