package com.music.semantic.rules

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
    
    fun matchTemplate(signals: StandardizedSignals): RuleMatchResult {
        val context = buildRuleContext(signals)
        val candidates = mutableListOf<Pair<String, Double>>()
        
        signals.signals.vehicle?.let { vehicle ->
            if (isMorningCommute(context)) {
                candidates.add("TPL_001" to 80.0)
            }
            if (isNightDrive(context)) {
                candidates.add("TPL_002" to 75.0)
            }
        }
        
        signals.signals.environment?.let { env ->
            if (isRainyNight(context, env)) {
                candidates.add("TPL_005" to 90.0)
            }
            if (isSunnyMorning(context, env)) {
                candidates.add("TPL_010" to 85.0)
            }
        }
        
        signals.signals.internal_camera?.let { camera ->
            if (isFatigueAlert(camera.mood)) {
                candidates.add("TPL_004" to 95.0)
            }
            if (isHappyMood(camera.mood)) {
                candidates.add("TPL_058" to 70.0)
            }
        }
        
        signals.signals.internal_camera?.passengers?.let { passengers ->
            if (hasChildren(passengers.children)) {
                candidates.add("TPL_006" to 85.0)
            }
            if (hasMultiplePassengers(passengers.adults, passengers.children, passengers.seniors)) {
                candidates.add("TPL_003" to 75.0)
            }
        }
        
        if (candidates.isEmpty()) {
            return RuleMatchResult(
                matched = false,
                templateId = "TPL_020",
                score = 0.0,
                ruleName = "default"
            )
        }
        
        val best = candidates.maxByOrNull { it.second }
        return RuleMatchResult(
            matched = true,
            templateId = best?.first,
            score = best?.second ?: 0.0,
            ruleName = getRuleNameForTemplate(best?.first)
        )
    }
    
    private fun buildRuleContext(signals: StandardizedSignals): RuleContext {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        
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
            dateType = signals.signals.environment?.date_type
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
    
    private fun isRainyNight(context: RuleContext, env: com.music.core.api.models.Environment?): Boolean {
        val isNight = context.hour >= 18 || context.hour < 6
        val isRainy = env?.weather in listOf("rain", "storm")
        return isNight && isRainy
    }
    
    private fun isSunnyMorning(context: RuleContext, env: com.music.core.api.models.Environment?): Boolean {
        return context.hour in 6..9 && env?.weather in listOf("sunny", "clear")
    }
    
    private fun isFatigueAlert(mood: String?): Boolean {
        return mood == "tired"
    }
    
    private fun isHappyMood(mood: String?): Boolean {
        return mood == "happy" || mood == "excited"
    }
    
    private fun hasChildren(childrenCount: Int?): Boolean {
        return (childrenCount ?: 0) > 0
    }
    
    private fun hasMultiplePassengers(adults: Int?, children: Int?, seniors: Int?): Boolean {
        val total = (adults ?: 0) + (children ?: 0) + (seniors ?: 0)
        return total >= 2
    }
    
    private fun getRuleNameForTemplate(templateId: String?): String {
        return when (templateId) {
            "TPL_001" -> "morning_commute"
            "TPL_002" -> "night_drive"
            "TPL_003" -> "road_trip"
            "TPL_004" -> "fatigue_alert"
            "TPL_005" -> "rainy_night"
            "TPL_006" -> "family_outing"
            "TPL_010" -> "sunny_morning"
            "TPL_058" -> "happy_mood"
            else -> "unknown"
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
        val dateType: String?
    )
}
