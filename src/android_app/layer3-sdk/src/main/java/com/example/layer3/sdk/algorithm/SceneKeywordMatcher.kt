package com.example.layer3.sdk.algorithm

import com.example.layer3.api.model.SceneDescriptor
import com.example.layer3.sdk.data.SceneTemplate
import com.example.layer3.sdk.util.Logger

class SceneKeywordMatcher {
    private val keywordWeights = mutableMapOf<String, Double>()

    fun matchScene(
        templates: List<SceneTemplate>,
        scene: SceneDescriptor
    ): List<Pair<SceneTemplate, Double>> {
        val sceneKeywords = extractSceneKeywords(scene)
        
        return templates.map { template ->
            val score = calculateMatchScore(template, sceneKeywords)
            template to score
        }.filter { it.second > MIN_MATCH_SCORE }
          .sortedByDescending { it.second }
    }

    fun matchByKeywords(
        templates: List<SceneTemplate>,
        keywords: List<String>
    ): List<Pair<SceneTemplate, Double>> {
        return templates.map { template ->
            val score = calculateKeywordMatchScore(template, keywords)
            template to score
        }.filter { it.second > MIN_MATCH_SCORE }
          .sortedByDescending { it.second }
    }

    private fun extractSceneKeywords(scene: SceneDescriptor): List<String> {
        val keywords = mutableListOf<String>()
        
        scene.sceneName?.let { keywords.addAll(extractKeywords(it)) }
        scene.sceneNarrative?.let { keywords.addAll(extractKeywords(it)) }
        scene.intent.atmosphere.let { keywords.addAll(extractKeywords(it)) }
        
        scene.hints?.music?.genres?.let { keywords.addAll(it) }
        scene.hints?.music?.artists?.let { keywords.addAll(it) }
        scene.hints?.lighting?.colorTheme?.let { keywords.addAll(extractKeywords(it)) }
        
        return keywords.distinct()
    }

    private fun extractKeywords(text: String): List<String> {
        return text.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length >= 2 }
            .filterNot { STOP_WORDS.contains(it) }
    }

    private fun calculateMatchScore(
        template: SceneTemplate,
        sceneKeywords: List<String>
    ): Double {
        if (sceneKeywords.isEmpty()) return 0.0
        
        var matchCount = 0
        var totalWeight = 0.0
        
        for (keyword in sceneKeywords) {
            val weight = keywordWeights.getOrDefault(keyword, 1.0)
            totalWeight += weight
            
            if (template.keywords.any { it.equals(keyword, ignoreCase = true) }) {
                matchCount++
            } else if (template.templateName.lowercase().contains(keyword)) {
                matchCount++
            } else if (template.description?.lowercase()?.contains(keyword) == true) {
                matchCount += 0.5.toInt()
            }
        }
        
        return if (totalWeight > 0) matchCount / sceneKeywords.size.toDouble() else 0.0
    }

    private fun calculateKeywordMatchScore(
        template: SceneTemplate,
        keywords: List<String>
    ): Double {
        if (keywords.isEmpty()) return 0.0
        
        var matchCount = 0
        for (keyword in keywords) {
            if (template.keywords.any { it.equals(keyword, ignoreCase = true) }) {
                matchCount++
            } else if (template.templateName.lowercase().contains(keyword)) {
                matchCount += 0.8
            }
        }
        
        return matchCount / keywords.size
    }

    fun setKeywordWeight(keyword: String, weight: Double) {
        keywordWeights[keyword.lowercase()] = weight
    }

    companion object {
        private const val MIN_MATCH_SCORE = 0.1
        private val STOP_WORDS = setOf(
            "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for",
            "of", "with", "by", "from", "is", "are", "was", "were", "be", "been",
            "being", "have", "has", "had", "do", "does", "did", "will", "would",
            "could", "should", "may", "might", "must", "shall", "can"
        )
    }
}
