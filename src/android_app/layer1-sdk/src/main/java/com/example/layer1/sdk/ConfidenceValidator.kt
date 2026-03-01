package com.example.layer1.sdk

import java.util.Locale

class ConfidenceValidator(private var threshold: Double = 0.6) {

    data class ValidationResult(
        val originalConfidence: Double,
        val isValid: Boolean,
        val formattedConfidence: String,
        val message: String,
        val suggestion: String
    )

    fun setThreshold(newThreshold: Double) {
        if (newThreshold in 0.0..1.0) {
            this.threshold = newThreshold
        }
    }

    fun getThreshold(): Double = threshold

    fun validate(confidence: Double): ValidationResult {
        if (confidence < 0.0 || confidence > 1.0) {
            return ValidationResult(
                originalConfidence = confidence,
                isValid = false,
                formattedConfidence = "N/A",
                message = "Invalid confidence value: $confidence",
                suggestion = "Check AI model output or data parsing logic."
            )
        }

        val formatted = String.format(Locale.US, "%.2f%%", confidence * 100)
        val isValid = confidence >= threshold

        return if (isValid) {
            ValidationResult(
                originalConfidence = confidence,
                isValid = true,
                formattedConfidence = formatted,
                message = "Confidence acceptable.",
                suggestion = "Proceed with data."
            )
        } else {
            ValidationResult(
                originalConfidence = confidence,
                isValid = false,
                formattedConfidence = formatted,
                message = "Low confidence detected.",
                suggestion = "Discard result or mark as uncertain. Consider improving lighting or camera angle."
            )
        }
    }
}
