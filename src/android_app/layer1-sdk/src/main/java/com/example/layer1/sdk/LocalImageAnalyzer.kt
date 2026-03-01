package com.example.layer1.sdk

import android.graphics.Bitmap
import android.util.Log
import androidx.palette.graphics.Palette
import java.util.concurrent.atomic.AtomicLong

class LocalImageAnalyzer {

    data class AnalysisResult(
        val brightness: Double,
        val primaryColor: String,
        val secondaryColor: String,
        val isValid: Boolean = true,
        val timestamp: Long = System.currentTimeMillis()
    )

    fun analyze(bitmap: Bitmap): AnalysisResult {
        val startTime = System.currentTimeMillis()
        
        var totalLum = 0.0
        val width = bitmap.width
        val height = bitmap.height
        val step = 4 
        var pixelCount = 0

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        for (i in pixels.indices step step) {
            val color = pixels[i]
            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF
            
            val lum = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
            totalLum += lum
            pixelCount++
        }

        val avgBrightness = if (pixelCount > 0) totalLum / pixelCount else 0.0

        var primaryHex = "#000000"
        var secondaryHex = "#FFFFFF"

        try {
            val palette = Palette.from(bitmap).maximumColorCount(16).generate()
            
            val dominant = palette.dominantSwatch
            val vibrant = palette.vibrantSwatch
            val muted = palette.mutedSwatch

            val primarySwatch = dominant ?: vibrant ?: muted
            
            val swatches = palette.swatches.sortedByDescending { it.population }
            val secondarySwatch = swatches.firstOrNull { it != primarySwatch } ?: swatches.lastOrNull()

            if (primarySwatch != null) {
                primaryHex = String.format("#%06X", (0xFFFFFF and primarySwatch.rgb))
            }
            if (secondarySwatch != null) {
                secondaryHex = String.format("#%06X", (0xFFFFFF and secondarySwatch.rgb))
            }

        } catch (e: Exception) {
            Log.e("LocalImageAnalyzer", "Color extraction failed: ${e.message}")
        }

        val isValid = avgBrightness in 0.0..1.0

        val processTime = System.currentTimeMillis() - startTime
        Log.d("LocalImageAnalyzer", "Analysis finished in ${processTime}ms. B: $avgBrightness, P: $primaryHex")

        return AnalysisResult(
            brightness = (avgBrightness * 100).toInt() / 100.0,
            primaryColor = primaryHex,
            secondaryColor = secondaryHex,
            isValid = isValid
        )
    }
}
